package com.sqlcompiler.execution;

import java.util.List;

/**
 * 约束计划
 */
public class ConstraintPlan {
    public enum ConstraintType {
        PRIMARY_KEY,
        FOREIGN_KEY,
        UNIQUE,
        NOT_NULL,
        DEFAULT,
        AUTO_INCREMENT
    }
    
    private final String name;
    private final ConstraintType type;
    private final List<String> columns;
    private final String referencedTable;
    private final List<String> referencedColumns;
    private final String defaultValue;
    
    public ConstraintPlan(String name, ConstraintType type, List<String> columns,
                         String referencedTable, List<String> referencedColumns,
                         String defaultValue) {
        this.name = name;
        this.type = type;
        this.columns = columns;
        this.referencedTable = referencedTable;
        this.referencedColumns = referencedColumns;
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
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("      \"name\": \"").append(name != null ? name : "").append("\",\n");
        sb.append("      \"type\": \"").append(type.name()).append("\",\n");
        sb.append("      \"columns\": [");
        
        for (int i = 0; i < columns.size(); i++) {
            sb.append("\"").append(columns.get(i)).append("\"");
            if (i < columns.size() - 1) {
                sb.append(", ");
            }
        }
        
        sb.append("],\n");
        
        if (referencedTable != null) {
            sb.append("      \"referencedTable\": \"").append(referencedTable).append("\",\n");
        }
        
        if (referencedColumns != null && !referencedColumns.isEmpty()) {
            sb.append("      \"referencedColumns\": [");
            for (int i = 0; i < referencedColumns.size(); i++) {
                sb.append("\"").append(referencedColumns.get(i)).append("\"");
                if (i < referencedColumns.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("],\n");
        }
        
        if (defaultValue != null) {
            sb.append("      \"defaultValue\": \"").append(defaultValue).append("\",\n");
        }
        
        sb.append("    }");
        return sb.toString();
    }
    
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(CONSTRAINT \"").append(type.name()).append("\"");
        
        if (name != null) {
            sb.append(" \"").append(name).append("\"");
        }
        
        sb.append(" (");
        for (String column : columns) {
            sb.append(" \"").append(column).append("\"");
        }
        sb.append(")");
        
        if (referencedTable != null) {
            sb.append(" REFERENCES \"").append(referencedTable).append("\"");
            if (referencedColumns != null && !referencedColumns.isEmpty()) {
                sb.append(" (");
                for (String column : referencedColumns) {
                    sb.append(" \"").append(column).append("\"");
                }
                sb.append(")");
            }
        }
        
        if (defaultValue != null) {
            sb.append(" DEFAULT \"").append(defaultValue).append("\"");
        }
        
        sb.append(")");
        return sb.toString();
    }
}
