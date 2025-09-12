package com.sqlcompiler.ast;

import com.sqlcompiler.exception.CompilationException;
import com.sqlcompiler.lexer.Position;
import java.util.Map;

/**
 * AST打印器 - 用于显示抽象语法树的结构
 */
public class ASTPrinter implements ASTVisitor<String> {
    private int indentLevel = 0;
    
    private String getIndent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }
    
    private void increaseIndent() {
        indentLevel++;
    }
    
    private void decreaseIndent() {
        indentLevel--;
    }
    
    @Override
    public String visit(Statement node) throws CompilationException {
        return "Statement";
    }
    
    @Override
    public String visit(CreateTableStatement node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("CreateTableStatement {\n");
        increaseIndent();
        sb.append(getIndent()).append("tableName: ").append(node.getTableName()).append("\n");
        sb.append(getIndent()).append("columns: [\n");
        increaseIndent();
        for (ColumnDefinition col : node.getColumns()) {
            sb.append(getIndent()).append(col.accept(this)).append(",\n");
        }
        decreaseIndent();
        sb.append(getIndent()).append("]\n");
        sb.append(getIndent()).append("constraints: [\n");
        increaseIndent();
        for (Constraint constraint : node.getConstraints()) {
            sb.append(getIndent()).append(constraint.toString()).append(",\n");
        }
        decreaseIndent();
        sb.append(getIndent()).append("]\n");
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(InsertStatement node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("InsertStatement {\n");
        increaseIndent();
        sb.append(getIndent()).append("tableName: ").append(node.getTableName()).append("\n");
        sb.append(getIndent()).append("columns: ").append(node.getColumns()).append("\n");
        sb.append(getIndent()).append("values: [\n");
        increaseIndent();
        for (int i = 0; i < node.getValues().size(); i++) {
            sb.append(getIndent()).append("Row ").append(i + 1).append(": [\n");
            increaseIndent();
            for (Expression expr : node.getValues().get(i)) {
                sb.append(getIndent()).append(expr.accept(this)).append(",\n");
            }
            decreaseIndent();
            sb.append(getIndent()).append("],\n");
        }
        decreaseIndent();
        sb.append(getIndent()).append("]\n");
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(SelectStatement node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("SelectStatement {\n");
        increaseIndent();
        sb.append(getIndent()).append("distinct: ").append(node.isDistinct()).append("\n");
        sb.append(getIndent()).append("selectList: [\n");
        increaseIndent();
        for (Expression expr : node.getSelectList()) {
            sb.append(getIndent()).append(expr.accept(this)).append(",\n");
        }
        decreaseIndent();
        sb.append(getIndent()).append("]\n");
        
        if (node.getFromClause() != null) {
            sb.append(getIndent()).append("fromClause: [\n");
            increaseIndent();
            for (TableReference table : node.getFromClause()) {
                sb.append(getIndent()).append(table.accept(this)).append(",\n");
            }
            decreaseIndent();
            sb.append(getIndent()).append("]\n");
        }
        
        if (node.getWhereClause() != null) {
            sb.append(getIndent()).append("whereClause: ").append(node.getWhereClause().accept(this)).append("\n");
        }
        
        if (node.getOrderByClause() != null) {
            sb.append(getIndent()).append("orderByClause: ").append(node.getOrderByClause().accept(this)).append("\n");
        }
        
        if (node.getLimitClause() != null) {
            sb.append(getIndent()).append("limitClause: ").append(node.getLimitClause().accept(this)).append("\n");
        }
        
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(UpdateStatement node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("UpdateStatement {\n");
        increaseIndent();
        sb.append(getIndent()).append("tableName: ").append(node.getTableName()).append("\n");
        sb.append(getIndent()).append("setClause: {\n");
        increaseIndent();
        for (Map.Entry<String, Expression> entry : node.getSetClause().entrySet()) {
            sb.append(getIndent()).append(entry.getKey()).append(" = ").append(entry.getValue().accept(this)).append("\n");
        }
        decreaseIndent();
        sb.append(getIndent()).append("}\n");
        if (node.getWhereClause() != null) {
            sb.append(getIndent()).append("whereClause: ").append(node.getWhereClause().accept(this)).append("\n");
        }
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(DeleteStatement node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("DeleteStatement {\n");
        increaseIndent();
        sb.append(getIndent()).append("tableName: ").append(node.getTableName()).append("\n");
        if (node.getWhereClause() != null) {
            sb.append(getIndent()).append("whereClause: ").append(node.getWhereClause().accept(this)).append("\n");
        }
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(ColumnDefinition node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("ColumnDefinition {\n");
        increaseIndent();
        sb.append(getIndent()).append("name: ").append(node.getColumnName()).append("\n");
        sb.append(getIndent()).append("dataType: ").append(node.getDataType()).append("\n");
        if (node.getLength() != null) {
            sb.append(getIndent()).append("length: ").append(node.getLength()).append("\n");
        }
        sb.append(getIndent()).append("constraints: ").append(node.getConstraints()).append("\n");
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(Expression node) throws CompilationException {
        return "Expression";
    }
    
    @Override
    public String visit(BinaryExpression node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("BinaryExpression {\n");
        increaseIndent();
        sb.append(getIndent()).append("operator: ").append(node.getOperator().getValue()).append("\n");
        sb.append(getIndent()).append("left: ").append(node.getLeft().accept(this)).append("\n");
        sb.append(getIndent()).append("right: ").append(node.getRight().accept(this)).append("\n");
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(UnaryExpression node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("UnaryExpression {\n");
        increaseIndent();
        sb.append(getIndent()).append("operator: ").append(node.getOperator().getValue()).append("\n");
        sb.append(getIndent()).append("operand: ").append(node.getOperand().accept(this)).append("\n");
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(LiteralExpression node) throws CompilationException {
        return "LiteralExpression { value: \"" + node.getValue() + "\", type: " + node.getType().getValue() + " }";
    }
    
    @Override
    public String visit(IdentifierExpression node) throws CompilationException {
        return "IdentifierExpression { name: \"" + node.getName() + "\" }";
    }
    
    @Override
    public String visit(DotExpression node) throws CompilationException {
        return "DotExpression { tableName: \"" + node.getTableName() + "\", fieldName: \"" + node.getFieldName() + "\" }";
    }
    
    @Override
    public String visit(FunctionCallExpression node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("FunctionCallExpression {\n");
        increaseIndent();
        sb.append(getIndent()).append("functionName: ").append(node.getFunctionName()).append("\n");
        sb.append(getIndent()).append("arguments: [\n");
        increaseIndent();
        for (Expression arg : node.getArguments()) {
            sb.append(getIndent()).append(arg.accept(this)).append(",\n");
        }
        decreaseIndent();
        sb.append(getIndent()).append("]\n");
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(InExpression node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("InExpression {\n");
        increaseIndent();
        sb.append(getIndent()).append("left: ").append(node.getLeft().accept(this)).append("\n");
        sb.append(getIndent()).append("right: ");
        if (node.isSubquery()) {
            sb.append("Subquery {\n");
            increaseIndent();
            sb.append(getIndent()).append(node.getSubquery().accept(this)).append("\n");
            decreaseIndent();
            sb.append(getIndent()).append("}");
        } else {
            sb.append("Values [\n");
            increaseIndent();
            for (Expression value : node.getValues()) {
                sb.append(getIndent()).append(value.accept(this)).append(",\n");
            }
            decreaseIndent();
            sb.append(getIndent()).append("]");
        }
        sb.append("\n");
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(SubqueryExpression node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("SubqueryExpression {\n");
        increaseIndent();
        sb.append(getIndent()).append("subquery: ").append(node.getSubquery().accept(this)).append("\n");
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(ColumnReference node) throws CompilationException {
        return "ColumnReference { tableName: \"" + node.getTableName() + "\", columnName: \"" + node.getColumnName() + "\" }";
    }
    
    @Override
    public String visit(TableReference node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("TableReference {\n");
        increaseIndent();
        sb.append(getIndent()).append("tableName: ").append(node.getTableName()).append("\n");
        if (node.getAlias() != null) {
            sb.append(getIndent()).append("alias: ").append(node.getAlias()).append("\n");
        }
        if (node.getJoins() != null && !node.getJoins().isEmpty()) {
            sb.append(getIndent()).append("joins: [\n");
            increaseIndent();
            for (JoinClause join : node.getJoins()) {
                sb.append(getIndent()).append(join.accept(this)).append(",\n");
            }
            decreaseIndent();
            sb.append(getIndent()).append("]\n");
        }
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(JoinClause node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("JoinClause {\n");
        increaseIndent();
        sb.append(getIndent()).append("joinType: ").append(node.getJoinType()).append("\n");
        sb.append(getIndent()).append("tableName: ").append(node.getTableName()).append("\n");
        if (node.getAlias() != null) {
            sb.append(getIndent()).append("alias: ").append(node.getAlias()).append("\n");
        }
        sb.append(getIndent()).append("condition: ").append(node.getCondition().accept(this)).append("\n");
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(WhereClause node) throws CompilationException {
        return "WhereClause { condition: " + node.getCondition().accept(this) + " }";
    }
    
    @Override
    public String visit(OrderByClause node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("OrderByClause {\n");
        increaseIndent();
        sb.append(getIndent()).append("items: [\n");
        increaseIndent();
        for (OrderByClause.OrderByItem item : node.getItems()) {
            sb.append(getIndent()).append("OrderByItem {\n");
            increaseIndent();
            sb.append(getIndent()).append("expression: ").append(item.getExpression().accept(this)).append("\n");
            sb.append(getIndent()).append("order: ").append(item.getOrder()).append("\n");
            decreaseIndent();
            sb.append(getIndent()).append("},\n");
        }
        decreaseIndent();
        sb.append(getIndent()).append("]\n");
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(GroupByClause node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("GroupByClause {\n");
        increaseIndent();
        sb.append(getIndent()).append("expressions: [\n");
        increaseIndent();
        for (Expression expr : node.getExpressions()) {
            sb.append(getIndent()).append(expr.accept(this)).append(",\n");
        }
        decreaseIndent();
        sb.append(getIndent()).append("]\n");
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visit(HavingClause node) throws CompilationException {
        return "HavingClause { condition: " + node.getCondition().accept(this) + " }";
    }
    
    @Override
    public String visit(LimitClause node) throws CompilationException {
        StringBuilder sb = new StringBuilder();
        sb.append("LimitClause {\n");
        increaseIndent();
        sb.append(getIndent()).append("limit: ").append(node.getLimit().accept(this)).append("\n");
        if (node.getOffset() != null) {
            sb.append(getIndent()).append("offset: ").append(node.getOffset().accept(this)).append("\n");
        }
        decreaseIndent();
        sb.append(getIndent()).append("}");
        return sb.toString();
    }
}
