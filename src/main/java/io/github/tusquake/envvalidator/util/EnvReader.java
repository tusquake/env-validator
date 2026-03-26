package io.github.tusquake.envvalidator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Utility to read environment variables and application properties.
 */
public class EnvReader {
    private static final Logger log = LoggerFactory.getLogger(EnvReader.class);
    private final Environment environment;

    public EnvReader(Environment environment) {
        this.environment = environment;
    }

    /**
     * Resolves a value from the environment.
     * Supports ${var:default} syntax natively via Spring's Environment.
     */
    public String read(String key) {
        try {
            return environment.getProperty(key);
        } catch (Exception e) {
            log.warn("Error reading property: {}", key);
            return null;
        }
    }

    /**
     * Checks if a key exists in the environment.
     */
    public boolean exists(String key) {
        return environment.containsProperty(key);
    }
}
