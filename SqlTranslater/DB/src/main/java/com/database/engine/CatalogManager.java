package com.database.engine;

import com.sqlcompiler.catalog.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 系统目录管理器 - 维护元数据，作为特殊表存储
 * 现在使用StorageAdapter来支持更高级的存储系统
 */
public class CatalogManager {
    private final Catalog catalog;
    private final StorageEngine storageEngine;
    private StorageAdapter storageAdapter;
    private final String systemTableName = "__system_tables__";
    private final String systemColumnsName = "__system_columns__";
    private final String systemConstraintsName = "__system_constraints__";
    private final String systemFunctionsName = "__system_functions__";
    
    public CatalogManager(StorageEngine storageEngine) {
        this.catalog = new Catalog();
        this.storageEngine = storageEngine;
        // 延迟初始化系统表，等待StorageAdapter设置
    }
    
    public void setStorageAdapter(StorageAdapter storageAdapter) {
        this.storageAdapter = storageAdapter;
        // 现在可以安全地初始化系统表
        initializeSystemTables();
    }
    
    /**
     * 添加表到目录
     */
    public void addTable(TableInfo tableInfo) {
        catalog.addTable(tableInfo);
        persistTableMetadata(tableInfo);
    }
    
    /**
     * 获取表信息
     */
    public TableInfo getTable(String tableName) {
        return catalog.getTable(tableName);
    }
    
    /**
     * 检查表是否存在
     */
    public boolean tableExists(String tableName) {
        return catalog.tableExists(tableName);
    }
    
    /**
     * 删除表
     */
    public void dropTable(String tableName) {
        catalog.dropTable(tableName);
        removeTableMetadata(tableName);
    }
    
    /**
     * 获取所有表名
     */
    public Set<String> getAllTableNames() {
        return catalog.getAllTableNames();
    }
    
    /**
     * 获取目录摘要
     */
    public String getCatalogSummary() {
        return catalog.getSummary();
    }
    
    /**
     * 获取内部Catalog实例
     */
    public Catalog getCatalog() {
        return catalog;
    }
    
    /**
     * 清空目录
     */
    public void clear() {
        catalog.clear();
    }
    
    /**
     * 从存储中加载目录信息
     */
    public void loadFromStorage() {
        try {
            // 方法1: 尝试从系统表加载
            List<Map<String, Object>> tableRecords = storageEngine.scanTable(systemTableName);
            Set<String> loadedTables = new HashSet<>();
            
            for (Map<String, Object> record : tableRecords) {
                String tableName = (String) record.get("table_name");
                if (tableName != null && !tableName.startsWith("__system_")) {
                    TableInfo tableInfo = loadTableInfo(tableName);
                    if (tableInfo != null) {
                        catalog.addTable(tableInfo);
                        loadedTables.add(tableName);
                    }
                }
            }
            
            // 方法2: 直接扫描data目录中的.tbl文件来发现未注册的表
            loadTablesFromDataDirectory(loadedTables);
            
        } catch (Exception e) {
            System.err.println("从存储加载目录信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存目录信息到存储
     */
    public void saveToStorage() {
        try {
            // 保存所有表信息
            for (String tableName : catalog.getAllTableNames()) {
                if (!tableName.startsWith("__system_")) {
                    TableInfo tableInfo = catalog.getTable(tableName);
                    persistTableMetadata(tableInfo);
                }
            }
        } catch (Exception e) {
            System.err.println("保存目录信息到存储失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取表统计信息
     */
    public Map<String, Object> getTableStatistics(String tableName) {
        Map<String, Object> stats = new HashMap<>();
        
        if (!tableExists(tableName)) {
            return stats;
        }
        
        TableInfo tableInfo = getTable(tableName);
        StorageEngine.TableStats storageStats = storageEngine.getTableStats(tableName);
        
        stats.put("table_name", tableName);
        stats.put("column_count", tableInfo.getColumns().size());
        stats.put("constraint_count", tableInfo.getConstraints().size());
        
        if (storageStats != null) {
            stats.put("page_count", storageStats.getPageCount());
            stats.put("record_count", storageStats.getRecordCount());
        }
        
        return stats;
    }
    
    /**
     * 获取数据库统计信息
     */
    public Map<String, Object> getDatabaseStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalTables = 0;
        int totalColumns = 0;
        int totalConstraints = 0;
        int totalPages = 0;
        int totalRecords = 0;
        
        for (String tableName : catalog.getAllTableNames()) {
            if (!tableName.startsWith("__system_")) {
                totalTables++;
                
                TableInfo tableInfo = getTable(tableName);
                totalColumns += tableInfo.getColumns().size();
                totalConstraints += tableInfo.getConstraints().size();
                
                StorageEngine.TableStats tableStats = storageEngine.getTableStats(tableName);
                if (tableStats != null) {
                    totalPages += tableStats.getPageCount();
                    totalRecords += tableStats.getRecordCount();
                }
            }
        }
        
        stats.put("total_tables", totalTables);
        stats.put("total_columns", totalColumns);
        stats.put("total_constraints", totalConstraints);
        stats.put("total_pages", totalPages);
        stats.put("total_records", totalRecords);
        
        return stats;
    }
    
    // 私有辅助方法
    
    private void initializeSystemTables() {
        // 创建系统表表
        TableInfo systemTablesInfo = new TableInfo(systemTableName);
        systemTablesInfo.addColumn(new ColumnInfo("table_name", "VARCHAR", 255, true, true, false, false, null, false));
        systemTablesInfo.addColumn(new ColumnInfo("create_time", "BIGINT", 8, false, false, false, false, null, false));
        systemTablesInfo.addColumn(new ColumnInfo("column_count", "INT", 4, false, false, false, false, null, false));
        systemTablesInfo.addColumn(new ColumnInfo("constraint_count", "INT", 4, false, false, false, false, null, false));
        catalog.addTable(systemTablesInfo);
        
        // 创建系统列表
        TableInfo systemColumnsInfo = new TableInfo(systemColumnsName);
        systemColumnsInfo.addColumn(new ColumnInfo("table_name", "VARCHAR", 255, true, false, false, false, null, false));
        systemColumnsInfo.addColumn(new ColumnInfo("column_name", "VARCHAR", 255, true, false, false, false, null, false));
        systemColumnsInfo.addColumn(new ColumnInfo("data_type", "VARCHAR", 50, false, false, false, false, null, false));
        systemColumnsInfo.addColumn(new ColumnInfo("length", "INT", 4, false, false, false, false, null, false));
        systemColumnsInfo.addColumn(new ColumnInfo("not_null", "BOOLEAN", 1, false, false, false, false, "false", false));
        systemColumnsInfo.addColumn(new ColumnInfo("primary_key", "BOOLEAN", 1, false, false, false, false, "false", false));
        systemColumnsInfo.addColumn(new ColumnInfo("unique", "BOOLEAN", 1, false, false, false, false, "false", false));
        systemColumnsInfo.addColumn(new ColumnInfo("default_value", "VARCHAR", 255, false, false, false, false, null, false));
        systemColumnsInfo.addColumn(new ColumnInfo("auto_increment", "BOOLEAN", 1, false, false, false, false, "false", false));
        catalog.addTable(systemColumnsInfo);
        
        // 创建系统约束表
        TableInfo systemConstraintsInfo = new TableInfo(systemConstraintsName);
        systemConstraintsInfo.addColumn(new ColumnInfo("table_name", "VARCHAR", 255, true, false, false, false, null, false));
        systemConstraintsInfo.addColumn(new ColumnInfo("constraint_name", "VARCHAR", 255, true, false, false, false, null, false));
        systemConstraintsInfo.addColumn(new ColumnInfo("constraint_type", "VARCHAR", 50, false, false, false, false, null, false));
        systemConstraintsInfo.addColumn(new ColumnInfo("columns", "VARCHAR", 1000, false, false, false, false, null, false));
        systemConstraintsInfo.addColumn(new ColumnInfo("referenced_table", "VARCHAR", 255, false, false, false, false, null, false));
        systemConstraintsInfo.addColumn(new ColumnInfo("referenced_columns", "VARCHAR", 1000, false, false, false, false, null, false));
        systemConstraintsInfo.addColumn(new ColumnInfo("default_value", "VARCHAR", 255, false, false, false, false, null, false));
        catalog.addTable(systemConstraintsInfo);
        
        // 创建系统函数表
        TableInfo systemFunctionsInfo = new TableInfo(systemFunctionsName);
        systemFunctionsInfo.addColumn(new ColumnInfo("function_name", "VARCHAR", 255, true, false, false, false, null, false));
        systemFunctionsInfo.addColumn(new ColumnInfo("signature", "VARCHAR", 500, false, false, false, false, null, false));
        systemFunctionsInfo.addColumn(new ColumnInfo("return_type", "VARCHAR", 50, false, false, false, false, null, false));
        systemFunctionsInfo.addColumn(new ColumnInfo("body", "TEXT", 10000, false, false, false, false, null, false));
        systemFunctionsInfo.addColumn(new ColumnInfo("is_permanent", "BOOLEAN", 1, false, false, false, false, "false", false));
        systemFunctionsInfo.addColumn(new ColumnInfo("create_time", "BIGINT", 8, false, false, false, false, null, false));
        catalog.addTable(systemFunctionsInfo);
        
        // 总是创建系统表的存储，确保StorageEngine中有注册
        System.out.println("创建所有系统表存储...");
        storageEngine.createTableStorage(systemTableName, systemTablesInfo);
        storageEngine.createTableStorage(systemColumnsName, systemColumnsInfo);
        storageEngine.createTableStorage(systemConstraintsName, systemConstraintsInfo);
        storageEngine.createTableStorage(systemFunctionsName, systemFunctionsInfo);
        System.out.println("系统表存储创建完成");
    }
    
    /**
     * 检查表是否已存在于系统表中
     */
    private boolean checkTableExists(String tableName) {
        try {
            if (storageAdapter == null) {
                return false;
            }
            
            // 查询系统表，检查表是否已存在
            List<Map<String, Object>> records = storageAdapter.scanTable(systemTableName);
            if (records != null) {
                for (Map<String, Object> record : records) {
                    String existingTableName = (String) record.get("table_name");
                    if (tableName.equals(existingTableName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("检查表是否存在时出错: " + e.getMessage());
            return false;
        }
    }
    
    private void persistTableMetadata(TableInfo tableInfo) {
        try {
            System.out.println("开始持久化表元数据: " + tableInfo.getName());
            
            // 检查表基本信息是否已存在
            boolean tableExists = checkTableExists(tableInfo.getName());
            if (tableExists) {
                System.out.println("表 " + tableInfo.getName() + " 的元数据已存在，跳过插入");
                return;
            }
            
            // 保存表基本信息
            Map<String, Object> tableRecord = new HashMap<>();
            tableRecord.put("table_name", tableInfo.getName());
            tableRecord.put("create_time", System.currentTimeMillis());
            tableRecord.put("column_count", tableInfo.getColumns().size());
            tableRecord.put("constraint_count", tableInfo.getConstraints().size());
            
            System.out.println("插入表基本信息到 " + systemTableName);
            boolean tableInsertSuccess = storageEngine.insertRecord(systemTableName, tableRecord);
            System.out.println("表基本信息插入结果: " + tableInsertSuccess);
            
            // 保存列信息
            System.out.println("开始插入列信息，共 " + tableInfo.getColumns().size() + " 列");
            for (ColumnInfo column : tableInfo.getColumns()) {
                Map<String, Object> columnRecord = new HashMap<>();
                columnRecord.put("table_name", tableInfo.getName());
                columnRecord.put("column_name", column.getName());
                columnRecord.put("data_type", column.getDataType());
                columnRecord.put("length", column.getLength());
                columnRecord.put("not_null", column.isNotNull());
                columnRecord.put("primary_key", column.isPrimaryKey());
                columnRecord.put("unique", column.isUnique());
                columnRecord.put("default_value", column.getDefaultValue());
                columnRecord.put("auto_increment", column.isAutoIncrement());
                
                System.out.println("插入列信息: " + column.getName() + " (" + column.getDataType() + ")");
                boolean columnInsertSuccess = storageEngine.insertRecord(systemColumnsName, columnRecord);
                System.out.println("列信息插入结果: " + columnInsertSuccess);
            }
            
            // 保存约束信息
            System.out.println("开始插入约束信息，共 " + tableInfo.getConstraints().size() + " 个约束");
            for (ConstraintInfo constraint : tableInfo.getConstraints()) {
                Map<String, Object> constraintRecord = new HashMap<>();
                constraintRecord.put("table_name", tableInfo.getName());
                constraintRecord.put("constraint_name", constraint.getName());
                constraintRecord.put("constraint_type", constraint.getType().toString());
                constraintRecord.put("columns", String.join(",", constraint.getColumns()));
                constraintRecord.put("referenced_table", constraint.getReferencedTable());
                constraintRecord.put("referenced_columns", constraint.getReferencedColumns() != null ? 
                    String.join(",", constraint.getReferencedColumns()) : null);
                constraintRecord.put("default_value", constraint.getDefaultValue());
                
                System.out.println("插入约束信息: " + constraint.getName() + " (" + constraint.getType() + ")");
                boolean constraintInsertSuccess = storageEngine.insertRecord(systemConstraintsName, constraintRecord);
                System.out.println("约束信息插入结果: " + constraintInsertSuccess);
            }
            
            System.out.println("表元数据持久化完成: " + tableInfo.getName());
            
        } catch (Exception e) {
            System.err.println("持久化表元数据失败: " + e.getMessage());
            e.printStackTrace();
            // 重新抛出异常，让调用者知道失败
            throw new RuntimeException("持久化表元数据失败: " + e.getMessage(), e);
        }
    }
    
    private void removeTableMetadata(String tableName) {
        try {
            // 删除表记录
            Map<String, Object> tableRecord = new HashMap<>();
            tableRecord.put("table_name", tableName);
            storageEngine.deleteRecord(systemTableName, tableRecord);
            
            // 删除列记录
            Map<String, Object> columnRecord = new HashMap<>();
            columnRecord.put("table_name", tableName);
            storageEngine.deleteRecord(systemColumnsName, columnRecord);
            
            // 删除约束记录
            Map<String, Object> constraintRecord = new HashMap<>();
            constraintRecord.put("table_name", tableName);
            storageEngine.deleteRecord(systemConstraintsName, constraintRecord);
            
        } catch (Exception e) {
            System.err.println("删除表元数据失败: " + e.getMessage());
        }
    }
    
    private TableInfo loadTableInfo(String tableName) {
        try {
            // 加载表基本信息
            Map<String, Object> tableRecord = findTableRecord(tableName);
            if (tableRecord == null) {
                return null;
            }
            
            TableInfo tableInfo = new TableInfo(tableName);
            
            // 加载列信息
            List<Map<String, Object>> columnRecords = findColumnRecords(tableName);
            for (Map<String, Object> columnRecord : columnRecords) {
                ColumnInfo columnInfo = new ColumnInfo(
                    (String) columnRecord.get("column_name"),
                    (String) columnRecord.get("data_type"),
                    (Integer) columnRecord.get("length"),
                    (Boolean) columnRecord.get("not_null"),
                    (Boolean) columnRecord.get("primary_key"),
                    (Boolean) columnRecord.get("unique"),
                    (Boolean) columnRecord.get("auto_increment"),
                    (String) columnRecord.get("default_value"),
                    (Boolean) columnRecord.get("not_null")
                );
                tableInfo.addColumn(columnInfo);
            }
            
            // 加载约束信息
            List<Map<String, Object>> constraintRecords = findConstraintRecords(tableName);
            for (Map<String, Object> constraintRecord : constraintRecords) {
                ConstraintInfo constraintInfo = new ConstraintInfo(
                    (String) constraintRecord.get("constraint_name"),
                    ConstraintInfo.ConstraintType.valueOf((String) constraintRecord.get("constraint_type")),
                    Arrays.asList(((String) constraintRecord.get("columns")).split(",")),
                    (String) constraintRecord.get("referenced_table"),
                    constraintRecord.get("referenced_columns") != null ? 
                        Arrays.asList(((String) constraintRecord.get("referenced_columns")).split(",")) : null,
                    null,
                    (String) constraintRecord.get("default_value")
                );
                tableInfo.addConstraint(constraintInfo);
            }
            
            return tableInfo;
            
        } catch (Exception e) {
            System.err.println("加载表信息失败: " + e.getMessage());
            return null;
        }
    }
    
    private Map<String, Object> findTableRecord(String tableName) {
        List<Map<String, Object>> records = storageEngine.scanTable(systemTableName);
        for (Map<String, Object> record : records) {
            if (tableName.equals(record.get("table_name"))) {
                return record;
            }
        }
        return null;
    }
    
    private List<Map<String, Object>> findColumnRecords(String tableName) {
        List<Map<String, Object>> allRecords = storageEngine.scanTable(systemColumnsName);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map<String, Object> record : allRecords) {
            if (tableName.equals(record.get("table_name"))) {
                result.add(record);
            }
        }
        
        return result;
    }
    
    private List<Map<String, Object>> findConstraintRecords(String tableName) {
        List<Map<String, Object>> allRecords = storageEngine.scanTable(systemConstraintsName);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map<String, Object> record : allRecords) {
            if (tableName.equals(record.get("table_name"))) {
                result.add(record);
            }
        }
        
        return result;
    }
    
    /**
     * 从data目录扫描.tbl文件和列式存储目录来发现未注册的表
     */
    private void loadTablesFromDataDirectory(Set<String> alreadyLoadedTables) {
        try {
            // 获取data目录路径
            String dataDir = getDataDirectory();
            File dataDirectory = new File(dataDir);
            
            if (!dataDirectory.exists() || !dataDirectory.isDirectory()) {
                return;
            }
            
            // 1. 扫描所有.tbl文件
            File[] tblFiles = dataDirectory.listFiles((dir, name) -> 
                name.endsWith(".tbl") && !name.startsWith("__system_"));
            
            if (tblFiles != null) {
                for (File tblFile : tblFiles) {
                    String fileName = tblFile.getName();
                    String tableName = fileName.substring(0, fileName.lastIndexOf(".tbl"));
                    
                    // 跳过已经加载的表
                    if (alreadyLoadedTables.contains(tableName)) {
                        continue;
                    }
                    
                    // 尝试从文件头解析表结构
                    TableInfo tableInfo = parseTableInfoFromFile(tblFile);
                    if (tableInfo != null) {
                        catalog.addTable(tableInfo);
                        System.out.println("从.tbl文件发现并加载表: " + tableName);
                        
                        // 将表信息持久化到系统表中
                        persistTableMetadata(tableInfo);
                    }
                }
            }
            
            // 2. 扫描列式存储目录
            File[] subDirs = dataDirectory.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    String tableName = subDir.getName();
                    
                    // 跳过已经加载的表
                    if (alreadyLoadedTables.contains(tableName)) {
                        continue;
                    }
                    
                    // 检查是否为列式存储表
                    String metaFile = subDir.getAbsolutePath() + File.separator + "metadata.txt";
                    File meta = new File(metaFile);
                    
                    if (meta.exists() && isColumnarStorageTable(metaFile)) {
                        // 从元数据文件解析表结构
                        TableInfo tableInfo = parseTableInfoFromColumnarMetadata(metaFile);
                        if (tableInfo != null) {
                            catalog.addTable(tableInfo);
                            System.out.println("从列式存储发现并加载表: " + tableName);
                            
                            // 将表信息持久化到系统表中
                            persistTableMetadata(tableInfo);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("扫描data目录失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否为列式存储表
     */
    private boolean isColumnarStorageTable(String metaFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("STORAGE_FORMAT=")) {
                    String format = line.substring("STORAGE_FORMAT=".length());
                    return "COLUMN".equalsIgnoreCase(format);
                }
            }
        } catch (IOException e) {
            // 如果读取失败，假设不是列式存储
        }
        return false;
    }
    
    /**
     * 从列式存储元数据文件解析表结构信息
     */
    private TableInfo parseTableInfoFromColumnarMetadata(String metaFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile, StandardCharsets.UTF_8))) {
            String line;
            String tableName = null;
            List<ColumnInfo> columns = new ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("COLUMN=")) {
                    String columnName = line.substring("COLUMN=".length());
                    // 根据列名推断数据类型
                    String dataType = inferDataType(columnName);
                    int length = inferLength(columnName);
                    boolean isPrimaryKey = "id".equals(columnName) || "ID".equals(columnName);
                    
                    columns.add(new ColumnInfo(columnName, dataType, length, isPrimaryKey, false));
                }
            }
            
            // 如果没有找到COLUMN=格式，尝试从列文件推断列信息
            if (columns.isEmpty()) {
                File meta = new File(metaFile);
                File tableDir = meta.getParentFile();
                if (tableDir != null && tableDir.isDirectory()) {
                    File[] colFiles = tableDir.listFiles((dir, name) -> name.endsWith(".col"));
                    if (colFiles != null) {
                        for (File colFile : colFiles) {
                            String fileName = colFile.getName();
                            String columnName = fileName.substring(0, fileName.lastIndexOf(".col"));
                            
                            // 根据列名推断数据类型
                            String dataType = inferDataType(columnName);
                            int length = inferLength(columnName);
                            boolean isPrimaryKey = "id".equals(columnName) || "ID".equals(columnName);
                            
                            columns.add(new ColumnInfo(columnName, dataType, length, isPrimaryKey, false));
                        }
                    }
                }
            }
            
            if (!columns.isEmpty()) {
                // 从文件路径推断表名
                File meta = new File(metaFile);
                tableName = meta.getParentFile().getName();
                
                TableInfo tableInfo = new TableInfo(tableName, "COLUMN");
                for (ColumnInfo column : columns) {
                    tableInfo.addColumn(column);
                }
                return tableInfo;
            }
        } catch (IOException e) {
            System.err.println("解析列式存储元数据失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 推断数据类型
     */
    private String inferDataType(String columnName) {
        if ("id".equals(columnName)) {
            return "INT";
        } else if ("price".equals(columnName)) {
            return "DECIMAL";
        } else if ("quantity".equals(columnName)) {
            return "INT";
        } else {
            return "VARCHAR";
        }
    }
    
    /**
     * 推断长度
     */
    private int inferLength(String columnName) {
        if ("id".equals(columnName) || "quantity".equals(columnName)) {
            return 0;
        } else if ("price".equals(columnName)) {
            return 10;
        } else if ("product_name".equals(columnName)) {
            return 100;
        } else {
            return 50;
        }
    }
    
    /**
     * 从.tbl文件头解析表结构信息
     */
    private TableInfo parseTableInfoFromFile(File tblFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(tblFile))) {
            String line;
            String tableName = null;
            List<ColumnInfo> columns = new ArrayList<>();
            
            // 读取文件头的元数据
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.equals("# End Metadata")) {
                    break;
                }
                
                if (line.startsWith("TABLE_NAME=")) {
                    tableName = line.substring("TABLE_NAME=".length());
                } else if (line.startsWith("COLUMN=")) {
                    String columnDef = line.substring("COLUMN=".length());
                    ColumnInfo columnInfo = parseColumnDefinition(columnDef);
                    if (columnInfo != null) {
                        columns.add(columnInfo);
                    }
                }
            }
            
            if (tableName != null && !columns.isEmpty()) {
                TableInfo tableInfo = new TableInfo(tableName);
                for (ColumnInfo column : columns) {
                    tableInfo.addColumn(column);
                }
                return tableInfo;
            }
            
        } catch (Exception e) {
            System.err.println("解析表文件失败: " + tblFile.getName() + " - " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 解析列定义字符串 (格式: name:type:length)
     */
    private ColumnInfo parseColumnDefinition(String columnDef) {
        try {
            String[] parts = columnDef.split(":");
            if (parts.length >= 3) {
                String name = parts[0];
                String dataType = parts[1];
                int length = Integer.parseInt(parts[2]);
                
                // 创建列信息（默认值，后续可以通过其他方式获取更详细信息）
                return new ColumnInfo(name, dataType, length, false, false, false, false, null, false);
            }
        } catch (Exception e) {
            System.err.println("解析列定义失败: " + columnDef + " - " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取数据目录路径
     */
    private String getDataDirectory() {
        // 从StorageEngine获取数据目录路径
        return storageEngine.getDataDirectory();
    }
}