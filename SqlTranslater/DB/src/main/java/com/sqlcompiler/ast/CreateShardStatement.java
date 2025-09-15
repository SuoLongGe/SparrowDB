package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * CREATE SHARD语句的AST节点
 * 格式: CREATE SHARD table_name BY shard_key_column USING strategy (shard_count)
 */
public class CreateShardStatement extends Statement {
    private final String tableName;
    private final String shardKeyColumn;
    private final String strategy;
    private final int shardCount;
    
    public CreateShardStatement(String tableName, String shardKeyColumn, String strategy, int shardCount, Position position) {
        super(position);
        this.tableName = tableName;
        this.shardKeyColumn = shardKeyColumn;
        this.strategy = strategy;
        this.shardCount = shardCount;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public String getShardKeyColumn() {
        return shardKeyColumn;
    }
    
    public String getStrategy() {
        return strategy;
    }
    
    public int getShardCount() {
        return shardCount;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return String.format("CREATE SHARD %s BY %s USING %s (%d)", 
                           tableName, shardKeyColumn, strategy, shardCount);
    }
}
