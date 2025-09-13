package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * DROP TABLE语句
 */
public class DropTableStatement extends Statement {
    private final String tableName;
    private final boolean ifExists;
    
    public DropTableStatement(String tableName, boolean ifExists, Position position) {
        super(position);
        this.tableName = tableName;
        this.ifExists = ifExists;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public boolean isIfExists() {
        return ifExists;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
