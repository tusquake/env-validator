package io.github.tusquake.envvalidator.spring;

import io.github.tusquake.envvalidator.core.ValidationEngine;
import io.github.tusquake.envvalidator.util.EnvReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration class to register library beans.
 */
@Configuration
public class EnvValidationConfig {

    @Bean
    public EnvReader envReader(Environment environment) {
        return new EnvReader(environment);
    }

    @Bean
    public ValidationEngine validationEngine(EnvReader envReader) {
        return new ValidationEngine(envReader);
    }

    @Bean
    public EnvValidationRunner envValidationRunner(ValidationEngine validationEngine) {
        return new EnvValidationRunner(validationEngine);
    }
}
