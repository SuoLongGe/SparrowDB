package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * JOIN子句
 */
public class JoinClause extends ASTNode {
    public enum JoinType {
        INNER,
        LEFT,
        RIGHT,
        FULL
    }
    
    private final JoinType joinType;
    private final String tableName;
    private final SelectStatement subquery;
    private final String alias;
    private final Expression condition;
    
    public JoinClause(JoinType joinType, String tableName, String alias, 
                     Expression condition, Position position) {
        super(position);
        this.joinType = joinType;
        this.tableName = tableName;
        this.subquery = null;
        this.alias = alias;
        this.condition = condition;
    }
    
    public JoinClause(JoinType joinType, SelectStatement subquery, String alias, 
                     Expression condition, Position position) {
        super(position);
        this.joinType = joinType;
        this.tableName = null;
        this.subquery = subquery;
        this.alias = alias;
        this.condition = condition;
    }
    
    public JoinType getJoinType() {
        return joinType;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public SelectStatement getSubquery() {
        return subquery;
    }
    
    public boolean isSubquery() {
        return subquery != null;
    }
    
    public String getAlias() {
        return alias;
    }
    
    public Expression getCondition() {
        return condition;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
