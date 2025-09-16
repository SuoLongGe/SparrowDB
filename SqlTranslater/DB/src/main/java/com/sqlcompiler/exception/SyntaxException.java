package com.sqlcompiler.exception;

import com.sqlcompiler.lexer.Position;

/**
 * 语法分析异常
 */
public class SyntaxException extends CompilationException {
    
    public SyntaxException(String message, Position position) {
        super(message, position, "语法错误");
    }
    
    public SyntaxException(String message, Position position, String expectedSymbol) {
        super(String.format("意外的符号 '%s'，期望: %s", message, expectedSymbol), position, "语法错误");
    }
}
