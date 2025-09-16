package com.sqlcompiler.catalog;

/**
 * 列信息
 */
public class ColumnInfo {
    private final String name;
    private final String dataType;
    private final int length;
    private final boolean nullable;
    private final boolean primaryKey;
    private final boolean unique;
    private final boolean autoIncrement;
    private final String defaultValue;
    private final boolean notNull;
    
    public ColumnInfo(String name, String dataType, int length) {
        this(name, dataType, length, true, false, false, false, null, false);
    }
    
    public ColumnInfo(String name, String dataType, int length, boolean nullable, Object defaultValue) {
        this(name, dataType, length, nullable, false, false, false, defaultValue != null ? defaultValue.toString() : null, false);
    }
    
    public ColumnInfo(String name, String dataType, int length, boolean nullable, boolean primaryKey, 
                     boolean unique, boolean autoIncrement, String defaultValue, boolean notNull) {
        this.name = name;
        this.dataType = dataType;
        this.length = length;
        this.nullable = nullable;
        this.primaryKey = primaryKey;
        this.unique = unique;
        this.autoIncrement = autoIncrement;
        this.defaultValue = defaultValue;
        this.notNull = notNull;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public int getLength() {
        return length;
    }
    
    public boolean isNullable() {
        return nullable;
    }
    
    public boolean isPrimaryKey() {
        return primaryKey;
    }
    
    public boolean isUnique() {
        return unique;
    }
    
    public boolean isAutoIncrement() {
        return autoIncrement;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public boolean isNotNull() {
        return notNull;
    }
    
    public boolean isCompatibleWith(String value) {
        if (value == null) {
            return nullable;
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
                return value.length() <= length;
            case "DECIMAL":
            case "FLOAT":
            case "DOUBLE":
                try {
                    Double.parseDouble(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            default:
                return true;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ").append(dataType);
        if (length > 0) {
            sb.append("(").append(length).append(")");
        }
        if (!nullable) {
            sb.append(" NOT NULL");
        }
        if (primaryKey) {
            sb.append(" PRIMARY KEY");
        }
        if (unique) {
            sb.append(" UNIQUE");
        }
        if (autoIncrement) {
            sb.append(" AUTO_INCREMENT");
        }
        if (defaultValue != null) {
            sb.append(" DEFAULT ").append(defaultValue);
        }
        return sb.toString();
    }
}