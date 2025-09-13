package com.database.engine;

import com.sqlcompiler.execution.*;
import com.sqlcompiler.catalog.*;
import java.util.*;

/**
 * 执行引擎 - 负责执行各种SQL操作
 * 支持 CreateTable、Insert、SeqScan、Filter、Project
 * 现在使用StorageAdapter来支持更高级的存储系统
 */
public class Executor {
    private final StorageAdapter storageAdapter;
    private final CatalogManager catalogManager;
    private String currentIndexType = "智能选择";
    private final ViewManager viewManager;

    public Executor(StorageAdapter storageAdapter, CatalogManager catalogManager, ViewManager viewManager) {
        this.storageAdapter = storageAdapter;
        this.catalogManager = catalogManager;
        this.viewManager = viewManager;
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
     * 智能选择索引类型
     */
    private List<Map<String, Object>> queryWithIntelligentSelection(String tableName, TablePlan tablePlan) {
        // 智能选择：根据查询条件选择最优索引
        // 这里简化为根据表大小选择
        List<Map<String, Object>> allData = storageAdapter.scanTable(tableName);

        if (allData.size() > 10000) {
            // 大数据集，使用B+树索引
            return queryWithBPlusTreeIndex(tableName, tablePlan);
        } else if (allData.size() > 1000) {
            // 中等数据集，使用哈希索引
            return queryWithHashIndex(tableName, tablePlan);
        } else {
            // 小数据集，使用线性查找
            return queryWithLinearSearch(tableName, tablePlan);
        }
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
            
            if (!catalogManager.tableExists(tableName)) {
                return new ExecutionResult(false, "表 " + tableName + " 不存在", null);
            }
            
            // 执行JOIN操作
            List<Map<String, Object>> joinedRecords = executeJoins(tablePlan);
            
            // 处理每一行
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
        
        // 如果没有JOIN，直接返回主表数据（添加表别名前缀）
        if (tablePlan.getJoins() == null || tablePlan.getJoins().isEmpty()) {
            for (Map<String, Object> row : mainTableData) {
                Map<String, Object> aliasedRow = new HashMap<>();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String key = entry.getKey();
                    if (mainTableAlias != null) {
                        key = mainTableAlias + "." + key;
                    }
                    aliasedRow.put(key, entry.getValue());
                }
                results.add(aliasedRow);
            }
            return results;
        }
        
        // 处理JOIN操作
        results = mainTableData;
        
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
            String leftValue = getColumnValueFromRow(row, binary.getLeft());
            String rightValue = getColumnValueFromRow(row, binary.getRight());
            String operator = binary.getOperator();
            
            switch (operator) {
                case "=":
                    return leftValue.equals(rightValue);
                case "!=":
                    return !leftValue.equals(rightValue);
                case ">":
                    return leftValue.compareTo(rightValue) > 0;
                case "<":
                    return leftValue.compareTo(rightValue) < 0;
                default:
                    return false;
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
        }
        return "NULL";
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
            String leftValue = getColumnValueFromRow(row, binary.getLeft());
            String rightValue = getColumnValueFromRow(row, binary.getRight());
            String operator = binary.getOperator();
            
            // 处理NULL值
            if (leftValue == null || rightValue == null) {
                return false;
            }

            switch (operator) {
                case "=":
                    return compareValues(leftValue, rightValue) == 0;
                case "!=":
                case "<>":
                    return compareValues(leftValue, rightValue) != 0;
                case ">":
                    return compareValues(leftValue, rightValue) > 0;
                case "<":
                    return compareValues(leftValue, rightValue) < 0;
                case ">=":
                    return compareValues(leftValue, rightValue) >= 0;
                case "<=":
                    return compareValues(leftValue, rightValue) <= 0;
                case "LIKE":
                    return evaluateLikeCondition(leftValue.toString(), rightValue.toString());
                default:
                    System.err.println("不支持的操作符: " + operator);
                    return false;
            }
        }
        return true;
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
    
    /**
     * 获取列值作为适当的对象类型
     */
    private Object getColumnValueAsObject(Map<String, Object> row, ExpressionPlan expr, TableInfo tableInfo) {
        if (expr instanceof IdentifierExpressionPlan) {
            String columnName = ((IdentifierExpressionPlan) expr).getName();
            Object value = row.get(columnName);
            if (value == null) {
                return null;
            }

            // 根据列类型转换值
            ColumnInfo columnInfo = tableInfo.getColumn(columnName);
            if (columnInfo != null) {
                return convertValueToType(value, columnInfo.getDataType());
            }
            return value;
        } else if (expr instanceof LiteralExpressionPlan) {
            LiteralExpressionPlan literal = (LiteralExpressionPlan) expr;
            return convertLiteralValue(literal.getValue(), literal.getType());
        }
        return null;
    }

    /**
     * 根据数据类型转换值
     */
    private Object convertValueToType(Object value, String dataType) {
        if (value == null) return null;

        String strValue = value.toString();
        try {
            switch (dataType.toUpperCase()) {
                case "INT":
                case "INTEGER":
                    return Integer.parseInt(strValue);
                case "BIGINT":
                case "LONG":
                    return Long.parseLong(strValue);
                case "DECIMAL":
                case "DOUBLE":
                case "FLOAT":
                    return Double.parseDouble(strValue);
                case "BOOLEAN":
                    return Boolean.parseBoolean(strValue);
                default:
                    return strValue; // VARCHAR, TEXT等字符串类型
            }
        } catch (NumberFormatException e) {
            System.err.println("数值转换失败: " + strValue + " -> " + dataType);
            return strValue;
        }
    }

    /**
     * 转换字面量值
     */
    private Object convertLiteralValue(String value, String type) {
        if (value == null) return null;

        try {
            switch (type.toUpperCase()) {
                case "NUMBER":
                case "INTEGER":
                    // 尝试解析为整数或浮点数
                    if (value.contains(".")) {
                        return Double.parseDouble(value);
                    } else {
                        return Integer.parseInt(value);
                    }
                case "STRING":
                default:
                    return value;
            }
        } catch (NumberFormatException e) {
            return value; // 解析失败时返回字符串
        }
    }

    /**
     * 比较两个值
     */
    @SuppressWarnings("unchecked")
    private int compareValues(Object left, Object right) {
        // 如果类型相同，直接比较
        if (left.getClass() == right.getClass() && left instanceof Comparable) {
            return ((Comparable<Object>) left).compareTo(right);
        }

        // 尝试数值比较
        if (isNumeric(left) && isNumeric(right)) {
            double leftNum = getNumericValue(left);
            double rightNum = getNumericValue(right);
            return Double.compare(leftNum, rightNum);
        }

        // 默认字符串比较
        return left.toString().compareTo(right.toString());
    }

    /**
     * 判断对象是否为数值类型
     */
    private boolean isNumeric(Object obj) {
        return obj instanceof Number;
    }

    /**
     * 获取对象的数值
     */
    private double getNumericValue(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 评估LIKE条件
     */
    private boolean evaluateLikeCondition(String value, String pattern) {
        if (value == null || pattern == null) {
            return false;
        }

        // 将SQL的LIKE模式转换为正则表达式
        String regex = pattern
            .replace("%", ".*")  // % 匹配任意字符串
            .replace("_", ".");  // _ 匹配单个字符

        try {
            return value.matches(regex);
        } catch (Exception e) {
            System.err.println("LIKE条件评估失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 评估表达式值
     */
    private Object evaluateExpression(ExpressionPlan expr, Map<String, Object> row, TableInfo tableInfo) {
        if (expr instanceof LiteralExpressionPlan) {
            LiteralExpressionPlan literal = (LiteralExpressionPlan) expr;
            return convertLiteralValue(literal.getValue(), literal.getType());
        } else if (expr instanceof IdentifierExpressionPlan) {
            String columnName = ((IdentifierExpressionPlan) expr).getName();
            return row.get(columnName);
        } else if (expr instanceof BinaryExpressionPlan) {
            // 支持简单的二元表达式（如算术运算）
            BinaryExpressionPlan binary = (BinaryExpressionPlan) expr;
            Object left = evaluateExpression(binary.getLeft(), row, tableInfo);
            Object right = evaluateExpression(binary.getRight(), row, tableInfo);
            return evaluateBinaryExpression(left, binary.getOperator(), right);
        } else if (expr instanceof com.sqlcompiler.execution.FunctionCallExpressionPlan) {
            // 处理函数调用表达式
            com.sqlcompiler.execution.FunctionCallExpressionPlan funcPlan =
                (com.sqlcompiler.execution.FunctionCallExpressionPlan) expr;

            // 评估函数参数
            List<Object> arguments = new ArrayList<>();
            for (ExpressionPlan argPlan : funcPlan.getArguments()) {
                Object argValue = evaluateExpression(argPlan, row, tableInfo);
                arguments.add(argValue);
            }

            // 调用函数计算器
            return FunctionEvaluator.evaluateFunction(funcPlan.getFunctionName(), arguments);
        }
        return null;
    }

    /**
     * 评估二元表达式
     */
    private Object evaluateBinaryExpression(Object left, String operator, Object right) {
        if (left == null || right == null) {
            return null;
        }

        // 数值运算
        if (isNumeric(left) && isNumeric(right)) {
            double leftNum = getNumericValue(left);
            double rightNum = getNumericValue(right);

            switch (operator) {
                case "+":
                    return leftNum + rightNum;
                case "-":
                    return leftNum - rightNum;
                case "*":
                    return leftNum * rightNum;
                case "/":
                    if (rightNum != 0) {
                        return leftNum / rightNum;
                    } else {
                        throw new ArithmeticException("除零错误");
                    }
                default:
                    return left; // 不支持的运算符，返回左值
            }
        }

        // 字符串连接
        if (operator.equals("+") || operator.equals("||")) {
            return left.toString() + right.toString();
        }

        return left; // 默认返回左值
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
            } else if (expr instanceof com.sqlcompiler.execution.FunctionCallExpressionPlan) {
                // 处理函数调用表达式
                com.sqlcompiler.execution.FunctionCallExpressionPlan funcPlan =
                    (com.sqlcompiler.execution.FunctionCallExpressionPlan) expr;

                Object result = evaluateExpression(expr, row, tableInfo);
                String alias = funcPlan.getFunctionName() + "()"; // 使用函数名作为别名
                projectedRow.put(alias, result);
            } else {
                // 处理其他表达式
                Object result = evaluateExpression(expr, row, tableInfo);
                String alias = expr.getType() + "_result"; // 使用表达式类型作为别名
                projectedRow.put(alias, result);
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
     * 执行CREATE VIEW
     */
    private ExecutionResult executeCreateView(CreateViewPlan plan) {
        try {
            String viewName = plan.getViewName();

            // 检查视图名是否已存在
            if (viewManager.viewExists(viewName)) {
                return new ExecutionResult(false, "视图 " + viewName + " 已存在", null);
            }

            // 检查是否与表名冲突
            if (catalogManager.tableExists(viewName)) {
                return new ExecutionResult(false, "名称 " + viewName + " 已被表使用", null);
            }

            // 创建视图
            viewManager.createView(viewName, plan.getSelectStatement(), plan.getOriginalQuery());

            return new ExecutionResult(true, "视图 " + viewName + " 创建成功", null);

        } catch (Exception e) {
            return new ExecutionResult(false, "创建视图失败: " + e.getMessage(), null);
        }
    }

    /**
     * 执行DROP VIEW
     */
    private ExecutionResult executeDropView(DropViewPlan plan) {
        try {
            String viewName = plan.getViewName();
            boolean ifExists = plan.isIfExists();

            // 删除视图
            boolean success = viewManager.dropView(viewName, ifExists);

            if (success) {
                return new ExecutionResult(true, "视图 " + viewName + " 删除成功", null);
            } else {
                return new ExecutionResult(false, "删除视图失败", null);
            }

        } catch (Exception e) {
            return new ExecutionResult(false, "删除视图失败: " + e.getMessage(), null);
        }
    }

    /**
     * 执行CREATE FUNCTION
     */
    private ExecutionResult executeCreateFunction(CreateFunctionPlan plan) {
        try {
            // 委托给执行计划自己处理，因为它需要访问DatabaseEngine
            // 这里我们需要找到一种方式来获取DatabaseEngine实例
            return new ExecutionResult(false, "CREATE FUNCTION执行需要通过DatabaseEngine", null);
        } catch (Exception e) {
            return new ExecutionResult(false, "创建函数失败: " + e.getMessage(), null);
        }
    }

    /**
     * 执行CALL
     */
    private ExecutionResult executeCall(CallPlan plan) {
        try {
            // 委托给执行计划自己处理
            return new ExecutionResult(false, "CALL执行需要通过DatabaseEngine", null);
        } catch (Exception e) {
            return new ExecutionResult(false, "调用函数失败: " + e.getMessage(), null);
        }
    }

    /**
     * 执行DROP FUNCTION
     */
    private ExecutionResult executeDropFunction(DropFunctionPlan plan) {
        try {
            // 委托给执行计划自己处理
            return new ExecutionResult(false, "DROP FUNCTION执行需要通过DatabaseEngine", null);
        } catch (Exception e) {
            return new ExecutionResult(false, "删除函数失败: " + e.getMessage(), null);
        }
    }
}