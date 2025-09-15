package com.sqlcompiler.execution;

import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import com.database.engine.sharding.*;
import com.sqlcompiler.ast.ShowShardsStatement;

import java.util.*;

/**
 * SHOW SHARDS执行计划
 */
public class ShowShardsPlan extends ExecutionPlan {
    private final ShowShardsStatement statement;
    
    public ShowShardsPlan(ShowShardsStatement statement) {
        super("SHOW_SHARDS");
        this.statement = statement;
    }
    
    public ShowShardsStatement getStatement() {
        return statement;
    }
    
    
    @Override
    public String toString() {
        return statement.toString();
    }
    
    @Override
    public String toSExpression() {
        if (statement.hasTableName()) {
            return String.format("(SHOW_SHARDS %s)", statement.getTableName());
        } else {
            return "(SHOW_SHARDS)";
        }
    }
    
    @Override
    public String toJSON() {
        if (statement.hasTableName()) {
            return String.format("{\"type\":\"SHOW_SHARDS\",\"table\":\"%s\"}", statement.getTableName());
        } else {
            return "{\"type\":\"SHOW_SHARDS\"}";
        }
    }
}
