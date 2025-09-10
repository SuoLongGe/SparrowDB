package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import java.util.List;

/**
 * 约束定义
 */
public class Constraint extends ASTNode {
    public enum ConstraintType {
        PRIMARY_KEY,
        FOREIGN_KEY,
        UNIQUE,
        NOT_NULL,
        DEFAULT,
<<<<<<< HEAD
        AUTO_INCREMENT
=======
        AUTO_INCREMENT,
        CHECK
>>>>>>> 62d958b6bcc46722dfbc5dd2897cfc16d17ca1d3
    }
    
    private final ConstraintType type;
    private final String name;
    private final List<String> columns;
    private final String referencedTable;
    private final List<String> referencedColumns;
    private final String defaultValue;
    
    public Constraint(ConstraintType type, String name, List<String> columns, 
                     String referencedTable, List<String> referencedColumns, 
                     String defaultValue, Position position) {
        super(position);
        this.type = type;
        this.name = name;
        this.columns = columns;
        this.referencedTable = referencedTable;
        this.referencedColumns = referencedColumns;
        this.defaultValue = defaultValue;
    }
    
    public ConstraintType getType() {
        return type;
    }
    
    public String getName() {
        return name;
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
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return null; // 约束不直接访问
    }
}
