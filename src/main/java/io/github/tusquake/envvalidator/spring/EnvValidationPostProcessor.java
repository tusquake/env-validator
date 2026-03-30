package io.github.tusquake.envvalidator.spring;

import io.github.tusquake.envvalidator.annotation.ValidateEnv;
import io.github.tusquake.envvalidator.core.ValidationEngine;
import io.github.tusquake.envvalidator.util.EnvReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * BeanPostProcessor that handles @ValidateEnv on fields for auto-injection and validation.
 */
public class EnvValidationPostProcessor implements BeanPostProcessor, PriorityOrdered {
    private static final Logger log = LoggerFactory.getLogger(EnvValidationPostProcessor.class);
    private final ValidationEngine validationEngine;
    private final EnvReader envReader;

    public EnvValidationPostProcessor(ValidationEngine validationEngine, EnvReader envReader) {
        this.validationEngine = validationEngine;
        this.envReader = envReader;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        
        // Handle CGLIB proxies
        if (clazz.getName().contains("$$")) {
            clazz = clazz.getSuperclass();
        }

        for (Field field : clazz.getDeclaredFields()) {
            ValidateEnv[] annotations = field.getAnnotationsByType(ValidateEnv.class);
            for (ValidateEnv annotation : annotations) {
                processField(bean, field, annotation);
            }
        }
        return bean;
    }

    private void processField(Object bean, Field field, ValidateEnv annotation) {
        String[] vars = annotation.value();
        if (vars.length == 0) {
            vars = new String[]{field.getName()};
        }

        // 1. Determine value to inject
        String varToInject = vars[0]; // Take the first one for injection
        String valueToInject = envReader.read(varToInject);

        if (valueToInject == null && !annotation.defaultValue().isEmpty()) {
            valueToInject = annotation.defaultValue();
        }

        if (valueToInject != null) {
            injectValue(bean, field, valueToInject);
        }

        // 2. Validate the whole bean to ensure consistency
        List<String> errors = validationEngine.validate(bean);
        if (!errors.isEmpty()) {
            throw new RuntimeException("Environment validation failed for bean [" + bean.getClass().getSimpleName() + "]: " + errors);
        }
    }

    private void injectValue(Object bean, Field field, String value) {
        try {
            ReflectionUtils.makeAccessible(field);
            Object convertedValue = convert(value, field.getType());
            field.set(bean, convertedValue);
        } catch (Exception e) {
            log.error("Failed to inject value into field {}: {}", field.getName(), e.getMessage());
        }
    }

    private Object convert(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == Integer.class || type == int.class) return Integer.parseInt(value);
        if (type == Long.class || type == long.class) return Long.parseLong(value);
        if (type == Double.class || type == double.class) return Double.parseDouble(value);
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(value);
        if (type.isEnum()) {
            for (Object constant : type.getEnumConstants()) {
                if (constant.toString().equalsIgnoreCase(value)) return constant;
            }
        }
        return value;
    }

    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE; // Run early
    }
}
