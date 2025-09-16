package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import java.util.List;

/**
 * IN表达式
 * 支持两种形式：
 * 1. column IN (value1, value2, ...) - 值列表
 * 2. column IN (SELECT ...) - 子查询
 */
public class InExpression extends Expression {
    private final Expression left;
    private final Object right; // 可以是List<Expression>或SelectStatement
    
    /**
     * 构造函数 - 值列表形式
     */
    public InExpression(Expression left, List<Expression> values, Position position) {
        super(position);
        this.left = left;
        this.right = values;
    }
    
    /**
     * 构造函数 - 子查询形式
     */
    public InExpression(Expression left, SelectStatement subquery, Position position) {
        super(position);
        this.left = left;
        this.right = subquery;
    }
    
    public Expression getLeft() {
        return left;
    }
    
    public Object getRight() {
        return right;
    }
    
    /**
     * 判断是否为子查询形式
     */
    public boolean isSubquery() {
        return right instanceof SelectStatement;
    }
    
    /**
     * 获取值列表（仅当不是子查询时）
     */
    @SuppressWarnings("unchecked")
    public List<Expression> getValues() {
        if (isSubquery()) {
            throw new IllegalStateException("这不是值列表形式的IN表达式");
        }
        return (List<Expression>) right;
    }
    
    /**
     * 获取子查询（仅当是子查询时）
     */
    public SelectStatement getSubquery() {
        if (!isSubquery()) {
            throw new IllegalStateException("这不是子查询形式的IN表达式");
        }
        return (SelectStatement) right;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
