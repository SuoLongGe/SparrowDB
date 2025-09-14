package com.sqlcompiler.execution;

import com.sqlcompiler.ast.CreateFunctionStatement;
import com.database.engine.DatabaseEngine;
import com.database.engine.FunctionManager;
import com.database.engine.ExecutionResult;
import com.database.exception.DatabaseException;

/**
 * CREATE FUNCTION语句执行计划
 */
public class CreateFunctionPlan extends ExecutionPlan {
    private final CreateFunctionStatement statement;
    
    public CreateFunctionPlan(CreateFunctionStatement statement) {
        super("CREATE_FUNCTION");
        this.statement = statement;
    }
    
    public ExecutionResult execute(DatabaseEngine engine) throws DatabaseException {
        try {
            // 获取函数管理器
            FunctionManager functionManager = engine.getFunctionManager();
            
            // 创建函数
            functionManager.createFunction(statement);
            
            String message = String.format("函数 '%s' 创建成功", statement.getFunctionName());
            return new ExecutionResult(true, message, null);
            
        } catch (Exception e) {
            throw new DatabaseException("创建函数失败: " + e.getMessage());
        }
    }
    
    @Override
    public String toJSON() {
        return String.format("{\"type\":\"CREATE_FUNCTION\",\"function\":\"%s\"}", 
                           statement.getFunctionName());
    }
    
    @Override
    public String toSExpression() {
        return String.format("(CREATE_FUNCTION %s)", statement.getFunctionName());
    }
    
    @Override
    public String toString() {
        return "CreateFunctionPlan{function=" + statement.getFunctionName() + "}";
    }
}
