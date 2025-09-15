package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * SHARD STATS语句的AST节点
 * 格式: SHARD STATS table_name
 */
public class ShardStatsStatement extends Statement {
    private final String tableName;
    
    public ShardStatsStatement(String tableName, Position position) {
        super(position);
        this.tableName = tableName;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return String.format("SHARD STATS %s", tableName);
    }
}
