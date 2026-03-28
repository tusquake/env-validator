package io.github.tusquake.envvalidator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to validate environment variables or application properties.
 * Can be applied to classes (for global validation) or specific fields.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
@Repeatable(ValidateEnv.List.class)
public @interface ValidateEnv {
    /**
     * Names of the environment variables or properties to validate.
     */
    String[] value() default {};

    /**
     * Regex pattern to validate the value against.
     */
    String pattern() default "";

    /**
     * Default value if the variable is missing.
     */
    String defaultValue() default "";

    /**
     * Whether the validation is mandatory.
     */
    boolean required() default true;

    /**
     * Container for repeatable @ValidateEnv annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    @interface List {
        ValidateEnv[] value();
    }
}
