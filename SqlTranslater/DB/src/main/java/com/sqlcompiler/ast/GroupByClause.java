package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import java.util.List;

/**
 * GROUP BY子句
 */
public class GroupByClause extends ASTNode {
    private final List<Expression> expressions;
    
    public GroupByClause(List<Expression> expressions, Position position) {
        super(position);
        this.expressions = expressions;
    }
    
    public List<Expression> getExpressions() {
        return expressions;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
