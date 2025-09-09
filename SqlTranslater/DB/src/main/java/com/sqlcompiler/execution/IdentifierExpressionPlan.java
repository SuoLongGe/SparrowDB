package com.sqlcompiler.execution;

/**
 * 标识符表达式计划
 */
public class IdentifierExpressionPlan extends ExpressionPlan {
    private final String name;
    
    public IdentifierExpressionPlan(String name) {
        super("IDENTIFIER");
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toJSON() {
        return "{\n      \"type\": \"IDENTIFIER\",\n      \"name\": \"" + name + "\"\n    }";
    }
    
    @Override
    public String toSExpression() {
        return "(IDENTIFIER \"" + name + "\")";
    }
}
