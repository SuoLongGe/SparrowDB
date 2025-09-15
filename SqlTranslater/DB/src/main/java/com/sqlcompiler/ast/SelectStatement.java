package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import java.util.List;

/**
 * SELECT语句
 */
public class SelectStatement extends Statement {
    private final boolean distinct;
    private final List<Expression> selectList;
    private final List<TableReference> fromClause;
    private final WhereClause whereClause;
    private final GroupByClause groupByClause;
    private final HavingClause havingClause;
    private final OrderByClause orderByClause;
    private final LimitClause limitClause;
    
    public SelectStatement(boolean distinct, List<Expression> selectList, 
                         List<TableReference> fromClause, WhereClause whereClause,
                         GroupByClause groupByClause, HavingClause havingClause,
                         OrderByClause orderByClause, LimitClause limitClause,
                         Position position) {
        super(position);
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
    
    public List<Expression> getSelectList() {
        return selectList;
    }
    
    public List<TableReference> getFromClause() {
        return fromClause;
    }
    
    public WhereClause getWhereClause() {
        return whereClause;
    }
    
    public GroupByClause getGroupByClause() {
        return groupByClause;
    }
    
    public HavingClause getHavingClause() {
        return havingClause;
    }
    
    public OrderByClause getOrderByClause() {
        return orderByClause;
    }
    
    public LimitClause getLimitClause() {
        return limitClause;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
    
    /**
     * 生成SQL字符串
     */
    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        
        sql.append("SELECT ");
        
        if (distinct) {
            sql.append("DISTINCT ");
        }
        
        // SELECT列表
        if (selectList != null && !selectList.isEmpty()) {
            for (int i = 0; i < selectList.size(); i++) {
                if (i > 0) sql.append(", ");
                Expression expr = selectList.get(i);
                if (expr instanceof IdentifierExpression) {
                    sql.append(((IdentifierExpression) expr).getName());
                } else {
                    sql.append(expr.toString());
                }
            }
        } else {
            sql.append("*");
        }
        
        // FROM子句
        if (fromClause != null && !fromClause.isEmpty()) {
            sql.append(" FROM ");
            for (int i = 0; i < fromClause.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(fromClause.get(i).getTableName());
            }
        }
        
        // WHERE子句
        if (whereClause != null) {
            sql.append(" WHERE ").append(whereClause.toString());
        }
        
        // GROUP BY子句
        if (groupByClause != null) {
            sql.append(" ").append(groupByClause.toString());
        }
        
        // HAVING子句
        if (havingClause != null) {
            sql.append(" ").append(havingClause.toString());
        }
        
        // ORDER BY子句
        if (orderByClause != null) {
            sql.append(" ").append(orderByClause.toString());
        }
        
        // LIMIT子句
        if (limitClause != null) {
            sql.append(" ").append(limitClause.toString());
        }
        
        return sql.toString();
    }
}
