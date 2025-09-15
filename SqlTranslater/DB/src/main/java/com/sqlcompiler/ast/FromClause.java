package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import com.sqlcompiler.exception.CompilationException;
import java.util.List;

/**
 * FROM子句
 * 表示SELECT语句中的表引用部分
 */
public class FromClause extends ASTNode {
    private final List<TableReference> tableReferences;
    
    public FromClause(List<TableReference> tableReferences, Position position) {
        super(position);
        this.tableReferences = tableReferences;
    }
    
    public List<TableReference> getTableReferences() {
        return tableReferences;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws CompilationException {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tableReferences.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(tableReferences.get(i).toString());
        }
        return sb.toString();
    }
}
