package com.sqlcompiler.execution;

import java.util.List;

/**
 * SELECT执行计划
 */
public class SelectPlan extends ExecutionPlan {
    private final boolean distinct;
    private final List<ExpressionPlan> selectList;
    private final List<TablePlan> fromClause;
    private final ExpressionPlan whereClause;
    private final List<ExpressionPlan> groupByClause;
    private final ExpressionPlan havingClause;
    private final List<OrderByItem> orderByClause;
    private final LimitPlan limitClause;
    
    public SelectPlan(boolean distinct, List<ExpressionPlan> selectList, 
                     List<TablePlan> fromClause, ExpressionPlan whereClause,
                     List<ExpressionPlan> groupByClause, ExpressionPlan havingClause,
                     List<OrderByItem> orderByClause, LimitPlan limitClause) {
        super("SELECT");
        this.distinct = distinct;
        this.selectList = selectList;
        this.fromClause = fromClause;
        this.whereClause = whereClause;
        this.groupByClause = groupByClause;
        this.havingClause = havingClause;
        this.orderByClause = orderByClause;
        this.limitClause = limitClause;
    }
    
    public boolean isDistinct() {
        return distinct;
    }
    
    public List<ExpressionPlan> getSelectList() {
        return selectList;
    }
    
    public List<TablePlan> getFromClause() {
        return fromClause;
    }
    
    public ExpressionPlan getWhereClause() {
        return whereClause;
    }
    
    public List<ExpressionPlan> getGroupByClause() {
        return groupByClause;
    }
    
    public ExpressionPlan getHavingClause() {
        return havingClause;
    }
    
    public List<OrderByItem> getOrderByClause() {
        return orderByClause;
    }
    
    public LimitPlan getLimitClause() {
        return limitClause;
    }
    
    @Override
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"SELECT\",\n");
        sb.append("  \"distinct\": ").append(distinct).append(",\n");
        sb.append("  \"selectList\": [\n");
        
        for (int i = 0; i < selectList.size(); i++) {
            sb.append("    ").append(selectList.get(i).toJSON());
            if (i < selectList.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        
        sb.append("  ],\n");
        
        if (fromClause != null && !fromClause.isEmpty()) {
            sb.append("  \"fromClause\": [\n");
            for (int i = 0; i < fromClause.size(); i++) {
                sb.append("    ").append(fromClause.get(i).toJSON());
                if (i < fromClause.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ],\n");
        }
        
        if (whereClause != null) {
            sb.append("  \"whereClause\": ").append(whereClause.toJSON()).append(",\n");
        }
        
        if (groupByClause != null && !groupByClause.isEmpty()) {
            sb.append("  \"groupByClause\": [\n");
            for (int i = 0; i < groupByClause.size(); i++) {
                sb.append("    ").append(groupByClause.get(i).toJSON());
                if (i < groupByClause.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ],\n");
        }
        
        if (havingClause != null) {
            sb.append("  \"havingClause\": ").append(havingClause.toJSON()).append(",\n");
        }
        
        if (orderByClause != null && !orderByClause.isEmpty()) {
            sb.append("  \"orderByClause\": [\n");
            for (int i = 0; i < orderByClause.size(); i++) {
                sb.append("    ").append(orderByClause.get(i).toJSON());
                if (i < orderByClause.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ],\n");
        }
        
        if (limitClause != null) {
            sb.append("  \"limitClause\": ").append(limitClause.toJSON()).append("\n");
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(SELECT");
        
        if (distinct) {
            sb.append(" DISTINCT");
        }
        
        sb.append(" (SELECT_LIST ");
        for (ExpressionPlan expr : selectList) {
            sb.append(expr.toSExpression()).append(" ");
        }
        sb.append(")");
        
        if (fromClause != null && !fromClause.isEmpty()) {
            sb.append(" (FROM ");
            for (TablePlan table : fromClause) {
                sb.append(table.toSExpression()).append(" ");
            }
            sb.append(")");
        }
        
        if (whereClause != null) {
            sb.append(" (WHERE ").append(whereClause.toSExpression()).append(")");
        }
        
        if (groupByClause != null && !groupByClause.isEmpty()) {
            sb.append(" (GROUP_BY ");
            for (ExpressionPlan expr : groupByClause) {
                sb.append(expr.toSExpression()).append(" ");
            }
            sb.append(")");
        }
        
        if (havingClause != null) {
            sb.append(" (HAVING ").append(havingClause.toSExpression()).append(")");
        }
        
        if (orderByClause != null && !orderByClause.isEmpty()) {
            sb.append(" (ORDER_BY ");
            for (OrderByItem item : orderByClause) {
                sb.append(item.toSExpression()).append(" ");
            }
            sb.append(")");
        }
        
        if (limitClause != null) {
            sb.append(" ").append(limitClause.toSExpression());
        }
        
        sb.append(")");
        return sb.toString();
    }
}
