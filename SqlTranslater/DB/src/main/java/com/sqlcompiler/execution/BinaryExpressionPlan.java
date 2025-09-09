package com.sqlcompiler.execution;

/**
 * 二元表达式计划
 */
public class BinaryExpressionPlan extends ExpressionPlan {
    private final ExpressionPlan left;
    private final String operator;
    private final ExpressionPlan right;
    
    public BinaryExpressionPlan(ExpressionPlan left, String operator, ExpressionPlan right) {
        super("BINARY");
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
    
    public ExpressionPlan getLeft() {
        return left;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public ExpressionPlan getRight() {
        return right;
    }
    
    @Override
    public String toJSON() {
        return "{\n      \"type\": \"BINARY\",\n      \"operator\": \"" + operator + "\",\n      \"left\": " + left.toJSON() + ",\n      \"right\": " + right.toJSON() + "\n    }";
    }
    
    @Override
    public String toSExpression() {
        return "(BINARY \"" + operator + "\" " + left.toSExpression() + " " + right.toSExpression() + ")";
    }
}
