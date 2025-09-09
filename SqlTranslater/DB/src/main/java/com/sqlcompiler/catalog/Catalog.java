package com.sqlcompiler.catalog;

import java.util.*;

/**
 * 数据库模式目录
 * 维护数据库中的表、列、约束等元数据信息
 */
public class Catalog {
    private final Map<String, TableInfo> tables;
    
    public Catalog() {
        this.tables = new HashMap<>();
    }
    
    /**
     * 添加表信息
     */
    public void addTable(TableInfo tableInfo) {
        tables.put(tableInfo.getName().toLowerCase(), tableInfo);
    }
    
    /**
     * 获取表信息
     */
    public TableInfo getTable(String tableName) {
        return tables.get(tableName.toLowerCase());
    }
    
    /**
     * 检查表是否存在
     */
    public boolean tableExists(String tableName) {
        return tables.containsKey(tableName.toLowerCase());
    }
    
    /**
     * 获取所有表名
     */
    public Set<String> getAllTableNames() {
        return new HashSet<>(tables.keySet());
    }
    
    /**
     * 删除表
     */
    public void dropTable(String tableName) {
        tables.remove(tableName.toLowerCase());
    }
    
    /**
     * 清空目录
     */
    public void clear() {
        tables.clear();
    }
    
    /**
     * 获取目录信息摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("数据库目录摘要:\n");
        sb.append("表数量: ").append(tables.size()).append("\n");
        
        for (TableInfo table : tables.values()) {
            sb.append("表: ").append(table.getName())
              .append(" (列数: ").append(table.getColumns().size()).append(")\n");
        }
        
        return sb.toString();
    }
}
