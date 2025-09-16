package com.sqlcompiler.exception;

import com.sqlcompiler.lexer.Position;

/**
 * 编译异常基类
 */
public class CompilationException extends Exception {
    private final Position position;
    private final String errorType;
    
    public CompilationException(String message, Position position, String errorType) {
        super(message);
        this.position = position;
        this.errorType = errorType;
    }
    
    public Position getPosition() {
        return position;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    @Override
    public String toString() {
        return String.format("❌ %s\n   位置: 第%d行第%d列\n   错误: %s", 
                           errorType, position.getLine(), position.getColumn(), getMessage());
    }
}
