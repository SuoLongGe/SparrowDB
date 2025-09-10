package com.sqlcompiler.exception;

import com.sqlcompiler.lexer.Position;

/**
 * 语义分析异常
 */
public class SemanticException extends CompilationException {
    
    public SemanticException(String message, Position position) {
        super(message, position, "语义错误");
    }
}
