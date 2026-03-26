package io.github.tusquake.envvalidator;

import io.github.tusquake.envvalidator.annotation.ValidateEnv;
import io.github.tusquake.envvalidator.core.ValidationEngine;
import io.github.tusquake.envvalidator.exception.MissingEnvException;
import io.github.tusquake.envvalidator.util.EnvReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

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

        assertDoesNotThrow(() -> validationEngine.validate(new TestConfig()));
    }

    @Test
    void shouldFailWhenEnvVarIsMissing() {
        when(environment.getProperty("DB_URL")).thenReturn("jdbc:mysql://localhost:3306/db");
        when(environment.getProperty("API_KEY")).thenReturn(null);

        MissingEnvException exception = assertThrows(MissingEnvException.class, 
            () -> validationEngine.validate(new TestConfig()));
        
        assertTrue(exception.getMessage().contains("API_KEY"));
        assertFalse(exception.getMessage().contains("DB_URL"));
    }

    @ValidateEnv(value = "EMAIL", pattern = "^[A-Za-z0-9+_.-]+@(.+)$")
    static class RegexConfig {
    }

    @Test
    void shouldFailWhenRegexMismatch() {
        when(environment.getProperty("EMAIL")).thenReturn("invalid-email");

        MissingEnvException exception = assertThrows(MissingEnvException.class, 
            () -> validationEngine.validate(new RegexConfig()));
        
        assertTrue(exception.getMessage().contains("EMAIL (Regex Mismatch)"));
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

        MissingEnvException exception = assertThrows(MissingEnvException.class, 
            () -> validationEngine.validate(new FieldLevelConfig()));
        
        assertTrue(exception.getMessage().contains("port"));
        assertFalse(exception.getMessage().contains("host")); // host has default value
    }
}
