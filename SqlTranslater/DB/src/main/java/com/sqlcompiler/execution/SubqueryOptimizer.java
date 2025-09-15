package com.sqlcompiler.execution;

import com.sqlcompiler.catalog.Catalog;

/**
 * 子查询优化器
 * 集成子查询改写器，提供完整的子查询优化功能
 */
public class SubqueryOptimizer {
    private final SubqueryRewriter rewriter;
    private boolean enabled = true;
    
    public SubqueryOptimizer(Catalog catalog) {
        this.rewriter = new SubqueryRewriter(catalog);
    }
    
    /**
     * 设置优化器是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.rewriter.setEnabled(enabled);
    }
    
    /**
     * 是否启用优化器
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 优化执行计划
     */
    public SubqueryRewriter.RewriteResult optimize(ExecutionPlan plan) {
        if (!enabled) {
            return new SubqueryRewriter.RewriteResult(plan, null, "子查询优化已禁用");
        }
        
        System.out.println("=== 子查询优化器开始工作 ===");
        
        // 使用子查询改写器进行优化
        SubqueryRewriter.RewriteResult result = rewriter.rewrite(plan);
        
        if (result.isRewritten()) {
            System.out.println("子查询优化完成: " + result.getMessage());
        } else {
            System.out.println("没有发现可优化的子查询");
        }
        
        return result;
    }
    
    /**
     * 获取子查询改写器
     */
    public SubqueryRewriter getRewriter() {
        return rewriter;
    }
    
    /**
     * 获取优化统计信息
     */
    public String getOptimizationStats() {
        return "子查询优化器状态: " + (enabled ? "启用" : "禁用") + 
               " | " + rewriter.getOptimizationStats();
    }
}
