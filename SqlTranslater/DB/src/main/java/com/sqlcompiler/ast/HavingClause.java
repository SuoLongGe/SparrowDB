package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * HAVING子句
 */
public class HavingClause extends ASTNode {
    private final Expression condition;
    
    public HavingClause(Expression condition, Position position) {
        super(position);
        this.condition = condition;
    }
    
    public Expression getCondition() {
        return condition;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
