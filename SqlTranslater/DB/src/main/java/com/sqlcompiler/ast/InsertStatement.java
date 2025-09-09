package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import java.util.List;

/**
 * INSERT语句
 */
public class InsertStatement extends Statement {
    private final String tableName;
    private final List<String> columns;
    private final List<List<Expression>> values;
    
    public InsertStatement(String tableName, List<String> columns, 
                         List<List<Expression>> values, Position position) {
        super(position);
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
    
    public List<List<Expression>> getValues() {
        return values;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
