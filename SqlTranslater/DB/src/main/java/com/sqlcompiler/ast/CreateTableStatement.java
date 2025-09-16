package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import java.util.List;

/**
 * CREATE TABLE语句
 */
public class CreateTableStatement extends Statement {
    private final String tableName;
    private final List<ColumnDefinition> columns;
    private final List<Constraint> constraints;
    private final String storageFormat; // 存储格式：ROW 或 COLUMN
    
    public CreateTableStatement(String tableName, List<ColumnDefinition> columns, 
                              List<Constraint> constraints, Position position) {
        this(tableName, columns, constraints, "ROW", position);
    }
    
    public CreateTableStatement(String tableName, List<ColumnDefinition> columns, 
                              List<Constraint> constraints, String storageFormat, Position position) {
        super(position);
        this.tableName = tableName;
        this.columns = columns;
        this.constraints = constraints;
        this.storageFormat = storageFormat != null ? storageFormat : "ROW";
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public List<ColumnDefinition> getColumns() {
        return columns;
    }
    
    public List<Constraint> getConstraints() {
        return constraints;
    }
    
    public String getStorageFormat() {
        return storageFormat;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
