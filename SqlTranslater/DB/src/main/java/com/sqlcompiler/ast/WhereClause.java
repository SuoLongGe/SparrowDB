package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * WHERE子句
 */
public class WhereClause extends ASTNode {
    private final Expression condition;
    
    public WhereClause(Expression condition, Position position) {
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
