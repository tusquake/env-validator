package io.github.tusquake.envvalidator;

import io.github.tusquake.envvalidator.annotation.ValidateEnv;
import io.github.tusquake.envvalidator.core.ValidationEngine;
import io.github.tusquake.envvalidator.spring.EnvValidationPostProcessor;
import io.github.tusquake.envvalidator.util.EnvReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnvValidationPostProcessorTest {

    @Mock
    private EnvReader envReader;
    private ValidationEngine validationEngine;
    private EnvValidationPostProcessor postProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validationEngine = new ValidationEngine(envReader);
        postProcessor = new EnvValidationPostProcessor(validationEngine, envReader);
    }

    @Test
    void testAutoInjection() {
        when(envReader.read("SERVER_PORT")).thenReturn("9090");
        
        TestBean bean = new TestBean();
        postProcessor.postProcessBeforeInitialization(bean, "testBean");
        
        assertEquals(9090, bean.port, "Value should be automatically injected and converted to int");
    }

    @Test
    void testAutoInjection_DefaultValue() {
        when(envReader.read("SERVER_PORT")).thenReturn(null);
        
        TestBean bean = new TestBean();
        postProcessor.postProcessBeforeInitialization(bean, "testBean");
        
        assertEquals(8080, bean.port, "Default value should be injected when environment property is missing");
    }

    @Test
    void testValidationFailure_StopsInitialization() {
        when(envReader.read("SERVER_PORT")).thenReturn("invalid");
        
        TestBean bean = new TestBean();
        assertThrows(RuntimeException.class, () -> {
            postProcessor.postProcessBeforeInitialization(bean, "testBean");
        }, "Should throw exception during bean initialization if validation fails");
    }

    static class TestBean {
        @ValidateEnv(value = "SERVER_PORT", defaultValue = "8080", type = Integer.class)
        private int port;
    }
}
