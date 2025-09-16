package com.sqlcompiler.execution;

import java.util.List;

/**
 * 批量执行计划
 * 用于执行多个SQL语句
 */
public class BatchPlan extends ExecutionPlan {
    private final List<ExecutionPlan> plans;
    
    public BatchPlan(List<ExecutionPlan> plans) {
        super("BATCH");
        this.plans = plans;
    }
    
    public List<ExecutionPlan> getPlans() {
        return plans;
    }
    
    @Override
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"BATCH\",\n");
        sb.append("  \"plans\": [\n");
        
        for (int i = 0; i < plans.size(); i++) {
            sb.append("    ");
            sb.append(plans.get(i).toJSON());
            if (i < plans.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(BATCH");
        
        for (ExecutionPlan plan : plans) {
            sb.append(" ");
            sb.append(plan.toSExpression());
        }
        
        sb.append(")");
        return sb.toString();
    }
}
