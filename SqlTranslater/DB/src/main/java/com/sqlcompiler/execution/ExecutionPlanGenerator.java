package com.sqlcompiler.execution;

import com.sqlcompiler.ast.*;
import com.sqlcompiler.exception.CompilationException;
import com.sqlcompiler.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 执行计划生成器
 * 将AST转换为逻辑执行计划
 */
public class ExecutionPlanGenerator implements ASTVisitor<ExecutionPlan> {
    
    @Override
    public ExecutionPlan visit(Statement node) throws CompilationException {
        throw new CompilationException("不支持的语句类型", node.getPosition(), "执行计划生成错误");
    }
    @Override
    public ExecutionPlan visit(BatchStatement node) throws CompilationException {
        // 批量语句转换为批量执行计划
        List<ExecutionPlan> plans = new ArrayList<>();
        
        for (Statement statement : node.getStatements()) {
            ExecutionPlan plan = statement.accept(this);
            if (plan != null) {
                plans.add(plan);
            }
        }
        
        return new BatchPlan(plans);
    }
    @Override
    public ExecutionPlan visit(CreateTableStatement node) throws CompilationException {
        List<ColumnPlan> columns = new ArrayList<>();
        List<ConstraintPlan> constraints = new ArrayList<>();
        
        // 转换列定义
        for (ColumnDefinition columnDef : node.getColumns()) {
            ColumnPlan columnPlan = convertColumnDefinition(columnDef);
            columns.add(columnPlan);
        }
        
        // 转换约束
        for (Constraint constraint : node.getConstraints()) {
            ConstraintPlan constraintPlan = convertConstraint(constraint);
            constraints.add(constraintPlan);
        }
        
        return new CreateTablePlan(node.getTableName(), columns, constraints, node.getStorageFormat());
    }
    
    @Override
    public ExecutionPlan visit(InsertStatement node) throws CompilationException {
        List<List<ExpressionPlan>> values = new ArrayList<>();
        
        // 转换值列表
        for (List<Expression> valueList : node.getValues()) {
            List<ExpressionPlan> expressionPlans = new ArrayList<>();
            for (Expression expr : valueList) {
                ExpressionPlan exprPlan = convertExpression(expr);
                expressionPlans.add(exprPlan);
            }
            values.add(expressionPlans);
        }
        
        return new InsertPlan(node.getTableName(), node.getColumns(), values);
    }
    
    @Override
    public ExecutionPlan visit(SelectStatement node) throws CompilationException {
        List<ExpressionPlan> selectList = new ArrayList<>();
        List<TablePlan> fromClause = null;
        ExpressionPlan whereClause = null;
        List<ExpressionPlan> groupByClause = null;
        ExpressionPlan havingClause = null;
        List<OrderByItem> orderByClause = null;
        LimitPlan limitClause = null;
        
        // 转换SELECT列表
        for (Expression expr : node.getSelectList()) {
            selectList.add(convertExpression(expr));
        }
        
        // 转换FROM子句
        if (node.getFromClause() != null) {
            fromClause = new ArrayList<>();
            for (TableReference tableRef : node.getFromClause()) {
                TablePlan tablePlan = convertTableReference(tableRef);
                fromClause.add(tablePlan);
            }
        }
        
        // 转换WHERE子句
        if (node.getWhereClause() != null) {
            whereClause = convertExpression(node.getWhereClause().getCondition());
        }
        
        // 转换GROUP BY子句
        if (node.getGroupByClause() != null) {
            groupByClause = new ArrayList<>();
            for (Expression expr : node.getGroupByClause().getExpressions()) {
                groupByClause.add(convertExpression(expr));
            }
        }
        
        // 转换HAVING子句
        if (node.getHavingClause() != null) {
            havingClause = convertExpression(node.getHavingClause().getCondition());
        }
        
        // 转换ORDER BY子句
        if (node.getOrderByClause() != null) {
            orderByClause = new ArrayList<>();
            for (OrderByClause.OrderByItem item : node.getOrderByClause().getItems()) {
                ExpressionPlan exprPlan = convertExpression(item.getExpression());
                OrderByItem.SortOrder order = convertSortOrder(item.getOrder());
                orderByClause.add(new OrderByItem(exprPlan, order));
            }
        }
        
        // 转换LIMIT子句
        if (node.getLimitClause() != null) {
            ExpressionPlan limit = convertExpression(node.getLimitClause().getLimit());
            ExpressionPlan offset = null;
            if (node.getLimitClause().getOffset() != null) {
                offset = convertExpression(node.getLimitClause().getOffset());
            }
            limitClause = new LimitPlan(limit, offset);
        }
        
        return new SelectPlan(node.isDistinct(), selectList, fromClause, whereClause,
                            groupByClause, havingClause, orderByClause, limitClause);
    }
    
    @Override
    public ExecutionPlan visit(UpdateStatement node) throws CompilationException {
        // 转换SET子句
        Map<String, ExpressionPlan> setClause = new java.util.HashMap<>();
        for (Map.Entry<String, Expression> entry : node.getSetClause().entrySet()) {
            setClause.put(entry.getKey(), convertExpression(entry.getValue()));
        }
        
        // 转换WHERE子句
        ExpressionPlan whereClause = null;
        if (node.getWhereClause() != null) {
            whereClause = convertExpression(node.getWhereClause().getCondition());
        }
        
        return new UpdatePlan(node.getTableName(), setClause, whereClause);
    }
    
    @Override
    public ExecutionPlan visit(DeleteStatement node) throws CompilationException {
        ExpressionPlan whereClause = null;
        
        if (node.getWhereClause() != null) {
            whereClause = convertExpression(node.getWhereClause().getCondition());
        }
        
        return new DeletePlan(node.getTableName(), whereClause);
    }
    
    @Override
    public ExecutionPlan visit(ColumnDefinition node) {
        // 列定义在CREATE TABLE中处理
        return null;
    }
    
    @Override
    public ExecutionPlan visit(Expression node) {
        return null; // 表达式不直接转换为ExecutionPlan
    }
    
    @Override
    public ExecutionPlan visit(BinaryExpression node) {
        return null; // 表达式不直接转换为ExecutionPlan
    }
    
    @Override
    public ExecutionPlan visit(UnaryExpression node) {
        return null; // 表达式不直接转换为ExecutionPlan
    }
    
    @Override
    public ExecutionPlan visit(LiteralExpression node) {
        return null; // 表达式不直接转换为ExecutionPlan
    }
    
    @Override
    public ExecutionPlan visit(IdentifierExpression node) {
        return null; // 表达式不直接转换为ExecutionPlan
    }
    
    @Override
    public ExecutionPlan visit(DotExpression node) {
        return null; // 表达式不直接转换为ExecutionPlan
    }
    
    @Override
    public ExecutionPlan visit(FunctionCallExpression node) {
        return null; // 表达式不直接转换为ExecutionPlan
    }
    
    @Override
    public ExecutionPlan visit(InExpression node) {
        return null; // IN表达式在WHERE子句中处理
    }
    
    @Override
    public ExecutionPlan visit(SubqueryExpression node) {
        return null; // 子查询表达式在WHERE子句中处理
    }
    
    @Override
    public ExecutionPlan visit(ColumnReference node) {
        return null; // 列引用不直接转换为ExecutionPlan
    }
    
    @Override
    public ExecutionPlan visit(TableReference node) {
        return null; // 表引用在SELECT中处理
    }
    
    @Override
    public ExecutionPlan visit(JoinClause node) {
        return null; // JOIN子句在SELECT中处理
    }
    
    @Override
    public ExecutionPlan visit(WhereClause node) {
        return null; // WHERE子句在SELECT中处理
    }
    
    @Override
    public ExecutionPlan visit(OrderByClause node) {
        return null; // ORDER BY子句在SELECT中处理
    }
    
    @Override
    public ExecutionPlan visit(GroupByClause node) {
        return null; // GROUP BY子句在SELECT中处理
    }
    
    @Override
    public ExecutionPlan visit(HavingClause node) {
        return null; // HAVING子句在SELECT中处理
    }
    
    @Override
    public ExecutionPlan visit(LimitClause node) {
        return null; // LIMIT子句在SELECT中处理
    }
    
    /**
     * 转换表达式
     */
    private ExpressionPlan convertExpression(Expression expr) throws CompilationException {
        if (expr instanceof LiteralExpression) {
            LiteralExpression literal = (LiteralExpression) expr;
            return new LiteralExpressionPlan(literal.getValue(), literal.getType().getValue());
        } else if (expr instanceof IdentifierExpression) {
            IdentifierExpression identifier = (IdentifierExpression) expr;
            return new IdentifierExpressionPlan(identifier.getName());
        } else if (expr instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) expr;
            ExpressionPlan left = convertExpression(binary.getLeft());
            ExpressionPlan right = convertExpression(binary.getRight());
            return new BinaryExpressionPlan(left, binary.getOperator().getValue(), right);
        } else if (expr instanceof UnaryExpression) {
            UnaryExpression unary = (UnaryExpression) expr;
            ExpressionPlan operand = convertExpression(unary.getOperand());
            return new BinaryExpressionPlan(null, unary.getOperator().getValue(), operand);
        } else if (expr instanceof DotExpression) {
            DotExpression dot = (DotExpression) expr;
            // 将点号表达式转换为表名.字段名的字符串形式
            return new IdentifierExpressionPlan(dot.getTableName() + "." + dot.getFieldName());
        } else if (expr instanceof FunctionCallExpression) {
            FunctionCallExpression func = (FunctionCallExpression) expr;
            // 简化处理：将函数调用转换为标识符表达式
            return new IdentifierExpressionPlan(func.getFunctionName() + "()");
        } else {
            throw new CompilationException("不支持的表达式类型: " + expr.getClass().getSimpleName(), 
                                        expr.getPosition(), "执行计划生成错误");
        }
    }
    
    /**
     * 转换列定义
     */
    private ColumnPlan convertColumnDefinition(ColumnDefinition columnDef) throws CompilationException {
        boolean notNull = false;
        boolean primaryKey = false;
        boolean unique = false;
        String defaultValue = null;
        boolean autoIncrement = false;
        
        for (Constraint constraint : columnDef.getConstraints()) {
            switch (constraint.getType()) {
                case NOT_NULL:
                    notNull = true;
                    break;
                case PRIMARY_KEY:
                    primaryKey = true;
                    break;
                case UNIQUE:
                    unique = true;
                    break;
                case DEFAULT:
                    defaultValue = constraint.getDefaultValue();
                    break;
                case AUTO_INCREMENT:
                    autoIncrement = true;
                    break;
            }
        }
        
        return new ColumnPlan(columnDef.getColumnName(), columnDef.getDataType(),
                            columnDef.getLength(), notNull, primaryKey, unique,
                            defaultValue, autoIncrement);
    }
    
    /**
     * 转换约束
     */
    private ConstraintPlan convertConstraint(Constraint constraint) throws CompilationException {
        ConstraintPlan.ConstraintType type = convertConstraintType(constraint.getType());
        return new ConstraintPlan(constraint.getName(), type, constraint.getColumns(),
                                constraint.getReferencedTable(), constraint.getReferencedColumns(),
                                constraint.getDefaultValue());
    }
    
    /**
     * 转换约束类型
     */
    private ConstraintPlan.ConstraintType convertConstraintType(Constraint.ConstraintType type) throws CompilationException {
        switch (type) {
            case PRIMARY_KEY:
                return ConstraintPlan.ConstraintType.PRIMARY_KEY;
            case FOREIGN_KEY:
                return ConstraintPlan.ConstraintType.FOREIGN_KEY;
            case UNIQUE:
                return ConstraintPlan.ConstraintType.UNIQUE;
            case NOT_NULL:
                return ConstraintPlan.ConstraintType.NOT_NULL;
            case DEFAULT:
                return ConstraintPlan.ConstraintType.DEFAULT;
            case AUTO_INCREMENT:
                return ConstraintPlan.ConstraintType.AUTO_INCREMENT;
            default:
                throw new CompilationException("未知的约束类型: " + type, null, "执行计划生成错误");
        }
    }
    
    /**
     * 转换表引用
     */
    private TablePlan convertTableReference(TableReference tableRef) throws CompilationException {
        List<JoinPlan> joins = new ArrayList<>();
        
        if (tableRef.getJoins() != null) {
            for (JoinClause joinClause : tableRef.getJoins()) {
                JoinPlan joinPlan = convertJoinClause(joinClause);
                joins.add(joinPlan);
            }
        }
        
        return new TablePlan(tableRef.getTableName(), tableRef.getAlias(), joins);
    }
    
    /**
     * 转换JOIN子句
     */
    private JoinPlan convertJoinClause(JoinClause joinClause) throws CompilationException {
        JoinPlan.JoinType joinType = convertJoinType(joinClause.getJoinType());
        ExpressionPlan condition = convertExpression(joinClause.getCondition());
        
        return new JoinPlan(joinType, joinClause.getTableName(), joinClause.getAlias(), condition);
    }
    
    /**
     * 转换JOIN类型
     */
    private JoinPlan.JoinType convertJoinType(JoinClause.JoinType type) {
        switch (type) {
            case INNER:
                return JoinPlan.JoinType.INNER;
            case LEFT:
                return JoinPlan.JoinType.LEFT;
            case RIGHT:
                return JoinPlan.JoinType.RIGHT;
            case FULL:
                return JoinPlan.JoinType.FULL;
            default:
                return JoinPlan.JoinType.INNER;
        }
    }
    
    /**
     * 转换排序顺序
     */
    private OrderByItem.SortOrder convertSortOrder(OrderByClause.SortOrder order) {
        switch (order) {
            case ASC:
                return OrderByItem.SortOrder.ASC;
            case DESC:
                return OrderByItem.SortOrder.DESC;
            default:
                return OrderByItem.SortOrder.ASC;
        }
    }
}
