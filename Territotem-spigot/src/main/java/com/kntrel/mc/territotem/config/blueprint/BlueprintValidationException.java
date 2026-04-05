package com.kntrel.mc.territotem.config.blueprint;

public class BlueprintValidationException extends RuntimeException {

    public BlueprintValidationException(String message) {
        super(message);
    }

    public BlueprintValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
