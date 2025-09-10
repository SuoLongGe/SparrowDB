package com.sqlcompiler.catalog;

import java.util.List;

/**
 * 列信息
 */
public class ColumnInfo {
    private final String name;
    private final String dataType;
    private final Integer length;
    private final boolean notNull;
    private final boolean primaryKey;
    private final boolean unique;
    private final String defaultValue;
    private final boolean autoIncrement;
    
    public ColumnInfo(String name, String dataType, Integer length, 
                     boolean notNull, boolean primaryKey, boolean unique,
                     String defaultValue, boolean autoIncrement) {
        this.name = name;
        this.dataType = dataType;
        this.length = length;
        this.notNull = notNull;
        this.primaryKey = primaryKey;
        this.unique = unique;
        this.defaultValue = defaultValue;
        this.autoIncrement = autoIncrement;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public Integer getLength() {
        return length;
    }
    
    public boolean isNotNull() {
        return notNull;
    }
    
    public boolean isPrimaryKey() {
        return primaryKey;
    }
    
    public boolean isUnique() {
        return unique;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public boolean isAutoIncrement() {
        return autoIncrement;
    }
    
    /**
     * 检查数据类型是否兼容
     */
    public boolean isCompatibleWith(String value) {
        if (value == null) {
            return !notNull;
        }
        
        switch (dataType.toUpperCase()) {
            case "INT":
            case "INTEGER":
                try {
                    Integer.parseInt(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case "VARCHAR":
            case "CHAR":
            case "TEXT":
                return true; // 字符串类型可以接受任何值
            case "DECIMAL":
            case "FLOAT":
            case "DOUBLE":
                try {
                    Double.parseDouble(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case "BOOLEAN":
                return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
            case "DATE":
            case "TIME":
            case "TIMESTAMP":
                // 简单检查，实际应用中需要更复杂的日期格式验证
                return value.matches("\\d{4}-\\d{2}-\\d{2}.*");
            default:
                return true;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ").append(dataType);
        if (length != null) {
            sb.append("(").append(length).append(")");
        }
        if (notNull) sb.append(" NOT NULL");
        if (primaryKey) sb.append(" PRIMARY KEY");
        if (unique) sb.append(" UNIQUE");
        if (defaultValue != null) sb.append(" DEFAULT ").append(defaultValue);
        if (autoIncrement) sb.append(" AUTO_INCREMENT");
        return sb.toString();
    }
}
