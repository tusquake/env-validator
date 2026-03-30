package io.github.tusquake.envvalidator.core;

import io.github.tusquake.envvalidator.annotation.ValidateEnv;
import io.github.tusquake.envvalidator.exception.MissingEnvException;
import io.github.tusquake.envvalidator.util.EnvReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Core engine to validate annotations against actual environment values.
 */
public class ValidationEngine {
    private static final Logger log = LoggerFactory.getLogger(ValidationEngine.class);
    private final EnvReader envReader;

    public ValidationEngine(EnvReader envReader) {
        this.envReader = envReader;
    }

    /**
     * Validates an object based on its class-level and field-level @ValidateEnv
     * annotations.
     * @return List of error messages, or empty list if valid.
     */
    public List<String> validate(Object target) {
        Class<?> clazz = target.getClass();
        
        // Handle CGLIB proxies
        if (clazz.getName().contains("$$")) {
            clazz = clazz.getSuperclass();
        }

        List<String> errors = new ArrayList<>();

        // 1. Class-level validation (Repeatable)
        ValidateEnv[] classAnnotations = clazz.getAnnotationsByType(ValidateEnv.class);
        for (ValidateEnv classAnnotation : classAnnotations) {
            errors.addAll(performValidation(classAnnotation));
        }

        // 2. Field-level validation (Repeatable)
        for (Field field : clazz.getDeclaredFields()) {
            ValidateEnv[] fieldAnnotations = field.getAnnotationsByType(ValidateEnv.class);
            for (ValidateEnv annotation : fieldAnnotations) {
                errors.addAll(performFieldValidation(field, annotation, target));
            }
        }

        return errors;
    }

    private List<String> performValidation(ValidateEnv annotation) {
        List<String> errors = new ArrayList<>();
        for (String var : annotation.value()) {
            String value = envReader.read(var);
            validateSingle(var, value, annotation, errors);
        }
        return errors;
    }

    private List<String> performFieldValidation(Field field, ValidateEnv annotation, Object target) {
        List<String> errors = new ArrayList<>();
        // For fields, 'value' attribute might be empty, so we use field name if it is
        String[] vars = annotation.value();
        if (vars.length == 0) {
            vars = new String[] { field.getName() };
        }

        for (String var : vars) {
            String value = envReader.read(var);
            validateSingle(var, value, annotation, errors);
        }
        return errors;
    }

    private void validateSingle(String var, String value, ValidateEnv annotation, List<String> errors) {
        // 1. Profile Check
        if (annotation.profiles().length > 0) {
            boolean profileActive = false;
            String[] activeProfiles = envReader.getActiveProfiles();
            for (String profile : annotation.profiles()) {
                for (String active : activeProfiles) {
                    if (profile.equalsIgnoreCase(active)) {
                        profileActive = true;
                        break;
                    }
                }
                if (profileActive) break;
            }
            if (!profileActive) {
                log.debug("Skipping validation for {} as profiles do not match", var);
                return;
            }
        }

        // 2. Handle missing value
        if (value == null || value.isBlank()) {
            if (!annotation.defaultValue().isEmpty()) {
                log.info("Using default value for {}: {}", var, annotation.masked() ? "****" : annotation.defaultValue());
                value = annotation.defaultValue();
            } else if (annotation.required()) {
                errors.add(var + " (Missing)");
                return;
            } else {
                return; // Not required and no default, skip further validation
            }
        }

        // 3. Type Validation
        if (annotation.type() != String.class) {
            try {
                validateType(value, annotation.type());
            } catch (Exception e) {
                log.error("Value for {} is not of type {}: {}", var, annotation.type().getSimpleName(), mask(value, annotation.masked()));
                errors.add(var + " (Type Mismatch: Expected " + annotation.type().getSimpleName() + ")");
            }
        }

        // 4. Regex Pattern Validation
        if (!annotation.pattern().isEmpty()) {
            if (!Pattern.matches(annotation.pattern(), value)) {
                log.error("Value for {} does not match pattern: {}", var, annotation.pattern());
                errors.add(var + " (Regex Mismatch)");
            }
        }

        // 5. Custom Validators
        for (Class<? extends CustomValidator> validatorClass : annotation.validators()) {
            try {
                CustomValidator validator = validatorClass.getDeclaredConstructor().newInstance();
                if (!validator.isValid(value)) {
                    errors.add(validator.errorMessage(var, mask(value, annotation.masked())));
                }
            } catch (Exception e) {
                log.error("Error invoking custom validator {}: {}", validatorClass.getName(), e.getMessage());
                errors.add(var + " (Validator Error: " + validatorClass.getSimpleName() + ")");
            }
        }

        // 6. SpEL Expression Validation
        if (!annotation.expression().isEmpty()) {
            try {
                if (!evaluateExpression(annotation.expression())) {
                    errors.add(var + " (Expression Validation Failed: " + annotation.expression() + ")");
                }
            } catch (Exception e) {
                log.error("Error evaluating expression for {}: {}", var, e.getMessage());
                errors.add(var + " (Expression Error)");
            }
        }
    }

    private void validateType(String value, Class<?> type) {
        if (type == Integer.class || type == int.class) {
            Integer.parseInt(value);
        } else if (type == Long.class || type == long.class) {
            Long.parseLong(value);
        } else if (type == Double.class || type == double.class) {
            Double.parseDouble(value);
        } else if (type == Boolean.class || type == boolean.class) {
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException("Not a boolean");
            }
        } else if (type.isEnum()) {
            boolean found = false;
            for (Object enumConstant : type.getEnumConstants()) {
                if (enumConstant.toString().equalsIgnoreCase(value)) {
                    found = true;
                    break;
                }
            }
            if (!found) throw new IllegalArgumentException("Not a valid enum constant");
        }
    }

    private boolean evaluateExpression(String expression) {
        try {
            org.springframework.expression.ExpressionParser parser = new org.springframework.expression.spel.standard.SpelExpressionParser();
            // Simple approach: resolve ${var} placeholders before evaluation
            String resolvedExpr = expression;
            java.util.regex.Matcher matcher = Pattern.compile("\\$\\{([^}]+)\\}").matcher(expression);
            while (matcher.find()) {
                String varName = matcher.group(1);
                String varValue = envReader.read(varName);
                if (varValue == null) varValue = "";
                resolvedExpr = resolvedExpr.replace("${" + varName + "}", varValue);
            }
            return Boolean.TRUE.equals(parser.parseExpression(resolvedExpr).getValue(Boolean.class));
        } catch (Exception e) {
            return false;
        }
    }

    private String mask(String value, boolean masked) {
        return masked ? "****" : value;
    }
}
