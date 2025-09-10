package com.sqlcompiler.execution;

/**
 * DELETE执行计划
 */
public class DeletePlan extends ExecutionPlan {
    private final String tableName;
    private final ExpressionPlan whereClause;
    
    public DeletePlan(String tableName, ExpressionPlan whereClause) {
        super("DELETE");
        this.tableName = tableName;
        this.whereClause = whereClause;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public ExpressionPlan getWhereClause() {
        return whereClause;
    }
    
    @Override
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"DELETE\",\n");
        sb.append("  \"tableName\": \"").append(tableName).append("\"");
        
        if (whereClause != null) {
            sb.append(",\n");
            sb.append("  \"whereClause\": ").append(whereClause.toJSON());
        }
        
        sb.append("\n}");
        return sb.toString();
    }
    
    @Override
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(DELETE \"").append(tableName).append("\"");
        
        if (whereClause != null) {
            sb.append(" (WHERE ").append(whereClause.toSExpression()).append(")");
        }
        
        sb.append(")");
        return sb.toString();
    }
}
