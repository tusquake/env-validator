package io.github.tusquake.envvalidator;

import io.github.tusquake.envvalidator.annotation.ValidateEnv;
import io.github.tusquake.envvalidator.core.ValidationEngine;
import io.github.tusquake.envvalidator.exception.MissingEnvException;
import io.github.tusquake.envvalidator.util.EnvReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class EnvValidatorTest {

    private EnvReader envReader;
    private ValidationEngine validationEngine;
    private Environment environment;

    @BeforeEach
    void setUp() {
        environment = Mockito.mock(Environment.class);
        envReader = new EnvReader(environment);
        validationEngine = new ValidationEngine(envReader);
    }

    @ValidateEnv({"DB_URL", "API_KEY"})
    static class TestConfig {
    }

    @Test
    void shouldPassWhenAllEnvVarsExist() {
        when(environment.getProperty("DB_URL")).thenReturn("jdbc:mysql://localhost:3306/db");
        when(environment.getProperty("API_KEY")).thenReturn("secret-key");

        List<String> errors = validationEngine.validate(new TestConfig());
        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldFailWhenEnvVarIsMissing() {
        when(environment.getProperty("DB_URL")).thenReturn("jdbc:mysql://localhost:3306/db");
        when(environment.getProperty("API_KEY")).thenReturn(null);

        List<String> errors = validationEngine.validate(new TestConfig());
        
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("API_KEY"));
        assertFalse(errors.contains("DB_URL"));
    }

    @ValidateEnv(value = "EMAIL", pattern = "^[A-Za-z0-9+_.-]+@(.+)$")
    static class RegexConfig {
    }

    @Test
    void shouldFailWhenRegexMismatch() {
        when(environment.getProperty("EMAIL")).thenReturn("invalid-email");

        List<String> errors = validationEngine.validate(new RegexConfig());
        
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("EMAIL (Regex Mismatch)"));
    }

    static class FieldLevelConfig {
        @ValidateEnv(defaultValue = "localhost")
        private String host;

        @ValidateEnv(required = true)
        private String port;
    }

    @Test
    void shouldValidateFieldLevelAnnotations() {
        when(environment.getProperty("host")).thenReturn(null);
        when(environment.getProperty("port")).thenReturn(null);

        List<String> errors = validationEngine.validate(new FieldLevelConfig());
        
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("port"));
        assertFalse(errors.contains("host")); // host has default value
    }

    @ValidateEnv(value = "PORT", pattern = "^[0-9]+$")
    @ValidateEnv(value = "PORT", defaultValue = "8080")
    static class RepeatableConfig {
    }

    @Test
    void shouldHandleRepeatableAnnotations() {
        // Mock situation where PORT is invalid (not numeric)
        when(environment.getProperty("PORT")).thenReturn("abc");

        List<String> errors = validationEngine.validate(new RepeatableConfig());
        
        // One error from the first annotation (regex)
        assertEquals(1, errors.size());
        assertTrue(errors.contains("PORT (Regex Mismatch)"));
    }
}
