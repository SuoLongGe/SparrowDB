package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import java.util.List;

/**
 * 函数调用表达式
 */
public class FunctionCallExpression extends Expression {
    private final String functionName;
    private final List<Expression> arguments;
    
    public FunctionCallExpression(String functionName, List<Expression> arguments, Position position) {
        super(position);
        this.functionName = functionName;
        this.arguments = arguments;
    }
    
    public String getFunctionName() {
        return functionName;
    }
    
    public List<Expression> getArguments() {
        return arguments;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException {
        return visitor.visit(this);
    }
}
