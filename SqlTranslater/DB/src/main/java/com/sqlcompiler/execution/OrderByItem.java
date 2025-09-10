package com.sqlcompiler.execution;

/**
 * ORDER BY项
 */
public class OrderByItem {
    public enum SortOrder {
        ASC,
        DESC
    }
    
    private final ExpressionPlan expression;
    private final SortOrder order;
    
    public OrderByItem(ExpressionPlan expression, SortOrder order) {
        this.expression = expression;
        this.order = order;
    }
    
    public ExpressionPlan getExpression() {
        return expression;
    }
    
    public SortOrder getOrder() {
        return order;
    }
    
    public String toJSON() {
        return "{\n      \"expression\": " + expression.toJSON() + ",\n      \"order\": \"" + order.name() + "\"\n    }";
    }
    
    public String toSExpression() {
        return "(ORDER_BY_ITEM " + expression.toSExpression() + " " + order.name() + ")";
    }
}
