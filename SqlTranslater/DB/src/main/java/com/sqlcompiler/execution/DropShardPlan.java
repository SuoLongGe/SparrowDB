package com.sqlcompiler.execution;

import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import com.sqlcompiler.ast.DropShardStatement;

/**
 * DROP SHARD执行计划
 */
public class DropShardPlan extends ExecutionPlan {
    private final DropShardStatement statement;
    
    public DropShardPlan(DropShardStatement statement) {
        super("DROP_SHARD");
        this.statement = statement;
    }
    
    public DropShardStatement getStatement() {
        return statement;
    }
    
    
    @Override
    public String toString() {
        return statement.toString();
    }
    
    @Override
    public String toSExpression() {
        return String.format("(DROP_SHARD %s)", statement.getTableName());
    }
    
    @Override
    public String toJSON() {
        return String.format("{\"type\":\"DROP_SHARD\",\"table\":\"%s\"}", statement.getTableName());
    }
}
