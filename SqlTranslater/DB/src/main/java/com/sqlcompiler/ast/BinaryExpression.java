package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import com.sqlcompiler.lexer.TokenType;

/**
 * 二元表达式
 */
public class BinaryExpression extends Expression {
    private final Expression left;
    private final TokenType operator;
    private final Expression right;
    
    public BinaryExpression(Expression left, TokenType operator, Expression right, Position position) {
        super(position);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
    
    public Expression getLeft() {
        return left;
    }
    
    public TokenType getOperator() {
        return operator;
    }
    
    public Expression getRight() {
        return right;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
