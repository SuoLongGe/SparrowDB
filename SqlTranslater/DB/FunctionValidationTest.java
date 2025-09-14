import com.sqlcompiler.Main;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 完整的函数功能验证测试
 * 使用实际的SQL查询和数据库数据来验证函数功能
 */
public class FunctionValidationTest {
    
    private static final String DATA_PATH = "../../data/";
    private static int testCount = 0;
    private static int passedTests = 0;
    
    public static void main(String[] args) {
        System.out.println("=== SparrowDB 函数功能完整验证测试 ===\n");
        
        // 验证数据库基础功能
        testBasicQueries();
        
        // 验证数学函数
        testMathFunctions();
        
        // 验证字符串函数
        testStringFunctions();
        
        // 验证日期函数
        testDateFunctions();
        
        // 验证复合函数调用
        testComplexFunctions();
        
        // 输出测试结果
        System.out.println("\n=== 测试总结 ===");
        System.out.println("总测试数: " + testCount);
        System.out.println("通过测试: " + passedTests);
        System.out.println("失败测试: " + (testCount - passedTests));
        System.out.println("成功率: " + String.format("%.1f%%", (passedTests * 100.0 / testCount)));
        
        if (passedTests == testCount) {
            System.out.println("🎉 所有函数功能验证通过！");
        } else {
            System.out.println("⚠️  部分测试失败，需要检查功能实现");
        }
    }
    
    /**
     * 测试基础查询功能
     */
    private static void testBasicQueries() {
        System.out.println("1. 验证基础查询功能:");
        
        // 测试简单SELECT
        runSQLTest("SELECT * FROM users;", "基础SELECT查询", true);
        
        // 测试带条件的SELECT
        runSQLTest("SELECT name, age FROM users WHERE age > 25;", "条件查询", true);
        
        System.out.println();
    }
    
    /**
     * 测试数学函数
     */
    private static void testMathFunctions() {
        System.out.println("2. 验证数学函数:");
        
        // ABS函数
        runSQLTest("SELECT name, age, ABS(age - 30) AS age_diff FROM users;", 
                  "ABS函数测试", true);
        
        // ROUND函数
        runSQLTest("SELECT name, age, ROUND(age * 1.5, 1) AS rounded_age FROM users;", 
                  "ROUND函数测试", true);
        
        // SQRT函数
        runSQLTest("SELECT name, age, SQRT(age) AS sqrt_age FROM users;", 
                  "SQRT函数测试", true);
        
        // POWER函数
        runSQLTest("SELECT name, age, POWER(age, 2) AS age_squared FROM users;", 
                  "POWER函数测试", true);
        
        // MOD函数
        runSQLTest("SELECT name, age, MOD(age, 10) AS age_mod FROM users;", 
                  "MOD函数测试", true);
        
        System.out.println();
    }
    
    /**
     * 测试字符串函数
     */
    private static void testStringFunctions() {
        System.out.println("3. 验证字符串函数:");
        
        // UPPER函数
        runSQLTest("SELECT name, UPPER(name) AS upper_name FROM users;", 
                  "UPPER函数测试", true);
        
        // LOWER函数
        runSQLTest("SELECT name, LOWER(name) AS lower_name FROM users;", 
                  "LOWER函数测试", true);
        
        // LENGTH函数
        runSQLTest("SELECT name, LENGTH(name) AS name_length FROM users;", 
                  "LENGTH函数测试", true);
        
        // SUBSTRING函数
        runSQLTest("SELECT name, SUBSTRING(name, 1, 3) AS name_prefix FROM users;", 
                  "SUBSTRING函数测试", true);
        
        // CONCAT函数
        runSQLTest("SELECT CONCAT(name, ' (', age, ')') AS name_with_age FROM users;", 
                  "CONCAT函数测试", true);
        
        System.out.println();
    }
    
    /**
     * 测试日期函数
     */
    private static void testDateFunctions() {
        System.out.println("4. 验证日期函数:");
        
        // NOW函数
        runSQLTest("SELECT NOW() AS current_time;", 
                  "NOW函数测试", true);
        
        // CURRENT_DATE函数
        runSQLTest("SELECT CURRENT_DATE() AS current_date;", 
                  "CURRENT_DATE函数测试", true);
        
        // YEAR函数
        runSQLTest("SELECT YEAR('2023-05-15') AS year_value;", 
                  "YEAR函数测试", true);
        
        // DATEDIFF函数
        runSQLTest("SELECT DATEDIFF('2023-05-20', '2023-05-15') AS date_diff;", 
                  "DATEDIFF函数测试", true);
        
        System.out.println();
    }
    
    /**
     * 测试复合函数调用
     */
    private static void testComplexFunctions() {
        System.out.println("5. 验证复合函数调用:");
        
        // 嵌套函数调用
        runSQLTest("SELECT name, UPPER(SUBSTRING(name, 1, 3)) AS name_code FROM users;", 
                  "嵌套函数调用测试", true);
        
        // 多个函数组合
        runSQLTest("SELECT CONCAT(UPPER(name), ' - AGE: ', ROUND(SQRT(age), 1)) AS formatted_info FROM users;", 
                  "多函数组合测试", true);
        
        // 函数与条件结合
        runSQLTest("SELECT name FROM users WHERE LENGTH(name) > 5;", 
                  "函数条件查询测试", true);
        
        // 函数与聚合结合
        runSQLTest("SELECT COUNT(*) AS user_count, AVG(age) AS avg_age FROM users;", 
                  "聚合函数测试", true);
        
        System.out.println();
    }
    
    /**
     * 运行SQL测试
     */
    private static void runSQLTest(String sql, String testName, boolean expectSuccess) {
        testCount++;
        
        try {
            System.out.print("  测试: " + testName + " ... ");
            
            // 创建临时输入文件
            String tempInput = "temp_input.sql";
            Files.write(Paths.get(tempInput), sql.getBytes());
            
            // 重定向输出
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            
            System.setOut(new PrintStream(baos));
            System.setErr(new PrintStream(baos));
            
            // 运行SQL编译器
            String[] args = {tempInput};
            Main.main(args);
            
            // 恢复输出
            System.setOut(originalOut);
            System.setErr(originalErr);
            
            String output = baos.toString();
            
            // 清理临时文件
            Files.deleteIfExists(Paths.get(tempInput));
            
            // 检查结果
            boolean hasError = output.toLowerCase().contains("error") || 
                             output.toLowerCase().contains("exception") ||
                             output.toLowerCase().contains("failed");
            
            if (expectSuccess && !hasError) {
                System.out.println("✅ 通过");
                passedTests++;
                
                // 显示部分输出结果
                if (!output.trim().isEmpty()) {
                    String[] lines = output.split("\n");
                    System.out.println("    结果预览: " + 
                        (lines.length > 0 ? lines[0].substring(0, Math.min(50, lines[0].length())) + "..." : "无输出"));
                }
            } else if (!expectSuccess && hasError) {
                System.out.println("✅ 通过 (预期失败)");
                passedTests++;
            } else {
                System.out.println("❌ 失败");
                System.out.println("    输出: " + output.substring(0, Math.min(100, output.length())) + "...");
            }
            
        } catch (Exception e) {
            System.out.println("❌ 异常: " + e.getMessage());
        }
    }
}
