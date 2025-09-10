package com.sqlcompiler.execution;

import java.util.List;

/**
 * 表计划
 */
public class TablePlan {
    private final String tableName;
    private final String alias;
    private final List<JoinPlan> joins;
    
    public TablePlan(String tableName, String alias, List<JoinPlan> joins) {
        this.tableName = tableName;
        this.alias = alias;
        this.joins = joins;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public String getAlias() {
        return alias;
    }
    
    public List<JoinPlan> getJoins() {
        return joins;
    }
    
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("      \"tableName\": \"").append(tableName).append("\",\n");
        if (alias != null) {
            sb.append("      \"alias\": \"").append(alias).append("\",\n");
        }
        if (joins != null && !joins.isEmpty()) {
            sb.append("      \"joins\": [\n");
            for (int i = 0; i < joins.size(); i++) {
                sb.append("        ").append(joins.get(i).toJSON());
                if (i < joins.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("      ]\n");
        }
        sb.append("    }");
        return sb.toString();
    }
    
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(TABLE \"").append(tableName).append("\"");
        
        if (alias != null) {
            sb.append(" \"").append(alias).append("\"");
        }
        
        if (joins != null && !joins.isEmpty()) {
            for (JoinPlan join : joins) {
                sb.append(" ").append(join.toSExpression());
            }
        }
        
        sb.append(")");
        return sb.toString();
    }
}
