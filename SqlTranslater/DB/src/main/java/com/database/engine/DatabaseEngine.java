package com.database.engine;

import com.sqlcompiler.execution.*;
import com.sqlcompiler.catalog.*;
import java.util.*;
import java.util.Arrays;

/**
 * 数据库引擎主类 - 整合所有组件
 */
public class DatabaseEngine {
    private final StorageEngine storageEngine;
    private final CatalogManager catalogManager;
    private final Executor executor;
    private final String databaseName;
    private boolean initialized = false;
    
    public DatabaseEngine(String databaseName, String dataDirectory) {
        this.databaseName = databaseName;
        this.storageEngine = new StorageEngine(dataDirectory);
        this.catalogManager = new CatalogManager(storageEngine);
        this.executor = new Executor(storageEngine, catalogManager);
    }
    
    /**
     * 初始化数据库引擎
     */
    public boolean initialize() {
        try {
            // 从存储中加载目录信息
            catalogManager.loadFromStorage();
            initialized = true;
            return true;
        } catch (Exception e) {
            System.err.println("数据库引擎初始化失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行SQL语句
     */
    public ExecutionResult executeSQL(String sql) {
        if (!initialized) {
            return new ExecutionResult(false, "数据库引擎未初始化", null);
        }
        
        try {
            // 这里应该调用SQL编译器来解析SQL并生成执行计划
            // 为了演示，我们创建一个简单的执行计划
            ExecutionPlan plan = parseSQL(sql);
            if (plan == null) {
                return new ExecutionResult(false, "SQL解析失败", null);
            }
            
            return executor.execute(plan);
            
        } catch (Exception e) {
            return new ExecutionResult(false, "执行SQL时发生错误: " + e.getMessage(), null);
        }
    }
    
    /**
     * 创建表
     */
    public ExecutionResult createTable(String tableName, List<ColumnPlan> columns, List<ConstraintPlan> constraints) {
        if (!initialized) {
            return new ExecutionResult(false, "数据库引擎未初始化", null);
        }
        
        try {
            CreateTablePlan plan = new CreateTablePlan(tableName, columns, constraints);
            return executor.execute(plan);
        } catch (Exception e) {
            return new ExecutionResult(false, "创建表时发生错误: " + e.getMessage(), null);
        }
    }
    
    /**
     * 插入数据
     */
    public ExecutionResult insertData(String tableName, List<String> columns, List<List<ExpressionPlan>> values) {
        if (!initialized) {
            return new ExecutionResult(false, "数据库引擎未初始化", null);
        }
        
        try {
            InsertPlan plan = new InsertPlan(tableName, columns, values);
            return executor.execute(plan);
        } catch (Exception e) {
            return new ExecutionResult(false, "插入数据时发生错误: " + e.getMessage(), null);
        }
    }
    
    /**
     * 查询数据
     */
    public ExecutionResult selectData(String tableName, List<ExpressionPlan> selectList, 
                                    ExpressionPlan whereClause, List<OrderByItem> orderByClause, 
                                    LimitPlan limitClause) {
        if (!initialized) {
            return new ExecutionResult(false, "数据库引擎未初始化", null);
        }
        
        try {
            List<TablePlan> fromClause = Arrays.asList(new TablePlan(tableName, null, null));
            SelectPlan plan = new SelectPlan(false, selectList, fromClause, whereClause, null, null, orderByClause, limitClause);
            return executor.execute(plan);
        } catch (Exception e) {
            return new ExecutionResult(false, "查询数据时发生错误: " + e.getMessage(), null);
        }
    }
    
    /**
     * 删除数据
     */
    public ExecutionResult deleteData(String tableName, ExpressionPlan whereClause) {
        if (!initialized) {
            return new ExecutionResult(false, "数据库引擎未初始化", null);
        }
        
        try {
            DeletePlan plan = new DeletePlan(tableName, whereClause);
            return executor.execute(plan);
        } catch (Exception e) {
            return new ExecutionResult(false, "删除数据时发生错误: " + e.getMessage(), null);
        }
    }
    
    /**
     * 获取数据库信息
     */
    public Map<String, Object> getDatabaseInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("database_name", databaseName);
        info.put("initialized", initialized);
        info.put("catalog_summary", catalogManager.getCatalogSummary());
        info.put("statistics", catalogManager.getDatabaseStatistics());
        return info;
    }
    
    /**
     * 获取表信息
     */
    public Map<String, Object> getTableInfo(String tableName) {
        if (!catalogManager.tableExists(tableName)) {
            return null;
        }
        
        Map<String, Object> info = new HashMap<>();
        TableInfo tableInfo = catalogManager.getTable(tableName);
        info.put("table_name", tableName);
        info.put("columns", tableInfo.getColumns());
        info.put("constraints", tableInfo.getConstraints());
        info.put("statistics", catalogManager.getTableStatistics(tableName));
        return info;
    }
    
    /**
     * 列出所有表
     */
    public List<String> listTables() {
        return new ArrayList<>(catalogManager.getAllTableNames());
    }
    
    /**
     * 关闭数据库引擎
     */
    public void shutdown() {
        try {
            // 保存目录信息
            catalogManager.saveToStorage();
            initialized = false;
        } catch (Exception e) {
            System.err.println("关闭数据库引擎时发生错误: " + e.getMessage());
        }
    }
    
    // 私有辅助方法
    
    private ExecutionPlan parseSQL(String sql) {
        // 简化的SQL解析 - 实际应该使用完整的SQL编译器
        sql = sql.trim().toUpperCase();
        
        if (sql.startsWith("CREATE TABLE")) {
            return parseCreateTable(sql);
        } else if (sql.startsWith("INSERT INTO")) {
            return parseInsert(sql);
        } else if (sql.startsWith("SELECT")) {
            return parseSelect(sql);
        } else if (sql.startsWith("DELETE FROM")) {
            return parseDelete(sql);
        }
        
        return null;
    }
    
    private ExecutionPlan parseCreateTable(String sql) {
        // 简化的CREATE TABLE解析
        // 实际实现应该使用完整的SQL编译器
        return null;
    }
    
    private ExecutionPlan parseInsert(String sql) {
        // 简化的INSERT解析
        return null;
    }
    
    private ExecutionPlan parseSelect(String sql) {
        // 简化的SELECT解析
        return null;
    }
    
    private ExecutionPlan parseDelete(String sql) {
        // 简化的DELETE解析
        return null;
    }
}
