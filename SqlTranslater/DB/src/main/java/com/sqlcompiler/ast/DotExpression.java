package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * 点号表达式（用于表别名.字段名）
 */
public class DotExpression extends Expression {
    private final String tableName;
    private final String fieldName;
    
    public DotExpression(String tableName, String fieldName, Position position) {
        super(position);
        this.tableName = tableName;
        this.fieldName = fieldName;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}

