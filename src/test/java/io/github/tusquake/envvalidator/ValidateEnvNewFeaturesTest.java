package io.github.tusquake.envvalidator;

import io.github.tusquake.envvalidator.annotation.ValidateEnv;
import io.github.tusquake.envvalidator.core.CustomValidator;
import io.github.tusquake.envvalidator.core.ValidationEngine;
import io.github.tusquake.envvalidator.util.EnvReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValidateEnvNewFeaturesTest {

    @Mock
    private EnvReader envReader;

    private ValidationEngine validationEngine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validationEngine = new ValidationEngine(envReader);
    }

    @Test
    void testProfileCheck_Skipped() {
        when(envReader.getActiveProfiles()).thenReturn(new String[]{"dev"});
        
        @ValidateEnv(value = "TEST_VAR", profiles = {"prod"})
        class TestBean {}
        
        List<String> errors = validationEngine.validate(new TestBean());
        assertTrue(errors.isEmpty(), "Should skip validation as profile 'prod' is not active");
    }

    @Test
    void testProfileCheck_Active() {
        when(envReader.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(envReader.read("TEST_VAR")).thenReturn(null);
        
        @ValidateEnv(value = "TEST_VAR", profiles = {"prod"}, required = true)
        class TestBean {}
        
        List<String> errors = validationEngine.validate(new TestBean());
        assertFalse(errors.isEmpty(), "Should NOT skip validation as profile 'prod' is active");
    }

    @Test
    void testTypeValidation_Integer_Success() {
        when(envReader.read("PORT")).thenReturn("8080");
        
        @ValidateEnv(value = "PORT", type = Integer.class)
        class TestBean {}
        
        List<String> errors = validationEngine.validate(new TestBean());
        assertTrue(errors.isEmpty());
    }

    @Test
    void testTypeValidation_Integer_Failure() {
        when(envReader.read("PORT")).thenReturn("invalid");
        
        @ValidateEnv(value = "PORT", type = Integer.class)
        class TestBean {}
        
        List<String> errors = validationEngine.validate(new TestBean());
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Type Mismatch"));
    }

    @Test
    void testCustomValidator() {
        when(envReader.read("TEST_VAR")).thenReturn("invalid-value");
        
        @ValidateEnv(value = "TEST_VAR", validators = {TestCustomValidator.class})
        class TestBean {}
        
        List<String> errors = validationEngine.validate(new TestBean());
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Validation Failed: invalid-value"));
    }

    @Test
    void testSpELExpression() {
        when(envReader.read("MIN")).thenReturn("10");
        when(envReader.read("MAX")).thenReturn("20");
        
        @ValidateEnv(value = "MIN", expression = "${MIN} < ${MAX}")
        class TestBean {}
        
        List<String> errors = validationEngine.validate(new TestBean());
        assertTrue(errors.isEmpty(), "Expression 10 < 20 should pass");

        when(envReader.read("MIN")).thenReturn("30");
        errors = validationEngine.validate(new TestBean());
        assertFalse(errors.isEmpty(), "Expression 30 < 20 should fail");
    }

    public static class TestCustomValidator implements CustomValidator {
        @Override
        public boolean isValid(String value) {
            return "valid".equals(value);
        }

        @Override
        public String errorMessage(String varName, String value) {
            return varName + " Validation Failed: " + value;
        }
    }
}
