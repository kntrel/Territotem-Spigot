package com.kntrel.mc.territotem.totem.deeds;

class DeedsDeTranspilingException extends Exception {

    //FIELDS
    private final String src_;
    private final int line_;
    private final DeedsDeTranspilingError cause_;


    //CONSTRUCTOR
    DeedsDeTranspilingException(String src, int lineNumber, DeedsDeTranspilingError cause) {
        this.src_ = src;
        this.line_ = lineNumber;
        this.cause_ = cause;
    }


    //GETTERS
    String getSource() {
        return this.src_;
    }
    int getLineNumber() {
        return this.line_;
    }
    DeedsDeTranspilingError getErrorCause() {
        return this.cause_;
    }
    String getLine() {
        return this.src_.split("\n")[this.line_];
    }
}
