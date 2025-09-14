package com.sqlcompiler.execution;

/**
 * 子查询表达式计划
 */
public class SubqueryExpressionPlan extends ExpressionPlan {
    private final SelectPlan subquery;
    
    public SubqueryExpressionPlan(SelectPlan subquery) {
        super("Subquery");
        this.subquery = subquery;
    }
    
    public SelectPlan getSubquery() {
        return subquery;
    }
    
    @Override
    public String toString() {
        return "(" + subquery.toString() + ")";
    }
    
    @Override
    public String toJSON() {
        return "{\"type\":\"Subquery\",\"subquery\":" + subquery.toJSON() + "}";
    }
    
    @Override
    public String toSExpression() {
        return "(SUBQUERY " + subquery.toSExpression() + ")";
    }
}
