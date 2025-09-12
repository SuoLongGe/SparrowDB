package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import java.util.List;

/**
 * 批量SQL语句
 * 用于表示多个SQL语句的集合
 */
public class BatchStatement extends Statement {
    private final List<Statement> statements;
    
    public BatchStatement(List<Statement> statements, Position position) {
        super(position);
        this.statements = statements;
    }
    
    public List<Statement> getStatements() {
        return statements;
    }
    
    public int getStatementCount() {
        return statements.size();
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}