package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * 别名表达式（用于SELECT列表中的AS别名）
 */
public class AliasExpression extends Expression {
    private final Expression expression;
    private final String alias;
    
    public AliasExpression(Expression expression, String alias, Position position) {
        super(position);
        this.expression = expression;
        this.alias = alias;
    }
    
    public Expression getExpression() {
        return expression;
    }
    
    public String getAlias() {
        return alias;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
