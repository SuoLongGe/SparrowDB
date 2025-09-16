package com.sqlcompiler.execution;

import java.util.List;

/**
 * Execution result class
 * Represents the result of executing an SQL statement
 */
public class ExecutionResult {
    private final boolean success;
    private final String message;
    private final List<String> columns;
    private final List<List<Object>> data;
    
    public ExecutionResult(boolean success, String message, List<String> columns, List<List<Object>> data) {
        this.success = success;
        this.message = message;
        this.columns = columns;
        this.data = data;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public List<String> getColumns() {
        return columns;
    }
    
    public List<List<Object>> getData() {
        return data;
    }
    
    @Override
    public String toString() {
        return String.format("ExecutionResult{success=%s, message='%s', columns=%d, rows=%d}", 
                           success, message, 
                           columns != null ? columns.size() : 0,
                           data != null ? data.size() : 0);
    }
}
