package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * IS NULL / IS NOT NULL表达式
 * 格式：column IS NULL 或 column IS NOT NULL
 */
public class IsNullExpression extends Expression {
    private final Expression left;
    private final boolean isNot; // true表示IS NOT NULL，false表示IS NULL
    
    public IsNullExpression(Expression left, boolean isNot, Position position) {
        super(position);
        this.left = left;
        this.isNot = isNot;
    }
    
    public Expression getLeft() {
        return left;
    }
    
    public boolean isNot() {
        return isNot;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
