package com.database.engine;

import com.sqlcompiler.execution.*;
import com.sqlcompiler.catalog.*;
import com.sqlcompiler.*;
import com.database.logging.*;
import com.database.io.SQLFileManager;
import com.database.engine.sharding.*;
import java.util.*;
import java.util.Arrays;
import java.io.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * 数据库引擎主类 - 整合所有组件
 * 统一整合SQL编译器、存储系统和执行引擎
 */
public class DatabaseEngine {
    private final StorageEngine storageEngine;
    private final CatalogManager catalogManager;
    private final ViewManager viewManager;
    private final FunctionManager functionManager;
    private final Executor executor;
    private final SQLCompiler sqlCompiler;
    private final LogManager logManager;
    private final SQLFileManager sqlFileManager;
    private final ShardManager shardManager;
    private final String databaseName;
    private final String dataDirectory;
    private final String currentNodeId;
    private boolean initialized = false;
    
    // 索引类型设置
    private String currentIndexType = "智能选择";
    
    // 存储格式设置
    private String currentStorageFormat = "行式存储";

    public DatabaseEngine(String databaseName, String dataDirectory) {
        this.databaseName = databaseName;
        this.dataDirectory = dataDirectory;
        this.currentNodeId = "node_" + System.currentTimeMillis();
        
        // 初始化存储引擎（整合Java存储系统）
        this.storageEngine = new StorageEngine(dataDirectory);
        
        // 初始化存储适配器
        StorageAdapter storageAdapter = new StorageAdapter(dataDirectory);

        // 初始化目录管理器
        this.catalogManager = new CatalogManager(storageEngine);
        this.catalogManager.setStorageAdapter(storageAdapter);
        
        // 初始化视图管理器
        this.viewManager = new ViewManager(storageEngine);

        // 初始化增强的函数管理器
        this.functionManager = new EnhancedFunctionManager(storageAdapter);

        // 初始化执行引擎
        this.executor = new Executor(storageAdapter, catalogManager, this);
        
        // 初始化SQL编译器 - 使用CatalogManager的Catalog实例
        this.sqlCompiler = new SQLCompiler(catalogManager.getCatalog());
        
        // 初始化日志管理器
        try {
            this.logManager = new LogManager(dataDirectory);
            // 设置回滚回调
            this.logManager.setRollbackCallback(storageAdapter);
        } catch (Exception e) {
            throw new RuntimeException("初始化日志管理器失败: " + e.getMessage(), e);
        }
        
        // 初始化SQL文件管理器
        this.sqlFileManager = new SQLFileManager(this, catalogManager);
        
        // 初始化分片管理器
        this.shardManager = new ShardManager(dataDirectory, currentNodeId, storageAdapter, catalogManager);
        
        // 初始化数据库引擎
        if (!initialize()) {
            throw new RuntimeException("数据库引擎初始化失败");
        }
        
        System.out.println("数据库引擎 '" + databaseName + "' 已创建，数据目录: " + dataDirectory + "，节点ID: " + currentNodeId);
    }
    
    /**
     * 初始化数据库引擎
     */
    public boolean initialize() {
        try {
            // 从存储中加载目录信息
            catalogManager.loadFromStorage();

            // 确保StorageAdapter中的列式存储表信息同步到CatalogManager
            syncColumnarTablesToCatalog();
            
            // 确保系统表存在
            ensureSystemTablesExist();
            
            // 设置ViewManager到SQLCompiler，以便语义分析器可以检查视图
            sqlCompiler.setViewManager(viewManager);

            initialized = true;
            return true;
        } catch (Exception e) {
            System.err.println("数据库引擎初始化失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 同步列式存储表信息到目录管理器
     */
    private void syncColumnarTablesToCatalog() {
        try {
            System.out.println("开始同步列式存储表到目录...");

            // 获取StorageAdapter中的列式存储引擎
            StorageAdapter storageAdapter = executor.getStorageAdapter();
            ColumnarStorageEngine columnarEngine = storageAdapter.getColumnarStorageEngine();

            // 获取所有列式存储表
            List<String> columnarTables = columnarEngine.getTableNames();
            System.out.println("发现列式存储表: " + columnarTables);

            for (String tableName : columnarTables) {
                // 检查表是否已经在目录中
                if (!catalogManager.tableExists(tableName)) {
                    // 获取表信息并添加到目录
                    TableInfo tableInfo = columnarEngine.getTableInfo(tableName);
                    if (tableInfo != null) {
                        catalogManager.addTable(tableInfo);
                        System.out.println("同步列式存储表到目录: " + tableName);
                    } else {
                        System.out.println("无法获取表信息: " + tableName);
                    }
                } else {
                    System.out.println("表已存在于目录中: " + tableName);
                }
            }
        } catch (Exception e) {
            System.err.println("同步列式存储表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 确保系统表存在
     */
    private void ensureSystemTablesExist() {
        try {
            // 确保系统列表存在
            String systemColumnsPath = dataDirectory + File.separator + "__system_columns__.tbl";
            File systemColumnsFile = new File(systemColumnsPath);
            
            if (!systemColumnsFile.exists()) {
                // 创建系统列表文件
                try (PrintWriter writer = new PrintWriter(new FileWriter(systemColumnsFile))) {
                    writer.println("# Table Metadata");
                    writer.println("TABLE_NAME=__system_columns__");
                    writer.println("COLUMN_COUNT=9");
                    writer.println("COLUMN=table_name:VARCHAR:255");
                    writer.println("COLUMN=column_name:VARCHAR:255");
                    writer.println("COLUMN=data_type:VARCHAR:50");
                    writer.println("COLUMN=length:INT:4");
                    writer.println("COLUMN=not_null:BOOLEAN:1");
                    writer.println("COLUMN=primary_key:BOOLEAN:1");
                    writer.println("COLUMN=unique:BOOLEAN:1");
                    writer.println("COLUMN=default_value:VARCHAR:255");
                    writer.println("COLUMN=auto_increment:BOOLEAN:1");
                    writer.println("# End Metadata");
                    writer.println();
                    writer.println("PAGE:1");
                    writer.println();
                }
                System.out.println("✅ 数据库初始化时创建系统列表文件: " + systemColumnsPath);
                
                // 如果已有表存在，需要重新扫描并填充系统列表
                rebuildSystemColumnsTable();
            }
        } catch (Exception e) {
            System.err.println("确保系统表存在失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 重建系统列表 - 扫描现有表并填充系统列表
     */
    private void rebuildSystemColumnsTable() {
        try {
            System.out.println("🔄 重建系统列表...");
            
            // 获取所有现有表
            Set<String> tableNames = catalogManager.getAllTableNames();
            
            if (!tableNames.isEmpty()) {
                String systemColumnsPath = dataDirectory + File.separator + "__system_columns__.tbl";
                
                // 重新写入系统列表，包含所有现有表的列信息
                try (PrintWriter writer = new PrintWriter(new FileWriter(systemColumnsPath))) {
                    writer.println("# Table Metadata");
                    writer.println("TABLE_NAME=__system_columns__");
                    writer.println("COLUMN_COUNT=9");
                    writer.println("COLUMN=table_name:VARCHAR:255");
                    writer.println("COLUMN=column_name:VARCHAR:255");
                    writer.println("COLUMN=data_type:VARCHAR:50");
                    writer.println("COLUMN=length:INT:4");
                    writer.println("COLUMN=not_null:BOOLEAN:1");
                    writer.println("COLUMN=primary_key:BOOLEAN:1");
                    writer.println("COLUMN=unique:BOOLEAN:1");
                    writer.println("COLUMN=default_value:VARCHAR:255");
                    writer.println("COLUMN=auto_increment:BOOLEAN:1");
                    writer.println("# End Metadata");
                    writer.println();
                    writer.println("PAGE:1");
                    
                    // 为每个表添加列信息
                    for (String tableName : tableNames) {
                        TableInfo tableInfo = catalogManager.getTable(tableName);
                        if (tableInfo != null) {
                            for (ColumnInfo column : tableInfo.getColumns()) {
                                boolean isPrimaryKey = tableInfo.getPrimaryKeyColumns().contains(column.getName());
                                boolean isUnique = tableInfo.getUniqueConstraints().stream()
                                    .anyMatch(constraint -> constraint.getColumns().contains(column.getName()));
                                
                                String record = String.format(
                                    "auto_increment=%s|not_null=%s|unique=%s|column_name=%s|data_type=%s|length=%d|default_value=%s|table_name=%s|primary_key=%s",
                                    column.isAutoIncrement() ? "true" : "false",
                                    column.isNotNull() ? "true" : "false",
                                    isUnique ? "true" : "false",
                                    column.getName(),
                                    column.getDataType(),
                                    column.getLength(),
                                    column.getDefaultValue() != null ? column.getDefaultValue().toString() : "null",
                                    tableName,
                                    isPrimaryKey ? "true" : "false"
                                );
                                writer.println(record);
                            }
                        }
                    }
                    
                    writer.println(); // 空行结束
                }
                
                System.out.println("✅ 已重建系统列表，包含 " + tableNames.size() + " 个表的信息");
            }
            
        } catch (Exception e) {
            System.err.println("重建系统列表失败: " + e.getMessage());
            e.printStackTrace();
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
            
            // 修改SQL语句以使用GUI选择的存储格式
            String modifiedSql = modifySQLForStorageFormat(sql);
            if (!modifiedSql.equals(sql)) {
                System.out.println("修改后的SQL: " + modifiedSql);
            }

            // 使用SQL编译器解析SQL并生成执行计划
            ExecutionPlan plan = null;
            try {
                // 检查是否是批量SQL语句
                boolean isMultiStatement = modifiedSql.contains(";") && modifiedSql.split(";").length > 1;
                
                SQLCompiler.CompilationResult result;
                if (isMultiStatement) {
                    result = sqlCompiler.compileBatch(modifiedSql);
                } else {
                    result = sqlCompiler.compile(modifiedSql);
                }
                
                if (result.isSuccess()) {
                    plan = result.getExecutionPlan();
                } else {
                    System.out.println("SQL编译失败: " + result.getErrors());
                    // 如果SQL编译器失败，回退到简单解析
                    System.out.println("SQL编译器不可用，使用简单解析: SQL编译失败");
                    plan = parseSQL(modifiedSql);
                }
            } catch (Exception e) {
                // 如果SQL编译器不可用，回退到简单解析
                System.out.println("SQL编译器不可用，使用简单解析: " + e.getMessage());
                plan = parseSQL(modifiedSql);
            }
            
            if (plan == null) {
                logManager.logSQLOperation(transactionId, sql, tableName, "SQL解析失败", null, null);
                logManager.abortTransaction(transactionId);
                return new ExecutionResult(false, "SQL解析失败", null);
            }
            
            // 执行计划
            ExecutionResult result = null;

            // 特殊处理分片相关的执行计划
            if (plan instanceof CreateShardPlan) {
                try {
                    CreateShardPlan shardPlan = (CreateShardPlan) plan;
                    String shardTableName = shardPlan.getStatement().getTableName();
                    String shardKeyColumn = shardPlan.getStatement().getShardKeyColumn();
                    String strategyName = shardPlan.getStatement().getStrategy();
                    int shardCount = shardPlan.getStatement().getShardCount();
                    
                    // 创建分片策略
                    ShardStrategy strategy;
                    if ("HASH".equals(strategyName)) {
                        strategy = new HashShardStrategy();
                    } else if ("RANGE".equals(strategyName)) {
                        strategy = new RangeShardStrategy();
                    } else {
                        result = new ExecutionResult(false, "不支持的分片策略: " + strategyName + "，支持: HASH, RANGE", null);
                        strategy = null; // 避免未初始化错误
                    }
                    
                    // 检查表是否存在
                    if (!catalogManager.tableExists(shardTableName)) {
                        result = new ExecutionResult(false, "表 " + shardTableName + " 不存在，请先创建表", null);
                    } else if (strategy != null) {
                        // 创建分片
                        boolean success = shardManager.createTableShards(shardTableName, shardKeyColumn, strategy, shardCount);
                        
                        if (success) {
                            String message = String.format("成功为表 %s 创建了 %d 个 %s 分片，分片键: %s", 
                                                         shardTableName, shardCount, strategyName, shardKeyColumn);
                            result = new ExecutionResult(true, message, null);
                        } else {
                            result = new ExecutionResult(false, "创建分片失败，可能表已存在分片或分片键列不存在", null);
                        }
                    }
                } catch (Exception e) {
                    result = new ExecutionResult(false, "创建分片失败: " + e.getMessage(), null);
                }
            } else if (plan instanceof DropShardPlan) {
                try {
                    DropShardPlan shardPlan = (DropShardPlan) plan;
                    String dropTableName = shardPlan.getStatement().getTableName();
                    boolean success = shardManager.dropTableShards(dropTableName);
                    
                    if (success) {
                        result = new ExecutionResult(true, "成功删除表 " + dropTableName + " 的分片", null);
                    } else {
                        result = new ExecutionResult(false, "删除分片失败，表 " + dropTableName + " 可能没有分片", null);
                    }
                } catch (Exception e) {
                    result = new ExecutionResult(false, "删除分片失败: " + e.getMessage(), null);
                }
            } else if (plan instanceof ShowShardsPlan) {
                try {
                    ShowShardsPlan shardPlan = (ShowShardsPlan) plan;
                    String showTableName = shardPlan.getStatement().getTableName();
                    List<Map<String, Object>> resultData = new ArrayList<>();
                    
                    if (shardPlan.getStatement().hasTableName()) {
                        // 显示指定表的分片信息
                        if (shardManager.isTableSharded(showTableName)) {
                            List<ShardInfo> shards = shardManager.getTableShards(showTableName);
                            ShardMetadata metadata = shardManager.getShardMetadata(showTableName);
                            
                            for (ShardInfo shard : shards) {
                                Map<String, Object> row = new HashMap<>();
                                row.put("table_name", shard.getTableName());
                                row.put("shard_id", shard.getShardId());
                                row.put("node_id", shard.getNodeId());
                                row.put("shard_type", shard.getShardType().toString());
                                row.put("is_active", shard.isActive());
                                row.put("record_count", shard.getRecordCount());
                                row.put("data_directory", shard.getDataDirectory());
                                row.put("shard_key_column", metadata.getShardKeyColumn());
                                row.put("strategy", metadata.getStrategy().getStrategyName());
                                resultData.add(row);
                            }
                        } else {
                            result = new ExecutionResult(false, "表 " + showTableName + " 没有分片", null);
                        }
                    } else {
                        // 显示所有分片信息
                        for (String tName : catalogManager.getAllTableNames()) {
                            if (shardManager.isTableSharded(tName)) {
                                List<ShardInfo> shards = shardManager.getTableShards(tName);
                                ShardMetadata metadata = shardManager.getShardMetadata(tName);
                                
                                for (ShardInfo shard : shards) {
                                    Map<String, Object> row = new HashMap<>();
                                    row.put("table_name", shard.getTableName());
                                    row.put("shard_id", shard.getShardId());
                                    row.put("node_id", shard.getNodeId());
                                    row.put("shard_type", shard.getShardType().toString());
                                    row.put("is_active", shard.isActive());
                                    row.put("record_count", shard.getRecordCount());
                                    row.put("data_directory", shard.getDataDirectory());
                                    row.put("shard_key_column", metadata.getShardKeyColumn());
                                    row.put("strategy", metadata.getStrategy().getStrategyName());
                                    resultData.add(row);
                                }
                            }
                        }
                        // 为显示所有分片的情况设置result
                        result = new ExecutionResult(true, "查询分片信息成功", resultData);
                    }
                    
                    if (result == null) {
                        result = new ExecutionResult(true, "查询分片信息成功", resultData);
                    }
                } catch (Exception e) {
                    result = new ExecutionResult(false, "查询分片信息失败: " + e.getMessage(), null);
                }
            } else if (plan instanceof ShardStatsPlan) {
                try {
                    ShardStatsPlan shardPlan = (ShardStatsPlan) plan;
                    String statsTableName = shardPlan.getStatement().getTableName();
                    
                    if (!shardManager.isTableSharded(statsTableName)) {
                        result = new ExecutionResult(false, "表 " + statsTableName + " 没有分片", null);
                    } else {
                        Map<String, Object> shardStats = shardManager.getShardStatistics(statsTableName);
                        Map<String, Object> loadBalanceInfo = shardManager.getLoadBalanceInfo(statsTableName);
                        ShardMetadata metadata = shardManager.getShardMetadata(statsTableName);
                        
                        List<Map<String, Object>> resultData = new ArrayList<>();
                        
                        // 基本统计信息
                        Map<String, Object> basicStats = new HashMap<>();
                        basicStats.put("table_name", statsTableName);
                        basicStats.put("shard_key_column", metadata.getShardKeyColumn());
                        basicStats.put("strategy", metadata.getStrategy().getStrategyName());
                        basicStats.put("total_shards", shardStats.get("totalShards"));
                        basicStats.put("active_shards", shardStats.get("activeShards"));
                        basicStats.put("total_records", shardStats.get("totalRecords"));
                        basicStats.put("average_records_per_shard", shardStats.get("averageRecordsPerShard"));
                        basicStats.put("is_balanced", loadBalanceInfo.get("balanced"));
                        basicStats.put("coefficient_of_variation", loadBalanceInfo.get("coefficientOfVariation"));
                        basicStats.put("max_records", loadBalanceInfo.get("maxRecords"));
                        basicStats.put("min_records", loadBalanceInfo.get("minRecords"));
                        resultData.add(basicStats);
                        
                        result = new ExecutionResult(true, "查询分片统计信息成功", resultData);
                    }
                } catch (Exception e) {
                    result = new ExecutionResult(false, "查询分片统计失败: " + e.getMessage(), null);
                }
            }
            // 特殊处理函数相关的执行计划
            else if (plan instanceof CreateFunctionPlan) {
                try {
                    result = ((CreateFunctionPlan) plan).execute(this);
                } catch (Exception e) {
                    result = new ExecutionResult(false, "创建函数失败: " + e.getMessage(), null);
                }
            } else if (plan instanceof CallPlan) {
                try {
                    result = ((CallPlan) plan).execute(this);
                } catch (Exception e) {
                    result = new ExecutionResult(false, "调用函数失败: " + e.getMessage(), null);
                }
            } else if (plan instanceof DropFunctionPlan) {
                try {
                    result = ((DropFunctionPlan) plan).execute(this);
                } catch (Exception e) {
                    result = new ExecutionResult(false, "删除函数失败: " + e.getMessage(), null);
                }
            } else if (plan instanceof BatchPlan) {
                result = executeBatchWithFunctionSupport((BatchPlan) plan);
            } else {
                result = executor.execute(plan);
            }
            
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
            // 将GUI的存储格式转换为内部格式
            String storageFormat = convertStorageFormat(currentStorageFormat);
            CreateTablePlan plan = new CreateTablePlan(tableName, columns, constraints, storageFormat);
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
     * 获取视图管理器
     */
    public ViewManager getViewManager() {
        return viewManager;
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
        // 将索引类型传递给执行器
        executor.setIndexType(indexType);
        System.out.println("索引类型已设置为: " + indexType);
    }
    
    /**
     * 获取当前索引类型
     */
    public String getCurrentIndexType() {
        return currentIndexType;
    }
    
    /**
     * 设置存储格式
     */
    public void setStorageFormat(String storageFormat) {
        this.currentStorageFormat = storageFormat;
        System.out.println("存储格式已设置为: " + storageFormat);
    }

    /**
     * 获取当前存储格式
     */
    public String getCurrentStorageFormat() {
        return currentStorageFormat;
    }

    /**
     * 转换存储格式
     */
    private String convertStorageFormat(String guiFormat) {
        if ("列式存储".equals(guiFormat)) {
            return "COLUMN";
        } else {
            return "ROW";
        }
    }

    /**
     * 修改SQL语句以使用GUI选择的存储格式
     */
    private String modifySQLForStorageFormat(String sql) {
        // 检查是否是批量SQL语句
        if (sql.contains(";") && sql.split(";").length > 1) {
            // 批量SQL语句，分别处理每个语句
            String[] statements = sql.split(";");
            StringBuilder modifiedSql = new StringBuilder();
            
            for (int i = 0; i < statements.length; i++) {
                String statement = statements[i].trim();
                if (!statement.isEmpty()) {
                    String modifiedStatement = modifySingleSQLForStorageFormat(statement);
                    modifiedSql.append(modifiedStatement);
                    if (i < statements.length - 1) {
                        modifiedSql.append("; ");
                    }
                }
            }
            
            return modifiedSql.toString();
        } else {
            // 单个SQL语句
            return modifySingleSQLForStorageFormat(sql);
        }
    }
    
    /**
     * 修改单个SQL语句以使用GUI选择的存储格式
     */
    private String modifySingleSQLForStorageFormat(String sql) {
        // 只处理CREATE TABLE语句
        if (!sql.trim().toUpperCase().startsWith("CREATE TABLE")) {
            return sql;
        }

        // 获取GUI选择的存储格式
        String guiStorageFormat = convertStorageFormat(currentStorageFormat);

        // 检查SQL中是否已经有STORAGE子句
        String upperSql = sql.toUpperCase();
        if (upperSql.contains("STORAGE")) {
            // 替换现有的STORAGE子句
            String pattern = "\\s+STORAGE\\s+(ROW|COLUMN)\\s*";
            String replacement = " STORAGE " + guiStorageFormat + " ";
            return sql.replaceAll("(?i)" + pattern, replacement);
        } else {
            // 添加STORAGE子句
            // 找到最后一个右括号
            int lastParenIndex = sql.lastIndexOf(')');
            if (lastParenIndex != -1) {
                // 在右括号后插入STORAGE子句
                String beforeParen = sql.substring(0, lastParenIndex + 1);
                String afterParen = sql.substring(lastParenIndex + 1);
                return beforeParen + " STORAGE " + guiStorageFormat + afterParen;
            }
        }

        return sql;
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
     * 获取函数管理器
     */
    public FunctionManager getFunctionManager() {
        return functionManager;
    }

    /**
     * 获取数据目录路径
     */
    public String getDataDirectory() {
        return dataDirectory;
    }

    /**
     * 手动回滚指定事务
     */
    public boolean rollbackTransaction(long transactionId) {
        try {
            System.out.println("开始回滚事务: " + transactionId);
            logManager.executeRollback(transactionId);
            System.out.println("事务 " + transactionId + " 回滚成功");
            return true;
        } catch (Exception e) {
            System.err.println("回滚事务失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取活跃事务列表
     */
    public List<Long> getActiveTransactions() {
        try {
            return new ArrayList<>(logManager.getTransactionLsnMap().keySet());
        } catch (Exception e) {
            System.err.println("获取活跃事务失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取事务的日志条目
     */
    public List<com.database.logging.LogEntry> getTransactionLogs(long transactionId) {
        try {
            return logManager.getTransactionLogs(transactionId);
        } catch (Exception e) {
            System.err.println("获取事务日志失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 执行批量计划（支持函数）
     */
    private ExecutionResult executeBatchWithFunctionSupport(BatchPlan plan) {
        try {
            List<ExecutionResult> results = new ArrayList<>();
            int successCount = 0;
            int totalCount = plan.getPlans().size();

            for (ExecutionPlan subPlan : plan.getPlans()) {
                ExecutionResult result;

                // 对函数相关的执行计划特殊处理
                if (subPlan instanceof CreateFunctionPlan) {
                    try {
                        result = ((CreateFunctionPlan) subPlan).execute(this);
                    } catch (Exception e) {
                        result = new ExecutionResult(false, "创建函数失败: " + e.getMessage(), null);
                    }
                } else if (subPlan instanceof CallPlan) {
                    try {
                        result = ((CallPlan) subPlan).execute(this);
                    } catch (Exception e) {
                        result = new ExecutionResult(false, "调用函数失败: " + e.getMessage(), null);
                    }
                } else if (subPlan instanceof DropFunctionPlan) {
                    try {
                        result = ((DropFunctionPlan) subPlan).execute(this);
                    } catch (Exception e) {
                        result = new ExecutionResult(false, "删除函数失败: " + e.getMessage(), null);
                    }
                } else {
                    // 其他类型的计划使用标准执行器
                    result = executor.execute(subPlan);
                }

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
    
    // ========== SQL文件管理功能 ==========
    
    /**
     * 导入并执行SQL文件
     * @param filePath SQL文件路径
     * @return 执行结果
     */
    public ExecutionResult importSQLFile(String filePath) {
        return sqlFileManager.importAndExecuteSQLFile(filePath);
    }
    
    /**
     * 导入并执行SQL文件
     * @param filePath SQL文件路径
     * @param continueOnError 是否在遇到错误时继续执行
     * @return 执行结果
     */
    public ExecutionResult importSQLFile(String filePath, boolean continueOnError) {
        return sqlFileManager.importAndExecuteSQLFile(filePath, continueOnError);
    }
    
    /**
     * 导出数据库为SQL文件
     * @param outputPath 输出文件路径
     * @return 执行结果
     */
    public ExecutionResult exportDatabaseToSQL(String outputPath) {
        return sqlFileManager.exportDatabaseToSQL(outputPath);
    }
    
    /**
     * 导出数据库为SQL文件
     * @param outputPath 输出文件路径
     * @param tableNames 要导出的表名列表，null表示导出所有表
     * @param includeStructure 是否包含表结构
     * @param includeData 是否包含数据
     * @return 执行结果
     */
    public ExecutionResult exportDatabaseToSQL(String outputPath, List<String> tableNames, 
                                              boolean includeStructure, boolean includeData) {
        return sqlFileManager.exportDatabaseToSQL(outputPath, tableNames, includeStructure, includeData);
    }
    
    /**
     * 导出单个表为SQL文件
     * @param tableName 表名
     * @param outputPath 输出文件路径
     * @return 执行结果
     */
    public ExecutionResult exportTableToSQL(String tableName, String outputPath) {
        return sqlFileManager.exportTableToSQL(tableName, outputPath);
    }
    
    /**
     * 批量导入SQL文件目录
     * @param directoryPath 目录路径
     * @param filePattern 文件名模式（如 "*.sql"）
     * @param continueOnError 是否在遇到错误时继续
     * @return 执行结果
     */
    public ExecutionResult importSQLDirectory(String directoryPath, String filePattern, boolean continueOnError) {
        return sqlFileManager.importSQLDirectory(directoryPath, filePattern, continueOnError);
    }
    
    /**
     * 获取SQL文件管理器
     * @return SQL文件管理器实例
     */
    public SQLFileManager getSQLFileManager() {
        return sqlFileManager;
    }
    
    /**
     * 获取存储适配器
     * @return 存储适配器实例
     */
    public StorageAdapter getStorageAdapter() {
        return executor.getStorageAdapter();
    }
    
    /**
     * 获取分片管理器
     * @return 分片管理器实例
     */
    public ShardManager getShardManager() {
        return shardManager;
    }
    
    /**
     * 获取当前节点ID
     * @return 当前节点ID
     */
    public String getCurrentNodeId() {
        return currentNodeId;
    }
}