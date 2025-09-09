package com.sqlcompiler.execution;

/**
 * LIMIT计划
 */
public class LimitPlan {
    private final ExpressionPlan limit;
    private final ExpressionPlan offset;
    
    public LimitPlan(ExpressionPlan limit, ExpressionPlan offset) {
        this.limit = limit;
        this.offset = offset;
    }
    
    public ExpressionPlan getLimit() {
        return limit;
    }
    
    public ExpressionPlan getOffset() {
        return offset;
    }
    
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("      \"limit\": ").append(limit.toJSON());
        if (offset != null) {
            sb.append(",\n");
            sb.append("      \"offset\": ").append(offset.toJSON());
        }
        sb.append("\n    }");
        return sb.toString();
    }
    
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(LIMIT ").append(limit.toSExpression());
        if (offset != null) {
            sb.append(" ").append(offset.toSExpression());
        }
        sb.append(")");
        return sb.toString();
    }
}
