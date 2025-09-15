package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import com.sqlcompiler.exception.CompilationException;
import java.util.List;

/**
 * SELECT列表子句
 * 表示SELECT语句中的列选择部分
 */
public class SelectListClause extends ASTNode {
    private final List<Expression> expressions;
    
    public SelectListClause(List<Expression> expressions, Position position) {
        super(position);
        this.expressions = expressions;
    }
    
    public List<Expression> getExpressions() {
        return expressions;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws CompilationException {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expressions.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(expressions.get(i).toString());
        }
        return sb.toString();
    }
}
