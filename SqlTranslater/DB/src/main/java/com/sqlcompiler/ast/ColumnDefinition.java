package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import java.util.List;

/**
 * 列定义
 */
public class ColumnDefinition extends ASTNode {
    private final String columnName;
    private final String dataType;
    private final Integer length;
    private final List<Constraint> constraints;
    
    public ColumnDefinition(String columnName, String dataType, Integer length, 
                          List<Constraint> constraints, Position position) {
        super(position);
        this.columnName = columnName;
        this.dataType = dataType;
        this.length = length;
        this.constraints = constraints;
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public Integer getLength() {
        return length;
    }
    
    public List<Constraint> getConstraints() {
        return constraints;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
