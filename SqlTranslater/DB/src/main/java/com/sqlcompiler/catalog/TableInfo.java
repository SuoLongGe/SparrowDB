package com.sqlcompiler.catalog;

import java.util.*;

/**
 * 表信息
 */
public class TableInfo {
    private final String name;
    private final Map<String, ColumnInfo> columns;
    private final List<ConstraintInfo> constraints;
    private final String storageFormat; // 存储格式：ROW 或 COLUMN
    
    public TableInfo(String name) {
        this(name, "ROW");
    }
    
    public TableInfo(String name, String storageFormat) {
        this.name = name;
        this.storageFormat = storageFormat != null ? storageFormat : "ROW";
        this.columns = new LinkedHashMap<>();
        this.constraints = new ArrayList<>();
    }
    
    public String getName() {
        return name;
    }
    
    public void addColumn(ColumnInfo column) {
        columns.put(column.getName().toLowerCase(), column);
    }
    
    public ColumnInfo getColumn(String columnName) {
        return columns.get(columnName.toLowerCase());
    }
    
    public boolean columnExists(String columnName) {
        return columns.containsKey(columnName.toLowerCase());
    }
    
    public Collection<ColumnInfo> getColumns() {
        return columns.values();
    }
    
    public List<String> getColumnNames() {
        return new ArrayList<>(columns.keySet());
    }
    
    public void addConstraint(ConstraintInfo constraint) {
        constraints.add(constraint);
    }
    
    public List<ConstraintInfo> getConstraints() {
        return constraints;
    }
    
    public String getStorageFormat() {
        return storageFormat;
    }
    
    public boolean isColumnarStorage() {
        return "COLUMN".equalsIgnoreCase(storageFormat);
    }
    
    public boolean isRowStorage() {
        return "ROW".equalsIgnoreCase(storageFormat);
    }
    
    /**
     * 获取主键列名
     */
    public List<String> getPrimaryKeyColumns() {
        for (ConstraintInfo constraint : constraints) {
            if (constraint.getType() == ConstraintInfo.ConstraintType.PRIMARY_KEY) {
                return constraint.getColumns();
            }
        }
        return new ArrayList<>();
    }
    
    /**
     * 获取外键约束
     */
    public List<ConstraintInfo> getForeignKeyConstraints() {
        List<ConstraintInfo> fkConstraints = new ArrayList<>();
        for (ConstraintInfo constraint : constraints) {
            if (constraint.getType() == ConstraintInfo.ConstraintType.FOREIGN_KEY) {
                fkConstraints.add(constraint);
            }
        }
        return fkConstraints;
    }
    
    /**
     * 获取唯一约束
     */
    public List<ConstraintInfo> getUniqueConstraints() {
        List<ConstraintInfo> uniqueConstraints = new ArrayList<>();
        for (ConstraintInfo constraint : constraints) {
            if (constraint.getType() == ConstraintInfo.ConstraintType.UNIQUE) {
                uniqueConstraints.add(constraint);
            }
        }
        return uniqueConstraints;
    }
}
