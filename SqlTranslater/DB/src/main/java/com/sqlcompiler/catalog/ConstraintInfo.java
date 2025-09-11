package com.sqlcompiler.catalog;

import java.util.List;

/**
 * 约束信息
 */
public class ConstraintInfo {
    public enum ConstraintType {
        PRIMARY_KEY,
        FOREIGN_KEY,
        UNIQUE,
        CHECK,
        NOT_NULL,
        DEFAULT,
        AUTO_INCREMENT
    }
    
    private final String name;
    private final ConstraintType type;
    private final List<String> columns;
    private final String referencedTable;
    private final List<String> referencedColumns;
    private final String checkCondition;
    private final String defaultValue;
    
    public ConstraintInfo(String name, ConstraintType type, List<String> columns) {
        this(name, type, columns, null, null, null, null);
    }
    
    public ConstraintInfo(String name, ConstraintType type, List<String> columns, 
                         String referencedTable, List<String> referencedColumns, String checkCondition, String defaultValue) {
        this.name = name;
        this.type = type;
        this.columns = columns;
        this.referencedTable = referencedTable;
        this.referencedColumns = referencedColumns;
        this.checkCondition = checkCondition;
        this.defaultValue = defaultValue;
    }
    
    public String getName() {
        return name;
    }
    
    public ConstraintType getType() {
        return type;
    }
    
    public List<String> getColumns() {
        return columns;
    }
    
    public String getReferencedTable() {
        return referencedTable;
    }
    
    public List<String> getReferencedColumns() {
        return referencedColumns;
    }
    
    public String getCheckCondition() {
        return checkCondition;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(type);
        sb.append(" (").append(String.join(", ", columns)).append(")");
        
        if (type == ConstraintType.FOREIGN_KEY && referencedTable != null) {
            sb.append(" REFERENCES ").append(referencedTable);
            if (referencedColumns != null && !referencedColumns.isEmpty()) {
                sb.append("(").append(String.join(", ", referencedColumns)).append(")");
            }
        }
        
        if (type == ConstraintType.CHECK && checkCondition != null) {
            sb.append(" CHECK (").append(checkCondition).append(")");
        }
        
        if (type == ConstraintType.DEFAULT && defaultValue != null) {
            sb.append(" DEFAULT ").append(defaultValue);
        }
        
        return sb.toString();
    }
}
