package com.iispl.exception;

public class AdapterException extends RuntimeException {

    private final String sourceSystemType;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================

    public AdapterException(String message) {
        super(message);
        this.sourceSystemType = null;
    }

    public AdapterException(String message, String sourceSystemType) {
        super(message);
        this.sourceSystemType = sourceSystemType;
    }

    public AdapterException(String message, Throwable cause) {
        super(message, cause);
        this.sourceSystemType = null;
    }

    public AdapterException(String message, String sourceSystemType, Throwable cause) {
        super(message + " [SourceSystem: " + sourceSystemType + "]", cause);
        this.sourceSystemType = sourceSystemType;
    }
    

    // =========================================================================
    // GETTER
    // =========================================================================

    public String getSourceSystemType() {
        return sourceSystemType;
    }
}