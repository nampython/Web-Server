package org.nampython.core;

public class CannotParseExpressionException extends RuntimeException {

    public CannotParseExpressionException(String message) {
        super(message);
    }

    public CannotParseExpressionException(String message, Throwable cause) {
        super(message, cause);
    }
}
