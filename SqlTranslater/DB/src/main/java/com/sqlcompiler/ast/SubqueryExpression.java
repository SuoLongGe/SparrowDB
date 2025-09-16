package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * 子查询表达式
 * 用于表示括号中的SELECT语句
 */
public class SubqueryExpression extends Expression {
    private final SelectStatement subquery;
    
    public SubqueryExpression(SelectStatement subquery, Position position) {
        super(position);
        this.subquery = subquery;
    }
    
    public SelectStatement getSubquery() {
        return subquery;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
