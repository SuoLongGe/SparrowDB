import com.database.engine.FunctionEvaluator;
import java.util.Arrays;
import java.util.Scanner;

/**
 * 交互式函数功能验证工具
 * 允许用户手动测试各种函数
 */
public class InteractiveFunctionTest {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== SparrowDB 交互式函数测试工具 ===");
        System.out.println("输入 'help' 查看可用函数列表");
        System.out.println("输入 'quit' 退出程序\n");
        
        while (true) {
            System.out.print("函数测试> ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("quit")) {
                System.out.println("再见！");
                break;
            }
            
            if (input.equalsIgnoreCase("help")) {
                showHelp();
                continue;
            }
            
            if (input.isEmpty()) {
                continue;
            }
            
            processFunction(input);
        }
        
        scanner.close();
    }
    
    /**
     * 显示帮助信息
     */
    private static void showHelp() {
        System.out.println("\n=== 可用函数列表 ===");
        
        System.out.println("\n📐 数学函数:");
        System.out.println("  ABS(-10)                    - 绝对值");
        System.out.println("  CEIL(3.2)                   - 向上取整");
        System.out.println("  FLOOR(3.8)                  - 向下取整");
        System.out.println("  ROUND(3.14159, 2)           - 四舍五入");
        System.out.println("  SQRT(16)                    - 平方根");
        System.out.println("  POWER(2, 3)                 - 幂运算");
        System.out.println("  MOD(10, 3)                  - 取模");
        System.out.println("  RAND()                      - 随机数");
        
        System.out.println("\n📝 字符串函数:");
        System.out.println("  UPPER('hello')              - 转大写");
        System.out.println("  LOWER('WORLD')              - 转小写");
        System.out.println("  LENGTH('Hello')             - 字符串长度");
        System.out.println("  SUBSTRING('Hello', 1, 3)    - 截取子串");
        System.out.println("  CONCAT('Hello', ' ', 'World') - 连接字符串");
        System.out.println("  TRIM(' hello ')             - 去除空格");
        System.out.println("  REPLACE('hello', 'l', 'x')  - 字符串替换");
        
        System.out.println("\n📅 日期函数:");
        System.out.println("  NOW()                       - 当前时间");
        System.out.println("  CURRENT_DATE()              - 当前日期");
        System.out.println("  YEAR('2023-05-15')          - 提取年份");
        System.out.println("  MONTH('2023-05-15')         - 提取月份");
        System.out.println("  DAY('2023-05-15')           - 提取日期");
        System.out.println("  DATEDIFF('2023-05-20', '2023-05-15') - 日期差");
        
        System.out.println("\n💡 使用示例:");
        System.out.println("  输入: ABS(-10)");
        System.out.println("  输入: UPPER('hello world')");
        System.out.println("  输入: ROUND(3.14159, 2)");
        System.out.println();
    }
    
    /**
     * 处理函数调用
     */
    private static void processFunction(String input) {
        try {
            // 简单的函数解析
            String functionCall = input.toUpperCase();
            
            if (functionCall.contains("ABS(")) {
                testFunction("ABS", extractParams(input, "ABS"));
            } else if (functionCall.contains("CEIL(")) {
                testFunction("CEIL", extractParams(input, "CEIL"));
            } else if (functionCall.contains("FLOOR(")) {
                testFunction("FLOOR", extractParams(input, "FLOOR"));
            } else if (functionCall.contains("ROUND(")) {
                testFunction("ROUND", extractParams(input, "ROUND"));
            } else if (functionCall.contains("SQRT(")) {
                testFunction("SQRT", extractParams(input, "SQRT"));
            } else if (functionCall.contains("POWER(")) {
                testFunction("POWER", extractParams(input, "POWER"));
            } else if (functionCall.contains("MOD(")) {
                testFunction("MOD", extractParams(input, "MOD"));
            } else if (functionCall.contains("RAND(")) {
                testFunction("RAND", extractParams(input, "RAND"));
            } else if (functionCall.contains("UPPER(")) {
                testFunction("UPPER", extractParams(input, "UPPER"));
            } else if (functionCall.contains("LOWER(")) {
                testFunction("LOWER", extractParams(input, "LOWER"));
            } else if (functionCall.contains("LENGTH(")) {
                testFunction("LENGTH", extractParams(input, "LENGTH"));
            } else if (functionCall.contains("SUBSTRING(")) {
                testFunction("SUBSTRING", extractParams(input, "SUBSTRING"));
            } else if (functionCall.contains("CONCAT(")) {
                testFunction("CONCAT", extractParams(input, "CONCAT"));
            } else if (functionCall.contains("TRIM(")) {
                testFunction("TRIM", extractParams(input, "TRIM"));
            } else if (functionCall.contains("REPLACE(")) {
                testFunction("REPLACE", extractParams(input, "REPLACE"));
            } else if (functionCall.contains("NOW(")) {
                testFunction("NOW", Arrays.asList());
            } else if (functionCall.contains("CURRENT_DATE(")) {
                testFunction("CURRENT_DATE", Arrays.asList());
            } else if (functionCall.contains("YEAR(")) {
                testFunction("YEAR", extractParams(input, "YEAR"));
            } else if (functionCall.contains("MONTH(")) {
                testFunction("MONTH", extractParams(input, "MONTH"));
            } else if (functionCall.contains("DAY(")) {
                testFunction("DAY", extractParams(input, "DAY"));
            } else if (functionCall.contains("DATEDIFF(")) {
                testFunction("DATEDIFF", extractParams(input, "DATEDIFF"));
            } else {
                System.out.println("❌ 未识别的函数: " + input);
                System.out.println("   输入 'help' 查看可用函数列表");
            }
            
        } catch (Exception e) {
            System.out.println("❌ 处理错误: " + e.getMessage());
            System.out.println("   请检查函数调用格式");
        }
    }
    
    /**
     * 测试函数调用
     */
    private static void testFunction(String functionName, java.util.List<Object> params) {
        try {
            Object result = FunctionEvaluator.evaluateFunction(functionName, params);
            System.out.println("✅ 结果: " + result);
            System.out.println("   类型: " + (result != null ? result.getClass().getSimpleName() : "null"));
        } catch (Exception e) {
            System.out.println("❌ 函数执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 提取函数参数
     */
    private static java.util.List<Object> extractParams(String input, String functionName) {
        try {
            // 找到函数名后的括号内容
            int start = input.toUpperCase().indexOf(functionName + "(") + functionName.length() + 1;
            int end = input.lastIndexOf(")");
            
            if (start >= end) {
                return Arrays.asList();
            }
            
            String paramStr = input.substring(start, end).trim();
            if (paramStr.isEmpty()) {
                return Arrays.asList();
            }
            
            // 简单的参数分割（不处理复杂的嵌套情况）
            String[] parts = paramStr.split(",");
            java.util.List<Object> params = new java.util.ArrayList<>();
            
            for (String part : parts) {
                part = part.trim();
                
                // 移除引号
                if ((part.startsWith("'") && part.endsWith("'")) || 
                    (part.startsWith("\"") && part.endsWith("\""))) {
                    params.add(part.substring(1, part.length() - 1));
                } else {
                    // 尝试解析为数字
                    try {
                        if (part.contains(".")) {
                            params.add(Double.parseDouble(part));
                        } else {
                            params.add(Integer.parseInt(part));
                        }
                    } catch (NumberFormatException e) {
                        params.add(part); // 作为字符串处理
                    }
                }
            }
            
            return params;
            
        } catch (Exception e) {
            System.out.println("❌ 参数解析失败: " + e.getMessage());
            return Arrays.asList();
        }
    }
}
