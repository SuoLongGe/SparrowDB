package com.sqlcompiler.execution;

import com.sqlcompiler.ast.CallStatement;
import com.sqlcompiler.ast.Expression;
import com.database.engine.DatabaseEngine;
import com.database.engine.FunctionManager;
import com.database.engine.ExecutionResult;
import com.database.exception.DatabaseException;

import java.util.*;
import java.util.Collections;

/**
 * CALL语句执行计划
 */
public class CallPlan extends ExecutionPlan {
    private final CallStatement statement;
    
    public CallPlan(CallStatement statement) {
        super("CALL");
        this.statement = statement;
    }
    
    public ExecutionResult execute(DatabaseEngine engine) throws DatabaseException {
        try {
            // 获取函数管理器
            FunctionManager functionManager = engine.getFunctionManager();
            
            // 计算参数值
            List<Object> arguments = new ArrayList<>();
            for (Expression arg : statement.getArguments()) {
                Object value = evaluateExpression(arg);
                arguments.add(value);
            }
            
            // 调用函数
            Object result = functionManager.callFunction(statement.getFunctionName(), arguments);
            
            // 构建结果
            List<Map<String, Object>> rows = Arrays.asList(
                Collections.singletonMap("result", result)
            );
            
            String message = String.format("函数 '%s' 执行成功", statement.getFunctionName());
            return new ExecutionResult(true, message, rows);
            
        } catch (Exception e) {
            throw new DatabaseException("调用函数失败: " + e.getMessage());
        }
    }
    
    /**
     * 简单的表达式求值
     */
    private Object evaluateExpression(Expression expr) {
        if (expr instanceof com.sqlcompiler.ast.LiteralExpression) {
            com.sqlcompiler.ast.LiteralExpression literal = (com.sqlcompiler.ast.LiteralExpression) expr;
            Object value = literal.getValue();
            
            // 尝试转换为数字
            if (value instanceof String) {
                String str = (String) value;
                try {
                    if (str.contains(".")) {
                        return Double.parseDouble(str);
                    } else {
                        return Long.parseLong(str);
                    }
                } catch (NumberFormatException e) {
                    return str; // 保持为字符串
                }
            }
            return value;
        }
        
        // 对于其他类型的表达式，暂时返回字符串表示
        return expr.toString();
    }
    
    @Override
    public String toJSON() {
        return String.format("{\"type\":\"CALL\",\"function\":\"%s\"}", 
                           statement.getFunctionName());
    }
    
    @Override
    public String toSExpression() {
        return String.format("(CALL %s)", statement.getFunctionName());
    }
    
    @Override
    public String toString() {
        return "CallPlan{function=" + statement.getFunctionName() + "}";
    }
}
