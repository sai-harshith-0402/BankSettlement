package com.iispl.exception;

public class AdapterException extends Exception {

    private final String sourceType;
    private final int    rowNumber;

    public AdapterException(String sourceType, int rowNumber, String message) {
        super("Adapter [" + sourceType + "] failed at row " + rowNumber + ": " + message);
        this.sourceType = sourceType;
        this.rowNumber  = rowNumber;
    }

    public AdapterException(String sourceType, int rowNumber, String message, Throwable cause) {
        super("Adapter [" + sourceType + "] failed at row " + rowNumber + ": " + message, cause);
        this.sourceType = sourceType;
        this.rowNumber  = rowNumber;
    }

    public String getSourceType() { return sourceType; }
    public int getRowNumber()     { return rowNumber; }
}