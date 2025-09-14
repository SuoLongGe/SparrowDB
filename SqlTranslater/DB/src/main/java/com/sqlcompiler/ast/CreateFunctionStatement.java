package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;
import com.sqlcompiler.exception.CompilationException;
import java.util.List;
import java.util.Objects;

/**
 * CREATE FUNCTION语句AST节点
 * 表示创建用户自定义函数的SQL语句
 */
public class CreateFunctionStatement extends Statement {
    private final String functionName;
    private final List<FunctionParameter> parameters;
    private final String returnType;
    private final String functionBody;
    private final boolean ifNotExists;
    private final boolean isPermanent;
    
    /**
     * 构造函数
     * @param functionName 函数名
     * @param parameters 参数列表
     * @param returnType 返回类型
     * @param functionBody 函数体
     * @param ifNotExists 是否包含IF NOT EXISTS子句
     * @param isPermanent 是否持久化保存函数
     * @param position 位置信息
     */
    public CreateFunctionStatement(String functionName, 
                                  List<FunctionParameter> parameters,
                                  String returnType, 
                                  String functionBody,
                                  boolean ifNotExists,
                                  boolean isPermanent,
                                  Position position) {
        super(position);
        this.functionName = Objects.requireNonNull(functionName, "函数名不能为空");
        this.parameters = Objects.requireNonNull(parameters, "参数列表不能为空");
        this.returnType = Objects.requireNonNull(returnType, "返回类型不能为空");
        this.functionBody = Objects.requireNonNull(functionBody, "函数体不能为空");
        this.ifNotExists = ifNotExists;
        this.isPermanent = isPermanent;
    }
    
    public String getFunctionName() {
        return functionName;
    }
    
    public List<FunctionParameter> getParameters() {
        return parameters;
    }
    
    public String getReturnType() {
        return returnType;
    }
    
    public String getFunctionBody() {
        return functionBody;
    }
    
    public boolean hasIfNotExists() {
        return ifNotExists;
    }
    
    public boolean isPermanent() {
        return isPermanent;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) throws CompilationException {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE");
        if (isPermanent) {
            sb.append(" PERMANENT");
        }
        if (ifNotExists) {
            sb.append(" OR REPLACE");
        }
        sb.append(" FUNCTION ").append(functionName).append("(");
        
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i));
        }
        
        sb.append(") RETURNS ").append(returnType);
        sb.append(" BEGIN ").append(functionBody).append(" END");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateFunctionStatement that = (CreateFunctionStatement) o;
        return ifNotExists == that.ifNotExists &&
               isPermanent == that.isPermanent &&
               Objects.equals(functionName, that.functionName) &&
               Objects.equals(parameters, that.parameters) &&
               Objects.equals(returnType, that.returnType) &&
               Objects.equals(functionBody, that.functionBody);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(functionName, parameters, returnType, functionBody, ifNotExists, isPermanent);
    }
    
    /**
     * 函数参数类
     */
    public static class FunctionParameter {
        private final String name;
        private final String type;
        
        public FunctionParameter(String name, String type) {
            this.name = Objects.requireNonNull(name, "参数名不能为空");
            this.type = Objects.requireNonNull(type, "参数类型不能为空");
        }
        
        public String getName() {
            return name;
        }
        
        public String getType() {
            return type;
        }
        
        @Override
        public String toString() {
            return name + " " + type;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FunctionParameter that = (FunctionParameter) o;
            return Objects.equals(name, that.name) && Objects.equals(type, that.type);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }
    }
}
