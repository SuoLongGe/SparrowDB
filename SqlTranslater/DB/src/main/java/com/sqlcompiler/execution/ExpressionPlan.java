package com.sqlcompiler.execution;

/**
 * 表达式计划基类
 */
public abstract class ExpressionPlan {
    protected String type;
    
    public ExpressionPlan(String type) {
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    public abstract String toJSON();
    public abstract String toSExpression();
}
