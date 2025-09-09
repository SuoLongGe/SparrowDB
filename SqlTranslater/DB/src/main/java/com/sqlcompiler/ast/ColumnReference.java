package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * 列引用
 */
public class ColumnReference extends ASTNode {
    private final String tableName;
    private final String columnName;
    
    public ColumnReference(String tableName, String columnName, Position position) {
        super(position);
        this.tableName = tableName;
        this.columnName = columnName;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
