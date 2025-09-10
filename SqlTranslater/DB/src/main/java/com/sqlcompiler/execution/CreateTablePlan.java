package com.sqlcompiler.execution;

import java.util.List;

/**
 * CREATE TABLE执行计划
 */
public class CreateTablePlan extends ExecutionPlan {
    private final String tableName;
    private final List<ColumnPlan> columns;
    private final List<ConstraintPlan> constraints;
    
    public CreateTablePlan(String tableName, List<ColumnPlan> columns, List<ConstraintPlan> constraints) {
        super("CREATE_TABLE");
        this.tableName = tableName;
        this.columns = columns;
        this.constraints = constraints;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public List<ColumnPlan> getColumns() {
        return columns;
    }
    
    public List<ConstraintPlan> getConstraints() {
        return constraints;
    }
    
    @Override
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"CREATE_TABLE\",\n");
        sb.append("  \"tableName\": \"").append(tableName).append("\",\n");
        sb.append("  \"columns\": [\n");
        
        for (int i = 0; i < columns.size(); i++) {
            sb.append("    ").append(columns.get(i).toJSON());
            if (i < columns.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        
        sb.append("  ],\n");
        sb.append("  \"constraints\": [\n");
        
        for (int i = 0; i < constraints.size(); i++) {
            sb.append("    ").append(constraints.get(i).toJSON());
            if (i < constraints.size() - 1) {
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
        sb.append("(CREATE_TABLE \"").append(tableName).append("\" ");
        
        sb.append("(COLUMNS ");
        for (ColumnPlan column : columns) {
            sb.append(column.toSExpression()).append(" ");
        }
        sb.append(") ");
        
        sb.append("(CONSTRAINTS ");
        for (ConstraintPlan constraint : constraints) {
            sb.append(constraint.toSExpression()).append(" ");
        }
        sb.append("))");
        
        return sb.toString();
    }
}
