import com.database.engine.*;
import com.sqlcompiler.SQLCompiler;

/**
 * 高级用户自定义函数测试工具
 * 展示与Navicat/MySQL对等的高级特性
 */
public class AdvancedFunctionTest {
    
    public static void main(String[] args) {
        System.out.println("=== SparrowDB 高级用户自定义函数测试 ===\n");
        
        try {
            // 初始化数据库引擎
            DatabaseEngine engine = new DatabaseEngine("sparrow_db", "./data");
            
            System.out.println("🚀 数据库引擎初始化成功！\n");
            
            // 测试1: 带条件判断的函数
            testConditionalFunction(engine);
            
            // 测试2: 带循环的函数
            testLoopFunction(engine);
            
            // 测试3: 函数重载
            testFunctionOverloading(engine);
            
            // 测试4: 复杂数据类型支持
            testAdvancedDataTypes(engine);
            
            // 测试5: CASE语句
            testCaseStatement(engine);
            
            // 测试6: 错误处理验证
            testErrorHandling(engine);
            
            System.out.println("\n🎉 === 所有高级功能测试完成！ ===");
            
        } catch (Exception e) {
            System.err.println("❌ 测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试条件判断函数
     */
    private static void testConditionalFunction(DatabaseEngine engine) {
        System.out.println("🔧 测试1: 带条件判断的函数");
        System.out.println("----------------------------------------");
        
        try {
            // 创建带IF条件的最大值函数
            String createMaxFunc = 
                "CREATE FUNCTION max_value(a INT, b INT) RETURNS INT " +
                "BEGIN " +
                "IF a > b THEN RETURN a; ELSE RETURN b; END IF " +
                "END";
            
            System.out.println("执行SQL: " + createMaxFunc);
            ExecutionResult result1 = engine.executeSQL(createMaxFunc);
            System.out.println("✅ 条件函数创建: " + (result1.isSuccess() ? "成功" : "失败"));
            
            // 测试函数调用
            String callMaxFunc1 = "CALL max_value(15, 8)";
            System.out.println("执行SQL: " + callMaxFunc1);
            ExecutionResult result2 = engine.executeSQL(callMaxFunc1);
            System.out.println("✅ 调用结果: " + result2.getData());
            System.out.println("预期结果: 15 (较大值)");
            
            String callMaxFunc2 = "CALL max_value(3, 12)";
            System.out.println("执行SQL: " + callMaxFunc2);
            ExecutionResult result3 = engine.executeSQL(callMaxFunc2);
            System.out.println("✅ 调用结果: " + result3.getData());
            System.out.println("预期结果: 12 (较大值)");
            
        } catch (Exception e) {
            System.err.println("❌ 条件函数测试失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * 测试循环函数
     */
    private static void testLoopFunction(DatabaseEngine engine) {
        System.out.println("🔧 测试2: 带循环的函数");
        System.out.println("----------------------------------------");
        
        try {
            // 创建阶乘函数（使用WHILE循环）
            String createFactorialFunc = 
                "CREATE FUNCTION factorial(n INT) RETURNS INT " +
                "BEGIN " +
                "DECLARE result INT DEFAULT 1; " +
                "DECLARE counter INT DEFAULT 1; " +
                "WHILE counter <= n DO " +
                "SET result = result * counter; " +
                "SET counter = counter + 1; " +
                "END WHILE " +
                "RETURN result; " +
                "END";
            
            System.out.println("执行SQL: " + createFactorialFunc);
            ExecutionResult result1 = engine.executeSQL(createFactorialFunc);
            System.out.println("✅ 循环函数创建: " + (result1.isSuccess() ? "成功" : "失败"));
            
            // 测试阶乘计算
            String callFactorial = "CALL factorial(5)";
            System.out.println("执行SQL: " + callFactorial);
            ExecutionResult result2 = engine.executeSQL(callFactorial);
            System.out.println("✅ 调用结果: " + result2.getData());
            System.out.println("预期结果: 120 (5的阶乘)");
            
        } catch (Exception e) {
            System.err.println("❌ 循环函数测试失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * 测试函数重载
     */
    private static void testFunctionOverloading(DatabaseEngine engine) {
        System.out.println("🔧 测试3: 函数重载");
        System.out.println("----------------------------------------");
        
        try {
            // 创建两个参数的加法函数
            String createAdd2 = 
                "CREATE FUNCTION add_numbers(a INT, b INT) RETURNS INT " +
                "BEGIN RETURN a + b; END";
            
            System.out.println("执行SQL: " + createAdd2);
            ExecutionResult result1 = engine.executeSQL(createAdd2);
            System.out.println("✅ 二参数加法函数: " + (result1.isSuccess() ? "成功" : "失败"));
            
            // 创建三个参数的加法函数（重载）
            String createAdd3 = 
                "CREATE FUNCTION add_numbers(a INT, b INT, c INT) RETURNS INT " +
                "BEGIN RETURN a + b + c; END";
            
            System.out.println("执行SQL: " + createAdd3);
            ExecutionResult result2 = engine.executeSQL(createAdd3);
            System.out.println("✅ 三参数加法函数: " + (result2.isSuccess() ? "成功" : "失败"));
            
            // 测试不同重载的调用
            String callAdd2 = "CALL add_numbers(10, 20)";
            System.out.println("执行SQL: " + callAdd2);
            ExecutionResult result3 = engine.executeSQL(callAdd2);
            System.out.println("✅ 二参数调用结果: " + result3.getData());
            
            String callAdd3 = "CALL add_numbers(10, 20, 30)";
            System.out.println("执行SQL: " + callAdd3);
            ExecutionResult result4 = engine.executeSQL(callAdd3);
            System.out.println("✅ 三参数调用结果: " + result4.getData());
            
        } catch (Exception e) {
            System.err.println("❌ 函数重载测试失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * 测试高级数据类型
     */
    private static void testAdvancedDataTypes(DatabaseEngine engine) {
        System.out.println("🔧 测试4: 高级数据类型支持");
        System.out.println("----------------------------------------");
        
        try {
            // 创建字符串处理函数
            String createStringFunc = 
                "CREATE FUNCTION format_name(first_name VARCHAR, last_name VARCHAR) RETURNS VARCHAR " +
                "BEGIN RETURN first_name + ' ' + last_name; END";
            
            System.out.println("执行SQL: " + createStringFunc);
            ExecutionResult result1 = engine.executeSQL(createStringFunc);
            System.out.println("✅ 字符串函数创建: " + (result1.isSuccess() ? "成功" : "失败"));
            
            // 测试字符串函数
            String callStringFunc = "CALL format_name('John', 'Doe')";
            System.out.println("执行SQL: " + callStringFunc);
            ExecutionResult result2 = engine.executeSQL(callStringFunc);
            System.out.println("✅ 字符串函数结果: " + result2.getData());
            System.out.println("预期结果: 'John Doe'");
            
            // 创建DECIMAL处理函数
            String createDecimalFunc = 
                "CREATE FUNCTION calculate_discount(price DECIMAL, discount_rate DECIMAL) RETURNS DECIMAL " +
                "BEGIN RETURN price * (1 - discount_rate); END";
            
            System.out.println("执行SQL: " + createDecimalFunc);
            ExecutionResult result3 = engine.executeSQL(createDecimalFunc);
            System.out.println("✅ DECIMAL函数创建: " + (result3.isSuccess() ? "成功" : "失败"));
            
        } catch (Exception e) {
            System.err.println("❌ 高级数据类型测试失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * 测试CASE语句
     */
    private static void testCaseStatement(DatabaseEngine engine) {
        System.out.println("🔧 测试5: CASE语句");
        System.out.println("----------------------------------------");
        
        try {
            // 创建带CASE的等级评定函数
            String createGradeFunc = 
                "CREATE FUNCTION get_grade(score INT) RETURNS VARCHAR " +
                "BEGIN " +
                "CASE " +
                "WHEN score >= 90 THEN RETURN 'A'; " +
                "WHEN score >= 80 THEN RETURN 'B'; " +
                "WHEN score >= 70 THEN RETURN 'C'; " +
                "WHEN score >= 60 THEN RETURN 'D'; " +
                "ELSE RETURN 'F'; " +
                "END " +
                "END";
            
            System.out.println("执行SQL: " + createGradeFunc);
            ExecutionResult result1 = engine.executeSQL(createGradeFunc);
            System.out.println("✅ CASE函数创建: " + (result1.isSuccess() ? "成功" : "失败"));
            
            // 测试不同分数的等级
            int[] scores = {95, 85, 75, 65, 45};
            String[] expectedGrades = {"A", "B", "C", "D", "F"};
            
            for (int i = 0; i < scores.length; i++) {
                String callGradeFunc = "CALL get_grade(" + scores[i] + ")";
                System.out.println("执行SQL: " + callGradeFunc);
                ExecutionResult result = engine.executeSQL(callGradeFunc);
                System.out.println("✅ 分数 " + scores[i] + " 的等级: " + result.getData() + 
                    " (预期: " + expectedGrades[i] + ")");
            }
            
        } catch (Exception e) {
            System.err.println("❌ CASE语句测试失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * 测试错误处理
     */
    private static void testErrorHandling(DatabaseEngine engine) {
        System.out.println("🔧 测试6: 错误处理验证");
        System.out.println("----------------------------------------");
        
        try {
            // 测试重复创建函数
            System.out.println("测试重复创建函数...");
            String duplicateFunc = 
                "CREATE FUNCTION add_numbers(a INT, b INT) RETURNS INT " +
                "BEGIN RETURN a - b; END";
            
            try {
                ExecutionResult result = engine.executeSQL(duplicateFunc);
                System.out.println("❌ 预期应该失败，但却成功了");
            } catch (Exception e) {
                System.out.println("✅ 正确捕获重复创建错误: " + e.getMessage());
            }
            
            // 测试调用不存在的函数
            System.out.println("测试调用不存在的函数...");
            try {
                ExecutionResult result = engine.executeSQL("CALL non_existent_function(1, 2)");
                System.out.println("❌ 预期应该失败，但却成功了");
            } catch (Exception e) {
                System.out.println("✅ 正确捕获函数不存在错误: " + e.getMessage());
            }
            
            // 测试参数数量不匹配
            System.out.println("测试参数数量不匹配...");
            try {
                ExecutionResult result = engine.executeSQL("CALL add_numbers(1)"); // 缺少一个参数
                System.out.println("❌ 预期应该失败，但却成功了");
            } catch (Exception e) {
                System.out.println("✅ 正确捕获参数不匹配错误: " + e.getMessage());
            }
            
            // 测试除零错误
            System.out.println("测试除零错误处理...");
            String divideFunc = 
                "CREATE FUNCTION safe_divide(a INT, b INT) RETURNS INT " +
                "BEGIN " +
                "IF b = 0 THEN RETURN 0; ELSE RETURN a / b; END IF " +
                "END";
            
            ExecutionResult result1 = engine.executeSQL(divideFunc);
            System.out.println("✅ 安全除法函数创建: " + (result1.isSuccess() ? "成功" : "失败"));
            
            ExecutionResult result2 = engine.executeSQL("CALL safe_divide(10, 0)");
            System.out.println("✅ 除零保护结果: " + result2.getData() + " (应该返回0)");
            
        } catch (Exception e) {
            System.err.println("❌ 错误处理测试失败: " + e.getMessage());
        }
        
        System.out.println();
    }
}
