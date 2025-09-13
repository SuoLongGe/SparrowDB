package com.database.engine;

import java.util.List;
import java.util.Map;

/**
 * 执行结果类
 */
public class ExecutionResult {
    private final boolean success;
    private final String message;
    private final List<Map<String, Object>> data;
    private final List<ExecutionResult> batchResults;
    
    public ExecutionResult(boolean success, String message, List<Map<String, Object>> data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.batchResults = null;
    }
    
    public ExecutionResult(boolean success, String message, List<ExecutionResult> batchResults, boolean isBatch) {
        this.success = success;
        this.message = message;
        this.data = null;
        this.batchResults = batchResults;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public List<Map<String, Object>> getData() {
        return data;
    }
    
    public List<ExecutionResult> getBatchResults() {
        return batchResults;
    }
    
    @Override
    public String toString() {
        if (success) {
            return "执行成功: " + message + (data != null ? " (返回 " + data.size() + " 行)" : "");
        } else {
            return "执行失败: " + message;
        }
    }
}