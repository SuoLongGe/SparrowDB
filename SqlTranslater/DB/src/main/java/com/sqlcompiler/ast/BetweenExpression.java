package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * BETWEEN表达式
 * 格式：column BETWEEN lower_bound AND upper_bound
 */
public class BetweenExpression extends Expression {
    private final Expression left;
    private final Expression lowerBound;
    private final Expression upperBound;
    
    public BetweenExpression(Expression left, Expression lowerBound, Expression upperBound, Position position) {
        super(position);
        this.left = left;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }
    
    public Expression getLeft() {
        return left;
    }
    
    public Expression getLowerBound() {
        return lowerBound;
    }
    
    public Expression getUpperBound() {
        return upperBound;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
