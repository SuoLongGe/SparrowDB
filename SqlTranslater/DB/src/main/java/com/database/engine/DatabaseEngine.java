package com.database.engine;

import com.sqlcompiler.execution.*;
import com.sqlcompiler.catalog.*;
import com.sqlcompiler.*;
import com.database.logging.*;
import java.util.*;
import java.util.Arrays;

/**
 * 数据库引擎主类 - 整合所有组件
 * 统一整合SQL编译器、存储系统和执行引擎
 */
public class DatabaseEngine {
    private final StorageEngine storageEngine;
    private final CatalogManager catalogManager;
    private final Executor executor;
    private final SQLCompiler sqlCompiler;
    private final LogManager logManager;
    private final String databaseName;
    private final String dataDirectory;
    private boolean initialized = false;
    
    // 索引类型设置
    private String currentIndexType = "智能选择";
    
    public DatabaseEngine(String databaseName, String dataDirectory) {
        this.databaseName = databaseName;
        this.dataDirectory = dataDirectory;
        
        // 初始化存储引擎（整合Java存储系统）
        this.storageEngine = new StorageEngine(dataDirectory);
        
        // 初始化目录管理器
        this.catalogManager = new CatalogManager(storageEngine);
        
        // 初始化执行引擎
        this.executor = new Executor(new StorageAdapter(dataDirectory), catalogManager);
        
        // 初始化SQL编译器 - 使用CatalogManager的Catalog实例
        this.sqlCompiler = new SQLCompiler(catalogManager.getCatalog());
        
        // 初始化日志管理器
        try {
            this.logManager = new LogManager(dataDirectory);
        } catch (Exception e) {
            throw new RuntimeException("初始化日志管理器失败: " + e.getMessage(), e);
        }
        
        System.out.println("数据库引擎 '" + databaseName + "' 已创建，数据目录: " + dataDirectory);
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
     * 执行SQL语句 - 整合SQL编译器和执行引擎，并记录日志
     */
    public ExecutionResult executeSQL(String sql) {
        if (!initialized) {
            return new ExecutionResult(false, "数据库引擎未初始化", null);
        }
        
        long transactionId = 0;
        try {
            System.out.println("执行SQL: " + sql);
            
            // 开始事务（用于日志记录）
            transactionId = logManager.beginTransaction();
            
            // 记录SQL操作开始
            String tableName = extractTableName(sql);
            logManager.logSQLOperation(transactionId, sql, tableName, "SQL执行开始", null, null);
            
            // 使用SQL编译器解析SQL并生成执行计划
            ExecutionPlan plan = null;
            try {
                // 检查是否是批量SQL语句
                boolean isMultiStatement = sql.contains(";") && sql.split(";").length > 1;
                
                SQLCompiler.CompilationResult result;
                if (isMultiStatement) {
                    result = sqlCompiler.compileBatch(sql);
                } else {
                    result = sqlCompiler.compile(sql);
                }
                
                if (result.isSuccess()) {
                    plan = result.getExecutionPlan();
                } else {
                    System.out.println("SQL编译失败: " + result.getErrors());
                    // 如果SQL编译器失败，回退到简单解析
                    System.out.println("SQL编译器不可用，使用简单解析: SQL编译失败");
                    plan = parseSQL(sql);
                }
            } catch (Exception e) {
                // 如果SQL编译器不可用，回退到简单解析
                System.out.println("SQL编译器不可用，使用简单解析: " + e.getMessage());
                plan = parseSQL(sql);
            }
            
            if (plan == null) {
                logManager.logSQLOperation(transactionId, sql, tableName, "SQL解析失败", null, null);
                logManager.abortTransaction(transactionId);
                return new ExecutionResult(false, "SQL解析失败", null);
            }
            
            // 执行计划
            ExecutionResult result = executor.execute(plan);
            
            // 记录执行结果
            if (result.isSuccess()) {
                System.out.println("SQL执行成功");
                logManager.logSQLOperation(transactionId, sql, tableName, "SQL执行成功", null, 
                    result.getData() != null ? "返回 " + result.getData().size() + " 条记录" : "无数据返回");
                logManager.commitTransaction(transactionId);
            } else {
                System.out.println("SQL执行失败: " + result.getMessage());
                logManager.logSQLOperation(transactionId, sql, tableName, "SQL执行失败", null, result.getMessage());
                logManager.abortTransaction(transactionId);
            }
            
            return result;
            
        } catch (Exception e) {
            String errorMsg = "执行SQL时发生错误: " + e.getMessage();
            System.err.println(errorMsg);
            e.printStackTrace();
            
            // 记录错误日志
            try {
                String tableName = extractTableName(sql);
                logManager.logSQLOperation(transactionId, sql, tableName, "SQL执行异常", null, errorMsg);
                logManager.abortTransaction(transactionId);
            } catch (Exception logException) {
                System.err.println("记录错误日志失败: " + logException.getMessage());
            }
            
            return new ExecutionResult(false, errorMsg, null);
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
            // 创建检查点
            logManager.createCheckpoint();
            
            // 保存目录信息
            catalogManager.saveToStorage();
            
            // 关闭日志管理器
            logManager.close();
            
            initialized = false;
        } catch (Exception e) {
            System.err.println("关闭数据库引擎时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 从SQL语句中提取表名
     */
    private String extractTableName(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }
        
        String upperSql = sql.toUpperCase().trim();
        
        if (upperSql.startsWith("SELECT")) {
            // SELECT * FROM table_name
            String[] parts = upperSql.split("FROM");
            if (parts.length > 1) {
                String tablePart = parts[1].trim().split("\\s+")[0];
                return tablePart.toLowerCase();
            }
        } else if (upperSql.startsWith("INSERT")) {
            // INSERT INTO table_name
            String[] parts = upperSql.split("INTO");
            if (parts.length > 1) {
                String tablePart = parts[1].trim().split("\\s+")[0];
                return tablePart.toLowerCase();
            }
        } else if (upperSql.startsWith("UPDATE")) {
            // UPDATE table_name
            String[] parts = upperSql.split("\\s+");
            if (parts.length > 1) {
                return parts[1].toLowerCase();
            }
        } else if (upperSql.startsWith("DELETE")) {
            // DELETE FROM table_name
            String[] parts = upperSql.split("FROM");
            if (parts.length > 1) {
                String tablePart = parts[1].trim().split("\\s+")[0];
                return tablePart.toLowerCase();
            }
        } else if (upperSql.startsWith("CREATE TABLE")) {
            // CREATE TABLE table_name
            String[] parts = upperSql.split("\\s+");
            if (parts.length > 2) {
                return parts[2].toLowerCase();
            }
        } else if (upperSql.startsWith("DROP TABLE")) {
            // DROP TABLE table_name
            String[] parts = upperSql.split("\\s+");
            if (parts.length > 2) {
                return parts[2].toLowerCase();
            }
        }
        
        return null;
    }
    
    /**
     * 设置索引类型
     */
    public void setIndexType(String indexType) {
        this.currentIndexType = indexType;
        System.out.println("索引类型已设置为: " + indexType);
    }
    
    /**
     * 获取当前索引类型
     */
    public String getCurrentIndexType() {
        return currentIndexType;
    }
    
    /**
     * 获取日志管理器
     */
    public LogManager getLogManager() {
        return logManager;
    }
    
    /**
     * 显示日志统计信息
     */
    public void printLogStats() {
        try {
            Map<String, Object> stats = logManager.getLogStats();
            System.out.println("\n=== 日志统计信息 ===");
            System.out.println("下一个LSN: " + stats.get("nextLsn"));
            System.out.println("活跃事务数: " + stats.get("activeTransactions"));
            System.out.println("总日志条目数: " + stats.get("totalLogEntries"));
            System.out.println("日志目录: " + stats.get("logDirectory"));
            System.out.println("当前日志文件: " + stats.get("currentLogFile"));
            System.out.println("当前日志文件大小: " + stats.get("currentLogFileSize") + " bytes");
        } catch (Exception e) {
            System.err.println("获取日志统计信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建检查点
     */
    public void createCheckpoint() {
        try {
            logManager.createCheckpoint();
            System.out.println("检查点创建成功");
        } catch (Exception e) {
            System.err.println("创建检查点失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理已提交的事务日志
     */
    public void cleanupLogs() {
        try {
            logManager.cleanupCommittedTransactions();
            System.out.println("日志清理完成");
        } catch (Exception e) {
            System.err.println("日志清理失败: " + e.getMessage());
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
        try {
            // 简单的CREATE TABLE解析
            // 格式: CREATE TABLE table_name (column1 type1, column2 type2, ...)
            String[] parts = sql.split("\\(", 2);
            if (parts.length != 2) {
                return null;
            }
            
            String tableName = parts[0].replace("CREATE TABLE", "").trim().toLowerCase();
            String columnsPart = parts[1].trim();
            if (columnsPart.endsWith(")")) {
                columnsPart = columnsPart.substring(0, columnsPart.length() - 1);
            }
            
            String[] columnDefs = columnsPart.split(",");
            List<ColumnPlan> columns = new ArrayList<>();
            
            for (String columnDef : columnDefs) {
                String[] colParts = columnDef.trim().split("\\s+");
                if (colParts.length >= 2) {
                    String colName = colParts[0].trim();
                    String colType = colParts[1].trim();
                    boolean isPrimary = columnDef.toUpperCase().contains("PRIMARY KEY");
                    
                    columns.add(new ColumnPlan(
                        colName.toLowerCase(),
                        colType.toUpperCase(),
                        100,  // 默认长度
                        false,  // 不允许为空
                        isPrimary,  // 是否主键
                        false,  // 不自增
                        null,  // 默认值
                        false  // 不唯一
                    ));
                }
            }
            
            return new CreateTablePlan(tableName, columns, new ArrayList<>());
        } catch (Exception e) {
            System.err.println("解析CREATE TABLE失败: " + e.getMessage());
        return null;
        }
    }
    
    private ExecutionPlan parseInsert(String sql) {
        try {
            // 简单的INSERT解析
            // 格式: INSERT INTO table_name VALUES (value1, value2, ...)
            String[] parts = sql.split("VALUES");
            if (parts.length != 2) {
                return null;
            }
            
            String tableName = parts[0].replace("INSERT INTO", "").trim().toLowerCase();
            String valuesPart = parts[1].trim();
            if (valuesPart.startsWith("(")) {
                valuesPart = valuesPart.substring(1);
            }
            if (valuesPart.endsWith(")")) {
                valuesPart = valuesPart.substring(0, valuesPart.length() - 1);
            }
            
            List<String> values = new ArrayList<>();
            StringBuilder currentValue = new StringBuilder();
            boolean inString = false;
            
            for (char c : valuesPart.toCharArray()) {
                if (c == '\'') {
                    inString = !inString;
                    currentValue.append(c);
                } else if (c == ',' && !inString) {
                    values.add(currentValue.toString().trim());
                    currentValue = new StringBuilder();
                } else {
                    currentValue.append(c);
                }
            }
            if (currentValue.length() > 0) {
                values.add(currentValue.toString().trim());
            }
            
            List<List<ExpressionPlan>> valuePlans = new ArrayList<>();
            List<ExpressionPlan> rowValues = new ArrayList<>();
            
            for (String value : values) {
                if (value.startsWith("'") && value.endsWith("'")) {
                    // 字符串值
                    rowValues.add(new LiteralExpressionPlan(value.substring(1, value.length() - 1), "STRING"));
                } else {
                    // 数字值
                    rowValues.add(new LiteralExpressionPlan(value, "NUMBER"));
                }
            }
            valuePlans.add(rowValues);
            
            return new InsertPlan(tableName, new ArrayList<>(), valuePlans);
        } catch (Exception e) {
            System.err.println("解析INSERT失败: " + e.getMessage());
        return null;
        }
    }
    
    private ExecutionPlan parseSelect(String sql) {
        try {
            // 简单的SELECT解析
            // 格式: SELECT column1, column2 FROM table_name [WHERE condition]
            String[] parts = sql.split("FROM");
            if (parts.length < 2) {
                return null;
            }
            
            String selectPart = parts[0].replace("SELECT", "").trim();
            String[] remainingParts = parts[1].trim().split("WHERE");
            String tableName = remainingParts[0].trim().toLowerCase();
            
            List<ExpressionPlan> selectList = new ArrayList<>();
            for (String col : selectPart.split(",")) {
                col = col.trim();
                if (col.equals("*")) {
                    selectList.add(new IdentifierExpressionPlan("*"));
                } else {
                    selectList.add(new IdentifierExpressionPlan(col.toLowerCase()));
                }
            }
            
            ExpressionPlan whereClause = null;
            if (remainingParts.length > 1) {
                String condition = remainingParts[1].trim();
                whereClause = new BinaryExpressionPlan(
                    new IdentifierExpressionPlan(condition.split("=")[0].trim()),
                    "=",
                    new LiteralExpressionPlan(condition.split("=")[1].trim(), "STRING")
                );
            }
            
            List<TablePlan> fromClause = Arrays.asList(new TablePlan(tableName, null, null));
            return new SelectPlan(false, selectList, fromClause, whereClause, null, null, null, null);
        } catch (Exception e) {
            System.err.println("解析SELECT失败: " + e.getMessage());
        return null;
        }
    }
    
    private ExecutionPlan parseDelete(String sql) {
        try {
            // 简单的DELETE解析
            // 格式: DELETE FROM table_name [WHERE condition]
            String[] parts = sql.split("WHERE");
            String tablePart = parts[0].replace("DELETE FROM", "").trim();
            String tableName = tablePart.toLowerCase();
            
            ExpressionPlan whereClause = null;
            if (parts.length > 1) {
                String condition = parts[1].trim();
                whereClause = new BinaryExpressionPlan(
                    new IdentifierExpressionPlan(condition.split("=")[0].trim()),
                    "=",
                    new LiteralExpressionPlan(condition.split("=")[1].trim(), "STRING")
                );
            }
            
            return new DeletePlan(tableName, whereClause);
        } catch (Exception e) {
            System.err.println("解析DELETE失败: " + e.getMessage());
        return null;
    }
}

    /**
     * 获取目录管理器
     */
    public CatalogManager getCatalogManager() {
        return catalogManager;
    }
    
    /**
     * 获取SQL编译器
     */
    public SQLCompiler getSQLCompiler() {
        return sqlCompiler;
    }
    
    /**
     * 获取数据目录路径
     */
    public String getDataDirectory() {
        return dataDirectory;
    }
}