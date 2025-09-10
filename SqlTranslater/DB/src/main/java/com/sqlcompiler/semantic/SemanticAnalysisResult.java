package com.sqlcompiler.semantic;

import java.util.List;

/**
 * 语义分析结果
 */
public class SemanticAnalysisResult {
    private final List<String> errors;
    private final List<String> warnings;
    
    public SemanticAnalysisResult(List<String> errors, List<String> warnings) {
        this.errors = errors;
        this.warnings = warnings;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public boolean isSuccess() {
        return !hasErrors();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if (isSuccess()) {
            sb.append("语义分析成功\n");
        } else {
            sb.append("语义分析失败\n");
        }
        
        if (hasErrors()) {
            sb.append("错误:\n");
            for (String error : errors) {
                sb.append("  ").append(error).append("\n");
            }
        }
        
        if (hasWarnings()) {
            sb.append("警告:\n");
            for (String warning : warnings) {
                sb.append("  ").append(warning).append("\n");
            }
        }
        
        return sb.toString();
    }
}
