package com.sqlcompiler.semantic;

import com.sqlcompiler.ast.*;
import com.sqlcompiler.catalog.*;
import com.sqlcompiler.exception.CompilationException;
// import com.sqlcompiler.exception.SemanticException;
import com.sqlcompiler.lexer.Position;
// import com.sqlcompiler.lexer.TokenType;

import java.util.*;

/**
 * 语义分析器
 * 负责进行存在性检查、类型一致性检查、列数/列序检查等语义分析
 */
public class SemanticAnalyzer implements ASTVisitor<Void> {
    private final Catalog catalog;
    private final List<String> errors;
    private final List<String> warnings;
    // 临时表目录：用于批量语句分析时，按顺序记录本批次内新创建的表，避免影响真实目录
    private final Map<String, TableInfo> temporaryTables;
    
    public SemanticAnalyzer(Catalog catalog) {
        this.catalog = catalog;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.temporaryTables = new HashMap<>();
    }
    
    /**
     * 执行语义分析
     */
    public SemanticAnalysisResult analyze(Statement statement) {
        errors.clear();
        warnings.clear();
        temporaryTables.clear();
        
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
    public Void visit(BatchStatement node) throws CompilationException {
        // 按顺序验证批量语句中的每个语句
        // 对于CREATE TABLE，先进行基本校验，再把表登记到临时目录，
        // 使后续语句（如INSERT/SELECT）在同一批次内可见
        for (Statement stmt : node.getStatements()) {
            if (stmt instanceof CreateTableStatement) {
                // 先对CREATE语句本身做校验
                stmt.accept(this);

                CreateTableStatement create = (CreateTableStatement) stmt;
                String tableName = create.getTableName();

                // 如果真实目录已存在，则错误已经在visit(CreateTableStatement)中记录，这里跳过登记
                if (!catalog.tableExists(tableName)) {
                    // 构造一个临时的TableInfo，仅用于本次批量语义分析
                    TableInfo tableInfo = new TableInfo(tableName);

                    // 添加列
                    for (ColumnDefinition columnDef : create.getColumns()) {
                        ColumnInfo columnInfo = createColumnInfo(columnDef);
                        tableInfo.addColumn(columnInfo);
                    }

                    // 添加约束
                    for (Constraint constraint : create.getConstraints()) {
                        ConstraintInfo constraintInfo = createConstraintInfo(constraint, tableInfo);
                        tableInfo.addConstraint(constraintInfo);
                    }

                    temporaryTables.put(tableName.toLowerCase(), tableInfo);
                }
            } else {
                // 其他语句正常校验，但其表/列检查会同时参考temporaryTables
                stmt.accept(this);
            }
        }
        return null;
    }
    @Override
    public Void visit(CreateViewStatement node) throws CompilationException {
        // 检查视图名是否已存在（这里简化处理，实际应该有专门的视图目录）
        String viewName = node.getViewName();
        if (catalog.tableExists(viewName)) {
            errors.add(String.format("[语义错误, %s, 名称 '%s' 已被表使用]", 
                                   node.getPosition(), viewName));
            return null;
        }
        
        // 分析视图的SELECT语句
        SelectStatement selectStatement = node.getSelectStatement();
        selectStatement.accept(this);
        
        return null;
    }
    
    @Override
    public Void visit(DropViewStatement node) throws CompilationException {
        // 这里简化处理，实际应该检查视图是否存在
        // 如果不是IF EXISTS语义，应该验证视图存在性
        if (!node.isIfExists()) {
            // 应该检查视图是否存在，这里暂时跳过
        }
        
        return null;
    }
    
    @Override
    public Void visit(CreateTableStatement node) throws CompilationException {
        String tableName = node.getTableName();
        
        // 检查表是否已存在
        if (catalog.tableExists(tableName) || temporaryTables.containsKey(tableName.toLowerCase())) {
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
        
        // 注意：不在语义分析阶段添加表到目录，这应该在执行阶段完成
        // catalog.addTable(tableInfo);
        
        return null;
    }
    
    @Override
    public Void visit(InsertStatement node) throws CompilationException {
        String tableName = node.getTableName();
        
        // 检查表是否存在
        TableInfo tableInfo = temporaryTables.getOrDefault(tableName.toLowerCase(), catalog.getTable(tableName));
        if (tableInfo == null) {
            errors.add(String.format("❌ 语义错误\n   位置: 第%d行第%d列\n   错误: 表 '%s' 不存在", 
                                   node.getPosition().getLine(), node.getPosition().getColumn(), tableName));
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
                errors.add(String.format("❌ 语义错误\n   位置: 第%d行第%d列\n   错误: 列 '%s' 在表 '%s' 中不存在", 
                                       node.getPosition().getLine(), node.getPosition().getColumn(), columnName, tableName));
            }
        }
        
        // 检查列数一致性
        for (int i = 0; i < values.size(); i++) {
            List<Expression> valueList = values.get(i);
            if (valueList.size() != insertColumns.size()) {
                errors.add(String.format("❌ 语义错误\n   位置: 第%d行第%d列\n   错误: 第%d行值的数量(%d)与列数(%d)不匹配", 
                                       node.getPosition().getLine(), node.getPosition().getColumn(), i + 1, valueList.size(), insertColumns.size()));
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
        // 检查FROM子句中的表或视图是否存在
        if (node.getFromClause() != null) {
            for (TableReference tableRef : node.getFromClause()) {
                String tableName = tableRef.getTableName();
                boolean existsInTemp = temporaryTables.containsKey(tableName.toLowerCase());
                if (!existsInTemp && !catalog.tableOrViewExists(tableName)) {
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
    public Void visit(UpdateStatement node) throws CompilationException {
        String tableName = node.getTableName();
        
        // 检查表是否存在
        TableInfo tableInfo = temporaryTables.getOrDefault(tableName.toLowerCase(), catalog.getTable(tableName));
        if (tableInfo == null) {
            errors.add(String.format("❌ 语义错误\n   位置: 第%d行第%d列\n   错误: 表 '%s' 不存在", 
                                   node.getPosition().getLine(), node.getPosition().getColumn(), tableName));
            return null;
        }
        
        // 检查SET子句中的列是否存在
        for (String columnName : node.getSetClause().keySet()) {
            if (!tableInfo.columnExists(columnName)) {
                errors.add(String.format("❌ 语义错误\n   位置: 第%d行第%d列\n   错误: 列 '%s' 在表 '%s' 中不存在", 
                                       node.getPosition().getLine(), node.getPosition().getColumn(), columnName, tableName));
            }
        }
        
        // 检查SET子句中的表达式类型
        for (Map.Entry<String, Expression> entry : node.getSetClause().entrySet()) {
            String columnName = entry.getKey();
            Expression expr = entry.getValue();
            ColumnInfo columnInfo = tableInfo.getColumn(columnName);
            
            if (columnInfo != null) {
                validateExpressionType(expr, columnInfo, node.getPosition());
            }
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
    public Void visit(DeleteStatement node) throws CompilationException {
        String tableName = node.getTableName();
        
        // 检查表是否存在
        if (!catalog.tableExists(tableName) && !temporaryTables.containsKey(tableName.toLowerCase())) {
            errors.add(String.format("❌ 语义错误\n   位置: 第%d行第%d列\n   错误: 表 '%s' 不存在", 
                                   node.getPosition().getLine(), node.getPosition().getColumn(), tableName));
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
    public Void visit(DotExpression node) throws CompilationException {
        // 点号表达式在validateExpression中处理
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
    public Void visit(AliasExpression node) throws CompilationException {
        // 验证别名表达式中的内部表达式
        node.getExpression().accept(this);
        return null;
    }
    
    @Override
    public Void visit(InExpression node) throws CompilationException {
        // 验证左侧表达式
        node.getLeft().accept(this);
        
        // 验证右侧（子查询或值列表）
        if (node.isSubquery()) {
            // 验证子查询
            node.getSubquery().accept(this);
        } else {
            // 验证值列表
            for (Expression value : node.getValues()) {
                value.accept(this);
            }
        }
        return null;
    }
    
    @Override
    public Void visit(SubqueryExpression node) throws CompilationException {
        // 验证子查询
        node.getSubquery().accept(this);
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
    
    @Override
    public Void visit(SelectListClause node) throws CompilationException {
        for (Expression expr : node.getExpressions()) {
            expr.accept(this);
        }
        return null;
    }
    
    @Override
    public Void visit(FromClause node) throws CompilationException {
        for (TableReference tableRef : node.getTableReferences()) {
            tableRef.accept(this);
        }
        return null;
    }
    
    @Override
    public Void visit(CreateFunctionStatement node) throws CompilationException {
        // Function semantic validation could be added here
        return null;
    }
    
    @Override
    public Void visit(CallStatement node) throws CompilationException {
        // Call statement semantic validation could be added here
        for (Expression arg : node.getArguments()) {
            arg.accept(this);
        }
        return null;
    }
    
    @Override
    public Void visit(DropFunctionStatement node) throws CompilationException {
        // Drop function semantic validation could be added here
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
                case FOREIGN_KEY:
                    // 列级外键在CREATE TABLE的表级处理中验证，这里忽略布尔标记
                    break;
                case DEFAULT:
                    defaultValue = constraint.getDefaultValue();
                    break;
                case AUTO_INCREMENT:
                    autoIncrement = true;
                    break;
                case CHECK:
                    // 列级CHECK当前不做表达式验证
                    break;
            }
        }
        
        return new ColumnInfo(columnDef.getColumnName(), columnDef.getDataType(),
                            columnDef.getLength() != null ? columnDef.getLength() : 0, 
                            !notNull, primaryKey, unique,
                            autoIncrement, defaultValue, notNull);
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
                                null,
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
            case CHECK:
                // 目前不执行CHECK表达式验证，这里映射为UNIQUE之外的通用类型或保留DEFAULT
                // 为了覆盖枚举分支，先返回UNIQUE（不影响后续执行阶段）
                return ConstraintInfo.ConstraintType.UNIQUE;
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
                TableInfo tableInfo = temporaryTables.getOrDefault(tableRef.getTableName().toLowerCase(),
                    catalog.getTable(tableRef.getTableName()));
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
    
    @Override
    public Void visit(DropTableStatement node) throws CompilationException {
        String tableName = node.getTableName();
        
        // 检查表是否存在（除非使用了IF EXISTS）
        if (!node.isIfExists() && !catalog.tableExists(tableName)) {
            errors.add(String.format("[语义错误, %s, 表 '%s' 不存在]", 
                                   node.getPosition(), tableName));
        }
        
        return null;
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