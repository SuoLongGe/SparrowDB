package com.sqlcompiler.execution;

/**
 * JOIN计划
 */
public class JoinPlan {
    public enum JoinType {
        INNER,
        LEFT,
        RIGHT,
        FULL
    }
    
    private final JoinType joinType;
    private final String tableName;
    private final String alias;
    private final ExpressionPlan condition;
    
    public JoinPlan(JoinType joinType, String tableName, String alias, ExpressionPlan condition) {
        this.joinType = joinType;
        this.tableName = tableName;
        this.alias = alias;
        this.condition = condition;
    }
    
    public JoinType getJoinType() {
        return joinType;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public String getAlias() {
        return alias;
    }
    
    public ExpressionPlan getCondition() {
        return condition;
    }
    
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("        \"joinType\": \"").append(joinType.name()).append("\",\n");
        sb.append("        \"tableName\": \"").append(tableName).append("\",\n");
        if (alias != null) {
            sb.append("        \"alias\": \"").append(alias).append("\",\n");
        }
        sb.append("        \"condition\": ").append(condition.toJSON()).append("\n");
        sb.append("      }");
        return sb.toString();
    }
    
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(JOIN ").append(joinType.name()).append(" \"").append(tableName).append("\"");
        
        if (alias != null) {
            sb.append(" \"").append(alias).append("\"");
        }
        
        sb.append(" ").append(condition.toSExpression()).append(")");
        return sb.toString();
    }
}
