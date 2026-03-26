package io.github.tusquake.envvalidator.exception;

import java.util.List;

/**
 * Exception thrown when required environment variables are missing or invalid.
 */
public class MissingEnvException extends RuntimeException {

    public MissingEnvException(String message) {
        super(message);
    }

    public MissingEnvException(List<String> missingVars) {
        super(formatMissingMsg(missingVars));
    }

    private static String formatMissingMsg(List<String> missingVars) {
        StringBuilder sb = new StringBuilder("\nMissing required environment variables:\n");
        for (String var : missingVars) {
            sb.append("- ").append(var).append("\n");
        }
        return sb.toString();
    }
}
