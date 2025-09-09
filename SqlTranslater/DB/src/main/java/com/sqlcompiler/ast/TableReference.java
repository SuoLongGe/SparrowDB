package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import java.util.List;

/**
 * 表引用
 */
public class TableReference extends ASTNode {
    private final String tableName;
    private final String alias;
    private final List<JoinClause> joins;
    
    public TableReference(String tableName, String alias, List<JoinClause> joins, Position position) {
        super(position);
        this.tableName = tableName;
        this.alias = alias;
        this.joins = joins;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public String getAlias() {
        return alias;
    }
    
    public List<JoinClause> getJoins() {
        return joins;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
