package com.sqlcompiler.ast;

import com.sqlcompiler.exception.CompilationException;

/**
 * AST访问者接口
 */
public interface ASTVisitor<T> {
    T visit(Statement node) throws CompilationException;
    T visit(CreateTableStatement node) throws CompilationException;
    T visit(InsertStatement node) throws CompilationException;
    T visit(SelectStatement node) throws CompilationException;
    T visit(DeleteStatement node) throws CompilationException;
    T visit(ColumnDefinition node) throws CompilationException;
    T visit(Expression node) throws CompilationException;
    T visit(BinaryExpression node) throws CompilationException;
    T visit(UnaryExpression node) throws CompilationException;
    T visit(LiteralExpression node) throws CompilationException;
    T visit(IdentifierExpression node) throws CompilationException;
    T visit(FunctionCallExpression node) throws CompilationException;
    T visit(ColumnReference node) throws CompilationException;
    T visit(TableReference node) throws CompilationException;
    T visit(JoinClause node) throws CompilationException;
    T visit(WhereClause node) throws CompilationException;
    T visit(OrderByClause node) throws CompilationException;
    T visit(GroupByClause node) throws CompilationException;
    T visit(HavingClause node) throws CompilationException;
    T visit(LimitClause node) throws CompilationException;
}
