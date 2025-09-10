package com.sqlcompiler.semantic;

import com.sqlcompiler.ast.*;
import com.sqlcompiler.catalog.*;
import com.sqlcompiler.exception.CompilationException;
import com.sqlcompiler.exception.SemanticException;
import com.sqlcompiler.lexer.Position;
import com.sqlcompiler.lexer.TokenType;

import java.util.*;

/**
 * 语义分析器
 * 负责进行存在性检查、类型一致性检查、列数/列序检查等语义分析
 */
public class SemanticAnalyzer implements ASTVisitor<Void> {
    private final Catalog catalog;
    private final List<String> errors;
    private final List<String> warnings;
    
    public SemanticAnalyzer(Catalog catalog) {
        this.catalog = catalog;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }
    
    /**
     * 执行语义分析
     */
    public SemanticAnalysisResult analyze(Statement statement) {
        errors.clear();
        warnings.clear();
        
        try {
            statement.accept(this);
        } catch (Exception e) {
            errors.add(e.toString());
        }
        
        return new SemanticAnalysisResult(errors, warnings);
    }
    
    @Override
    public Void visit(Statement node) throws CompilationException {
        // 基类，不需要处理
        return null;
    }
    
    @Override
    public Void visit(CreateTableStatement node) throws CompilationException {
        String tableName = node.getTableName();
        
        // 检查表是否已存在
        if (catalog.tableExists(tableName)) {
            errors.add(String.format("[语义错误, %s, 表 '%s' 已存在]", 
                                   node.getPosition(), tableName));
            return null;
        }
        
        // 创建表信息
        TableInfo tableInfo = new TableInfo(tableName);
        
        // 分析列定义
        Set<String> columnNames = new HashSet<>();
        for (ColumnDefinition columnDef : node.getColumns()) {
            String columnName = columnDef.getColumnName();
            
            // 检查列名重复
            if (columnNames.contains(columnName.toLowerCase())) {
                errors.add(String.format("[语义错误, %s, 列 '%s' 重复定义]", 
                                       columnDef.getPosition(), columnName));
                continue;
            }
            columnNames.add(columnName.toLowerCase());
            
            // 创建列信息
            ColumnInfo columnInfo = createColumnInfo(columnDef);
            tableInfo.addColumn(columnInfo);
        }
        
        // 分析约束
        for (Constraint constraint : node.getConstraints()) {
            ConstraintInfo constraintInfo = createConstraintInfo(constraint, tableInfo);
            tableInfo.addConstraint(constraintInfo);
        }
        
        // 验证约束
        validateConstraints(tableInfo);
        
        // 将表信息添加到目录
        catalog.addTable(tableInfo);
        
        return null;
    }
    
    @Override
    public Void visit(InsertStatement node) throws CompilationException {
        String tableName = node.getTableName();
        
        // 检查表是否存在
        TableInfo tableInfo = catalog.getTable(tableName);
        if (tableInfo == null) {
            errors.add(String.format("[语义错误, %s, 表 '%s' 不存在]", 
                                   node.getPosition(), tableName));
            return null;
        }
        
        List<String> insertColumns = node.getColumns();
        List<List<Expression>> values = node.getValues();
        
        // 如果没有指定列名，使用所有列
        if (insertColumns.isEmpty()) {
            insertColumns = tableInfo.getColumnNames();
        }
        
        // 检查列是否存在
        for (String columnName : insertColumns) {
            if (!tableInfo.columnExists(columnName)) {
                errors.add(String.format("[语义错误, %s, 列 '%s' 在表 '%s' 中不存在]", 
                                       node.getPosition(), columnName, tableName));
            }
        }
        
        // 检查列数一致性
        for (int i = 0; i < values.size(); i++) {
            List<Expression> valueList = values.get(i);
            if (valueList.size() != insertColumns.size()) {
                errors.add(String.format("[语义错误, %s, 第%d行值的数量(%d)与列数(%d)不匹配]", 
                                       node.getPosition(), i + 1, valueList.size(), insertColumns.size()));
            }
        }
        
        // 检查类型一致性
        for (int i = 0; i < values.size(); i++) {
            List<Expression> valueList = values.get(i);
            for (int j = 0; j < valueList.size() && j < insertColumns.size(); j++) {
                Expression expr = valueList.get(j);
                String columnName = insertColumns.get(j);
                ColumnInfo columnInfo = tableInfo.getColumn(columnName);
                
                if (columnInfo != null) {
                    validateExpressionType(expr, columnInfo, node.getPosition());
                }
            }
        }
        
        return null;
    }
    
    @Override
    public Void visit(SelectStatement node) throws CompilationException {
        // 检查FROM子句中的表是否存在
        if (node.getFromClause() != null) {
            for (TableReference tableRef : node.getFromClause()) {
                String tableName = tableRef.getTableName();
                if (!catalog.tableExists(tableName)) {
                    errors.add(String.format("[语义错误, %s, 表 '%s' 不存在]", 
                                           tableRef.getPosition(), tableName));
                }
            }
        }
        
        // 分析SELECT列表中的表达式
        for (Expression expr : node.getSelectList()) {
            validateExpression(expr, node.getFromClause(), node.getPosition());
        }
        
        // 分析WHERE子句
        if (node.getWhereClause() != null) {
            validateExpression(node.getWhereClause().getCondition(), 
                             node.getFromClause(), node.getPosition());
        }
        
        // 分析GROUP BY子句
        if (node.getGroupByClause() != null) {
            for (Expression expr : node.getGroupByClause().getExpressions()) {
                validateExpression(expr, node.getFromClause(), node.getPosition());
            }
        }
        
        // 分析HAVING子句
        if (node.getHavingClause() != null) {
            validateExpression(node.getHavingClause().getCondition(), 
                             node.getFromClause(), node.getPosition());
        }
        
        // 分析ORDER BY子句
        if (node.getOrderByClause() != null) {
            for (OrderByClause.OrderByItem item : node.getOrderByClause().getItems()) {
                validateExpression(item.getExpression(), node.getFromClause(), node.getPosition());
            }
        }
        
        return null;
    }
    
    @Override
    public Void visit(DeleteStatement node) throws CompilationException {
        String tableName = node.getTableName();
        
        // 检查表是否存在
        if (!catalog.tableExists(tableName)) {
            errors.add(String.format("[语义错误, %s, 表 '%s' 不存在]", 
                                   node.getPosition(), tableName));
            return null;
        }
        
        // 分析WHERE子句
        if (node.getWhereClause() != null) {
            List<TableReference> fromClause = Arrays.asList(
                new TableReference(tableName, null, new ArrayList<>(), node.getPosition())
            );
            validateExpression(node.getWhereClause().getCondition(), fromClause, node.getPosition());
        }
        
        return null;
    }
    
    @Override
    public Void visit(ColumnDefinition node) throws CompilationException {
        // 列定义在CREATE TABLE中处理
        return null;
    }
    
    @Override
    public Void visit(Expression node) throws CompilationException {
        // 表达式基类，不需要处理
        return null;
    }
    
    @Override
    public Void visit(BinaryExpression node) throws CompilationException {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        return null;
    }
    
    @Override
    public Void visit(UnaryExpression node) throws CompilationException {
        node.getOperand().accept(this);
        return null;
    }
    
    @Override
    public Void visit(LiteralExpression node) throws CompilationException {
        // 字面量表达式不需要额外验证
        return null;
    }
    
    @Override
    public Void visit(IdentifierExpression node) throws CompilationException {
        // 标识符表达式在validateExpression中处理
        return null;
    }
    
    @Override
    public Void visit(FunctionCallExpression node) throws CompilationException {
        // 验证函数参数
        for (Expression arg : node.getArguments()) {
            arg.accept(this);
        }
        return null;
    }
    
    @Override
    public Void visit(ColumnReference node) throws CompilationException {
        // 列引用在validateExpression中处理
        return null;
    }
    
    @Override
    public Void visit(TableReference node) throws CompilationException {
        // 表引用在SELECT/DELETE中处理
        return null;
    }
    
    @Override
    public Void visit(JoinClause node) throws CompilationException {
        // JOIN子句在SELECT中处理
        return null;
    }
    
    @Override
    public Void visit(WhereClause node) throws CompilationException {
        node.getCondition().accept(this);
        return null;
    }
    
    @Override
    public Void visit(OrderByClause node) throws CompilationException {
        for (OrderByClause.OrderByItem item : node.getItems()) {
            item.getExpression().accept(this);
        }
        return null;
    }
    
    @Override
    public Void visit(GroupByClause node) throws CompilationException {
        for (Expression expr : node.getExpressions()) {
            expr.accept(this);
        }
        return null;
    }
    
    @Override
    public Void visit(HavingClause node) throws CompilationException {
        node.getCondition().accept(this);
        return null;
    }
    
    @Override
    public Void visit(LimitClause node) throws CompilationException {
        node.getLimit().accept(this);
        if (node.getOffset() != null) {
            node.getOffset().accept(this);
        }
        return null;
    }
    
    /**
     * 创建列信息
     */
    private ColumnInfo createColumnInfo(ColumnDefinition columnDef) {
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
        
        return new ColumnInfo(columnDef.getColumnName(), columnDef.getDataType(),
                            columnDef.getLength(), notNull, primaryKey, unique,
                            defaultValue, autoIncrement);
    }
    
    /**
     * 创建约束信息
     */
    private ConstraintInfo createConstraintInfo(Constraint constraint, TableInfo tableInfo) {
        return new ConstraintInfo(constraint.getName(), 
                                convertConstraintType(constraint.getType()),
                                constraint.getColumns(),
                                constraint.getReferencedTable(),
                                constraint.getReferencedColumns(),
                                constraint.getDefaultValue());
    }
    
    /**
     * 转换约束类型
     */
    private ConstraintInfo.ConstraintType convertConstraintType(Constraint.ConstraintType type) {
        switch (type) {
            case PRIMARY_KEY:
                return ConstraintInfo.ConstraintType.PRIMARY_KEY;
            case FOREIGN_KEY:
                return ConstraintInfo.ConstraintType.FOREIGN_KEY;
            case UNIQUE:
                return ConstraintInfo.ConstraintType.UNIQUE;
            case NOT_NULL:
                return ConstraintInfo.ConstraintType.NOT_NULL;
            case DEFAULT:
                return ConstraintInfo.ConstraintType.DEFAULT;
            case AUTO_INCREMENT:
                return ConstraintInfo.ConstraintType.AUTO_INCREMENT;
            default:
                throw new IllegalArgumentException("未知的约束类型: " + type);
        }
    }
    
    /**
     * 验证约束
     */
    private void validateConstraints(TableInfo tableInfo) {
        // 验证主键约束
        List<String> primaryKeyColumns = tableInfo.getPrimaryKeyColumns();
        if (primaryKeyColumns.isEmpty()) {
            warnings.add(String.format("[警告, 表 '%s' 没有主键]", tableInfo.getName()));
        }
        
        // 验证外键约束
        for (ConstraintInfo constraint : tableInfo.getForeignKeyConstraints()) {
            String refTable = constraint.getReferencedTable();
            if (!catalog.tableExists(refTable)) {
                errors.add(String.format("[语义错误, 外键引用的表 '%s' 不存在]", refTable));
            }
        }
    }
    
    /**
     * 验证表达式
     */
    private void validateExpression(Expression expr, List<TableReference> fromClause, Position position) throws CompilationException {
        if (expr instanceof IdentifierExpression) {
            IdentifierExpression idExpr = (IdentifierExpression) expr;
            String columnName = idExpr.getName();
            
            // 特殊处理 * 通配符
            if ("*".equals(columnName)) {
                // * 通配符不需要检查列存在性
                return;
            }
            
            // 检查列是否存在于FROM子句的表中
            boolean columnFound = false;
            for (TableReference tableRef : fromClause) {
                TableInfo tableInfo = catalog.getTable(tableRef.getTableName());
                if (tableInfo != null && tableInfo.columnExists(columnName)) {
                    columnFound = true;
                    break;
                }
            }
            
            if (!columnFound) {
                errors.add(String.format("[语义错误, %s, 列 '%s' 不存在]", position, columnName));
            }
        } else {
            expr.accept(this);
        }
    }
    
    /**
     * 验证表达式类型
     */
    private void validateExpressionType(Expression expr, ColumnInfo columnInfo, Position position) {
        if (expr instanceof LiteralExpression) {
            LiteralExpression literal = (LiteralExpression) expr;
            String value = literal.getValue();
            
            if (!columnInfo.isCompatibleWith(value)) {
                errors.add(String.format("[语义错误, %s, 值 '%s' 与列 '%s' 的类型 '%s' 不兼容]", 
                                       position, value, columnInfo.getName(), columnInfo.getDataType()));
            }
        }
    }
}
