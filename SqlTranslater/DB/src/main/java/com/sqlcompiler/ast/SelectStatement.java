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
}
