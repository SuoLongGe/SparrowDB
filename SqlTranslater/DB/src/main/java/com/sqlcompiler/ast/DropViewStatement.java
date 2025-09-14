package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import com.sqlcompiler.exception.CompilationException;

/**
 * DROP VIEW语句AST节点
 */
public class DropViewStatement extends Statement {
    private final String viewName;
    private final boolean ifExists;
    
    public DropViewStatement(String viewName, boolean ifExists, Position position) {
        super(position);
        this.viewName = viewName;
        this.ifExists = ifExists;
    }
    
    public String getViewName() {
        return viewName;
    }
    
    public boolean isIfExists() {
        return ifExists;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws CompilationException {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return String.format("DropView(name=%s, ifExists=%s)", viewName, ifExists);
    }
}
