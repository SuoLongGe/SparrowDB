package com.database.engine;

import com.sqlcompiler.ast.CreateFunctionStatement;
import com.sqlcompiler.ast.Expression;
import com.database.exception.DatabaseException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 增强的用户自定义函数管理器
 * 支持Navicat/MySQL级别的高级特性
 */
public class EnhancedFunctionManager extends FunctionManager {
    private final Map<String, List<UserDefinedFunction>> functionOverloads;
    private final StorageAdapter storageAdapter;
    private final Map<String, Object> globalVariables;
    private final Map<String, Object> typeConverters;
    
    // 支持的数据类型映射
    private static final Map<String, Class<?>> DATA_TYPE_MAPPING = new HashMap<>();
    
    static {
        DATA_TYPE_MAPPING.put("INT", Integer.class);
        DATA_TYPE_MAPPING.put("INTEGER", Integer.class);
        DATA_TYPE_MAPPING.put("DECIMAL", BigDecimal.class);
        DATA_TYPE_MAPPING.put("FLOAT", Float.class);
        DATA_TYPE_MAPPING.put("DOUBLE", Double.class);
        DATA_TYPE_MAPPING.put("VARCHAR", String.class);
        DATA_TYPE_MAPPING.put("CHAR", String.class);
        DATA_TYPE_MAPPING.put("TEXT", String.class);
        DATA_TYPE_MAPPING.put("BOOLEAN", Boolean.class);
        DATA_TYPE_MAPPING.put("DATE", LocalDateTime.class);
        DATA_TYPE_MAPPING.put("DATETIME", LocalDateTime.class);
        DATA_TYPE_MAPPING.put("TIMESTAMP", LocalDateTime.class);
    }
    
    public EnhancedFunctionManager(StorageAdapter storageAdapter) {
        super(storageAdapter); // 调用父类构造函数
        this.functionOverloads = new ConcurrentHashMap<>();
        this.storageAdapter = storageAdapter;
        this.globalVariables = new ConcurrentHashMap<>();
        this.typeConverters = initializeTypeConverters();
        loadAdvancedFunctionsFromStorage();
    }
    
    /**
     * 创建用户自定义函数 - 支持函数重载
     */
    public void createFunction(CreateFunctionStatement statement) throws DatabaseException {
        String functionName = statement.getFunctionName().toLowerCase();
        
        // 获取函数重载列表
        List<UserDefinedFunction> overloads = functionOverloads.computeIfAbsent(
            functionName, k -> new ArrayList<>()
        );
        
        // 检查是否有相同参数签名的函数
        String signature = buildParameterSignature(statement.getParameters());
        for (UserDefinedFunction existing : overloads) {
            if (existing instanceof EnhancedUserDefinedFunction && 
                ((EnhancedUserDefinedFunction) existing).getParameterSignature().equals(signature)) {
                if (!statement.hasIfNotExists()) {
                    throw new DatabaseException("函数 '" + functionName + "(" + signature + ")' 已存在");
                }
                return; // 如果有IF NOT EXISTS，则忽略
            }
        }
        
        // 创建增强的函数对象
        EnhancedUserDefinedFunction function = new EnhancedUserDefinedFunction(
            functionName,
            statement.getParameters(),
            statement.getReturnType(),
            statement.getFunctionBody(),
            signature,
            statement.isPermanent()
        );
        
        // 验证函数体语法
        validateFunctionBody(function);
        
        // 添加到重载列表
        overloads.add(function);
        
        // 持久化到存储
        saveFunctionToStorage(function);
        
        System.out.println("✅ 函数 '" + functionName + "(" + signature + ")' 创建成功");
    }
    
    /**
     * 调用函数 - 支持重载解析
     */
    public Object callFunction(String functionName, List<Object> arguments) throws DatabaseException {
        String lowerName = functionName.toLowerCase();
        List<UserDefinedFunction> overloads = functionOverloads.get(lowerName);
        
        if (overloads == null || overloads.isEmpty()) {
            throw new DatabaseException("函数 '" + functionName + "' 不存在");
        }
        
        // 查找匹配的重载
        EnhancedUserDefinedFunction matchedFunction = null;
        for (UserDefinedFunction func : overloads) {
            if (func instanceof EnhancedUserDefinedFunction) {
                EnhancedUserDefinedFunction enhancedFunc = (EnhancedUserDefinedFunction) func;
                if (enhancedFunc.getParameters().size() == arguments.size()) {
                    // 简单的参数数量匹配，后续可以添加类型匹配
                    matchedFunction = enhancedFunc;
                    break;
                }
            }
        }
        
        if (matchedFunction == null) {
            throw new DatabaseException("没有找到匹配的函数重载: " + functionName + 
                "(" + arguments.size() + " 个参数)");
        }
        
        return executeEnhancedFunction(matchedFunction, arguments);
    }
    
    /**
     * 执行增强的函数 - 支持控制结构
     */
    private Object executeEnhancedFunction(EnhancedUserDefinedFunction function, 
                                         List<Object> arguments) throws DatabaseException {
        try {
            // 创建函数执行上下文
            FunctionExecutionContext context = new FunctionExecutionContext();
            
            // 设置参数
            List<CreateFunctionStatement.FunctionParameter> params = function.getParameters();
            for (int i = 0; i < params.size(); i++) {
                context.setVariable(params.get(i).getName(), arguments.get(i));
            }
            
            // 解析并执行函数体
            return executeFunctionBody(function.getBody(), context);
            
        } catch (Exception e) {
            throw new DatabaseException("函数执行错误: " + e.getMessage());
        }
    }
    
    /**
     * 执行函数体 - 支持控制结构
     */
    private Object executeFunctionBody(String body, FunctionExecutionContext context) throws DatabaseException {
        String normalizedBody = body.trim();
        
        // 支持多种函数体结构
        if (normalizedBody.toUpperCase().startsWith("RETURN")) {
            return executeReturnStatement(normalizedBody, context);
        } else if (normalizedBody.toUpperCase().contains("IF")) {
            return executeIfStatement(normalizedBody, context);
        } else if (normalizedBody.toUpperCase().contains("WHILE")) {
            return executeWhileStatement(normalizedBody, context);
        } else if (normalizedBody.toUpperCase().contains("CASE")) {
            return executeCaseStatement(normalizedBody, context);
        } else {
            // 尝试作为表达式执行
            return evaluateExpression(normalizedBody, context.getVariables());
        }
    }
    
    /**
     * 执行RETURN语句
     */
    private Object executeReturnStatement(String statement, FunctionExecutionContext context) throws DatabaseException {
        String expression = statement.substring(6).trim();
        if (expression.endsWith(";")) {
            expression = expression.substring(0, expression.length() - 1);
        }
        return evaluateExpression(expression, context.getVariables());
    }
    
    /**
     * 执行IF语句
     */
    private Object executeIfStatement(String statement, FunctionExecutionContext context) throws DatabaseException {
        // 简化的IF语句解析: IF condition THEN statement ELSE statement END IF
        Pattern ifPattern = Pattern.compile(
            "IF\\s+(.+?)\\s+THEN\\s+(.+?)(?:\\s+ELSE\\s+(.+?))?\\s+END\\s+IF", 
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = ifPattern.matcher(statement);
        if (matcher.find()) {
            String condition = matcher.group(1).trim();
            String thenStatement = matcher.group(2).trim();
            String elseStatement = matcher.group(3) != null ? matcher.group(3).trim() : null;
            
            // 评估条件
            Object conditionResult = evaluateExpression(condition, context.getVariables());
            boolean isTrue = convertToBoolean(conditionResult);
            
            if (isTrue) {
                return executeFunctionBody(thenStatement, context);
            } else if (elseStatement != null) {
                return executeFunctionBody(elseStatement, context);
            }
        }
        
        return null;
    }
    
    /**
     * 执行WHILE语句
     */
    private Object executeWhileStatement(String statement, FunctionExecutionContext context) throws DatabaseException {
        Pattern whilePattern = Pattern.compile(
            "WHILE\\s+(.+?)\\s+DO\\s+(.+?)\\s+END\\s+WHILE", 
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = whilePattern.matcher(statement);
        if (matcher.find()) {
            String condition = matcher.group(1).trim();
            String loopBody = matcher.group(2).trim();
            
            Object result = null;
            int maxIterations = 1000; // 防止无限循环
            int iterations = 0;
            
            while (iterations < maxIterations) {
                Object conditionResult = evaluateExpression(condition, context.getVariables());
                if (!convertToBoolean(conditionResult)) {
                    break;
                }
                
                result = executeFunctionBody(loopBody, context);
                iterations++;
            }
            
            if (iterations >= maxIterations) {
                throw new DatabaseException("WHILE循环超过最大迭代次数限制");
            }
            
            return result;
        }
        
        return null;
    }
    
    /**
     * 执行CASE语句
     */
    private Object executeCaseStatement(String statement, FunctionExecutionContext context) throws DatabaseException {
        // 简化的CASE语句实现
        Pattern casePattern = Pattern.compile(
            "CASE\\s+(.+?)\\s+WHEN\\s+(.+?)\\s+THEN\\s+(.+?)\\s+(?:ELSE\\s+(.+?)\\s+)?END", 
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = casePattern.matcher(statement);
        if (matcher.find()) {
            String caseExpression = matcher.group(1).trim();
            String whenCondition = matcher.group(2).trim();
            String thenStatement = matcher.group(3).trim();
            String elseStatement = matcher.group(4) != null ? matcher.group(4).trim() : null;
            
            Object caseValue = evaluateExpression(caseExpression, context.getVariables());
            Object whenValue = evaluateExpression(whenCondition, context.getVariables());
            
            if (Objects.equals(caseValue, whenValue)) {
                return executeFunctionBody(thenStatement, context);
            } else if (elseStatement != null) {
                return executeFunctionBody(elseStatement, context);
            }
        }
        
        return null;
    }
    
    /**
     * 增强的表达式求值器 - 支持更多运算符和函数
     */
    private Object evaluateExpression(String expression, Map<String, Object> variables) throws DatabaseException {
        expression = expression.trim();
        
        // 处理变量替换
        for (Map.Entry<String, Object> var : variables.entrySet()) {
            expression = expression.replaceAll("\\b" + var.getKey() + "\\b", 
                String.valueOf(var.getValue()));
        }
        
        // 支持基本的算术运算
        if (expression.contains("+")) {
            String[] parts = expression.split("\\+");
            if (parts.length == 2) {
                Object left = parseValue(parts[0].trim());
                Object right = parseValue(parts[1].trim());
                return performArithmetic(left, right, "+");
            }
        } else if (expression.contains("-")) {
            String[] parts = expression.split("-");
            if (parts.length == 2) {
                Object left = parseValue(parts[0].trim());
                Object right = parseValue(parts[1].trim());
                return performArithmetic(left, right, "-");
            }
        } else if (expression.contains("*")) {
            String[] parts = expression.split("\\*");
            if (parts.length == 2) {
                Object left = parseValue(parts[0].trim());
                Object right = parseValue(parts[1].trim());
                return performArithmetic(left, right, "*");
            }
        } else if (expression.contains("/")) {
            String[] parts = expression.split("/");
            if (parts.length == 2) {
                Object left = parseValue(parts[0].trim());
                Object right = parseValue(parts[1].trim());
                return performArithmetic(left, right, "/");
            }
        }
        
        // 支持比较运算
        if (expression.contains(">")) {
            String[] parts = expression.split(">");
            if (parts.length == 2) {
                Object left = parseValue(parts[0].trim());
                Object right = parseValue(parts[1].trim());
                return performComparison(left, right, ">");
            }
        } else if (expression.contains("<")) {
            String[] parts = expression.split("<");
            if (parts.length == 2) {
                Object left = parseValue(parts[0].trim());
                Object right = parseValue(parts[1].trim());
                return performComparison(left, right, "<");
            }
        } else if (expression.contains("=")) {
            String[] parts = expression.split("=");
            if (parts.length == 2) {
                Object left = parseValue(parts[0].trim());
                Object right = parseValue(parts[1].trim());
                return performComparison(left, right, "=");
            }
        }
        
        // 单一值
        return parseValue(expression);
    }
    
    /**
     * 解析值
     */
    private Object parseValue(String value) {
        value = value.trim();
        
        // 数字
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // 不是数字
        }
        
        // 字符串（去掉引号）
        if ((value.startsWith("'") && value.endsWith("'")) ||
            (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }
        
        // 布尔值
        if ("TRUE".equalsIgnoreCase(value)) return true;
        if ("FALSE".equalsIgnoreCase(value)) return false;
        
        // 默认返回字符串
        return value;
    }
    
    /**
     * 执行算术运算
     */
    private Object performArithmetic(Object left, Object right, String operator) throws DatabaseException {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            
            switch (operator) {
                case "+": return leftVal + rightVal;
                case "-": return leftVal - rightVal;
                case "*": return leftVal * rightVal;
                case "/": 
                    if (rightVal == 0) throw new DatabaseException("除零错误");
                    return leftVal / rightVal;
                default: throw new DatabaseException("不支持的算术运算符: " + operator);
            }
        }
        
        // 字符串连接
        if ("+".equals(operator) && (left instanceof String || right instanceof String)) {
            return String.valueOf(left) + String.valueOf(right);
        }
        
        throw new DatabaseException("无法执行算术运算: " + left + " " + operator + " " + right);
    }
    
    /**
     * 执行比较运算
     */
    private Boolean performComparison(Object left, Object right, String operator) throws DatabaseException {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            
            switch (operator) {
                case ">": return leftVal > rightVal;
                case "<": return leftVal < rightVal;
                case "=": return leftVal == rightVal;
                case ">=": return leftVal >= rightVal;
                case "<=": return leftVal <= rightVal;
                case "!=": return leftVal != rightVal;
                default: throw new DatabaseException("不支持的比较运算符: " + operator);
            }
        }
        
        // 字符串比较
        if (left instanceof String && right instanceof String) {
            int comparison = ((String) left).compareTo((String) right);
            switch (operator) {
                case ">": return comparison > 0;
                case "<": return comparison < 0;
                case "=": return comparison == 0;
                case ">=": return comparison >= 0;
                case "<=": return comparison <= 0;
                case "!=": return comparison != 0;
                default: throw new DatabaseException("不支持的比较运算符: " + operator);
            }
        }
        
        // 相等性比较
        if ("=".equals(operator)) {
            return Objects.equals(left, right);
        } else if ("!=".equals(operator)) {
            return !Objects.equals(left, right);
        }
        
        throw new DatabaseException("无法执行比较运算: " + left + " " + operator + " " + right);
    }
    
    /**
     * 转换为布尔值
     */
    private boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        } else if (value instanceof String) {
            String str = (String) value;
            return !"".equals(str) && !"0".equals(str) && !"FALSE".equalsIgnoreCase(str);
        }
        return value != null;
    }
    
    /**
     * 构建参数签名
     */
    private String buildParameterSignature(List<CreateFunctionStatement.FunctionParameter> parameters) {
        if (parameters.isEmpty()) {
            return "";
        }
        
        StringBuilder signature = new StringBuilder();
        for (CreateFunctionStatement.FunctionParameter param : parameters) {
            if (signature.length() > 0) {
                signature.append(",");
            }
            signature.append(param.getType());
        }
        return signature.toString();
    }
    
    /**
     * 验证函数体语法
     */
    private void validateFunctionBody(EnhancedUserDefinedFunction function) throws DatabaseException {
        String body = function.getBody().trim();
        
        // 基本语法验证
        if (body.isEmpty()) {
            throw new DatabaseException("函数体不能为空");
        }
        
        // 检查控制结构匹配
        validateControlStructures(body);
    }
    
    /**
     * 验证控制结构匹配
     */
    private void validateControlStructures(String body) throws DatabaseException {
        // 简化的语法验证
        String upperBody = body.toUpperCase();
        
        // 检查IF-END IF匹配
        int ifCount = countOccurrences(upperBody, "IF");
        int endIfCount = countOccurrences(upperBody, "END IF");
        if (ifCount != endIfCount) {
            throw new DatabaseException("IF语句不匹配：IF数量=" + ifCount + ", END IF数量=" + endIfCount);
        }
        
        // 检查WHILE-END WHILE匹配
        int whileCount = countOccurrences(upperBody, "WHILE");
        int endWhileCount = countOccurrences(upperBody, "END WHILE");
        if (whileCount != endWhileCount) {
            throw new DatabaseException("WHILE语句不匹配：WHILE数量=" + whileCount + ", END WHILE数量=" + endWhileCount);
        }
    }
    
    /**
     * 计算字符串出现次数
     */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
    
    
    /**
     * 保存函数到存储
     */
    private void saveFunctionToStorage(UserDefinedFunction function) {
        System.out.println("🔍 开始保存函数到存储: " + function.getName());
        
        if (!(function instanceof EnhancedUserDefinedFunction)) {
            System.out.println("🔍 函数不是增强型函数，跳过保存");
            return; // 只保存增强型函数
        }
        
        EnhancedUserDefinedFunction enhancedFunction = (EnhancedUserDefinedFunction) function;
        
        // 只有标记为永久的函数才会被保存
        if (!enhancedFunction.isPermanent()) {
            System.out.println("🔍 函数不是永久函数，跳过保存");
            return;
        }
        
        try {
            // 准备插入数据
            Map<String, Object> record = new HashMap<>();
            record.put("function_name", enhancedFunction.getName());
            
            // 序列化参数信息到signature字段（包含参数名和类型）
            StringBuilder paramInfo = new StringBuilder();
            for (int i = 0; i < enhancedFunction.getParameters().size(); i++) {
                if (i > 0) paramInfo.append(",");
                CreateFunctionStatement.FunctionParameter param = enhancedFunction.getParameters().get(i);
                paramInfo.append(param.getName()).append(":").append(param.getType());
            }
            record.put("signature", paramInfo.toString());
            record.put("return_type", enhancedFunction.getReturnType());
            record.put("body", enhancedFunction.getBody());
            record.put("is_permanent", enhancedFunction.isPermanent());
            record.put("create_time", System.currentTimeMillis());
            
            System.out.println("🔍 准备插入记录: " + record);
            
            // 插入到系统函数表
            boolean success = storageAdapter.insertRecord("__system_functions__", record);
            System.out.println("🔍 insertRecord返回结果: " + success);
            
            if (success) {
                System.out.println("✅ 函数 '" + enhancedFunction.getName() + "' 已持久化保存");
            } else {
                System.err.println("❌ 函数 '" + enhancedFunction.getName() + "' 持久化保存失败");
            }
        } catch (Exception e) {
            System.err.println("❌ 保存函数到存储失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 删除函数
     */
    public void dropFunction(String functionName, boolean ifExists) throws DatabaseException {
        String lowerName = functionName.toLowerCase();
        List<UserDefinedFunction> overloads = functionOverloads.get(lowerName);
        
        if (overloads == null || overloads.isEmpty()) {
            if (!ifExists) {
                throw new DatabaseException("函数 '" + functionName + "' 不存在");
            }
            return;
        }
        
        // 检查是否有持久化函数需要从存储中删除
        boolean hasPermanentFunction = false;
        for (UserDefinedFunction function : overloads) {
            if (function instanceof EnhancedUserDefinedFunction) {
                EnhancedUserDefinedFunction enhancedFunction = (EnhancedUserDefinedFunction) function;
                if (enhancedFunction.isPermanent()) {
                    hasPermanentFunction = true;
                    break;
                }
            }
        }
        
        // 删除所有重载
        functionOverloads.remove(lowerName);
        
        // 如果有持久化函数，从存储中删除
        if (hasPermanentFunction) {
            deleteFunctionFromStorage(lowerName);
        }
        
        System.out.println("✅ 函数 '" + functionName + "' 及其所有重载已删除");
    }
    
    /**
     * 从存储中删除函数
     */
    private void deleteFunctionFromStorage(String functionName) {
        try {
            if (!storageAdapter.tableExists("__system_functions__")) {
                System.out.println("ℹ️ 系统函数表不存在，无法删除持久化函数");
                return;
            }
            
            // 先查询出要删除的记录
            List<Map<String, Object>> records = storageAdapter.scanTable("__system_functions__");
            if (records == null || records.isEmpty()) {
                System.out.println("ℹ️ 系统函数表为空，无需删除");
                return;
            }
            
            // 找到匹配的记录并删除
            boolean foundAndDeleted = false;
            for (Map<String, Object> record : records) {
                String recordFunctionName = (String) record.get("function_name");
                if (functionName.equals(recordFunctionName)) {
                    // 使用完整记录进行删除
                    boolean success = storageAdapter.deleteRecord("__system_functions__", record);
                    if (success) {
                        System.out.println("✅ 函数 '" + functionName + "' 已从持久化存储中删除");
                        foundAndDeleted = true;
                        break;
                    } else {
                        System.err.println("❌ 删除持久化函数 '" + functionName + "' 失败");
                    }
                }
            }
            
            if (!foundAndDeleted) {
                System.out.println("ℹ️ 未找到函数 '" + functionName + "' 的持久化记录");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 从存储中删除函数失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有函数
     */
    public Map<String, List<UserDefinedFunction>> getAllFunctions() {
        return new HashMap<>(functionOverloads);
    }
    
    /**
     * 获取所有函数名 - 重写父类方法
     */
    @Override
    public Set<String> getAllFunctionNames() {
        return new HashSet<>(functionOverloads.keySet());
    }
    
    /**
     * 检查函数是否存在 - 重写父类方法
     */
    @Override
    public boolean functionExists(String functionName) {
        return functionOverloads.containsKey(functionName.toLowerCase());
    }
    
    /**
     * 函数执行上下文
     */
    private static class FunctionExecutionContext {
        private final Map<String, Object> variables = new HashMap<>();
        
        public void setVariable(String name, Object value) {
            variables.put(name, value);
        }
        
        public Object getVariable(String name) {
            return variables.get(name);
        }
        
        public Map<String, Object> getVariables() {
            return variables;
        }
    }
    
    /**
     * 增强的用户定义函数类
     */
    public static class EnhancedUserDefinedFunction extends UserDefinedFunction {
        private final String parameterSignature;
        private final boolean isPermanent;
        
        public EnhancedUserDefinedFunction(String name, 
                                         List<CreateFunctionStatement.FunctionParameter> parameters,
                                         String returnType, 
                                         String body,
                                         String parameterSignature,
                                         boolean isPermanent) {
            super(name, parameters, returnType, body);
            this.parameterSignature = parameterSignature;
            this.isPermanent = isPermanent;
        }
        
        public String getParameterSignature() {
            return parameterSignature;
        }
        
        public boolean isPermanent() {
            return isPermanent;
        }
        
        public boolean isDeterministic() {
            // 默认函数都是确定性的，可以根据需要进行扩展
            return true;
        }
    }
    
    /**
     * 初始化类型转换器
     */
    private Map<String, Object> initializeTypeConverters() {
        Map<String, Object> converters = new HashMap<>();
        // 基本类型转换器（使用匿名类替代lambda）
        converters.put("STRING_TO_INT", new Object() {
            public Integer convert(String s) { return Integer.parseInt(s); }
        });
        converters.put("STRING_TO_DOUBLE", new Object() {
            public Double convert(String s) { return Double.parseDouble(s); }
        });
        converters.put("INT_TO_STRING", new Object() {
            public String convert(Integer i) { return i.toString(); }
        });
        return converters;
    }
    
    /**
     * 加载高级函数存储
     */
    private void loadAdvancedFunctionsFromStorage() {
        try {
            // 确保系统函数表存在
            ensureSystemFunctionsTableExists();
            
            // 加载已存储的函数
            loadStoredFunctions();
            
            System.out.println("Enhanced Function Manager initialized with advanced features");
        } catch (DatabaseException e) {
            System.err.println("警告: 无法加载存储的函数: " + e.getMessage());
        }
    }
    
    /**
     * 确保系统函数表存在
     */
    private void ensureSystemFunctionsTableExists() throws DatabaseException {
        try {
            // 检查表是否存在
            if (!storageAdapter.tableExists("__system_functions__")) {
                // 创建系统函数表
                createSystemFunctionsTable();
            }
        } catch (Exception e) {
            throw new DatabaseException("无法创建系统函数表: " + e.getMessage());
        }
    }
    
    /**
     * 创建系统函数表
     */
    private void createSystemFunctionsTable() throws DatabaseException {
        try {
            // 创建函数存储表的结构
            List<String> columnDefinitions = new ArrayList<>();
            columnDefinitions.add("function_name VARCHAR(255)");
            columnDefinitions.add("parameter_types TEXT");
            columnDefinitions.add("return_type VARCHAR(100)");
            columnDefinitions.add("function_body TEXT");
            columnDefinitions.add("is_deterministic BOOLEAN");
            columnDefinitions.add("created_at TIMESTAMP");
            
            boolean success = storageAdapter.createSystemTable("__system_functions__", columnDefinitions);
            if (success) {
                System.out.println("✅ 系统函数表 __system_functions__ 创建成功");
            } else {
                throw new DatabaseException("系统函数表创建失败");
            }
        } catch (Exception e) {
            throw new DatabaseException("创建系统函数表失败: " + e.getMessage());
        }
    }
    
    /**
     * 从存储中加载函数
     */
    private void loadStoredFunctions() {
        try {
            System.out.println("🔍 开始加载持久化函数");
            
            if (!storageAdapter.tableExists("__system_functions__")) {
                System.out.println("ℹ️ 系统函数表不存在，跳过加载持久化函数");
                return;
            }
            
            System.out.println("🔍 系统函数表存在，开始扫描");
            
            // 查询所有存储的函数
            List<Map<String, Object>> records = storageAdapter.scanTable("__system_functions__");
            System.out.println("🔍 scanTable返回记录数: " + (records != null ? records.size() : "null"));
            
            if (records == null || records.isEmpty()) {
                System.out.println("ℹ️ 没有找到已保存的持久化函数");
                return;
            }
            
            int loadedCount = 0;
            for (Map<String, Object> record : records) {
                try {
                    if (record.containsKey("function_name")) {
                        String functionName = (String) record.get("function_name");
                        // 兼容不同的表结构，先尝试新字段名signature，再尝试旧字段名parameter_types和parameters
                        String parameterTypesStr = (String) record.get("signature");
                        if (parameterTypesStr == null) {
                            parameterTypesStr = (String) record.get("parameter_types");
                        }
                        if (parameterTypesStr == null) {
                            parameterTypesStr = (String) record.get("parameters");
                        }
                        String returnType = (String) record.get("return_type");
                        
                        // 兼容新旧字段名：body/function_body
                        String functionBody = (String) record.get("body");
                        if (functionBody == null) {
                            functionBody = (String) record.get("function_body");
                        }
                        
                        Boolean isPermanent = null;
                        // 尝试新字段名is_permanent，再尝试旧字段名is_deterministic
                        Object isPermanentObj = record.get("is_permanent");
                        if (isPermanentObj == null) {
                            isPermanentObj = record.get("is_deterministic");
                        }
                        if (isPermanentObj instanceof Boolean) {
                            isPermanent = (Boolean) isPermanentObj;
                        } else if (isPermanentObj instanceof String) {
                            isPermanent = Boolean.parseBoolean((String) isPermanentObj);
                        } else {
                            isPermanent = true; // 默认值
                        }
                        // record.get("created_at") 暂时不需要
                        
                        // 解析参数类型
                        List<CreateFunctionStatement.FunctionParameter> parameters = new ArrayList<>();
                        if (parameterTypesStr != null && !parameterTypesStr.trim().isEmpty()) {
                            try {
                                // 尝试解析JSON格式的参数
                                if (parameterTypesStr.startsWith("[")) {
                                    parameters = parseParametersFromJson(parameterTypesStr);
                                } else {
                                    // 解析逗号分隔的参数信息字符串（格式：name:type,name:type）
                                    String[] paramInfos = parameterTypesStr.split(",");
                                    for (String paramInfo : paramInfos) {
                                        String[] parts = paramInfo.split(":");
                                        if (parts.length == 2) {
                                            String paramName = parts[0].trim();
                                            String paramType = parts[1].trim();
                                            parameters.add(new CreateFunctionStatement.FunctionParameter(paramName, paramType));
                                        } else {
                                            // 兼容旧格式：只有类型，没有参数名
                                            // 尝试从函数体中提取参数名
                                            String paramName = extractParameterNameFromBody(functionBody, parameters.size());
                                            parameters.add(new CreateFunctionStatement.FunctionParameter(
                                                paramName, paramInfo.trim()));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("❌ 解析参数时出错: " + e.getMessage());
                                continue; // 跳过这个函数
                            }
                        }
                        
                        // 构建参数签名
                        String signature = buildParameterSignature(parameters);
                        
                        // 创建函数对象
                        EnhancedUserDefinedFunction function = new EnhancedUserDefinedFunction(
                            functionName,
                            parameters,
                            returnType,
                            functionBody,
                            signature,
                            isPermanent // 使用从记录中读取的isPermanent值
                        );
                        
                        // 添加到内存中的函数映射
                        List<UserDefinedFunction> overloads = functionOverloads.computeIfAbsent(
                            functionName.toLowerCase(), k -> new ArrayList<>());
                        overloads.add(function);
                        
                        loadedCount++;
                        System.out.println("✅ 已加载持久化函数: " + functionName + signature);
                    }
                } catch (Exception e) {
                    System.err.println("❌ 加载函数记录时出错: " + e.getMessage());
                }
            }
            
            if (loadedCount > 0) {
                System.out.println("✅ 成功加载 " + loadedCount + " 个持久化函数");
            }
        } catch (Exception e) {
            System.err.println("❌ 加载存储函数时出错: " + e.getMessage());
        }
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
     * 从函数体中提取参数名（用于兼容旧格式）
     */
    private String extractParameterNameFromBody(String functionBody, int paramIndex) {
        if (functionBody == null || functionBody.trim().isEmpty()) {
            return "param" + (paramIndex + 1);
        }
        
        // 简单的参数名提取逻辑
        // 常见的参数名模式
        String[] commonParamNames = {"a", "b", "c", "d", "x", "y", "z", "n", "m", "p", "q", "r", "s", "t"};
        
        // 遍历所有常见的参数名，找到在函数体中出现的参数名
        int foundCount = 0;
        for (String candidateName : commonParamNames) {
            if (functionBody.contains(candidateName)) {
                if (foundCount == paramIndex) {
                    return candidateName;
                }
                foundCount++;
            }
        }
        
        // 如果找不到匹配的参数名，使用默认名称
        return "param" + (paramIndex + 1);
    }
}
