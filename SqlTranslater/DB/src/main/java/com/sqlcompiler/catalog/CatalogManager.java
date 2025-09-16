package com.sqlcompiler.catalog;

import com.database.engine.StorageEngine;
import java.util.*;
import java.io.*;

/**
 * 目录管理器 - 管理数据库的元数据
 */
public class CatalogManager {
    private final StorageEngine storageEngine;
    private final Map<String, TableInfo> tables;
    private final Map<String, Map<String, Object>> tableStats;
    
    public CatalogManager(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
        this.tables = new HashMap<>();
        this.tableStats = new HashMap<>();
    }
    
    /**
     * 从存储加载目录信息
     */
    public void loadFromStorage() {
        // 从系统表文件加载表信息
        File catalogFile = new File("catalog.dat");
        if (catalogFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(catalogFile))) {
                String line;
                TableInfo currentTable = null;
                
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("TABLE:")) {
                        String tableName = line.substring(6).trim();
                        currentTable = new TableInfo(tableName);
                        tables.put(tableName, currentTable);
                    } else if (line.startsWith("COLUMN:") && currentTable != null) {
                        String[] parts = line.substring(7).split(":");
                        if (parts.length >= 3) {
                            String name = parts[0];
                            String type = parts[1];
                            int length = Integer.parseInt(parts[2]);
                            boolean isPrimary = parts.length > 3 && Boolean.parseBoolean(parts[3]);
                            boolean isUnique = parts.length > 4 && Boolean.parseBoolean(parts[4]);
                            
                            ColumnInfo column = new ColumnInfo(name, type, length);
                            currentTable.addColumn(column);
                            
                            if (isPrimary) {
                                ConstraintInfo pkConstraint = new ConstraintInfo(
                                    "PK_" + currentTable.getName() + "_" + name,
                                    ConstraintInfo.ConstraintType.PRIMARY_KEY,
                                    Arrays.asList(name)
                                );
                                currentTable.addConstraint(pkConstraint);
                            }
                            if (isUnique) {
                                ConstraintInfo uniqueConstraint = new ConstraintInfo(
                                    "UQ_" + currentTable.getName() + "_" + name,
                                    ConstraintInfo.ConstraintType.UNIQUE,
                                    Arrays.asList(name)
                                );
                                currentTable.addConstraint(uniqueConstraint);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("加载目录信息失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 保存目录信息到存储
     */
    public void saveToStorage() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("catalog.dat"))) {
            for (TableInfo table : tables.values()) {
                writer.println("TABLE:" + table.getName());
                
                for (ColumnInfo column : table.getColumns()) {
                    boolean isPrimary = table.getPrimaryKeyColumns().contains(column.getName());
                    boolean isUnique = false;
                    for (ConstraintInfo constraint : table.getUniqueConstraints()) {
                        if (constraint.getColumns().contains(column.getName())) {
                            isUnique = true;
                            break;
                        }
                    }
                    
                    writer.println("COLUMN:" + column.getName() + ":" + 
                                 column.getDataType() + ":" + column.getLength() + ":" +
                                 isPrimary + ":" + isUnique);
                }
                writer.println();
            }
        } catch (IOException e) {
            System.err.println("保存目录信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建表
     */
    public boolean createTable(String tableName, List<ColumnInfo> columns, List<ConstraintInfo> constraints) {
        if (tables.containsKey(tableName)) {
            return false;
        }
        
        TableInfo table = new TableInfo(tableName);
        for (ColumnInfo column : columns) {
            table.addColumn(column);
        }
        for (ConstraintInfo constraint : constraints) {
            table.addConstraint(constraint);
        }
        
        tables.put(tableName, table);
        
        // 创建物理存储
        boolean result = storageEngine.createTableStorage(tableName, table);
        
        // 如果创建成功，更新系统列表
        if (result) {
            updateSystemColumnsTable(tableName, table);
        }
        
        return result;
    }
    
    /**
     * 删除表
     */
    public boolean dropTable(String tableName) {
        if (!tables.containsKey(tableName)) {
            return false;
        }
        
        tables.remove(tableName);
        tableStats.remove(tableName);
        return true;
    }
    
    /**
     * 获取表信息
     */
    public TableInfo getTable(String tableName) {
        return tables.get(tableName);
    }
    
    /**
     * 检查表是否存在
     */
    public boolean tableExists(String tableName) {
        return tables.containsKey(tableName);
    }
    
    /**
     * 获取所有表名
     */
    public Set<String> getAllTableNames() {
        return new HashSet<>(tables.keySet());
    }
    
    /**
     * 获取目录摘要信息
     */
    public Map<String, Object> getCatalogSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("table_count", tables.size());
        summary.put("tables", new ArrayList<>(tables.keySet()));
        return summary;
    }
    
    /**
     * 获取数据库统计信息
     */
    public Map<String, Object> getDatabaseStatistics() {
        Map<String, Object> stats = new HashMap<>();
        int totalTables = tables.size();
        int totalColumns = 0;
        int totalConstraints = 0;
        
        for (TableInfo table : tables.values()) {
            totalColumns += table.getColumns().size();
            totalConstraints += table.getConstraints().size();
        }
        
        stats.put("total_tables", totalTables);
        stats.put("total_columns", totalColumns);
        stats.put("total_constraints", totalConstraints);
        return stats;
    }
    
    /**
     * 获取表统计信息
     */
    public Map<String, Object> getTableStatistics(String tableName) {
        TableInfo table = tables.get(tableName);
        if (table == null) {
            return null;
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("column_count", table.getColumns().size());
        stats.put("constraint_count", table.getConstraints().size());
        stats.put("primary_key_columns", table.getPrimaryKeyColumns());
        stats.put("unique_constraints", table.getUniqueConstraints().size());
        stats.put("foreign_key_constraints", table.getForeignKeyConstraints().size());
        
        return stats;
    }
    
    /**
     * 更新系统列表 - 当创建新表时调用
     */
    private void updateSystemColumnsTable(String tableName, TableInfo tableInfo) {
        try {
            // 确保系统列表存在
            ensureSystemColumnsTableExists();
            
            // 获取系统列表文件路径
            String systemColumnsPath = storageEngine.getDataDirectory() + File.separator + "__system_columns__.tbl";
            File systemColumnsFile = new File(systemColumnsPath);
            
            // 读取现有内容
            List<String> existingLines = new ArrayList<>();
            boolean foundPageMarker = false;
            
            if (systemColumnsFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(systemColumnsFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        existingLines.add(line);
                        if (line.startsWith("PAGE:")) {
                            foundPageMarker = true;
                        }
                    }
                }
            }
            
            // 如果没有找到PAGE标记，添加一个
            if (!foundPageMarker) {
                existingLines.add("");
                existingLines.add("PAGE:1");
            }
            
            // 写回文件，添加新表的列信息
            try (PrintWriter writer = new PrintWriter(new FileWriter(systemColumnsFile))) {
                // 写入现有内容
                for (String line : existingLines) {
                    writer.println(line);
                }
                
                // 添加新表的列信息
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
                
                writer.println(); // 空行结束
            }
            
            System.out.println("✅ 已更新系统列表，添加表: " + tableName);
            
        } catch (Exception e) {
            System.err.println("更新系统列表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 确保系统列表存在
     */
    private void ensureSystemColumnsTableExists() {
        try {
            String systemColumnsPath = storageEngine.getDataDirectory() + File.separator + "__system_columns__.tbl";
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
                System.out.println("✅ 已创建系统列表文件: " + systemColumnsPath);
            }
        } catch (Exception e) {
            System.err.println("创建系统列表文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
