package com.sqlcompiler.execution;

import java.util.List;

/**
 * 执行计划基类
 */
public abstract class ExecutionPlan {
    protected String planType;
    protected List<ExecutionPlan> children;
    
    public ExecutionPlan(String planType) {
        this.planType = planType;
        this.children = new java.util.ArrayList<>();
    }
    
    public String getPlanType() {
        return planType;
    }
    
    public List<ExecutionPlan> getChildren() {
        return children;
    }
    
    public void addChild(ExecutionPlan child) {
        children.add(child);
    }
    
    /**
     * 转换为JSON格式
     */
    public abstract String toJSON();
    
    /**
     * 转换为S表达式格式
     */
    public abstract String toSExpression();
    
    /**
     * 转换为树形结构字符串
     */
    public String toTreeString() {
        return toTreeString(0);
    }
    
    private String toTreeString(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        sb.append(planType).append("\n");
        
        for (ExecutionPlan child : children) {
            sb.append(child.toTreeString(indent + 1));
        }
        
        return sb.toString();
    }
}
