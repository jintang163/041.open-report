package com.openreport.scheduler.listener;

public class ReportExecuteException extends RuntimeException {

    private final boolean outputGenerated;

    public ReportExecuteException(String message, boolean outputGenerated) {
        super(message);
        this.outputGenerated = outputGenerated;
    }

    public ReportExecuteException(String message, Throwable cause, boolean outputGenerated) {
        super(message, cause);
        this.outputGenerated = outputGenerated;
    }

    public boolean isOutputGenerated() {
        return outputGenerated;
    }
}
