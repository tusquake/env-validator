package io.github.tusquake.envvalidator.spring;

import io.github.tusquake.envvalidator.annotation.ValidateEnv;
import io.github.tusquake.envvalidator.core.ValidationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.github.tusquake.envvalidator.exception.MissingEnvException;

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

        List<String> allErrors = new ArrayList<>();

        // Validate all beans annotated with @ValidateEnv at class level
        // We use GetBeansWithAnnotation because we want both single and repeatable containers
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(ValidateEnv.class);
        // Also look for the container annotation if multiple @ValidateEnv are present
        Map<String, Object> beansWithAnnotationList = applicationContext.getBeansWithAnnotation(ValidateEnv.List.class);

        // Put them all together
        beansWithAnnotation.putAll(beansWithAnnotationList);
        
        for (Map.Entry<String, Object> entry : beansWithAnnotation.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            
            log.info("Validating bean: {}", beanName);
            List<String> errors = validationEngine.validate(bean);
            
            for (String error : errors) {
                allErrors.add("[" + beanName + "] " + error);
            }
        }

        if (!allErrors.isEmpty()) {
            throw new MissingEnvException(allErrors);
        }

        log.info("Environment validation completed successfully.");
    }
}
