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
        if (value == null || value.isBlank()) {
            if (!annotation.defaultValue().isEmpty()) {
                log.info("Using default value for {}: {}", var, annotation.defaultValue());
            } else if (annotation.required()) {
                errors.add(var);
            }
        } else if (!annotation.pattern().isEmpty()) {
            if (!Pattern.matches(annotation.pattern(), value)) {
                log.error("Value for {} does not match pattern: {}", var, annotation.pattern());
                errors.add(var + " (Regex Mismatch)");
            }
        }
    }
}
