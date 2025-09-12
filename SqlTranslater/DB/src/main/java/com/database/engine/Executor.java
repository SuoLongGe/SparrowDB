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
    
    public Executor(StorageAdapter storageAdapter, CatalogManager catalogManager) {
        this.storageAdapter = storageAdapter;
        this.catalogManager = catalogManager;
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
            TableInfo tableInfo = new TableInfo(tableName);
            
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
            
            TableInfo tableInfo = catalogManager.getTable(tableName);
            
            // 执行全表扫描
            List<Map<String, Object>> allRecords = storageAdapter.scanTable(tableName);
            
            // 处理每一行
            for (Map<String, Object> row : allRecords) {
                
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
            String leftValue = getColumnValue(row, binary.getLeft(), tableInfo);
            String rightValue = getColumnValue(row, binary.getRight(), tableInfo);
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
                    // SELECT *
                    projectedRow.putAll(row);
                } else {
                    projectedRow.put(columnName, row.getOrDefault(columnName, "NULL"));
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
}