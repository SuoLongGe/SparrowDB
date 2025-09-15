package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * SHOW SHARDS语句的AST节点
 * 格式: SHOW SHARDS [table_name]
 */
public class ShowShardsStatement extends Statement {
    private final String tableName; // 可选，为null表示显示所有分片
    
    public ShowShardsStatement(String tableName, Position position) {
        super(position);
        this.tableName = tableName;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public boolean hasTableName() {
        return tableName != null && !tableName.isEmpty();
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        if (hasTableName()) {
            return String.format("SHOW SHARDS %s", tableName);
        } else {
            return "SHOW SHARDS";
        }
    }
}
