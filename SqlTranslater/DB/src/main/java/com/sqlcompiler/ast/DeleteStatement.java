package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * DELETE语句
 */
public class DeleteStatement extends Statement {
    private final String tableName;
    private final WhereClause whereClause;
    
    public DeleteStatement(String tableName, WhereClause whereClause, Position position) {
        super(position);
        this.tableName = tableName;
        this.whereClause = whereClause;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public WhereClause getWhereClause() {
        return whereClause;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
