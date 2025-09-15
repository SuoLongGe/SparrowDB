package com.sqlcompiler.execution;

import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import com.database.engine.sharding.*;
import com.sqlcompiler.ast.ShardStatsStatement;

import java.util.*;

/**
 * SHARD STATS执行计划
 */
public class ShardStatsPlan extends ExecutionPlan {
    private final ShardStatsStatement statement;
    
    public ShardStatsPlan(ShardStatsStatement statement) {
        super("SHARD_STATS");
        this.statement = statement;
    }
    
    public ShardStatsStatement getStatement() {
        return statement;
    }
    
    
    @Override
    public String toString() {
        return statement.toString();
    }
    
    @Override
    public String toSExpression() {
        return String.format("(SHARD_STATS %s)", statement.getTableName());
    }
    
    @Override
    public String toJSON() {
        return String.format("{\"type\":\"SHARD_STATS\",\"table\":\"%s\"}", statement.getTableName());
    }
}
