package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import com.sqlcompiler.exception.CompilationException;
import java.util.List;
import java.util.Objects;

/**
 * CALL语句AST节点
 * 表示调用用户自定义函数或存储过程的SQL语句
 */
public class CallStatement extends Statement {
    private final String functionName;
    private final List<Expression> arguments;
    
    /**
     * 构造函数
     * @param functionName 函数名
     * @param arguments 参数列表
     * @param position 位置信息
     */
    public CallStatement(String functionName, List<Expression> arguments, Position position) {
        super(position);
        this.functionName = Objects.requireNonNull(functionName, "函数名不能为空");
        this.arguments = Objects.requireNonNull(arguments, "参数列表不能为空");
    }
    
    public String getFunctionName() {
        return functionName;
    }
    
    public List<Expression> getArguments() {
        return arguments;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws CompilationException {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CALL ").append(functionName).append("(");
        
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(arguments.get(i));
        }
        
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallStatement that = (CallStatement) o;
        return Objects.equals(functionName, that.functionName) &&
               Objects.equals(arguments, that.arguments);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(functionName, arguments);
    }
}
