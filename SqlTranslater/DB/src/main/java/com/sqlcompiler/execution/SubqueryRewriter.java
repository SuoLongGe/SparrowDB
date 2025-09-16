package com.sqlcompiler.execution;

import com.sqlcompiler.catalog.Catalog;

import java.util.*;

/**
 * 子查询改写器
 * 将子查询改写为JOIN或其他更高效的查询形式
 */
public class SubqueryRewriter {
    private final Catalog catalog;
    private boolean enabled = true;
    
    public SubqueryRewriter(Catalog catalog) {
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
     * 改写执行计划中的子查询
     */
    public RewriteResult rewrite(ExecutionPlan plan) {
        if (!enabled) {
            return new RewriteResult(plan, null, "子查询优化已禁用");
        }
        
        if (plan instanceof SelectPlan) {
            return rewriteSelectPlan((SelectPlan) plan);
        }
        
        return new RewriteResult(plan, null, "不支持的计划类型");
    }
    
    /**
     * 改写SELECT计划中的子查询
     */
    private RewriteResult rewriteSelectPlan(SelectPlan plan) {
        System.out.println("=== 子查询改写器开始工作 ===");
        
        List<String> rewriteMessages = new ArrayList<>();
        String rewrittenSql = null;
        boolean hasOptimization = false;
        
        // 1. 检查WHERE子句中的子查询
        if (plan.getWhereClause() != null) {
            RewriteResult whereResult = analyzeWhereClause(plan.getWhereClause(), plan.getFromClause());
            if (whereResult.isRewritten()) {
                rewriteMessages.add(whereResult.getMessage());
                rewrittenSql = whereResult.getRewrittenSql();
                hasOptimization = true;
            }
        }
        
        // 2. 检查HAVING子句中的子查询
        if (plan.getHavingClause() != null) {
            RewriteResult havingResult = analyzeHavingClause(plan.getHavingClause(), plan.getFromClause());
            if (havingResult.isRewritten()) {
                rewriteMessages.add(havingResult.getMessage());
                if (rewrittenSql == null) {
                    rewrittenSql = havingResult.getRewrittenSql();
                }
                hasOptimization = true;
            }
        }
        
        // 3. 检查SELECT列表中的子查询
        for (ExpressionPlan expr : plan.getSelectList()) {
            RewriteResult selectResult = analyzeExpression(expr, plan.getFromClause());
            if (selectResult.isRewritten()) {
                rewriteMessages.add(selectResult.getMessage());
                if (rewrittenSql == null) {
                    rewrittenSql = selectResult.getRewrittenSql();
                }
                hasOptimization = true;
            }
        }
        
        String combinedMessage = rewriteMessages.isEmpty() ? 
            "没有发现可优化的子查询" : 
            String.join("; ", rewriteMessages);
        
        return new RewriteResult(plan, rewrittenSql, combinedMessage);
    }
    
    /**
     * 分析WHERE子句中的子查询
     */
    private RewriteResult analyzeWhereClause(ExpressionPlan whereClause, List<TablePlan> fromClause) {
        return analyzeExpression(whereClause, fromClause);
    }
    
    /**
     * 分析HAVING子句中的子查询
     */
    private RewriteResult analyzeHavingClause(ExpressionPlan havingClause, List<TablePlan> fromClause) {
        return analyzeExpression(havingClause, fromClause);
    }
    
    /**
     * 分析表达式中的子查询
     */
    private RewriteResult analyzeExpression(ExpressionPlan expr, List<TablePlan> fromClause) {
        if (expr instanceof BinaryExpressionPlan) {
            return analyzeBinaryExpression((BinaryExpressionPlan) expr, fromClause);
        } else if (expr instanceof SubqueryExpressionPlan) {
            return analyzeSubqueryExpression((SubqueryExpressionPlan) expr, fromClause);
        } else if (expr instanceof AliasExpressionPlan) {
            // 处理别名表达式，递归分析内部表达式
            AliasExpressionPlan alias = (AliasExpressionPlan) expr;
            return analyzeExpression(alias.getExpression(), fromClause);
        }
        
        return new RewriteResult(null, null, "表达式不包含可优化的子查询");
    }
    
    /**
     * 分析二元表达式中的子查询
     */
    private RewriteResult analyzeBinaryExpression(BinaryExpressionPlan binary, List<TablePlan> fromClause) {
        String operator = binary.getOperator();
        
        System.out.println("分析二元表达式: 操作符=" + operator + ", 右操作数类型=" + binary.getRight().getClass().getSimpleName());
        
        // 处理IN子查询改写为EXISTS或JOIN
        if ("IN".equals(operator) && binary.getRight() instanceof SubqueryExpressionPlan) {
            System.out.println("检测到IN子查询");
            return analyzeInSubquery(binary, fromClause);
        }
        
        // 处理EXISTS子查询改写为JOIN
        if ("EXISTS".equals(operator) && binary.getRight() instanceof SubqueryExpressionPlan) {
            System.out.println("检测到EXISTS子查询");
            return analyzeExistsSubquery(binary, fromClause);
        }
        
        // 处理比较操作符中的子查询
        if (isComparisonOperator(operator) && binary.getRight() instanceof SubqueryExpressionPlan) {
            System.out.println("检测到比较操作符中的子查询: " + operator);
            return analyzeComparisonSubquery(binary, fromClause);
        }
        
        // 递归处理左右操作数
        RewriteResult leftResult = analyzeExpression(binary.getLeft(), fromClause);
        RewriteResult rightResult = analyzeExpression(binary.getRight(), fromClause);
        
        if (leftResult.isRewritten() || rightResult.isRewritten()) {
            String message = combineMessages(leftResult.getMessage(), rightResult.getMessage());
            String rewrittenSql = leftResult.getRewrittenSql() != null ? leftResult.getRewrittenSql() : rightResult.getRewrittenSql();
            return new RewriteResult(null, rewrittenSql, message);
        }
        
        return new RewriteResult(null, null, "二元表达式不包含可优化的子查询");
    }
    
    /**
     * 分析IN子查询
     */
    private RewriteResult analyzeInSubquery(BinaryExpressionPlan binary, List<TablePlan> fromClause) {
        SubqueryExpressionPlan subqueryExpr = (SubqueryExpressionPlan) binary.getRight();
        SelectPlan subquery = subqueryExpr.getSubquery();
        
        // 检查是否可以改写为EXISTS
        if (canRewriteToExists(subquery, fromClause)) {
            System.out.println("检测到IN子查询，可改写为EXISTS");
            String rewrittenSql = generateExistsSql(binary, subquery);
            return new RewriteResult(null, rewrittenSql, "IN子查询改写为EXISTS");
        }
        
        // 检查是否可以改写为JOIN
        if (canRewriteToJoin(subquery, fromClause)) {
            System.out.println("检测到IN子查询，可改写为LEFT JOIN");
            String rewrittenSql = generateJoinSql(binary, subquery, "LEFT JOIN");
            return new RewriteResult(null, rewrittenSql, "IN子查询改写为LEFT JOIN");
        }
        
        return new RewriteResult(null, null, "IN子查询无法优化");
    }
    
    /**
     * 分析EXISTS子查询
     */
    private RewriteResult analyzeExistsSubquery(BinaryExpressionPlan binary, List<TablePlan> fromClause) {
        SubqueryExpressionPlan subqueryExpr = (SubqueryExpressionPlan) binary.getRight();
        SelectPlan subquery = subqueryExpr.getSubquery();
        
        // 检查是否可以改写为JOIN
        if (canRewriteToJoin(subquery, fromClause)) {
            System.out.println("检测到EXISTS子查询，可改写为INNER JOIN");
            String rewrittenSql = generateJoinSql(binary, subquery, "INNER JOIN");
            return new RewriteResult(null, rewrittenSql, "EXISTS子查询改写为INNER JOIN");
        }
        
        return new RewriteResult(null, null, "EXISTS子查询无法优化");
    }
    
    /**
     * 分析比较操作符中的子查询
     */
    private RewriteResult analyzeComparisonSubquery(BinaryExpressionPlan binary, List<TablePlan> fromClause) {
        SubqueryExpressionPlan subqueryExpr = (SubqueryExpressionPlan) binary.getRight();
        SelectPlan subquery = subqueryExpr.getSubquery();
        
        // 检查子查询是否返回单个值
        if (isScalarSubquery(subquery)) {
            System.out.println("检测到标量子查询，可进行优化");
            String rewrittenSql = generateScalarSubquerySql(binary, subquery);
            return new RewriteResult(null, rewrittenSql, "标量子查询已优化");
        }
        
        return new RewriteResult(null, null, "比较子查询无法优化");
    }
    
    /**
     * 分析子查询表达式
     */
    private RewriteResult analyzeSubqueryExpression(SubqueryExpressionPlan subqueryExpr, List<TablePlan> fromClause) {
        SelectPlan subquery = subqueryExpr.getSubquery();
        
        // 检查是否可以改写为JOIN
        if (canRewriteToJoin(subquery, fromClause)) {
            System.out.println("检测到子查询，可改写为JOIN");
            String rewrittenSql = generateSubqueryToJoinSql(subquery);
            return new RewriteResult(null, rewrittenSql, "子查询改写为JOIN");
        }
        
        return new RewriteResult(null, null, "子查询无法优化");
    }
    
    /**
     * 检查子查询是否可以改写为EXISTS
     */
    private boolean canRewriteToExists(SelectPlan subquery, List<TablePlan> fromClause) {
        // 简单的启发式规则：如果子查询没有GROUP BY和聚合函数，可以改写为EXISTS
        return subquery.getGroupByClause() == null && 
               !hasAggregateFunctions(subquery.getSelectList()) &&
               subquery.getFromClause() != null && 
               !subquery.getFromClause().isEmpty();
    }
    
    /**
     * 检查子查询是否可以改写为JOIN
     */
    private boolean canRewriteToJoin(SelectPlan subquery, List<TablePlan> fromClause) {
        // 检查子查询是否只涉及一个表
        if (subquery.getFromClause() == null || subquery.getFromClause().size() != 1) {
            return false;
        }
        
        // 检查是否有合适的连接条件
        TablePlan subqueryTable = subquery.getFromClause().get(0);
        return canFindJoinCondition(subqueryTable, fromClause);
    }
    
    /**
     * 检查是否可以找到连接条件
     */
    private boolean canFindJoinCondition(TablePlan subqueryTable, List<TablePlan> fromClause) {
        // 简化实现：检查表名是否匹配
        for (TablePlan table : fromClause) {
            if (!table.getTableName().equals(subqueryTable.getTableName())) {
                return true; // 不同表名，可能有连接条件
            }
        }
        return false;
    }
    
    /**
     * 检查是否为标量子查询
     */
    private boolean isScalarSubquery(SelectPlan subquery) {
        // 标量子查询：返回单个值的子查询
        return subquery.getSelectList().size() == 1 &&
               !hasAggregateFunctions(subquery.getSelectList()) &&
               subquery.getGroupByClause() == null;
    }
    
    /**
     * 检查表达式列表是否包含聚合函数
     */
    private boolean hasAggregateFunctions(List<ExpressionPlan> expressions) {
        for (ExpressionPlan expr : expressions) {
            if (expr instanceof FunctionCallExpressionPlan) {
                FunctionCallExpressionPlan func = (FunctionCallExpressionPlan) expr;
                String functionName = func.getFunctionName().toUpperCase();
                if ("COUNT".equals(functionName) || "SUM".equals(functionName) || 
                    "AVG".equals(functionName) || "MAX".equals(functionName) || 
                    "MIN".equals(functionName)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 检查是否为比较操作符
     */
    private boolean isComparisonOperator(String operator) {
        return "=".equals(operator) || "!=".equals(operator) || 
               ">".equals(operator) || "<".equals(operator) ||
               ">=".equals(operator) || "<=".equals(operator);
    }
    
    /**
     * 合并消息
     */
    private String combineMessages(String msg1, String msg2) {
        if (msg1 == null && msg2 == null) return null;
        if (msg1 == null) return msg2;
        if (msg2 == null) return msg1;
        return msg1 + "; " + msg2;
    }
    
    /**
     * 生成EXISTS改写后的SQL
     */
    private String generateExistsSql(BinaryExpressionPlan binary, SelectPlan subquery) {
        String leftExpr = extractColumnName(binary.getLeft().toString());
        String subquerySql = generateReadableSubquerySql(subquery);
        
        return "SELECT * FROM main_table WHERE EXISTS (" + subquerySql + 
               " AND main_table." + leftExpr + " = subquery." + leftExpr + ")";
    }
    
    /**
     * 生成JOIN改写后的SQL
     */
    private String generateJoinSql(BinaryExpressionPlan binary, SelectPlan subquery, String joinType) {
        String leftExpr = extractColumnName(binary.getLeft().toString());
        String subquerySql = generateReadableSubquerySql(subquery);
        
        return "SELECT main_table.* FROM main_table " + joinType + " (" + subquerySql + 
               ") subquery ON main_table." + leftExpr + " = subquery." + leftExpr;
    }
    
    /**
     * 生成标量子查询改写后的SQL
     */
    private String generateScalarSubquerySql(BinaryExpressionPlan binary, SelectPlan subquery) {
        String leftExpr = extractColumnName(binary.getLeft().toString());
        String operator = binary.getOperator();
        String subquerySql = generateReadableSubquerySql(subquery);
        
        return "SELECT * FROM main_table WHERE " + leftExpr + " " + operator + 
               " (" + subquerySql + ")";
    }
    
    /**
     * 生成子查询到JOIN的SQL
     */
    private String generateSubqueryToJoinSql(SelectPlan subquery) {
        String subquerySql = generateReadableSubquerySql(subquery);
        return "SELECT main_table.* FROM main_table INNER JOIN (" + subquerySql + 
               ") subquery ON main_table.id = subquery.id";
    }
    
    /**
     * 生成可读的子查询SQL
     */
    private String generateReadableSubquerySql(SelectPlan subquery) {
        StringBuilder sql = new StringBuilder();
        
        // SELECT子句
        sql.append("SELECT ");
        if (subquery.getSelectList().isEmpty()) {
            sql.append("*");
        } else {
            for (int i = 0; i < subquery.getSelectList().size(); i++) {
                if (i > 0) sql.append(", ");
                ExpressionPlan expr = subquery.getSelectList().get(i);
                sql.append(generateReadableExpressionSql(expr));
            }
        }
        
        // FROM子句
        if (subquery.getFromClause() != null && !subquery.getFromClause().isEmpty()) {
            sql.append(" FROM ");
            for (int i = 0; i < subquery.getFromClause().size(); i++) {
                if (i > 0) sql.append(", ");
                TablePlan table = subquery.getFromClause().get(i);
                sql.append(table.getTableName());
            }
        }
        
        // WHERE子句
        if (subquery.getWhereClause() != null) {
            sql.append(" WHERE ");
            sql.append(generateReadableExpressionSql(subquery.getWhereClause()));
        }
        
        return sql.toString();
    }
    
    /**
     * 生成可读的表达式SQL
     */
    private String generateReadableExpressionSql(ExpressionPlan expr) {
        if (expr instanceof BinaryExpressionPlan) {
            BinaryExpressionPlan binary = (BinaryExpressionPlan) expr;
            String left = generateReadableExpressionSql(binary.getLeft());
            String right = generateReadableExpressionSql(binary.getRight());
            return left + " " + binary.getOperator() + " " + right;
        } else if (expr instanceof IdentifierExpressionPlan) {
            IdentifierExpressionPlan id = (IdentifierExpressionPlan) expr;
            return id.getName();
        } else if (expr instanceof LiteralExpressionPlan) {
            LiteralExpressionPlan lit = (LiteralExpressionPlan) expr;
            String val = lit.getValue();
            String type = lit.getDataType();
            if (type != null && type.toUpperCase().contains("STRING")) {
                return "'" + val + "'";
            }
            return val;
        } else if (expr instanceof SubqueryExpressionPlan) {
            SubqueryExpressionPlan sq = (SubqueryExpressionPlan) expr;
            return "(" + generateReadableSubquerySql(sq.getSubquery()) + ")";
        } else {
            // 兜底：尽力提取最后的列名片段
            return extractColumnName(String.valueOf(expr));
        }
    }
    
    /**
     * 从表达式中提取列名
     */
    private String extractColumnName(String expr) {
        // 简化实现：假设表达式是列名
        if (expr.contains(".")) {
            return expr.substring(expr.lastIndexOf(".") + 1);
        }
        return expr;
    }
    
    /**
     * 获取优化统计信息
     */
    public String getOptimizationStats() {
        return "子查询改写器状态: " + (enabled ? "启用" : "禁用");
    }
    
    /**
     * 改写结果类
     */
    public static class RewriteResult {
        private final ExecutionPlan rewrittenPlan;
        private final String originalSql;
        private final String rewrittenSql;
        private final String message;
        
        public RewriteResult(ExecutionPlan rewrittenPlan, String rewrittenSql, String message) {
            this.rewrittenPlan = rewrittenPlan;
            this.originalSql = null;
            this.rewrittenSql = rewrittenSql;
            this.message = message;
        }
        
        public RewriteResult(ExecutionPlan rewrittenPlan, String originalSql, String rewrittenSql, String message) {
            this.rewrittenPlan = rewrittenPlan;
            this.originalSql = originalSql;
            this.rewrittenSql = rewrittenSql;
            this.message = message;
        }
        
        public boolean isRewritten() {
            return rewrittenSql != null && !rewrittenSql.isEmpty();
        }
        
        public ExecutionPlan getRewrittenPlan() {
            return rewrittenPlan;
        }
        
        public String getOriginalSql() {
            return originalSql;
        }
        
        public String getRewrittenSql() {
            return rewrittenSql;
        }
        
        public String getMessage() {
            return message;
        }
    }
}