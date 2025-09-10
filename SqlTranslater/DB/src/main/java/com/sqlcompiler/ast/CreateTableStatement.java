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
    
    public CreateTableStatement(String tableName, List<ColumnDefinition> columns, 
                              List<Constraint> constraints, Position position) {
        super(position);
        this.tableName = tableName;
        this.columns = columns;
        this.constraints = constraints;
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
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
