package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * DROP SHARD语句的AST节点
 * 格式: DROP SHARD table_name
 */
public class DropShardStatement extends Statement {
    private final String tableName;
    
    public DropShardStatement(String tableName, Position position) {
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
        return String.format("DROP SHARD %s", tableName);
    }
}
