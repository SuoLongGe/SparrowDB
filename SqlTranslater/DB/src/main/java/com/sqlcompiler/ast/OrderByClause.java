package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import java.util.List;

/**
 * ORDER BY子句
 */
public class OrderByClause extends ASTNode {
    public enum SortOrder {
        ASC,
        DESC
    }
    
    public static class OrderByItem {
        private final Expression expression;
        private final SortOrder order;
        
        public OrderByItem(Expression expression, SortOrder order) {
            this.expression = expression;
            this.order = order;
        }
        
        public Expression getExpression() {
            return expression;
        }
        
        public SortOrder getOrder() {
            return order;
        }
    }
    
    private final List<OrderByItem> items;
    
    public OrderByClause(List<OrderByItem> items, Position position) {
        super(position);
        this.items = items;
    }
    
    public List<OrderByItem> getItems() {
        return items;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
