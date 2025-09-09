package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import com.sqlcompiler.lexer.TokenType;

/**
 * 字面量表达式
 */
public class LiteralExpression extends Expression {
    private final TokenType type;
    private final String value;
    
    public LiteralExpression(TokenType type, String value, Position position) {
        super(position);
        this.type = type;
        this.value = value;
    }
    
    public TokenType getType() {
        return type;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
