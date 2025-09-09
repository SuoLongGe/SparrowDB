package com.sqlcompiler.execution;

import java.util.List;

/**
 * INSERT执行计划
 */
public class InsertPlan extends ExecutionPlan {
    private final String tableName;
    private final List<String> columns;
    private final List<List<ExpressionPlan>> values;
    
    public InsertPlan(String tableName, List<String> columns, List<List<ExpressionPlan>> values) {
        super("INSERT");
        this.tableName = tableName;
        this.columns = columns;
        this.values = values;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public List<String> getColumns() {
        return columns;
    }
    
    public List<List<ExpressionPlan>> getValues() {
        return values;
    }
    
    @Override
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"INSERT\",\n");
        sb.append("  \"tableName\": \"").append(tableName).append("\",\n");
        sb.append("  \"columns\": [");
        
        for (int i = 0; i < columns.size(); i++) {
            sb.append("\"").append(columns.get(i)).append("\"");
            if (i < columns.size() - 1) {
                sb.append(", ");
            }
        }
        
        sb.append("],\n");
        sb.append("  \"values\": [\n");
        
        for (int i = 0; i < values.size(); i++) {
            sb.append("    [");
            List<ExpressionPlan> valueList = values.get(i);
            for (int j = 0; j < valueList.size(); j++) {
                sb.append(valueList.get(j).toJSON());
                if (j < valueList.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            if (i < values.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(INSERT \"").append(tableName).append("\" ");
        
        sb.append("(COLUMNS ");
        for (String column : columns) {
            sb.append("\"").append(column).append("\" ");
        }
        sb.append(") ");
        
        sb.append("(VALUES ");
        for (List<ExpressionPlan> valueList : values) {
            sb.append("(ROW ");
            for (ExpressionPlan expr : valueList) {
                sb.append(expr.toSExpression()).append(" ");
            }
            sb.append(") ");
        }
        sb.append("))");
        
        return sb.toString();
    }
}
