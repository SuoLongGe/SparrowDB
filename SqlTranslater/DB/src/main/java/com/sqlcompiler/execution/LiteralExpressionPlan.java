package com.sqlcompiler.execution;

/**
 * 字面量表达式计划
 */
public class LiteralExpressionPlan extends ExpressionPlan {
    private final String value;
    private final String dataType;
    
    public LiteralExpressionPlan(String value, String dataType) {
        super("LITERAL");
        this.value = value;
        this.dataType = dataType;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    @Override
    public String toJSON() {
        return "{\n      \"type\": \"LITERAL\",\n      \"value\": \"" + value + "\",\n      \"dataType\": \"" + dataType + "\"\n    }";
    }
    
    @Override
    public String toSExpression() {
        return "(LITERAL \"" + value + "\" \"" + dataType + "\")";
    }
}
