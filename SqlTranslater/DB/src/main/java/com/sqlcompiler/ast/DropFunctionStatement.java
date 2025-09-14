package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import com.sqlcompiler.exception.CompilationException;
import java.util.Objects;

/**
 * DROP FUNCTION语句AST节点
 * 表示删除用户自定义函数的SQL语句
 */
public class DropFunctionStatement extends Statement {
    private final String functionName;
    private final boolean ifExists;
    
    /**
     * 构造函数
     * @param functionName 函数名
     * @param ifExists 是否包含IF EXISTS子句
     * @param position 位置信息
     */
    public DropFunctionStatement(String functionName, boolean ifExists, Position position) {
        super(position);
        this.functionName = Objects.requireNonNull(functionName, "函数名不能为空");
        this.ifExists = ifExists;
    }
    
    public String getFunctionName() {
        return functionName;
    }
    
    public boolean hasIfExists() {
        return ifExists;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws CompilationException {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DROP FUNCTION ");
        if (ifExists) {
            sb.append("IF EXISTS ");
        }
        sb.append(functionName);
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DropFunctionStatement that = (DropFunctionStatement) o;
        return ifExists == that.ifExists &&
               Objects.equals(functionName, that.functionName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(functionName, ifExists);
    }
}
