package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * 标识符表达式
 */
public class IdentifierExpression extends Expression {
    private final String name;
    
    public IdentifierExpression(String name, Position position) {
        super(position);
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
