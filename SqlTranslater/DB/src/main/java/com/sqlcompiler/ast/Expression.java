package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * 表达式基类
 */
public abstract class Expression extends ASTNode {
    
    public Expression(Position position) {
        super(position);
    }
}
