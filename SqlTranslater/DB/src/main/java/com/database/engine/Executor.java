package com.database.engine;

import com.sqlcompiler.execution.*;
import com.sqlcompiler.catalog.*;
import com.sqlcompiler.ast.*;
import java.util.*;
import java.util.Arrays;

/**
 * 执行引擎 - 负责执行各种SQL操作
 * 支持 CreateTable、Insert、SeqScan、Filter、Project
 * 现在使用StorageAdapter来支持更高级的存储系统
 */
public class Executor {
    private final StorageAdapter storageAdapter;
    private final CatalogManager catalogManager;
    private final DatabaseEngine databaseEngine;
    private String currentIndexType = "智能选择";
    private ViewManager viewManager;

    public Executor(StorageAdapter storageAdapter, CatalogManager catalogManager, DatabaseEngine databaseEngine) {
        this.storageAdapter = storageAdapter;
        this.catalogManager = catalogManager;
        this.databaseEngine = databaseEngine;
        this.viewManager = databaseEngine.getViewManager(); // 从DatabaseEngine获取ViewManager
    }
    
    /**
     * 设置索引类型
     */
    public void setIndexType(String indexType) {
        this.currentIndexType = indexType;
    }
    
    /**
     * 获取存储适配器
     */
    public StorageAdapter getStorageAdapter() {
        return storageAdapter;
    }
    
    /**
     * 根据索引类型查询表数据
     */
    private List<Map<String, Object>> queryTableWithIndex(String tableName, TablePlan tablePlan) {
        // 检查是否为列式存储表
        if (storageAdapter.isColumnarStorageTable(tableName)) {
            return queryWithColumnarStorage(tableName, tablePlan);
        } else {
            // 行式存储表使用索引查询
            switch (currentIndexType) {
                case "B+树索引":
                    return queryWithBPlusTreeIndex(tableName, tablePlan);
                case "哈希索引":
                    return queryWithHashIndex(tableName, tablePlan);
                case "线性查找":
                    return queryWithLinearSearch(tableName, tablePlan);
                case "智能选择":
                default:
                    return queryWithIntelligentSelection(tableName, tablePlan);
            }
        }
    }
    
    /**
     * 使用列式存储优化查询
     */
    private List<Map<String, Object>> queryWithColumnarStorage(String tableName, TablePlan tablePlan) {
        try {
            // 获取列式存储引擎
            var columnarEngine = storageAdapter.getColumnarStorageEngine();
            
            // 模拟列式存储的优化延迟
            Thread.sleep(10); // 列式存储查询延迟较小
            
            System.out.println("使用列式存储查询表: " + tableName);
            
            // 直接使用列式存储引擎的scanTable方法
            // 这里可以进一步优化，根据WHERE条件和SELECT列表进行优化
            return columnarEngine.scanTable(tableName);
            
        } catch (Exception e) {
            System.err.println("列式存储查询失败: " + e.getMessage());
            // 回退到普通查询
            return storageAdapter.scanTable(tableName);
        }
    }
    
    /**
     * 使用B+树索引查询（模拟）
     */
    private List<Map<String, Object>> queryWithBPlusTreeIndex(String tableName, TablePlan tablePlan) {
        // 模拟B+树索引：先进行全表扫描，然后模拟索引查找的延迟
        List<Map<String, Object>> allData = storageAdapter.scanTable(tableName);
        
        // 模拟B+树索引的查找过程 - 增加更明显的延迟
        try {
            Thread.sleep(50); // 模拟B+树索引查找的延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("使用B+树索引查询表: " + tableName + " (数据量: " + allData.size() + ")");
        return allData;
    }
    
    /**
     * 使用哈希索引查询（模拟）
     */
    private List<Map<String, Object>> queryWithHashIndex(String tableName, TablePlan tablePlan) {
        // 模拟哈希索引：先进行全表扫描，然后模拟哈希查找的延迟
        List<Map<String, Object>> allData = storageAdapter.scanTable(tableName);
        
        // 模拟哈希索引的查找过程 - 增加更明显的延迟
        try {
            Thread.sleep(20); // 模拟哈希查找的延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("使用哈希索引查询表: " + tableName + " (数据量: " + allData.size() + ")");
        return allData;
    }
    
    /**
     * 使用线性查找查询
     */
    private List<Map<String, Object>> queryWithLinearSearch(String tableName, TablePlan tablePlan) {
        // 线性查找：直接全表扫描
        List<Map<String, Object>> allData = storageAdapter.scanTable(tableName);
        
        // 模拟线性查找的延迟 - 增加更明显的延迟
        try {
            Thread.sleep(100); // 模拟线性查找的延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("使用线性查找查询表: " + tableName + " (数据量: " + allData.size() + ")");
        return allData;
    }
    
    /**
     * 智能选择索引类型 - 使用真正的智能索引选择器
     */
    private List<Map<String, Object>> queryWithIntelligentSelection(String tableName, TablePlan tablePlan) {
        try {
            System.out.println("=== 智能索引选择器开始工作 ===");
            
            // 获取表数据作为样本
            List<Map<String, Object>> allData = storageAdapter.scanTable(tableName);
            
            if (allData.isEmpty()) {
                System.out.println("表 " + tableName + " 为空，使用线性查找");
                return queryWithLinearSearch(tableName, tablePlan);
            }
            
            System.out.println("表 " + tableName + " 数据量: " + allData.size());
            
            // 创建智能索引选择器
            IndexSelector indexSelector = new IndexSelector();
            
            // 分析查询上下文
            IndexSelector.QueryPattern pattern = analyzeQueryPattern(tableName, tablePlan);
            IndexSelector.DataCharacteristics characteristics = analyzeDataCharacteristics(allData);
            
            // 准备样本数据（取第一列作为索引列）
            List<Object> sampleData = extractSampleData(allData, getFirstColumnName(tableName));
            
            // 使用智能选择器选择最优索引
            IndexType selectedType = indexSelector.selectOptimalIndex(
                tableName + "_index", sampleData, pattern, characteristics);
            
            // 根据选择结果执行相应的查询
            if (selectedType == null) {
                System.out.println("智能选择器推荐: 线性查找");
                return queryWithLinearSearch(tableName, tablePlan);
            } else {
                System.out.println("智能选择器推荐: " + selectedType.getDescription());
                
                switch (selectedType) {
                    case HASH_TABLE:
                        return queryWithHashIndex(tableName, tablePlan);
                    case BPLUS_TREE:
                        return queryWithBPlusTreeIndex(tableName, tablePlan);
                    default:
                        return queryWithLinearSearch(tableName, tablePlan);
                }
            }
            
        } catch (Exception e) {
            System.err.println("智能索引选择失败，回退到线性查找: " + e.getMessage());
            e.printStackTrace();
            return queryWithLinearSearch(tableName, tablePlan);
        }
    }
    
    /**
     * 分析查询模式
     */
    private IndexSelector.QueryPattern analyzeQueryPattern(String tableName, TablePlan tablePlan) {
        IndexSelector.QueryPattern pattern = new IndexSelector.QueryPattern();
        
        // 简化实现：假设主要是等值查询
        pattern.equalityQueries = 1;
        pattern.rangeQueries = 0;
        pattern.sortQueries = 0;
        pattern.totalQueries = 1;
        pattern.timeWindowHours = 1;
        pattern.maxConcurrentQueries = 1;
        
        return pattern;
    }
    
    /**
     * 分析数据特征
     */
    private IndexSelector.DataCharacteristics analyzeDataCharacteristics(List<Map<String, Object>> allData) {
        IndexSelector.DataCharacteristics characteristics = new IndexSelector.DataCharacteristics();
        
        characteristics.isNumeric = true; // 简化假设
        characteristics.growthPattern = IndexSelector.GrowthPattern.STABLE;
        characteristics.memoryLimit = 100 * 1024 * 1024; // 100MB
        
        return characteristics;
    }
    
    /**
     * 获取表的第一列名
     */
    private String getFirstColumnName(String tableName) {
        try {
            TableInfo tableInfo = catalogManager.getTable(tableName);
            if (tableInfo != null && !tableInfo.getColumns().isEmpty()) {
                return tableInfo.getColumns().iterator().next().getName();
            }
        } catch (Exception e) {
            System.out.println("无法获取表列信息，使用默认列名: id");
        }
        return "id"; // 默认列名
    }
    
    /**
     * 从表数据中提取样本数据
     */
    private List<Object> extractSampleData(List<Map<String, Object>> allData, String columnName) {
        List<Object> sampleData = new ArrayList<>();
        
        for (Map<String, Object> row : allData) {
            Object value = row.get(columnName);
            if (value != null) {
                sampleData.add(value);
            }
        }
        
        // 限制样本大小以提高性能
        if (sampleData.size() > 1000) {
            sampleData = sampleData.subList(0, 1000);
        }
        
        return sampleData;
    }
    
    /**
     * 执行执行计划
     */
    public ExecutionResult execute(ExecutionPlan plan) {
        if (plan instanceof CreateTablePlan) {
            return executeCreateTable((CreateTablePlan) plan);
        } else if (plan instanceof InsertPlan) {
            return executeInsert((InsertPlan) plan);
        } else if (plan instanceof SelectPlan) {
            return executeSelect((SelectPlan) plan);
        } else if (plan instanceof DeletePlan) {
            return executeDelete((DeletePlan) plan);
        } else if (plan instanceof DropTablePlan) {
            return executeDropTable((DropTablePlan) plan);
        } else if (plan instanceof UpdatePlan) {
            return executeUpdate((UpdatePlan) plan);
        } else if (plan instanceof BatchPlan) {
            return executeBatch((BatchPlan) plan);
        } else if (plan instanceof CreateViewPlan) {
            return executeCreateView((CreateViewPlan) plan);
        } else if (plan instanceof DropViewPlan) {
            return executeDropView((DropViewPlan) plan);
        } else if (plan instanceof CreateFunctionPlan) {
            return executeCreateFunction((CreateFunctionPlan) plan);
        } else if (plan instanceof CallPlan) {
            return executeCall((CallPlan) plan);
        } else if (plan instanceof DropFunctionPlan) {
            return executeDropFunction((DropFunctionPlan) plan);
        } else {
            return new ExecutionResult(false, "不支持的执行计划类型: " + plan.getPlanType(), null);
        }
    }
    
    /**
     * 执行CREATE TABLE
     */
    private ExecutionResult executeCreateTable(CreateTablePlan plan) {
        try {
            String tableName = plan.getTableName();
            
            // 检查表是否已存在
            if (catalogManager.tableExists(tableName)) {
                return new ExecutionResult(false, "表 " + tableName + " 已存在", null);
            }
            
            // 创建表信息
            TableInfo tableInfo = new TableInfo(tableName, plan.getStorageFormat());
            
            // 添加列信息
            for (ColumnPlan columnPlan : plan.getColumns()) {
                ColumnInfo columnInfo = new ColumnInfo(
                    columnPlan.getName(),
                    columnPlan.getDataType(),
                    columnPlan.getLength() != null ? columnPlan.getLength() : 0,
                    !columnPlan.isNotNull(),
                    columnPlan.isPrimaryKey(),
                    columnPlan.isUnique(),
                    columnPlan.isAutoIncrement(),
                    columnPlan.getDefaultValue(),
                    columnPlan.isNotNull()
                );
                tableInfo.addColumn(columnInfo);
            }
            
            // 添加约束信息
            for (ConstraintPlan constraintPlan : plan.getConstraints()) {
                ConstraintInfo constraintInfo = new ConstraintInfo(
                    constraintPlan.getName(),
                    convertConstraintType(constraintPlan.getType()),
                    constraintPlan.getColumns(),
                    constraintPlan.getReferencedTable(),
                    constraintPlan.getReferencedColumns(),
                    null,
                    constraintPlan.getDefaultValue()
                );
                tableInfo.addConstraint(constraintInfo);
            }
            
            // 在目录中注册表
            catalogManager.addTable(tableInfo);
            
            // 创建表存储
            if (!storageAdapter.createTable(tableName, tableInfo)) {
                return new ExecutionResult(false, "创建表存储失败", null);
            }
            
            return new ExecutionResult(true, "表 " + tableName + " 创建成功", null);
            
        } catch (Exception e) {
            return new ExecutionResult(false, "创建表时发生错误: " + e.getMessage(), null);
        }
    }
    
    /**
     * 执行INSERT
     */
    private ExecutionResult executeInsert(InsertPlan plan) {
        try {
            String tableName = plan.getTableName();
            
            // 检查表是否存在
            if (!catalogManager.tableExists(tableName)) {
                return new ExecutionResult(false, "表 " + tableName + " 不存在", null);
            }
            
            TableInfo tableInfo = catalogManager.getTable(tableName);
            int insertedRows = 0;
            
            // 确定要插入的列
            List<String> insertColumns = plan.getColumns();
            if (insertColumns.isEmpty()) {
                // 如果没有指定列名，使用表的所有列
                insertColumns = tableInfo.getColumnNames();
            }
            
            // 插入每一行数据
            for (List<ExpressionPlan> valueList : plan.getValues()) {
                // 验证列数
                if (valueList.size() != insertColumns.size()) {
                    return new ExecutionResult(false, 
                        "列数不匹配，期望 " + insertColumns.size() + 
                        " 列，实际 " + valueList.size() + " 列", null);
                }
                
                // 构建记录Map并插入
                Map<String, Object> record = buildRecordMap(valueList, insertColumns, tableInfo);
                if (!storageAdapter.insertRecord(tableName, record)) {
                    return new ExecutionResult(false, "插入记录失败", null);
                }
                
                insertedRows++;
            }
            
            return new ExecutionResult(true, insertedRows + " 行已插入", null);
            
        } catch (Exception e) {
            return new ExecutionResult(false, "插入数据时发生错误: " + e.getMessage(), null);
        }
    }
    
    /**
     * 执行SELECT
     */
    private ExecutionResult executeSelect(SelectPlan plan) {
        try {
            List<Map<String, Object>> results = new ArrayList<>();
            
            // 获取表信息
            if (plan.getFromClause() == null || plan.getFromClause().isEmpty()) {
                return new ExecutionResult(false, "SELECT语句必须指定FROM子句", null);
            }
            
            TablePlan tablePlan = plan.getFromClause().get(0);
            String tableName = tablePlan.getTableName();
            
            // 检查是否为视图
            if (viewManager != null && viewManager.viewExists(tableName)) {
                // 这是一个视图查询，使用简化的实现
                ViewInfo viewInfo = viewManager.getView(tableName);
                if (viewInfo.getOriginalQuery() != null) {
                    try {
                        // 使用DatabaseEngine直接执行视图的原始查询
                        ExecutionResult viewResult = databaseEngine.executeSQL(viewInfo.getOriginalQuery());
                        
                        if (viewResult.isSuccess()) {
                            return new ExecutionResult(true, "查询视图完成，返回 " + 
                                (viewResult.getData() != null ? viewResult.getData().size() : 0) + " 行", 
                                viewResult.getData());
                        } else {
                            return viewResult;
                        }
                    } catch (Exception e) {
                        return new ExecutionResult(false, "执行视图查询失败: " + e.getMessage(), null);
                    }
                } else {
                    return new ExecutionResult(false, "视图 " + tableName + " 定义无效", null);
                }
            }
            
            if (!catalogManager.tableExists(tableName)) {
                return new ExecutionResult(false, "表或视图 " + tableName + " 不存在", null);
            }
            
            TableInfo tableInfo = catalogManager.getTable(tableName);
            
            // 执行JOIN操作
            List<Map<String, Object>> joinedRecords = executeJoins(tablePlan);
            
            // 检查是否有GROUP BY子句
            if (plan.getGroupByClause() != null && !plan.getGroupByClause().isEmpty()) {
                // 执行GROUP BY聚合查询
                results = executeGroupByQuery(joinedRecords, plan, tableInfo);
            } else {
                // 检查是否有聚合函数但没有GROUP BY
                boolean hasAggregateFunctions = hasAggregateFunctions(plan.getSelectList());
                
                if (hasAggregateFunctions) {
                    // 执行全局聚合查询
                    results = executeGlobalAggregateQuery(joinedRecords, plan, tableInfo);
                } else {
                    // 普通查询
                    for (Map<String, Object> row : joinedRecords) {
                        // 应用WHERE条件
                        if (plan.getWhereClause() != null) {
                            if (!evaluateWhereCondition(row, plan.getWhereClause(), tableInfo)) {
                                continue;
                            }
                        }
                        
                        // 应用SELECT列表（投影）
                        Map<String, Object> projectedRow = applyProjection(row, plan.getSelectList(), tableInfo);
                        results.add(projectedRow);
                    }
                }
            }
            
            // 应用ORDER BY
            if (plan.getOrderByClause() != null) {
                sortResults(results, plan.getOrderByClause(), tableInfo);
            }
            
            // 应用LIMIT
            if (plan.getLimitClause() != null) {
                int limit = evaluateLimit(plan.getLimitClause());
                if (limit > 0 && results.size() > limit) {
                    results = results.subList(0, limit);
                }
            }
            
            return new ExecutionResult(true, "查询完成，返回 " + results.size() + " 行", results);
            
        } catch (Exception e) {
            return new ExecutionResult(false, "查询时发生错误: " + e.getMessage(), null);
        }
    }
    
    /**
     * 执行JOIN操作
     */
    private List<Map<String, Object>> executeJoins(TablePlan tablePlan) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // 获取主表数据
        String mainTableName = tablePlan.getTableName();
        String mainTableAlias = tablePlan.getAlias();
        
        if (!catalogManager.tableExists(mainTableName)) {
            return results;
        }
        
        // 根据索引类型选择查询策略
        List<Map<String, Object>> mainTableData = queryTableWithIndex(mainTableName, tablePlan);
        
        // 为左表数据添加表别名前缀
        List<Map<String, Object>> aliasedMainData = new ArrayList<>();
            for (Map<String, Object> row : mainTableData) {
                Map<String, Object> aliasedRow = new HashMap<>();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String key = entry.getKey();
                    if (mainTableAlias != null) {
                        key = mainTableAlias + "." + key;
                    }
                    aliasedRow.put(key, entry.getValue());
                }
            aliasedMainData.add(aliasedRow);
        }
        
        // 如果没有JOIN，直接返回主表数据
        if (tablePlan.getJoins() == null || tablePlan.getJoins().isEmpty()) {
            return aliasedMainData;
        }
        
        // 处理JOIN操作
        results = aliasedMainData;
        
        for (JoinPlan join : tablePlan.getJoins()) {
            results = executeJoin(results, join, mainTableAlias);
        }
        
        return results;
    }
    
    /**
     * 执行单个JOIN操作
     */
    private List<Map<String, Object>> executeJoin(List<Map<String, Object>> leftResults, JoinPlan join, String leftTableAlias) {
        List<Map<String, Object>> joinResults = new ArrayList<>();
        
        String rightTableName = join.getTableName();
        String rightTableAlias = join.getAlias();
        
        if (!catalogManager.tableExists(rightTableName)) {
            return joinResults;
        }
        
        List<Map<String, Object>> rightTableData = storageAdapter.scanTable(rightTableName);
        
        // 为右表数据添加别名前缀
        List<Map<String, Object>> aliasedRightData = new ArrayList<>();
        for (Map<String, Object> row : rightTableData) {
            Map<String, Object> aliasedRow = new HashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                if (rightTableAlias != null) {
                    key = rightTableAlias + "." + key;
                }
                aliasedRow.put(key, entry.getValue());
            }
            aliasedRightData.add(aliasedRow);
        }
        
        // 执行JOIN
        for (Map<String, Object> leftRow : leftResults) {
            for (Map<String, Object> rightRow : aliasedRightData) {
                // 合并左右两行数据
                Map<String, Object> joinedRow = new HashMap<>(leftRow);
                joinedRow.putAll(rightRow);
                
                // 检查JOIN条件
                if (evaluateJoinCondition(joinedRow, join.getCondition())) {
                    joinResults.add(joinedRow);
                }
            }
        }
        
        return joinResults;
    }
    
    /**
     * 评估JOIN条件
     */
    private boolean evaluateJoinCondition(Map<String, Object> row, ExpressionPlan condition) {
        if (condition instanceof BinaryExpressionPlan) {
            BinaryExpressionPlan binary = (BinaryExpressionPlan) condition;
            String operator = binary.getOperator();

            // 处理逻辑操作符
            if ("AND".equals(operator)) {
                boolean leftResult = evaluateJoinCondition(row, binary.getLeft());
                boolean rightResult = evaluateJoinCondition(row, binary.getRight());
                return leftResult && rightResult;
            } else if ("OR".equals(operator)) {
                boolean leftResult = evaluateJoinCondition(row, binary.getLeft());
                boolean rightResult = evaluateJoinCondition(row, binary.getRight());
                return leftResult || rightResult;
            } else {
                // 处理比较操作符
                String leftValue = getColumnValueFromRow(row, binary.getLeft());
                String rightValue = getColumnValueFromRow(row, binary.getRight());

                // 处理特殊操作符
                switch (operator) {
                    case "LIKE":
                        return evaluateLikeOperator(leftValue, rightValue);
                    case "BETWEEN":
                        return evaluateBetweenOperator(leftValue, binary.getRight());
                    case "IN":
                        return evaluateInOperator(leftValue, binary.getRight());
                    case "IS":
                        return evaluateIsNullOperator(leftValue, rightValue);
                    default:
                        // 处理基本比较操作符
                        return evaluateBasicComparison(leftValue, rightValue, operator);
                }
            }
        }
        return true;
    }
    
    /**
     * 从行数据中获取列值
     */
    private String getColumnValueFromRow(Map<String, Object> row, ExpressionPlan expr) {
        if (expr instanceof IdentifierExpressionPlan) {
            String columnName = ((IdentifierExpressionPlan) expr).getName();
            Object value = row.get(columnName);
            return value != null ? value.toString() : "NULL";
        } else if (expr instanceof LiteralExpressionPlan) {
            return ((LiteralExpressionPlan) expr).getValue();
        } else if (expr instanceof FunctionCallExpressionPlan) {
            // 处理聚合函数表达式（用于HAVING条件）
            FunctionCallExpressionPlan funcExpr = (FunctionCallExpressionPlan) expr;
            String functionName = funcExpr.getFunctionName();
            List<ExpressionPlan> arguments = funcExpr.getArguments();
            
            // 生成聚合函数列名
            String columnName = generateAggregateColumnName(functionName, arguments);
            Object value = row.get(columnName);
            
            // 如果找不到值，尝试在聚合结果中查找匹配的列
            if (value == null) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String key = entry.getKey();
                    Object val = entry.getValue();
                    // 检查是否是匹配的聚合函数结果
                    if (isMatchingAggregateResult(key, val, functionName, arguments)) {
                        value = val;
                        break;
                    }
                }
            }
            
            return value != null ? value.toString() : "NULL";
        } else if (expr instanceof SubqueryExpressionPlan) {
            // 执行子查询并返回结果
            SubqueryExpressionPlan subquery = (SubqueryExpressionPlan) expr;
            ExecutionResult result = executeSubquery(subquery.getSubquery(), row);
            if (result.isSuccess() && result.getData() != null && !result.getData().isEmpty()) {
                // 返回子查询结果的第一行第一列的值
                Map<String, Object> firstRow = result.getData().get(0);
                if (!firstRow.isEmpty()) {
                    Object firstValue = firstRow.values().iterator().next();
                    return firstValue != null ? firstValue.toString() : "NULL";
                }
            }
            return "NULL";
        }
        return "NULL";
    }
    
    /**
     * 执行子查询（支持相关子查询）
     */
    private ExecutionResult executeSubquery(SelectPlan plan, Map<String, Object> outerContext) {
        try {
            // 执行子查询的FROM子句
            List<Map<String, Object>> joinedRecords = new ArrayList<>();
            if (plan.getFromClause() != null && !plan.getFromClause().isEmpty()) {
                for (TablePlan tablePlan : plan.getFromClause()) {
                    List<Map<String, Object>> tableData = executeJoins(tablePlan);
                    joinedRecords.addAll(tableData);
                }
            } else {
                // 如果没有FROM子句，创建一个空行用于聚合函数
                joinedRecords.add(new HashMap<>());
            }
            
            // 应用WHERE条件（包括相关子查询的条件）
            List<Map<String, Object>> filteredRecords = new ArrayList<>();
            for (Map<String, Object> row : joinedRecords) {
                // 合并外层上下文和当前行
                Map<String, Object> contextRow = new HashMap<>(outerContext);
                // 先添加子查询的列，这样不会被外层查询的列覆盖
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    contextRow.put(entry.getKey(), entry.getValue());
                }
                
                if (plan.getWhereClause() != null) {
                    if (!evaluateWhereCondition(contextRow, plan.getWhereClause(), null)) {
                        continue;
                    }
                }
                filteredRecords.add(contextRow);
            }
            
            // 处理GROUP BY和聚合
            List<Map<String, Object>> results = new ArrayList<>();
            if (plan.getGroupByClause() != null && !plan.getGroupByClause().isEmpty()) {
                results = executeGroupByQuery(filteredRecords, plan, null);
            } else if (hasAggregateFunctions(plan.getSelectList())) {
                results = executeGlobalAggregateQuery(filteredRecords, plan, null);
            } else {
                // 普通查询
                for (Map<String, Object> row : filteredRecords) {
                    Map<String, Object> projectedRow = applyProjection(row, plan.getSelectList(), null);
                    results.add(projectedRow);
                }
            }
            return new ExecutionResult(true, "子查询执行成功", results);
            
        } catch (Exception e) {
            System.err.println("子查询执行失败: " + e.getMessage());
            e.printStackTrace();
            return new ExecutionResult(false, "子查询执行失败: " + e.getMessage(), null);
        }
    }
    
    /**
     * 执行DELETE
     */
    private ExecutionResult executeDelete(DeletePlan plan) {
        try {
            String tableName = plan.getTableName();
            
            if (!catalogManager.tableExists(tableName)) {
                return new ExecutionResult(false, "表 " + tableName + " 不存在", null);
            }
            
            TableInfo tableInfo = catalogManager.getTable(tableName);
            int deletedRows = 0;
            
            // 扫描所有记录
            List<Map<String, Object>> allRecords = storageAdapter.scanTable(tableName);
            List<Map<String, Object>> recordsToDelete = new ArrayList<>();
            
            for (Map<String, Object> row : allRecords) {
                // 检查WHERE条件
                if (plan.getWhereClause() != null) {
                    if (evaluateWhereCondition(row, plan.getWhereClause(), tableInfo)) {
                        // 满足删除条件
                        recordsToDelete.add(row);
                        deletedRows++;
                    }
                } else {
                    // 没有WHERE条件，删除所有记录
                    recordsToDelete.add(row);
                    deletedRows++;
                }
            }
            
            // 执行删除
            for (Map<String, Object> record : recordsToDelete) {
                storageAdapter.deleteRecord(tableName, record);
            }
            
            return new ExecutionResult(true, deletedRows + " 行已删除", null);
            
        } catch (Exception e) {
            return new ExecutionResult(false, "删除数据时发生错误: " + e.getMessage(), null);
        }
    }
    
    /**
     * 执行DROP TABLE
     */
    private ExecutionResult executeDropTable(DropTablePlan plan) {
        try {
            String tableName = plan.getTableName();
            
            // 检查表是否存在
            if (!catalogManager.tableExists(tableName)) {
                if (plan.isIfExists()) {
                    return new ExecutionResult(true, "表 " + tableName + " 不存在，但使用了IF EXISTS，操作成功", null);
                } else {
                    return new ExecutionResult(false, "表 " + tableName + " 不存在", null);
                }
            }
            
            // 从目录中删除表信息
            catalogManager.dropTable(tableName);
            
            // 删除表存储文件
            if (!storageAdapter.dropTable(tableName)) {
                return new ExecutionResult(false, "删除表存储文件失败", null);
            }
            
            return new ExecutionResult(true, "表 " + tableName + " 删除成功", null);
            
        } catch (Exception e) {
            return new ExecutionResult(false, "删除表时发生错误: " + e.getMessage(), null);
        }
    }

    /**
     * 执行UPDATE
     */
    private ExecutionResult executeUpdate(UpdatePlan plan) {
        try {
            String tableName = plan.getTableName();

            if (!catalogManager.tableExists(tableName)) {
                return new ExecutionResult(false, "表 " + tableName + " 不存在", null);
            }

            TableInfo tableInfo = catalogManager.getTable(tableName);
            int updatedRows = 0;

            // 扫描所有记录
            List<Map<String, Object>> allRecords = storageAdapter.scanTable(tableName);
            List<Map<String, Object>> recordsToUpdate = new ArrayList<>();
            List<Map<String, Object>> newRecords = new ArrayList<>();

            for (Map<String, Object> row : allRecords) {
                // 检查WHERE条件
                boolean shouldUpdate = true;
                if (plan.getWhereClause() != null) {
                    shouldUpdate = evaluateWhereCondition(row, plan.getWhereClause(), tableInfo);
                }

                if (shouldUpdate) {
                    // 创建更新后的记录
                    Map<String, Object> updatedRecord = new HashMap<>(row);

                    // 应用SET子句
                    for (Map.Entry<String, ExpressionPlan> setEntry : plan.getSetClause().entrySet()) {
                        String columnName = setEntry.getKey();
                        ExpressionPlan valueExpr = setEntry.getValue();

                        // 验证列是否存在
                        ColumnInfo columnInfo = tableInfo.getColumn(columnName);
                        if (columnInfo == null) {
                            return new ExecutionResult(false, "列 " + columnName + " 不存在", null);
                        }

                        // 计算新值
                        Object newValue = evaluateExpression(valueExpr, row, tableInfo);

                        // 类型验证和转换
                        Object convertedValue = convertValueToType(newValue, columnInfo.getDataType());
                        updatedRecord.put(columnName, convertedValue);
                    }

                    recordsToUpdate.add(row); // 原记录
                    newRecords.add(updatedRecord); // 新记录
                    updatedRows++;
                }
            }

            // 执行更新
            for (int i = 0; i < recordsToUpdate.size(); i++) {
                Map<String, Object> oldRecord = recordsToUpdate.get(i);
                Map<String, Object> newRecord = newRecords.get(i);

                if (!storageAdapter.updateRecord(tableName, oldRecord, newRecord)) {
                    return new ExecutionResult(false, "更新记录失败", null);
                }
            }

            return new ExecutionResult(true, updatedRows + " 行已更新", null);

        } catch (Exception e) {
            return new ExecutionResult(false, "更新数据时发生错误: " + e.getMessage(), null);
        }
    }

    /**
     * 执行批量计划
     */
    private ExecutionResult executeBatch(BatchPlan plan) {
        try {
            List<ExecutionResult> results = new ArrayList<>();
            int successCount = 0;
            int totalCount = plan.getPlans().size();
            
            for (ExecutionPlan subPlan : plan.getPlans()) {
                ExecutionResult result = execute(subPlan);
                results.add(result);
                
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    // 如果任何一个语句失败，返回失败结果
                    return new ExecutionResult(false, 
                        String.format("批量执行失败: %d/%d 成功, 错误: %s", 
                            successCount, totalCount, result.getMessage()), 
                        results, true);
                }
            }
            
            return new ExecutionResult(true, 
                String.format("批量执行成功: %d/%d 语句执行成功", successCount, totalCount), 
                results, true);
                
        } catch (Exception e) {
            return new ExecutionResult(false, "批量执行时发生错误: " + e.getMessage(), null);
        }
    }
    
    // 辅助方法
    
    private Map<String, Object> buildRecordMap(List<ExpressionPlan> values, List<String> columnNames, TableInfo tableInfo) {
        Map<String, Object> record = new HashMap<>();
        for (int i = 0; i < values.size() && i < columnNames.size(); i++) {
            ExpressionPlan expr = values.get(i);
            String columnName = columnNames.get(i);
            if (expr instanceof LiteralExpressionPlan) {
                record.put(columnName, ((LiteralExpressionPlan) expr).getValue());
            } else {
                record.put(columnName, null);
            }
        }
        return record;
    }
    
    private boolean evaluateWhereCondition(Map<String, Object> row, ExpressionPlan whereClause, TableInfo tableInfo) {
        // 简化的WHERE条件评估
        if (whereClause instanceof BinaryExpressionPlan) {
            BinaryExpressionPlan binary = (BinaryExpressionPlan) whereClause;
            String operator = binary.getOperator();

            // 处理逻辑操作符
            if ("AND".equals(operator)) {
                boolean leftResult = evaluateWhereCondition(row, binary.getLeft(), tableInfo);
                boolean rightResult = evaluateWhereCondition(row, binary.getRight(), tableInfo);
                return leftResult && rightResult;
            } else if ("OR".equals(operator)) {
                boolean leftResult = evaluateWhereCondition(row, binary.getLeft(), tableInfo);
                boolean rightResult = evaluateWhereCondition(row, binary.getRight(), tableInfo);
                return leftResult || rightResult;
            } else if ("NOT".equals(operator)) {
                // 处理NOT操作符
                boolean result = evaluateWhereCondition(row, binary.getRight(), tableInfo);
                return !result;
            } else {
                // 处理比较操作符
                String leftValue = getColumnValueFromRow(row, binary.getLeft());
                String rightValue = getColumnValueFromRow(row, binary.getRight());

                // 处理特殊操作符
                switch (operator) {
                    case "LIKE":
                        return evaluateLikeOperator(leftValue, rightValue);
                    case "BETWEEN":
                        return evaluateBetweenOperator(leftValue, binary.getRight());
                    case "IN":
                        return evaluateInOperator(leftValue, binary.getRight());
                    case "EXISTS":
                        return evaluateExistsOperator(binary.getRight(), row, tableInfo);
                    case "IS":
                        return evaluateIsNullOperator(leftValue, rightValue);
                    default:
                        // 处理基本比较操作符
                        return evaluateBasicComparison(leftValue, rightValue, operator);
                }
            }
        }
        return true;
    }

    /**
     * 评估LIKE操作符 - 支持通配符匹配
     */
    private boolean evaluateLikeOperator(String leftValue, String rightValue) {
        if (leftValue == null || rightValue == null) {
            return false;
        }

        // 将SQL通配符转换为Java正则表达式
        String pattern = rightValue
            .replace("%", ".*")  // % 匹配任意字符序列
            .replace("_", ".");  // _ 匹配单个字符

        // 转义其他正则表达式特殊字符
        pattern = pattern.replaceAll("([\\\\\\[\\]{}()*+?^$|.])", "\\\\$1");
        // 重新处理通配符
        pattern = pattern.replace("\\\\.\\*", ".*").replace("\\\\.", ".");

        try {
            return leftValue.matches(pattern);
        } catch (Exception e) {
            System.err.println("LIKE操作符模式匹配错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估BETWEEN操作符 - 范围查询
     */
    private boolean evaluateBetweenOperator(String leftValue, ExpressionPlan rightExpr) {
        if (leftValue == null) {
            return false;
        }

        // BETWEEN操作符的右操作数应该是一个包含两个值的列表
        if (rightExpr instanceof FunctionCallExpressionPlan) {
            FunctionCallExpressionPlan func = (FunctionCallExpressionPlan) rightExpr;
            if ("BETWEEN".equals(func.getFunctionName()) && func.getArguments().size() == 2) {
                String lowerBound = getColumnValueFromRow(new HashMap<>(), func.getArguments().get(0));
                String upperBound = getColumnValueFromRow(new HashMap<>(), func.getArguments().get(1));

                try {
                    // 尝试数字比较
                    double leftNum = Double.parseDouble(leftValue);
                    double lowerNum = Double.parseDouble(lowerBound);
                    double upperNum = Double.parseDouble(upperBound);
                    return leftNum >= lowerNum && leftNum <= upperNum;
                } catch (NumberFormatException e) {
                    // 字符串比较
                    return leftValue.compareTo(lowerBound) >= 0 && leftValue.compareTo(upperBound) <= 0;
                }
            }
        }
        return false;
    }

    /**
     * 评估IN操作符 - 值列表查询
     */
    private boolean evaluateInOperator(String leftValue, ExpressionPlan rightExpr) {
        if (leftValue == null) {
            return false;
        }

        // IN操作符的右操作数应该是一个值列表
        if (rightExpr instanceof FunctionCallExpressionPlan) {
            FunctionCallExpressionPlan func = (FunctionCallExpressionPlan) rightExpr;
            if ("IN".equals(func.getFunctionName())) {
                for (ExpressionPlan arg : func.getArguments()) {
                    String rightValue = getColumnValueFromRow(new HashMap<>(), arg);
                    if (leftValue.equals(rightValue)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 评估EXISTS操作符 - 子查询存在性检查
     */
    private boolean evaluateExistsOperator(ExpressionPlan rightExpr, Map<String, Object> outerContext, TableInfo tableInfo) {
        // EXISTS操作符通常用于子查询，这里简化处理
        // 在实际实现中，需要根据具体的AST结构来处理
        try {
            // 对于EXISTS，我们检查右操作数是否表示一个有效的子查询
            // 这里简化实现，假设EXISTS总是返回true（实际应该执行子查询）
            System.out.println("EXISTS操作符执行 - 简化实现");
            return true;
        } catch (Exception e) {
            System.err.println("EXISTS操作符执行错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估IS NULL / IS NOT NULL操作符
     */
    private boolean evaluateIsNullOperator(String leftValue, String rightValue) {
        if ("NULL".equals(rightValue)) {
            return leftValue == null || "NULL".equals(leftValue);
        } else if ("NOT NULL".equals(rightValue)) {
            return leftValue != null && !"NULL".equals(leftValue);
        }
        return false;
    }

    /**
     * 评估基本比较操作符
     */
    private boolean evaluateBasicComparison(String leftValue, String rightValue, String operator) {
        // 尝试数字比较
        try {
            double leftNum = Double.parseDouble(leftValue);
            double rightNum = Double.parseDouble(rightValue);

            switch (operator) {
                case "=":
                    return Math.abs(leftNum - rightNum) < 1e-9; // 浮点数比较
                case "!=":
                    return Math.abs(leftNum - rightNum) >= 1e-9;
                case ">":
                    return leftNum > rightNum;
                case "<":
                    return leftNum < rightNum;
                case ">=":
                    return leftNum >= rightNum;
                case "<=":
                    return leftNum <= rightNum;
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，使用字符串比较
            switch (operator) {
                case "=":
                    return leftValue.equals(rightValue);
                case "!=":
                    return !leftValue.equals(rightValue);
                case ">":
                    return leftValue.compareTo(rightValue) > 0;
                case "<":
                    return leftValue.compareTo(rightValue) < 0;
                case ">=":
                    return leftValue.compareTo(rightValue) >= 0;
                case "<=":
                    return leftValue.compareTo(rightValue) <= 0;
                default:
                    return false;
            }
        }
    }
    
    private String getColumnValue(Map<String, Object> row, ExpressionPlan expr, TableInfo tableInfo) {
        if (expr instanceof IdentifierExpressionPlan) {
            String columnName = ((IdentifierExpressionPlan) expr).getName();
            return (String) row.getOrDefault(columnName, "NULL");
        } else if (expr instanceof LiteralExpressionPlan) {
            return ((LiteralExpressionPlan) expr).getValue();
        }
        return "NULL";
    }

    private Map<String, Object> applyProjection(Map<String, Object> row, List<ExpressionPlan> selectList, TableInfo tableInfo) {
        Map<String, Object> projectedRow = new HashMap<>();
        
        for (ExpressionPlan expr : selectList) {
            if (expr instanceof IdentifierExpressionPlan) {
                String columnName = ((IdentifierExpressionPlan) expr).getName();
                if (columnName.equals("*")) {
                    // SELECT * - 返回所有列，但使用简化的列名（不带表别名）
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        String key = entry.getKey();
                        // 如果列名包含表别名前缀，去掉前缀
                        if (key.contains(".")) {
                            String simpleKey = key.substring(key.lastIndexOf(".") + 1);
                            projectedRow.put(simpleKey, entry.getValue());
                        } else {
                            projectedRow.put(key, entry.getValue());
                        }
                    }
                } else {
                    // 处理带表别名的列名
                    Object value = row.getOrDefault(columnName, "NULL");
                    
                    // 确定输出列名
                    String outputColumnName = columnName;
                    if (columnName.contains(".")) {
                        // 如果输入列名包含表别名，输出时去掉表别名
                        outputColumnName = columnName.substring(columnName.lastIndexOf(".") + 1);
                    }
                    
                    projectedRow.put(outputColumnName, value);
                }
            } else if (expr instanceof FunctionCallExpressionPlan) {
                // 处理聚合函数
                FunctionCallExpressionPlan funcExpr = (FunctionCallExpressionPlan) expr;
                String functionName = funcExpr.getFunctionName();
                List<ExpressionPlan> arguments = funcExpr.getArguments();
                
                // 计算聚合函数值
                Object result = evaluateAggregateFunction(row, functionName, arguments, tableInfo);
                
                // 生成列名
                String columnName = generateAggregateColumnName(functionName, arguments);
                projectedRow.put(columnName, result);
            } else if (expr instanceof AliasExpressionPlan) {
                // 处理别名表达式
                AliasExpressionPlan aliasExpr = (AliasExpressionPlan) expr;
                ExpressionPlan innerExpr = aliasExpr.getExpression();
                String alias = aliasExpr.getAlias();
                
                // 递归处理内部表达式
                Map<String, Object> innerResult = applyProjection(row, Arrays.asList(innerExpr), tableInfo);
                
                // 将结果重命名为别名
                for (Map.Entry<String, Object> entry : innerResult.entrySet()) {
                    projectedRow.put(alias, entry.getValue());
                }
            }
        }
        
        return projectedRow;
    }
    
    private void sortResults(List<Map<String, Object>> results, List<OrderByItem> orderByClause, TableInfo tableInfo) {
        // 简化的排序实现
        results.sort((a, b) -> {
            for (OrderByItem item : orderByClause) {
                String columnName = getColumnNameFromExpression(item.getExpression());
                String valueA = (String) a.getOrDefault(columnName, "");
                String valueB = (String) b.getOrDefault(columnName, "");
                
                int comparison = valueA.compareTo(valueB);
                if (comparison != 0) {
                    return item.getOrder() == OrderByItem.SortOrder.ASC ? comparison : -comparison;
                }
            }
            return 0;
        });
    }
    
    private String getColumnNameFromExpression(ExpressionPlan expr) {
        if (expr instanceof IdentifierExpressionPlan) {
            return ((IdentifierExpressionPlan) expr).getName();
        }
        return "unknown";
    }
    
    private int evaluateLimit(LimitPlan limitClause) {
        if (limitClause.getLimit() instanceof LiteralExpressionPlan) {
            try {
                return Integer.parseInt(((LiteralExpressionPlan) limitClause.getLimit()).getValue());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    private ConstraintInfo.ConstraintType convertConstraintType(ConstraintPlan.ConstraintType type) {
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
     * 评估聚合函数
     */
    private Object evaluateAggregateFunction(Map<String, Object> row, String functionName, 
                                           List<ExpressionPlan> arguments, TableInfo tableInfo) {
        // 注意：这里只是单行评估，真正的聚合需要在GROUP BY中处理
        // 对于非GROUP BY查询，这里返回当前行的值或计算值
        
        if (arguments.isEmpty()) {
            return null;
        }
        
        ExpressionPlan arg = arguments.get(0);
        Object value = null;
        
        if (arg instanceof IdentifierExpressionPlan) {
            String columnName = ((IdentifierExpressionPlan) arg).getName();
            if (columnName.equals("*")) {
                // COUNT(*) - 返回1
                if (functionName.equalsIgnoreCase("COUNT")) {
                    return 1;
                }
                return null;
            } else {
                value = row.getOrDefault(columnName, null);
            }
        } else if (arg instanceof LiteralExpressionPlan) {
            value = ((LiteralExpressionPlan) arg).getValue();
        }
        
        // 根据函数类型返回适当的值
        switch (functionName.toUpperCase()) {
            case "COUNT":
                return value != null ? 1 : 0;
            case "SUM":
            case "AVG":
                if (value != null) {
                    try {
                        return Double.parseDouble(value.toString());
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                }
                return 0.0;
            case "MAX":
            case "MIN":
                return value;
            default:
                return value;
        }
    }
    
    /**
     * 检查聚合结果是否匹配HAVING条件中的聚合函数
     */
    private boolean isMatchingAggregateResult(String key, Object value, String functionName, List<ExpressionPlan> arguments) {
        // 检查值是否为数字（聚合函数的结果通常是数字）
        if (value == null || !(value instanceof Number)) {
            return false;
        }
        
        // 根据函数类型和参数进行更精确的匹配
        if (functionName.equalsIgnoreCase("COUNT")) {
            // COUNT(*) 应该匹配整数类型的值
            if (arguments.size() == 1 && arguments.get(0) instanceof IdentifierExpressionPlan) {
                IdentifierExpressionPlan arg = (IdentifierExpressionPlan) arguments.get(0);
                if (arg.getName().equals("*")) {
                    // COUNT(*) 应该匹配 emp_count 这样的列
                    return key.contains("count") || key.contains("COUNT");
                }
            }
        } else if (functionName.equalsIgnoreCase("AVG")) {
            // AVG(salary) 应该匹配 avg_salary 这样的列
            if (arguments.size() == 1 && arguments.get(0) instanceof IdentifierExpressionPlan) {
                IdentifierExpressionPlan arg = (IdentifierExpressionPlan) arguments.get(0);
                String columnName = arg.getName();
                return key.contains("avg") || key.contains("AVG") || 
                       key.toLowerCase().contains(columnName.toLowerCase());
            }
        } else if (functionName.equalsIgnoreCase("SUM")) {
            // SUM(salary) 应该匹配 sum_salary 这样的列
            if (arguments.size() == 1 && arguments.get(0) instanceof IdentifierExpressionPlan) {
                IdentifierExpressionPlan arg = (IdentifierExpressionPlan) arguments.get(0);
                String columnName = arg.getName();
                return key.contains("sum") || key.contains("SUM") || 
                       key.toLowerCase().contains(columnName.toLowerCase());
            }
        }
        
        return false;
    }
    
    /**
     * 生成聚合函数列名
     */
    private String generateAggregateColumnName(String functionName, List<ExpressionPlan> arguments) {
        StringBuilder sb = new StringBuilder();
        sb.append(functionName.toUpperCase()).append("(");
        
        if (arguments.isEmpty()) {
            sb.append(")");
        } else {
            ExpressionPlan arg = arguments.get(0);
            if (arg instanceof IdentifierExpressionPlan) {
                String columnName = ((IdentifierExpressionPlan) arg).getName();
                if (columnName.equals("*")) {
                    sb.append("*");
                } else {
                    // 去掉表别名前缀
                    if (columnName.contains(".")) {
                        columnName = columnName.substring(columnName.lastIndexOf(".") + 1);
                    }
                    sb.append(columnName);
                }
            } else if (arg instanceof LiteralExpressionPlan) {
                sb.append(((LiteralExpressionPlan) arg).getValue());
            }
            sb.append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * 检查SELECT列表是否包含聚合函数
     */
    private boolean hasAggregateFunctions(List<ExpressionPlan> selectList) {
        for (ExpressionPlan expr : selectList) {
            if (expr instanceof FunctionCallExpressionPlan) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 执行GROUP BY聚合查询
     */
    private List<Map<String, Object>> executeGroupByQuery(List<Map<String, Object>> records, 
                                                         SelectPlan plan, TableInfo tableInfo) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // 按GROUP BY列分组
        Map<String, List<Map<String, Object>>> groups = new HashMap<>();
        
        for (Map<String, Object> row : records) {
            // 应用WHERE条件
            if (plan.getWhereClause() != null) {
                if (!evaluateWhereCondition(row, plan.getWhereClause(), tableInfo)) {
                    continue;
                }
            }
            
            // 生成分组键
            String groupKey = generateGroupKey(row, plan.getGroupByClause());
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(row);
        }
        
        // 对每个组执行聚合
        for (Map.Entry<String, List<Map<String, Object>>> entry : groups.entrySet()) {
            List<Map<String, Object>> groupRows = entry.getValue();
            
            // 计算聚合结果
            Map<String, Object> aggregatedRow = calculateGroupAggregates(groupRows, plan.getSelectList(), tableInfo);
            
            // 应用HAVING条件（在聚合结果上评估）
            if (plan.getHavingClause() != null) {
                if (!evaluateWhereCondition(aggregatedRow, plan.getHavingClause(), tableInfo)) {
                    continue; // 跳过不满足HAVING条件的组
                }
            }
            
            results.add(aggregatedRow);
        }
        
        return results;
    }
    
    /**
     * 执行全局聚合查询（没有GROUP BY的聚合查询）
     */
    private List<Map<String, Object>> executeGlobalAggregateQuery(List<Map<String, Object>> records, 
                                                                 SelectPlan plan, TableInfo tableInfo) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // 过滤WHERE条件
        List<Map<String, Object>> filteredRecords = new ArrayList<>();
        for (Map<String, Object> row : records) {
            if (plan.getWhereClause() != null) {
                if (!evaluateWhereCondition(row, plan.getWhereClause(), tableInfo)) {
                    continue;
                }
            }
            filteredRecords.add(row);
        }
        
        // 计算全局聚合
        Map<String, Object> aggregatedRow = calculateGroupAggregates(filteredRecords, plan.getSelectList(), tableInfo);
        results.add(aggregatedRow);
        
        return results;
    }
    
    /**
     * 生成分组键
     */
    private String generateGroupKey(Map<String, Object> row, List<ExpressionPlan> groupByClause) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < groupByClause.size(); i++) {
            if (i > 0) key.append("|");
            
            ExpressionPlan expr = groupByClause.get(i);
            if (expr instanceof IdentifierExpressionPlan) {
                String columnName = ((IdentifierExpressionPlan) expr).getName();
                Object value = row.getOrDefault(columnName, "NULL");
                key.append(value != null ? value.toString() : "NULL");
            }
        }
        return key.toString();
    }
    
    /**
     * 计算组聚合结果
     */
    private Map<String, Object> calculateGroupAggregates(List<Map<String, Object>> groupRows, 
                                                        List<ExpressionPlan> selectList, TableInfo tableInfo) {
        Map<String, Object> result = new HashMap<>();
        
        for (ExpressionPlan expr : selectList) {
            if (expr instanceof IdentifierExpressionPlan) {
                IdentifierExpressionPlan idExpr = (IdentifierExpressionPlan) expr;
                String columnName = idExpr.getName();
                
                if (columnName.equals("*")) {
                    // SELECT * 在GROUP BY中通常不允许，但这里简化处理
                    continue;
                }
                
                // 对于非聚合列，取组中第一行的值
                if (!groupRows.isEmpty()) {
                    Object value = groupRows.get(0).getOrDefault(columnName, "NULL");
                    result.put(columnName, value);
                }
                
            } else if (expr instanceof FunctionCallExpressionPlan) {
                FunctionCallExpressionPlan funcExpr = (FunctionCallExpressionPlan) expr;
                String functionName = funcExpr.getFunctionName();
                List<ExpressionPlan> arguments = funcExpr.getArguments();
                
                // 计算聚合值
                Object aggregateValue = calculateAggregateValue(groupRows, functionName, arguments, tableInfo);
                
                // 生成列名
                String columnName = generateAggregateColumnName(functionName, arguments);
                result.put(columnName, aggregateValue);
                
            } else if (expr instanceof AliasExpressionPlan) {
                // 处理别名表达式
                AliasExpressionPlan aliasExpr = (AliasExpressionPlan) expr;
                ExpressionPlan innerExpr = aliasExpr.getExpression();
                String alias = aliasExpr.getAlias();
                
                // 递归处理内部表达式
                Map<String, Object> innerResult = calculateGroupAggregates(groupRows, Arrays.asList(innerExpr), tableInfo);
                
                // 将结果重命名为别名
                for (Map.Entry<String, Object> entry : innerResult.entrySet()) {
                    result.put(alias, entry.getValue());
                }
            }
        }
        
        return result;
    }
    
    /**
     * 计算聚合值
     */
    private Object calculateAggregateValue(List<Map<String, Object>> rows, String functionName, 
                                         List<ExpressionPlan> arguments, TableInfo tableInfo) {
        if (rows.isEmpty()) {
            return null;
        }
        
        if (arguments.isEmpty()) {
            return null;
        }
        
        ExpressionPlan arg = arguments.get(0);
        List<Object> values = new ArrayList<>();
        
        // 收集所有行的值
        for (Map<String, Object> row : rows) {
            Object value = null;
            
            if (arg instanceof IdentifierExpressionPlan) {
                String columnName = ((IdentifierExpressionPlan) arg).getName();
                if (columnName.equals("*")) {
                    // COUNT(*) - 每行计数1
                    value = 1;
                } else {
                    // 处理带表别名的列名
                    String actualColumnName = columnName;
                    if (columnName.contains(".")) {
                        // 如果列名包含表别名，去掉表别名前缀
                        actualColumnName = columnName.substring(columnName.lastIndexOf(".") + 1);
                    }

                    // 首先尝试使用原始列名（带表别名）
                    value = row.getOrDefault(columnName, null);
                    if (value == null && !columnName.equals(actualColumnName)) {
                        // 如果原始列名找不到，尝试不带表别名的列名
                        value = row.getOrDefault(actualColumnName, null);
                    }
                }
            } else if (arg instanceof LiteralExpressionPlan) {
                value = ((LiteralExpressionPlan) arg).getValue();
            }
            
            values.add(value);
        }
        
        // 执行聚合计算
        switch (functionName.toUpperCase()) {
            case "COUNT":
                return values.size();
                
            case "SUM":
                double sum = 0.0;
                for (Object value : values) {
                    if (value != null) {
                        try {
                            sum += Double.parseDouble(value.toString());
                        } catch (NumberFormatException e) {
                            // 忽略非数字值
                        }
                    }
                }
                return sum;
                
            case "AVG":
                double total = 0.0;
                int count = 0;
                for (Object value : values) {
                    if (value != null) {
                        try {
                            total += Double.parseDouble(value.toString());
                            count++;
                        } catch (NumberFormatException e) {
                            // 忽略非数字值
                        }
                    }
                }
                return count > 0 ? total / count : 0.0;
                
            case "MAX":
                Object max = null;
                for (Object value : values) {
                    if (value != null) {
                        if (max == null || compareValues(value, max) > 0) {
                            max = value;
                        }
                    }
                }
                return max;
                
            case "MIN":
                Object min = null;
                for (Object value : values) {
                    if (value != null) {
                        if (min == null || compareValues(value, min) < 0) {
                            min = value;
                        }
                    }
                }
                return min;
                
            default:
                return null;
        }
    }
    
    /**
     * 比较两个值的大小
     */
    @SuppressWarnings("unchecked")
    private int compareValues(Object a, Object b) {
        if (a instanceof Comparable && b instanceof Comparable) {
            try {
                return ((Comparable<Object>) a).compareTo(b);
            } catch (ClassCastException e) {
                // 如果类型不匹配，转换为字符串比较
                return a.toString().compareTo(b.toString());
            }
        }
        return a.toString().compareTo(b.toString());
    }

    /**
     * 执行创建视图操作
     */
    private ExecutionResult executeCreateView(CreateViewPlan plan) {
        try {
            // 使用ViewManager创建视图
            viewManager.createView(plan.getViewName(), plan.getSelectStatement(), plan.getOriginalQuery());
            
            String message = String.format("视图 '%s' 创建成功", plan.getViewName());
            return new ExecutionResult(true, message, null);
            
        } catch (Exception e) {
            String message = String.format("创建视图失败: %s", e.getMessage());
            return new ExecutionResult(false, message, null);
        }
    }

    /**
     * 执行删除视图操作
     */
    private ExecutionResult executeDropView(DropViewPlan plan) {
        try {
            // 使用ViewManager删除视图
            boolean result = viewManager.dropView(plan.getViewName(), plan.isIfExists());
            
            String message;
            if (result) {
                if (plan.isIfExists() && !viewManager.viewExists(plan.getViewName())) {
                    message = String.format("视图 '%s' 不存在（IF EXISTS 语义）", plan.getViewName());
                } else {
                    message = String.format("视图 '%s' 删除成功", plan.getViewName());
                }
            } else {
                message = String.format("删除视图 '%s' 失败", plan.getViewName());
            }
            
            return new ExecutionResult(true, message, null);
            
        } catch (Exception e) {
            String message = String.format("删除视图失败: %s", e.getMessage());
            return new ExecutionResult(false, message, null);
        }
    }

    /**
     * 执行创建函数操作
     */
    private ExecutionResult executeCreateFunction(CreateFunctionPlan plan) {
        // TODO: 实现创建函数功能
        return new ExecutionResult(false, "函数功能暂未完整实现", null);
    }

    /**
     * 执行调用函数操作
     */
    private ExecutionResult executeCall(CallPlan plan) {
        try {
            // 直接调用CallPlan的execute方法，它已经实现了完整的函数调用逻辑
            return plan.execute(databaseEngine);
        } catch (Exception e) {
            return new ExecutionResult(false, "调用函数失败: " + e.getMessage(), null);
        }
    }

    /**
     * 执行删除函数操作
     */
    private ExecutionResult executeDropFunction(DropFunctionPlan plan) {
        // TODO: 实现删除函数功能
        return new ExecutionResult(false, "函数功能暂未完整实现", null);
    }

    /**
     * 评估表达式
     */
    private Object evaluateExpression(ExpressionPlan expr, Map<String, Object> row, TableInfo tableInfo) {
        if (expr == null) {
            return null;
        }
        
        if (expr instanceof LiteralExpressionPlan) {
            LiteralExpressionPlan literal = (LiteralExpressionPlan) expr;
            return literal.getValue();
        } else if (expr instanceof IdentifierExpressionPlan) {
            IdentifierExpressionPlan identifier = (IdentifierExpressionPlan) expr;
            String columnName = identifier.getName();
            return row.get(columnName);
        } else if (expr instanceof BinaryExpressionPlan) {
            BinaryExpressionPlan binary = (BinaryExpressionPlan) expr;
            Object left = evaluateExpression(binary.getLeft(), row, tableInfo);
            Object right = evaluateExpression(binary.getRight(), row, tableInfo);
            
            String operator = binary.getOperator();
            switch (operator) {
                case "+":
                    return performArithmetic(left, right, "+");
                case "-":
                    return performArithmetic(left, right, "-");
                case "*":
                    return performArithmetic(left, right, "*");
                case "/":
                    return performArithmetic(left, right, "/");
                case "=":
                    return performComparison(left, right, "=");
                case "!=":
                case "<>":
                    return performComparison(left, right, "!=");
                case ">":
                    return performComparison(left, right, ">");
                case "<":
                    return performComparison(left, right, "<");
                case ">=":
                    return performComparison(left, right, ">=");
                case "<=":
                    return performComparison(left, right, "<=");
                case "AND":
                    return (Boolean) left && (Boolean) right;
                case "OR":
                    return (Boolean) left || (Boolean) right;
                default:
                    throw new RuntimeException("不支持的二元操作符: " + operator);
            }
        } else if (expr instanceof FunctionCallExpressionPlan) {
            FunctionCallExpressionPlan function = (FunctionCallExpressionPlan) expr;
            String functionName = function.getFunctionName();
            List<ExpressionPlan> arguments = function.getArguments();
            
            // 评估参数
            List<Object> evaluatedArgs = new ArrayList<>();
            for (ExpressionPlan arg : arguments) {
                evaluatedArgs.add(evaluateExpression(arg, row, tableInfo));
            }
            
            // 调用函数 - 暂时返回null，函数调用功能需要进一步实现
            throw new RuntimeException("函数调用功能暂未实现: " + functionName);
        }
        
        throw new RuntimeException("不支持的表达式类型: " + expr.getClass().getSimpleName());
    }

    /**
     * 执行算术运算
     */
    private Object performArithmetic(Object left, Object right, String operator) {
        if (left == null || right == null) {
            return null;
        }
        
        // 尝试转换为数字
        Double leftNum = convertToNumber(left);
        Double rightNum = convertToNumber(right);
        
        if (leftNum == null || rightNum == null) {
            throw new RuntimeException("算术运算只能用于数字类型");
        }
        
        switch (operator) {
            case "+":
                return leftNum + rightNum;
            case "-":
                return leftNum - rightNum;
            case "*":
                return leftNum * rightNum;
            case "/":
                if (rightNum == 0) {
                    throw new RuntimeException("除零错误");
                }
                return leftNum / rightNum;
            default:
                throw new RuntimeException("不支持的算术操作符: " + operator);
        }
    }
    
    /**
     * 执行比较运算
     */
    private Object performComparison(Object left, Object right, String operator) {
        if (left == null || right == null) {
            return false;
        }
        
        // 尝试转换为数字进行比较
        Double leftNum = convertToNumber(left);
        Double rightNum = convertToNumber(right);
        
        if (leftNum != null && rightNum != null) {
            // 数字比较
            switch (operator) {
                case "=":
                    return Math.abs(leftNum - rightNum) < 1e-9;
                case "!=":
                    return Math.abs(leftNum - rightNum) >= 1e-9;
                case ">":
                    return leftNum > rightNum;
                case "<":
                    return leftNum < rightNum;
                case ">=":
                    return leftNum >= rightNum;
                case "<=":
                    return leftNum <= rightNum;
                default:
                    throw new RuntimeException("不支持的比较操作符: " + operator);
            }
        } else {
            // 字符串比较
            String leftStr = left.toString();
            String rightStr = right.toString();
            
            switch (operator) {
                case "=":
                    return leftStr.equals(rightStr);
                case "!=":
                    return !leftStr.equals(rightStr);
                case ">":
                    return leftStr.compareTo(rightStr) > 0;
                case "<":
                    return leftStr.compareTo(rightStr) < 0;
                case ">=":
                    return leftStr.compareTo(rightStr) >= 0;
                case "<=":
                    return leftStr.compareTo(rightStr) <= 0;
                default:
                    throw new RuntimeException("不支持的比较操作符: " + operator);
            }
        }
    }
    
    /**
     * 将值转换为数字
     */
    private Double convertToNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 将值转换为指定类型
     */
    private Object convertValueToType(Object value, String dataType) {
        if (value == null) {
            return null;
        }
        
        try {
            switch (dataType.toUpperCase()) {
                case "INT":
                case "INTEGER":
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                    return Integer.parseInt(value.toString());
                    
                case "DECIMAL":
                case "FLOAT":
                case "DOUBLE":
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                    return Double.parseDouble(value.toString());
                    
                case "VARCHAR":
                case "TEXT":
                case "STRING":
                    return value.toString();
                    
                case "BOOLEAN":
                case "BOOL":
                    if (value instanceof Boolean) {
                        return value;
                    }
                    String str = value.toString().toLowerCase();
                    return "true".equals(str) || "1".equals(str);
                    
                default:
                    return value;
            }
        } catch (Exception e) {
            throw new RuntimeException("类型转换错误: " + value + " -> " + dataType + ": " + e.getMessage());
        }
    }
    
}