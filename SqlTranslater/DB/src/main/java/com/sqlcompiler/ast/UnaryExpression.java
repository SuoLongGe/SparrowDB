package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import com.sqlcompiler.lexer.TokenType;

/**
 * 一元表达式
 */
public class UnaryExpression extends Expression {
    private final TokenType operator;
    private final Expression operand;
    
    public UnaryExpression(TokenType operator, Expression operand, Position position) {
        super(position);
        this.operator = operator;
        this.operand = operand;
    }
    
    public TokenType getOperator() {
        return operator;
    }
    
    public Expression getOperand() {
        return operand;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
