package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import com.sqlcompiler.exception.CompilationException;

/**
 * CREATE VIEW语句AST节点
 */
public class CreateViewStatement extends Statement {
    private final String viewName;
    private final SelectStatement selectStatement;
    
    public CreateViewStatement(String viewName, SelectStatement selectStatement, Position position) {
        super(position);
        this.viewName = viewName;
        this.selectStatement = selectStatement;
    }
    
    public String getViewName() {
        return viewName;
    }
    
    public SelectStatement getSelectStatement() {
        return selectStatement;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws CompilationException {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return String.format("CreateView(name=%s, query=%s)", viewName, selectStatement);
    }
}
