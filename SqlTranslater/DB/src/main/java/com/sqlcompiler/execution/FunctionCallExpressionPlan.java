package com.sqlcompiler.execution;

import java.util.List;

/**
 * 函数调用表达式计划
 */
public class FunctionCallExpressionPlan extends ExpressionPlan {
    private final String functionName;
    private final List<ExpressionPlan> arguments;
    
    public FunctionCallExpressionPlan(String functionName, List<ExpressionPlan> arguments) {
        super("FunctionCall");
        this.functionName = functionName;
        this.arguments = arguments;
    }
    
    public String getFunctionName() {
        return functionName;
    }
    
    public List<ExpressionPlan> getArguments() {
        return arguments;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(functionName).append("(");
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(arguments.get(i).toString());
        }
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"FunctionCall\",\"function\":\"").append(functionName).append("\",\"args\":[");
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(arguments.get(i).toJSON());
        }
        sb.append("]}");
        return sb.toString();
    }
    
    @Override
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(functionName);
        for (ExpressionPlan arg : arguments) {
            sb.append(" ").append(arg.toSExpression());
        }
        sb.append(")");
        return sb.toString();
    }
}
