package com.sqlcompiler.execution;

import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import com.database.engine.sharding.*;
import com.sqlcompiler.ast.CreateShardStatement;

/**
 * CREATE SHARD执行计划
 */
public class CreateShardPlan extends ExecutionPlan {
    private final CreateShardStatement statement;
    
    public CreateShardPlan(CreateShardStatement statement) {
        super("CREATE_SHARD");
        this.statement = statement;
    }
    
    public CreateShardStatement getStatement() {
        return statement;
    }
    
    
    @Override
    public String toString() {
        return statement.toString();
    }
    
    @Override
    public String toSExpression() {
        return String.format("(CREATE_SHARD %s BY %s USING %s (%d))", 
                           statement.getTableName(), 
                           statement.getShardKeyColumn(), 
                           statement.getStrategy(), 
                           statement.getShardCount());
    }
    
    @Override
    public String toJSON() {
        return String.format("{\"type\":\"CREATE_SHARD\",\"table\":\"%s\",\"shardKey\":\"%s\",\"strategy\":\"%s\",\"count\":%d}", 
                           statement.getTableName(), 
                           statement.getShardKeyColumn(), 
                           statement.getStrategy(), 
                           statement.getShardCount());
    }
}
