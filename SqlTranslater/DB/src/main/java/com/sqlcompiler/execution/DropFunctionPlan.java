package com.sqlcompiler.execution;

import com.sqlcompiler.ast.DropFunctionStatement;
import com.database.engine.DatabaseEngine;
import com.database.engine.FunctionManager;
import com.database.engine.ExecutionResult;
import com.database.exception.DatabaseException;

/**
 * DROP FUNCTION语句执行计划
 */
public class DropFunctionPlan extends ExecutionPlan {
    private final DropFunctionStatement statement;
    
    public DropFunctionPlan(DropFunctionStatement statement) {
        super("DROP_FUNCTION");
        this.statement = statement;
    }
    
    public ExecutionResult execute(DatabaseEngine engine) throws DatabaseException {
        try {
            // 获取函数管理器
            FunctionManager functionManager = engine.getFunctionManager();
            
            // 删除函数
            functionManager.dropFunction(statement.getFunctionName(), statement.hasIfExists());
            
            String message = String.format("函数 '%s' 删除成功", statement.getFunctionName());
            return new ExecutionResult(true, message, null);
            
        } catch (Exception e) {
            throw new DatabaseException("删除函数失败: " + e.getMessage());
        }
    }
    
    @Override
    public String toJSON() {
        return String.format("{\"type\":\"DROP_FUNCTION\",\"function\":\"%s\"}", 
                           statement.getFunctionName());
    }
    
    @Override
    public String toSExpression() {
        return String.format("(DROP_FUNCTION %s)", statement.getFunctionName());
    }
    
    @Override
    public String toString() {
        return "DropFunctionPlan{function=" + statement.getFunctionName() + "}";
    }
}
