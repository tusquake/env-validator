package io.github.tusquake.envvalidator.core;

/**
 * Interface for custom environment variable validation logic.
 */
public interface CustomValidator {
    /**
     * Performs validation on the provided value.
     * @param value The value to validate.
     * @return true if valid, false otherwise.
     */
    boolean isValid(String value);

    /**
     * Default error message if validation fails.
     * @param varName Name of the variable being validated.
     * @param value The invalid value (may be null).
     * @return Formatted error message.
     */
    String errorMessage(String varName, String value);
}
