package com.sqlcompiler.execution;

/**
 * 列计划
 */
public class ColumnPlan {
    private final String name;
    private final String dataType;
    private final Integer length;
    private final boolean notNull;
    private final boolean primaryKey;
    private final boolean unique;
    private final String defaultValue;
    private final boolean autoIncrement;
    
    public ColumnPlan(String name, String dataType, Integer length, 
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
    
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("      \"name\": \"").append(name).append("\",\n");
        sb.append("      \"dataType\": \"").append(dataType).append("\",\n");
        if (length != null) {
            sb.append("      \"length\": ").append(length).append(",\n");
        }
        sb.append("      \"notNull\": ").append(notNull).append(",\n");
        sb.append("      \"primaryKey\": ").append(primaryKey).append(",\n");
        sb.append("      \"unique\": ").append(unique).append(",\n");
        if (defaultValue != null) {
            sb.append("      \"defaultValue\": \"").append(defaultValue).append("\",\n");
        }
        sb.append("      \"autoIncrement\": ").append(autoIncrement).append("\n");
        sb.append("    }");
        return sb.toString();
    }
    
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(COLUMN \"").append(name).append("\" \"").append(dataType).append("\"");
        
        if (length != null) {
            sb.append(" ").append(length);
        }
        
        if (notNull) sb.append(" NOT_NULL");
        if (primaryKey) sb.append(" PRIMARY_KEY");
        if (unique) sb.append(" UNIQUE");
        if (defaultValue != null) sb.append(" DEFAULT \"").append(defaultValue).append("\"");
        if (autoIncrement) sb.append(" AUTO_INCREMENT");
        
        sb.append(")");
        return sb.toString();
    }
}
