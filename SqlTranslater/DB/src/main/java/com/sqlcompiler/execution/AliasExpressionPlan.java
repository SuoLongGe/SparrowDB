package com.sqlcompiler.execution;

/**
 * 别名表达式计划
 */
public class AliasExpressionPlan extends ExpressionPlan {
    private final ExpressionPlan expression;
    private final String alias;
    
    public AliasExpressionPlan(ExpressionPlan expression, String alias) {
        super("Alias");
        this.expression = expression;
        this.alias = alias;
    }
    
    public ExpressionPlan getExpression() {
        return expression;
    }
    
    public String getAlias() {
        return alias;
    }
    
    @Override
    public String toString() {
        return expression.toString() + " AS " + alias;
    }
    
    @Override
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"Alias\",\"expression\":").append(expression.toJSON());
        sb.append(",\"alias\":\"").append(alias).append("\"}");
        return sb.toString();
    }
    
    @Override
    public String toSExpression() {
        return "(ALIAS " + expression.toSExpression() + " \"" + alias + "\")";
    }
}
