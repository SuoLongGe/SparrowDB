package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * LIMIT子句
 */
public class LimitClause extends ASTNode {
    private final Expression limit;
    private final Expression offset;
    
    public LimitClause(Expression limit, Expression offset, Position position) {
        super(position);
        this.limit = limit;
        this.offset = offset;
    }
    
    public Expression getLimit() {
        return limit;
    }
    
    public Expression getOffset() {
        return offset;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
