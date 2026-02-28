package com.kntrel.mc.state;

public class InvalidStateException extends RuntimeException {

    //FIELDS
    private final String stateAttempt_;

    InvalidStateException(String stateAttempt) {
        this.stateAttempt_ = stateAttempt;
    }

    //GETTERS
    public String getStateStringAttempt() {
        return this.stateAttempt_;
    }
    @Override public String getMessage() {
        return "Invalid BlockState string: '" + this.stateAttempt_ + "'";
    }

}
