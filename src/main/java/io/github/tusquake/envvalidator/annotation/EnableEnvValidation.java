package io.github.tusquake.envvalidator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import io.github.tusquake.envvalidator.spring.EnvValidationConfig;

/**
 * Enable environment variable validation in a Spring Boot application.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(EnvValidationConfig.class)
public @interface EnableEnvValidation {
}
