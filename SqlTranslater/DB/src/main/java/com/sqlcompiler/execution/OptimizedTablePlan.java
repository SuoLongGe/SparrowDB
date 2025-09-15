package com.sqlcompiler.execution;

import java.util.List;

/**
 * 优化的表计划，包含下推的谓词
 */
public class OptimizedTablePlan extends TablePlan {
    private final ExpressionPlan pushedPredicate;
    
    public OptimizedTablePlan(String tableName, String alias, List<JoinPlan> joins, ExpressionPlan pushedPredicate) {
        super(tableName, alias, joins);
        this.pushedPredicate = pushedPredicate;
    }
    
    public ExpressionPlan getPushedPredicate() {
        return pushedPredicate;
    }
    
    public boolean hasPushedPredicate() {
        return pushedPredicate != null;
    }
    
    @Override
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("      \"tableName\": \"").append(getTableName()).append("\",\n");
        if (getAlias() != null) {
            sb.append("      \"alias\": \"").append(getAlias()).append("\",\n");
        }
        if (hasPushedPredicate()) {
            sb.append("      \"pushedPredicate\": ").append(pushedPredicate.toJSON()).append(",\n");
        }
        if (getJoins() != null && !getJoins().isEmpty()) {
            sb.append("      \"joins\": [\n");
            for (int i = 0; i < getJoins().size(); i++) {
                sb.append("        ").append(getJoins().get(i).toJSON());
                if (i < getJoins().size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("      ]\n");
        }
        sb.append("    }");
        return sb.toString();
    }
    
    @Override
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(OPTIMIZED_TABLE \"").append(getTableName()).append("\"");
        
        if (getAlias() != null) {
            sb.append(" \"").append(getAlias()).append("\"");
        }
        
        if (hasPushedPredicate()) {
            sb.append(" (PUSHED_PREDICATE ").append(pushedPredicate.toSExpression()).append(")");
        }
        
        if (getJoins() != null && !getJoins().isEmpty()) {
            for (JoinPlan join : getJoins()) {
                sb.append(" ").append(join.toSExpression());
            }
        }
        
        sb.append(")");
        return sb.toString();
    }
}
