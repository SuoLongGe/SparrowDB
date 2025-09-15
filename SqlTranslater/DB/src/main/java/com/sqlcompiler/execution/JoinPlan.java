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
    private final SelectPlan subquery;
    private final String alias;
    private final ExpressionPlan condition;
    
    public JoinPlan(JoinType joinType, String tableName, String alias, ExpressionPlan condition) {
        this.joinType = joinType;
        this.tableName = tableName;
        this.subquery = null;
        this.alias = alias;
        this.condition = condition;
    }
    
    public JoinPlan(JoinType joinType, SelectPlan subquery, String alias, ExpressionPlan condition) {
        this.joinType = joinType;
        this.tableName = null;
        this.subquery = subquery;
        this.alias = alias;
        this.condition = condition;
    }
    
    public JoinType getJoinType() {
        return joinType;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public SelectPlan getSubquery() {
        return subquery;
    }
    
    public boolean isSubquery() {
        return subquery != null;
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
        if (isSubquery()) {
            sb.append("        \"subquery\": ").append(subquery.toJSON()).append(",\n");
        } else {
            sb.append("        \"tableName\": \"").append(tableName).append("\",\n");
        }
        if (alias != null) {
            sb.append("        \"alias\": \"").append(alias).append("\",\n");
        }
        sb.append("        \"condition\": ").append(condition.toJSON()).append("\n");
        sb.append("      }");
        return sb.toString();
    }
    
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(JOIN ").append(joinType.name());
        if (isSubquery()) {
            sb.append(" ").append(subquery.toSExpression());
        } else {
            sb.append(" \"").append(tableName).append("\"");
        }
        
        if (alias != null) {
            sb.append(" \"").append(alias).append("\"");
        }
        
        sb.append(" ").append(condition.toSExpression()).append(")");
        return sb.toString();
    }
}
