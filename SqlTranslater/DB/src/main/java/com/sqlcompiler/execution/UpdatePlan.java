package com.sqlcompiler.execution;

import java.util.Map;

/**
 * UPDATE执行计划
 */
public class UpdatePlan extends ExecutionPlan {
    private final String tableName;
    private final Map<String, ExpressionPlan> setClause;
    private final ExpressionPlan whereClause;
    
    public UpdatePlan(String tableName, Map<String, ExpressionPlan> setClause, ExpressionPlan whereClause) {
        super("UPDATE");
        this.tableName = tableName;
        this.setClause = setClause;
        this.whereClause = whereClause;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public Map<String, ExpressionPlan> getSetClause() {
        return setClause;
    }
    
    public ExpressionPlan getWhereClause() {
        return whereClause;
    }
    
    @Override
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"UPDATE\",\n");
        sb.append("  \"tableName\": \"").append(tableName).append("\",\n");
        sb.append("  \"setClause\": {\n");
        
        boolean first = true;
        for (Map.Entry<String, ExpressionPlan> entry : setClause.entrySet()) {
            if (!first) {
                sb.append(",\n");
            }
            sb.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue().toJSON());
            first = false;
        }
        
        sb.append("\n  }");
        
        if (whereClause != null) {
            sb.append(",\n  \"whereClause\": ").append(whereClause.toJSON());
        }
        
        sb.append("\n}");
        return sb.toString();
    }
    
    @Override
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(UPDATE \"").append(tableName).append("\" ");
        
        sb.append("(SET ");
        boolean first = true;
        for (Map.Entry<String, ExpressionPlan> entry : setClause.entrySet()) {
            if (!first) {
                sb.append(" ");
            }
            sb.append("(").append(entry.getKey()).append(" ").append(entry.getValue().toSExpression()).append(")");
            first = false;
        }
        sb.append(")");
        
        if (whereClause != null) {
            sb.append(" (WHERE ").append(whereClause.toSExpression()).append(")");
        }
        
        sb.append(")");
        return sb.toString();
    }
}
