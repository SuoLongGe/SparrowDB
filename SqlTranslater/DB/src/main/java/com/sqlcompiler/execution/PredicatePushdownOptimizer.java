package com.sqlcompiler.execution;

import com.sqlcompiler.catalog.Catalog;
import com.sqlcompiler.catalog.TableInfo;

import java.util.*;

/**
 * 谓词下推优化器
 * 将WHERE条件尽可能下推到数据源，减少数据传输量
 */
public class PredicatePushdownOptimizer {
    private final Catalog catalog;
    private boolean enabled = true;
    
    public PredicatePushdownOptimizer(Catalog catalog) {
        this.catalog = catalog;
    }
    
    /**
     * 设置优化器是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
    public ExecutionPlan optimize(ExecutionPlan plan) {
        if (!enabled) {
            return plan;
        }
        
        if (plan instanceof SelectPlan) {
            return optimizeSelectPlan((SelectPlan) plan);
        }
        
        return plan;
    }
    
    /**
     * 优化SELECT计划
     */
    private SelectPlan optimizeSelectPlan(SelectPlan plan) {
        System.out.println("=== 谓词下推优化器开始工作 ===");
        
        // 如果没有WHERE条件，直接返回
        if (plan.getWhereClause() == null) {
            System.out.println("没有WHERE条件，跳过谓词下推");
            return plan;
        }
        
        // 分析WHERE条件，提取可以下推的谓词
        List<ExpressionPlan> pushablePredicates = new ArrayList<>();
        List<ExpressionPlan> remainingPredicates = new ArrayList<>();
        
        analyzePredicates(plan.getWhereClause(), plan.getFromClause(), pushablePredicates, remainingPredicates);
        
        if (pushablePredicates.isEmpty()) {
            System.out.println("没有可下推的谓词");
            return plan;
        }
        
        System.out.println("发现 " + pushablePredicates.size() + " 个可下推的谓词");
        
        // 创建优化后的表计划，将谓词下推到表扫描
        List<TablePlan> optimizedFromClause = new ArrayList<>();
        for (TablePlan tablePlan : plan.getFromClause()) {
            TablePlan optimizedTablePlan = optimizeTablePlan(tablePlan, pushablePredicates);
            optimizedFromClause.add(optimizedTablePlan);
        }
        
        // 创建新的WHERE条件（只包含不能下推的谓词）
        ExpressionPlan optimizedWhereClause = null;
        if (!remainingPredicates.isEmpty()) {
            optimizedWhereClause = combinePredicates(remainingPredicates);
        }
        
        // 创建优化后的SELECT计划
        SelectPlan optimizedPlan = new SelectPlan(
            plan.isDistinct(),
            plan.getSelectList(),
            optimizedFromClause,
            optimizedWhereClause,
            plan.getGroupByClause(),
            plan.getHavingClause(),
            plan.getOrderByClause(),
            plan.getLimitClause()
        );
        
        System.out.println("谓词下推优化完成");
        return optimizedPlan;
    }
    
    /**
     * 分析谓词，分离可下推和不可下推的谓词
     */
    private void analyzePredicates(ExpressionPlan whereClause, List<TablePlan> fromClause,
                                 List<ExpressionPlan> pushablePredicates, 
                                 List<ExpressionPlan> remainingPredicates) {
        
        if (whereClause instanceof BinaryExpressionPlan) {
            BinaryExpressionPlan binary = (BinaryExpressionPlan) whereClause;
            String operator = binary.getOperator();
            
            if ("AND".equals(operator)) {
                // AND条件：分别分析左右两边
                analyzePredicates(binary.getLeft(), fromClause, pushablePredicates, remainingPredicates);
                analyzePredicates(binary.getRight(), fromClause, pushablePredicates, remainingPredicates);
            } else if ("OR".equals(operator)) {
                // OR条件：如果整个OR条件可以下推，则下推；否则保留
                if (canPushdownPredicate(binary, fromClause)) {
                    pushablePredicates.add(binary);
                } else {
                    remainingPredicates.add(binary);
                }
            } else {
                // 其他二元操作符：检查是否可以下推
                if (canPushdownPredicate(binary, fromClause)) {
                    pushablePredicates.add(binary);
                } else {
                    remainingPredicates.add(binary);
                }
            }
        } else {
            // 非二元表达式：检查是否可以下推
            if (canPushdownPredicate(whereClause, fromClause)) {
                pushablePredicates.add(whereClause);
            } else {
                remainingPredicates.add(whereClause);
            }
        }
    }
    
    /**
     * 检查谓词是否可以下推
     */
    private boolean canPushdownPredicate(ExpressionPlan predicate, List<TablePlan> fromClause) {
        // 获取谓词中引用的列
        Set<String> referencedColumns = extractReferencedColumns(predicate);
        
        // 检查这些列是否都来自同一个表
        String tableName = null;
        for (String column : referencedColumns) {
            String columnTable = findTableForColumn(column, fromClause);
            if (columnTable == null) {
                // 列无法解析到表，不能下推
                return false;
            }
            if (tableName == null) {
                tableName = columnTable;
            } else if (!tableName.equals(columnTable)) {
                // 列来自不同表，不能下推
                return false;
            }
        }
        
        // 检查谓词是否包含聚合函数或子查询
        if (containsAggregateOrSubquery(predicate)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 提取表达式中引用的列名
     */
    private Set<String> extractReferencedColumns(ExpressionPlan expr) {
        Set<String> columns = new HashSet<>();
        
        if (expr instanceof IdentifierExpressionPlan) {
            IdentifierExpressionPlan idExpr = (IdentifierExpressionPlan) expr;
            String columnName = idExpr.getName();
            if (!"*".equals(columnName)) {
                columns.add(columnName);
            }
        } else if (expr instanceof BinaryExpressionPlan) {
            BinaryExpressionPlan binary = (BinaryExpressionPlan) expr;
            columns.addAll(extractReferencedColumns(binary.getLeft()));
            columns.addAll(extractReferencedColumns(binary.getRight()));
        } else if (expr instanceof FunctionCallExpressionPlan) {
            FunctionCallExpressionPlan func = (FunctionCallExpressionPlan) expr;
            for (ExpressionPlan arg : func.getArguments()) {
                columns.addAll(extractReferencedColumns(arg));
            }
        }
        
        return columns;
    }
    
    /**
     * 查找列所属的表
     */
    private String findTableForColumn(String columnName, List<TablePlan> fromClause) {
        // 如果列名包含表别名前缀，直接提取表名
        if (columnName.contains(".")) {
            String tableAlias = columnName.substring(0, columnName.indexOf("."));
            for (TablePlan tablePlan : fromClause) {
                if (tableAlias.equals(tablePlan.getAlias()) || tableAlias.equals(tablePlan.getTableName())) {
                    return tablePlan.getTableName();
                }
            }
        } else {
            // 列名不包含表前缀，需要查找包含该列的表
            for (TablePlan tablePlan : fromClause) {
                TableInfo tableInfo = catalog.getTable(tablePlan.getTableName());
                if (tableInfo != null && tableInfo.columnExists(columnName)) {
                    return tablePlan.getTableName();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查表达式是否包含聚合函数或子查询
     */
    private boolean containsAggregateOrSubquery(ExpressionPlan expr) {
        if (expr instanceof FunctionCallExpressionPlan) {
            FunctionCallExpressionPlan func = (FunctionCallExpressionPlan) expr;
            String functionName = func.getFunctionName().toUpperCase();
            // 检查是否为聚合函数
            if ("COUNT".equals(functionName) || "SUM".equals(functionName) || 
                "AVG".equals(functionName) || "MAX".equals(functionName) || 
                "MIN".equals(functionName)) {
                return true;
            }
        } else if (expr instanceof SubqueryExpressionPlan) {
            return true;
        } else if (expr instanceof BinaryExpressionPlan) {
            BinaryExpressionPlan binary = (BinaryExpressionPlan) expr;
            return containsAggregateOrSubquery(binary.getLeft()) || 
                   containsAggregateOrSubquery(binary.getRight());
        }
        
        return false;
    }
    
    /**
     * 优化表计划，将谓词下推到表扫描
     */
    private TablePlan optimizeTablePlan(TablePlan tablePlan, List<ExpressionPlan> pushablePredicates) {
        // 找到属于该表的谓词
        List<ExpressionPlan> tablePredicates = new ArrayList<>();
        String tableName = tablePlan.getTableName();
        String tableAlias = tablePlan.getAlias();
        
        for (ExpressionPlan predicate : pushablePredicates) {
            Set<String> referencedColumns = extractReferencedColumns(predicate);
            boolean belongsToTable = true;
            
            for (String column : referencedColumns) {
                String columnTable = findTableForColumn(column, Arrays.asList(tablePlan));
                if (columnTable == null || !tableName.equals(columnTable)) {
                    belongsToTable = false;
                    break;
                }
            }
            
            if (belongsToTable) {
                tablePredicates.add(predicate);
            }
        }
        
        if (tablePredicates.isEmpty()) {
            return tablePlan;
        }
        
        // 创建带谓词的表计划
        ExpressionPlan combinedPredicate = combinePredicates(tablePredicates);
        return new OptimizedTablePlan(tableName, tableAlias, tablePlan.getJoins(), combinedPredicate);
    }
    
    /**
     * 合并多个谓词为一个AND表达式
     */
    private ExpressionPlan combinePredicates(List<ExpressionPlan> predicates) {
        if (predicates.isEmpty()) {
            return null;
        }
        
        if (predicates.size() == 1) {
            return predicates.get(0);
        }
        
        ExpressionPlan result = predicates.get(0);
        for (int i = 1; i < predicates.size(); i++) {
            result = new BinaryExpressionPlan(result, "AND", predicates.get(i));
        }
        
        return result;
    }
    
    /**
     * 获取优化统计信息
     */
    public String getOptimizationStats() {
        return "谓词下推优化器状态: " + (enabled ? "启用" : "禁用");
    }
}
