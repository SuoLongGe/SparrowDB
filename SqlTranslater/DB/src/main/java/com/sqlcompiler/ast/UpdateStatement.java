package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import java.util.List;
import java.util.Map;

/**
 * UPDATE语句
 */
public class UpdateStatement extends Statement {
    private final String tableName;
    private final Map<String, Expression> setClause;
    private final WhereClause whereClause;
    
    public UpdateStatement(String tableName, Map<String, Expression> setClause, 
                          WhereClause whereClause, Position position) {
        super(position);
        this.tableName = tableName;
        this.setClause = setClause;
        this.whereClause = whereClause;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public Map<String, Expression> getSetClause() {
        return setClause;
    }
    
    public WhereClause getWhereClause() {
        return whereClause;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
