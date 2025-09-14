import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

/**
 * 快速演示用户自定义函数功能
 */
public class QuickFunctionDemo {
    public static void main(String[] args) {
        System.out.println("🎯 SparrowDB 用户自定义函数功能演示");
        System.out.println("=======================================");
        
        try {
            // 创建数据库引擎
            DatabaseEngine engine = new DatabaseEngine("DemoDB", "data");
            engine.initialize(); // 初始化数据库引擎
            
            System.out.println("\n📝 测试1: 创建简单的加法函数");
            String createSQL = "CREATE FUNCTION add_numbers(a INT, b INT) RETURNS INT BEGIN RETURN a + b; END";
            System.out.println("SQL: " + createSQL);
            ExecutionResult result1 = engine.executeSQL(createSQL);
            System.out.println("结果: " + (result1.isSuccess() ? "✅ " + result1.getMessage() : "❌ " + result1.getMessage()));
            
            System.out.println("\n📝 测试2: 调用加法函数");
            String callSQL = "CALL add_numbers(15, 25)";
            System.out.println("SQL: " + callSQL);
            ExecutionResult result2 = engine.executeSQL(callSQL);
            System.out.println("结果: " + (result2.isSuccess() ? "✅ " + result2.getMessage() : "❌ " + result2.getMessage()));
            
            System.out.println("\n📝 测试3: 创建乘法函数");
            String multiplySQL = "CREATE FUNCTION multiply(x INT, y INT) RETURNS INT BEGIN RETURN x * y; END";
            System.out.println("SQL: " + multiplySQL);
            ExecutionResult result3 = engine.executeSQL(multiplySQL);
            System.out.println("结果: " + (result3.isSuccess() ? "✅ " + result3.getMessage() : "❌ " + result3.getMessage()));
            
            System.out.println("\n📝 测试4: 调用乘法函数");
            String callMultiplySQL = "CALL multiply(8, 9)";
            System.out.println("SQL: " + callMultiplySQL);
            ExecutionResult result4 = engine.executeSQL(callMultiplySQL);
            System.out.println("结果: " + (result4.isSuccess() ? "✅ " + result4.getMessage() : "❌ " + result4.getMessage()));
            
            System.out.println("\n📝 测试5: 测试错误处理 - 调用不存在的函数");
            String errorSQL = "CALL non_existent_function(1, 2)";
            System.out.println("SQL: " + errorSQL);
            ExecutionResult result5 = engine.executeSQL(errorSQL);
            System.out.println("结果: " + (result5.isSuccess() ? "🤔 意外成功" : "✅ 正确处理错误: " + result5.getMessage()));
            
            System.out.println("\n📝 测试6: 删除函数");
            String dropSQL = "DROP FUNCTION add_numbers";
            System.out.println("SQL: " + dropSQL);
            ExecutionResult result6 = engine.executeSQL(dropSQL);
            System.out.println("结果: " + (result6.isSuccess() ? "✅ " + result6.getMessage() : "❌ " + result6.getMessage()));
            
            System.out.println("\n🎉 演示完成！");
            System.out.println("\n💡 如何在GUI中验证:");
            System.out.println("1. 双击 run_gui.bat 启动GUI界面");
            System.out.println("2. 在SQL输入框中输入上述SQL语句");
            System.out.println("3. 观察词法分析、语法分析和执行结果");
            System.out.println("4. 检查Token列表中是否包含FUNCTION、CALL、BEGIN、END等");
            System.out.println("5. 检查AST结构是否正确显示函数相关节点");
            
        } catch (Exception e) {
            System.err.println("❌ 演示过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
