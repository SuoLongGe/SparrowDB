package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * SQL语句基类
 */
public abstract class Statement extends ASTNode {
    
    public Statement(Position position) {
        super(position);
    }
}
