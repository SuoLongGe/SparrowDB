import com.database.engine.*;
import com.sqlcompiler.ast.CreateFunctionStatement;
import com.sqlcompiler.lexer.Position;
import com.database.exception.DatabaseException;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * SparrowDB 增强函数功能演示程序
 * 展示专业级数据库函数管理功能
 */
public class EnhancedFunctionDemo {
    private static EnhancedFunctionManager functionManager;
    private static Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        try {
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║           SparrowDB 增强用户自定义函数系统演示             ║");
            System.out.println("║              专业级数据库函数管理功能                      ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.println();
            
            // 初始化系统
            initializeSystem();
            
            // 演示预定义函数
            demonstrateBuiltInFunctions();
            
            // 交互式函数创建
            interactiveFunctionCreation();
            
            System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                       演示完成                              ║");
            System.out.println("║    SparrowDB 现已具备专业级用户自定义函数功能！             ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.err.println("演示过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void initializeSystem() {
        System.out.println("🚀 正在初始化增强函数管理系统...");
        StorageAdapter storageAdapter = new StorageAdapter("data");
        functionManager = new EnhancedFunctionManager(storageAdapter);
        System.out.println("✅ 系统初始化完成！");
        System.out.println();
    }
    
    private static void demonstrateBuiltInFunctions() {
        System.out.println("📚 演示创建多种类型的用户自定义函数：");
        System.out.println();
        
        try {
            // 1. 数学计算函数
            createMathFunction();
            
            // 2. 字符串处理函数
            createStringFunction();
            
            // 3. 条件判断函数
            createConditionalFunction();
            
            System.out.println("✅ 预定义函数创建完成！");
            System.out.println();
            
        } catch (DatabaseException e) {
            System.err.println("❌ 函数创建失败: " + e.getMessage());
        }
    }
    
    private static void createMathFunction() throws DatabaseException {
        System.out.println("🔢 创建数学计算函数: calculate_circle_area");
        
        List<CreateFunctionStatement.FunctionParameter> params = new ArrayList<>();
        params.add(new CreateFunctionStatement.FunctionParameter("radius", "DOUBLE"));
        
        CreateFunctionStatement mathFunc = new CreateFunctionStatement(
            "calculate_circle_area",
            params,
            "DOUBLE",
            "RETURN 3.14159 * radius * radius;",
            false,
            new Position(1, 1)
        );
        
        functionManager.createFunction(mathFunc);
        System.out.println("   ✓ 函数 calculate_circle_area(DOUBLE) 创建成功");
    }
    
    private static void createStringFunction() throws DatabaseException {
        System.out.println("📝 创建字符串处理函数: format_name");
        
        List<CreateFunctionStatement.FunctionParameter> params = new ArrayList<>();
        params.add(new CreateFunctionStatement.FunctionParameter("first_name", "VARCHAR"));
        params.add(new CreateFunctionStatement.FunctionParameter("last_name", "VARCHAR"));
        
        CreateFunctionStatement stringFunc = new CreateFunctionStatement(
            "format_name",
            params,
            "VARCHAR",
            "RETURN CONCAT(last_name, ', ', first_name);",
            false,
            new Position(1, 1)
        );
        
        functionManager.createFunction(stringFunc);
        System.out.println("   ✓ 函数 format_name(VARCHAR, VARCHAR) 创建成功");
    }
    
    private static void createConditionalFunction() throws DatabaseException {
        System.out.println("🎯 创建条件判断函数: get_grade");
        
        List<CreateFunctionStatement.FunctionParameter> params = new ArrayList<>();
        params.add(new CreateFunctionStatement.FunctionParameter("score", "INT"));
        
        CreateFunctionStatement condFunc = new CreateFunctionStatement(
            "get_grade",
            params,
            "VARCHAR",
            "IF score >= 90 THEN RETURN 'A';" +
            "ELSEIF score >= 80 THEN RETURN 'B';" +
            "ELSEIF score >= 70 THEN RETURN 'C';" +
            "ELSEIF score >= 60 THEN RETURN 'D';" +
            "ELSE RETURN 'F'; END IF;",
            false,
            new Position(1, 1)
        );
        
        functionManager.createFunction(condFunc);
        System.out.println("   ✓ 函数 get_grade(INT) 创建成功");
    }
    
    private static void interactiveFunctionCreation() {
        System.out.println("🎮 交互式函数创建体验：");
        System.out.println("您可以创建自己的用户自定义函数!");
        System.out.println();
        
        while (true) {
            System.out.print("是否要创建新的函数？(y/n): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("n") || choice.equals("no")) {
                break;
            }
            
            if (choice.equals("y") || choice.equals("yes")) {
                try {
                    createCustomFunction();
                } catch (DatabaseException e) {
                    System.err.println("❌ 函数创建失败: " + e.getMessage());
                }
            } else {
                System.out.println("请输入 y 或 n");
            }
        }
    }
    
    private static void createCustomFunction() throws DatabaseException {
        System.out.println("\n--- 创建自定义函数 ---");
        
        System.out.print("请输入函数名: ");
        String functionName = scanner.nextLine().trim();
        
        System.out.print("请输入参数个数: ");
        int paramCount = Integer.parseInt(scanner.nextLine().trim());
        
        List<CreateFunctionStatement.FunctionParameter> params = new ArrayList<>();
        for (int i = 0; i < paramCount; i++) {
            System.out.print("参数 " + (i + 1) + " 名称: ");
            String paramName = scanner.nextLine().trim();
            System.out.print("参数 " + (i + 1) + " 类型 (INT/VARCHAR/DOUBLE): ");
            String paramType = scanner.nextLine().trim().toUpperCase();
            params.add(new CreateFunctionStatement.FunctionParameter(paramName, paramType));
        }
        
        System.out.print("返回类型 (INT/VARCHAR/DOUBLE): ");
        String returnType = scanner.nextLine().trim().toUpperCase();
        
        System.out.print("函数体 (如: RETURN param1 + param2;): ");
        String functionBody = scanner.nextLine().trim();
        
        CreateFunctionStatement customFunc = new CreateFunctionStatement(
            functionName,
            params,
            returnType,
            functionBody,
            false,
            new Position(1, 1)
        );
        
        functionManager.createFunction(customFunc);
        System.out.println("✅ 自定义函数 '" + functionName + "' 创建成功！");
        System.out.println();
    }
}

