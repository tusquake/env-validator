package io.github.tusquake.envvalidator.spring;

import io.github.tusquake.envvalidator.annotation.ValidateEnv;
import io.github.tusquake.envvalidator.core.ValidationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * Runner that executes environment validation immediately after application startup.
 */
public class EnvValidationRunner implements ApplicationRunner, ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(EnvValidationRunner.class);
    private final ValidationEngine validationEngine;
    private ApplicationContext applicationContext;

    public EnvValidationRunner(ValidationEngine validationEngine) {
        this.validationEngine = validationEngine;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting environment validation...");

        // Validate all beans annotated with @ValidateEnv at class level
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(ValidateEnv.class);
        
        for (Map.Entry<String, Object> entry : beansWithAnnotation.entrySet()) {
            log.info("Validating bean: {} of type {}", entry.getKey(), entry.getValue().getClass().getName());
            validationEngine.validate(entry.getValue());
        }

        log.info("Environment validation completed successfully.");
    }
}
