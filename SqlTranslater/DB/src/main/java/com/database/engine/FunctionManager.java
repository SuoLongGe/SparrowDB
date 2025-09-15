package com.database.engine;

import com.sqlcompiler.ast.CreateFunctionStatement;
import com.sqlcompiler.ast.Expression;
import com.database.exception.DatabaseException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户自定义函数管理器
 * 负责函数的创建、存储、删除和调用
 */
public class FunctionManager {
    private final Map<String, UserDefinedFunction> functions;
    private final StorageAdapter storageAdapter;
    
    public FunctionManager(StorageAdapter storageAdapter) {
        this.functions = new ConcurrentHashMap<>();
        this.storageAdapter = storageAdapter;
        loadFunctionsFromStorage();
    }
    
    /**
     * 创建用户自定义函数
     */
    public void createFunction(CreateFunctionStatement statement) throws DatabaseException {
        String functionName = statement.getFunctionName().toLowerCase();
        
        // 检查函数是否已存在
        if (functions.containsKey(functionName) && !statement.hasIfNotExists()) {
            throw new DatabaseException("函数 '" + functionName + "' 已存在");
        }
        
        // 创建函数对象
        UserDefinedFunction function = new UserDefinedFunction(
            functionName,
            statement.getParameters(),
            statement.getReturnType(),
            statement.getFunctionBody()
        );
        
        // 存储函数
        functions.put(functionName, function);
        
        // 持久化到存储
        saveFunctionToStorage(function);
        
        System.out.println("✅ 函数 '" + functionName + "' 创建成功");
    }
    
    /**
     * 删除用户自定义函数
     */
    public void dropFunction(String functionName, boolean ifExists) throws DatabaseException {
        String normalizedName = functionName.toLowerCase();
        
        if (!functions.containsKey(normalizedName)) {
            if (!ifExists) {
                throw new DatabaseException("函数 '" + functionName + "' 不存在");
            }
            return;
        }
        
        // 从内存中删除
        functions.remove(normalizedName);
        
        // 从存储中删除
        deleteFunctionFromStorage(normalizedName);
        
        System.out.println("✅ 函数 '" + functionName + "' 删除成功");
    }
    
    /**
     * 调用用户自定义函数
     */
    public Object callFunction(String functionName, List<Object> arguments) throws DatabaseException {
        String normalizedName = functionName.toLowerCase();
        UserDefinedFunction function = functions.get(normalizedName);
        
        if (function == null) {
            throw new DatabaseException("函数 '" + functionName + "' 不存在");
        }
        
        // 检查参数数量
        if (arguments.size() != function.getParameterCount()) {
            throw new DatabaseException(
                String.format("函数 '%s' 期望 %d 个参数，但提供了 %d 个",
                functionName, function.getParameterCount(), arguments.size())
            );
        }
        
        // 执行函数 - 简单实现
        return executeFunction(function, arguments);
    }
    
    /**
     * 检查函数是否存在
     */
    public boolean functionExists(String functionName) {
        return functions.containsKey(functionName.toLowerCase());
    }
    
    /**
     * 获取所有函数名
     */
    public Set<String> getAllFunctionNames() {
        return new HashSet<>(functions.keySet());
    }
    
    /**
     * 获取函数定义
     */
    public UserDefinedFunction getFunction(String functionName) {
        return functions.get(functionName.toLowerCase());
    }
    
    /**
     * 执行函数 - 简单的表达式计算器
     */
    private Object executeFunction(UserDefinedFunction function, List<Object> arguments) throws DatabaseException {
        try {
            // 创建参数映射
            Map<String, Object> paramMap = new HashMap<>();
            List<CreateFunctionStatement.FunctionParameter> params = function.getParameters();
            for (int i = 0; i < params.size(); i++) {
                paramMap.put(params.get(i).getName(), arguments.get(i));
            }
            
            // 简单的函数体执行 - 支持基本的算术和字符串操作
            String body = function.getBody().trim();
            
            // 如果是RETURN语句
            if (body.toUpperCase().startsWith("RETURN")) {
                String expression = body.substring(6).trim();
                if (expression.endsWith(";")) {
                    expression = expression.substring(0, expression.length() - 1);
                }
                return evaluateExpression(expression, paramMap);
            }
            
            throw new DatabaseException("不支持的函数体格式: " + body);
            
        } catch (Exception e) {
            throw new DatabaseException("函数执行错误: " + e.getMessage());
        }
    }
    
    /**
     * 简单的表达式求值器
     */
    private Object evaluateExpression(String expression, Map<String, Object> params) throws DatabaseException {
        expression = expression.trim();
        
        // 替换参数
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            expression = expression.replace(entry.getKey(), entry.getValue().toString());
        }
        
        try {
            // 简单的数学表达式计算
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                double result = 0;
                for (String part : parts) {
                    result += Double.parseDouble(part.trim());
                }
                return result;
            } else if (expression.contains("-") && !expression.startsWith("-")) {
                String[] parts = expression.split("-");
                double result = Double.parseDouble(parts[0].trim());
                for (int i = 1; i < parts.length; i++) {
                    result -= Double.parseDouble(parts[i].trim());
                }
                return result;
            } else if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                double result = 1;
                for (String part : parts) {
                    result *= Double.parseDouble(part.trim());
                }
                return result;
            } else if (expression.contains("/")) {
                String[] parts = expression.split("/");
                double result = Double.parseDouble(parts[0].trim());
                for (int i = 1; i < parts.length; i++) {
                    result /= Double.parseDouble(parts[i].trim());
                }
                return result;
            } else {
                // 尝试作为数字
                try {
                    return Double.parseDouble(expression);
                } catch (NumberFormatException e) {
                    // 作为字符串返回
                    return expression;
                }
            }
        } catch (Exception e) {
            throw new DatabaseException("表达式计算错误: " + expression);
        }
    }
    
    /**
     * 从存储加载函数
     */
    private void loadFunctionsFromStorage() {
        try {
            // 检查系统函数表是否存在
            if (!storageAdapter.tableExists("__system_functions__")) {
                // 创建系统函数表
                storageAdapter.createSystemTable("__system_functions__", Arrays.asList(
                    "function_name VARCHAR(255)",
                    "parameters TEXT",
                    "return_type VARCHAR(50)",
                    "function_body TEXT"
                ));
                return;
            }
            
            // 加载已存在的函数
            List<Map<String, Object>> rows = storageAdapter.selectAll("__system_functions__");
            for (Map<String, Object> row : rows) {
                String functionName = (String) row.get("function_name");
                String signature = (String) row.get("signature");
                String returnType = (String) row.get("return_type");
                String functionBody = (String) row.get("body");
                
                // 解析参数（从signature字段解析）
                List<CreateFunctionStatement.FunctionParameter> parameters = parseParametersFromSignature(signature);
                
                UserDefinedFunction function = new UserDefinedFunction(functionName, parameters, returnType, functionBody);
                functions.put(functionName.toLowerCase(), function);
                
                System.out.println("✅ 加载函数: " + functionName + " (参数: " + signature + ", 返回类型: " + returnType + ")");
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ 加载函数时出错: " + e.getMessage());
        }
    }
    
    /**
     * 保存函数到存储
     */
    private void saveFunctionToStorage(UserDefinedFunction function) throws DatabaseException {
        try {
            // 构建参数JSON字符串（简单格式）
            StringBuilder paramJson = new StringBuilder("[");
            List<CreateFunctionStatement.FunctionParameter> params = function.getParameters();
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) paramJson.append(",");
                paramJson.append("{\"name\":\"").append(params.get(i).getName())
                         .append("\",\"type\":\"").append(params.get(i).getType()).append("\"}");
            }
            paramJson.append("]");
            
            Map<String, Object> data = new HashMap<>();
            data.put("function_name", function.getName());
            data.put("parameters", paramJson.toString());
            data.put("return_type", function.getReturnType());
            data.put("function_body", function.getBody());
            
            storageAdapter.insertIntoSystemTable("__system_functions__", data);
            
        } catch (Exception e) {
            throw new DatabaseException("保存函数失败: " + e.getMessage());
        }
    }
    
    /**
     * 从存储删除函数
     */
    private void deleteFunctionFromStorage(String functionName) throws DatabaseException {
        try {
            Map<String, Object> condition = new HashMap<>();
            condition.put("function_name", functionName);
            storageAdapter.deleteFromSystemTable("__system_functions__", condition);
        } catch (Exception e) {
            throw new DatabaseException("删除函数失败: " + e.getMessage());
        }
    }
    
    /**
     * 从signature字符串解析参数
     */
    private List<CreateFunctionStatement.FunctionParameter> parseParametersFromSignature(String signature) {
        List<CreateFunctionStatement.FunctionParameter> params = new ArrayList<>();
        
        if (signature == null || signature.trim().isEmpty()) {
            return params;
        }
        
        try {
            // 解析signature格式，如 "INT,INT" 或 "VARCHAR(255),INT"
            String[] paramTypes = signature.split(",");
            for (int i = 0; i < paramTypes.length; i++) {
                String paramType = paramTypes[i].trim();
                String paramName = "param" + (i + 1); // 生成参数名
                CreateFunctionStatement.FunctionParameter param = 
                    new CreateFunctionStatement.FunctionParameter(paramName, paramType);
                params.add(param);
            }
        } catch (Exception e) {
            System.err.println("解析参数签名失败: " + signature + ", 错误: " + e.getMessage());
        }
        
        return params;
    }
    
    /**
     * 解析参数JSON（简单实现）
     */
    private List<CreateFunctionStatement.FunctionParameter> parseParametersFromJson(String json) {
        List<CreateFunctionStatement.FunctionParameter> params = new ArrayList<>();
        
        if (json == null || json.equals("[]")) {
            return params;
        }
        
        // 简单的JSON解析 - 仅用于演示
        try {
            // 去掉方括号
            json = json.substring(1, json.length() - 1);
            
            if (json.trim().isEmpty()) {
                return params;
            }
            
            // 分割参数
            String[] paramStrs = json.split("\\},\\{");
            for (String paramStr : paramStrs) {
                paramStr = paramStr.replace("{", "").replace("}", "");
                String[] parts = paramStr.split(",");
                
                String name = "";
                String type = "";
                for (String part : parts) {
                    String[] kv = part.split(":");
                    if (kv.length == 2) {
                        String key = kv[0].replace("\"", "").trim();
                        String value = kv[1].replace("\"", "").trim();
                        if (key.equals("name")) {
                            name = value;
                        } else if (key.equals("type")) {
                            type = value;
                        }
                    }
                }
                
                if (!name.isEmpty() && !type.isEmpty()) {
                    params.add(new CreateFunctionStatement.FunctionParameter(name, type));
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ 解析参数JSON失败: " + e.getMessage());
        }
        
        return params;
    }
    
    /**
     * 用户自定义函数类
     */
    public static class UserDefinedFunction {
        private final String name;
        private final List<CreateFunctionStatement.FunctionParameter> parameters;
        private final String returnType;
        private final String body;
        
        public UserDefinedFunction(String name, 
                                 List<CreateFunctionStatement.FunctionParameter> parameters,
                                 String returnType, 
                                 String body) {
            this.name = name;
            this.parameters = parameters;
            this.returnType = returnType;
            this.body = body;
        }
        
        public String getName() { return name; }
        public List<CreateFunctionStatement.FunctionParameter> getParameters() { return parameters; }
        public String getReturnType() { return returnType; }
        public String getBody() { return body; }
        public int getParameterCount() { return parameters.size(); }
        
        @Override
        public String toString() {
            return String.format("Function{name='%s', params=%d, returns='%s'}", 
                               name, parameters.size(), returnType);
        }
    }
}
