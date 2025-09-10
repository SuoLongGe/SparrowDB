package com.sqlcompiler.exception;

import com.sqlcompiler.lexer.Position;

/**
 * 词法分析异常
 */
public class LexicalException extends CompilationException {
    
    public LexicalException(String message, Position position) {
        super(message, position, "词法错误");
    }
}
