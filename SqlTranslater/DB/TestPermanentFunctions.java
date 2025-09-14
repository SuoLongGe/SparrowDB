import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

/**
 * 测试 CREATE PERMANENT FUNCTION 功能
 */
public class TestPermanentFunctions {
    public static void main(String[] args) {
        System.out.println("🎯 CREATE PERMANENT FUNCTION 功能测试");
        System.out.println("=======================================");
        
        try {
            // 创建数据库引擎
            DatabaseEngine engine = new DatabaseEngine("TestDB", "data");
            engine.initialize(); // 初始化数据库引擎
            
            System.out.println("\n📝 测试1: 创建普通函数（非持久化）");
            String sql1 = "CREATE FUNCTION temp_add(a INT, b INT) RETURNS INT BEGIN RETURN a + b; END";
            System.out.println("SQL: " + sql1);
            ExecutionResult result1 = engine.executeSQL(sql1);
            System.out.println("结果: " + (result1.isSuccess() ? "✅ " + result1.getMessage() : "❌ " + result1.getMessage()));
            
            System.out.println("\n📝 测试2: 创建持久化函数");
            String sql2 = "CREATE PERMANENT FUNCTION permanent_multiply(x INT, y INT) RETURNS INT BEGIN RETURN x * y; END";
            System.out.println("SQL: " + sql2);
            ExecutionResult result2 = engine.executeSQL(sql2);
            System.out.println("结果: " + (result2.isSuccess() ? "✅ " + result2.getMessage() : "❌ " + result2.getMessage()));
            
            System.out.println("\n📝 测试3: 创建另一个持久化函数（带条件判断）");
            String sql3 = "CREATE PERMANENT FUNCTION permanent_max(a INT, b INT) RETURNS INT BEGIN IF a > b THEN RETURN a; ELSE RETURN b; END IF; END";
            System.out.println("SQL: " + sql3);
            ExecutionResult result3 = engine.executeSQL(sql3);
            System.out.println("结果: " + (result3.isSuccess() ? "✅ " + result3.getMessage() : "❌ " + result3.getMessage()));
            
            System.out.println("\n📝 测试4: 调用普通函数");
            String call1 = "CALL temp_add(5, 3)";
            System.out.println("SQL: " + call1);
            ExecutionResult callResult1 = engine.executeSQL(call1);
            System.out.println("结果: " + (callResult1.isSuccess() ? "✅ " + callResult1.getMessage() : "❌ " + callResult1.getMessage()));
            
            System.out.println("\n📝 测试5: 调用持久化函数");
            String call2 = "CALL permanent_multiply(4, 6)";
            System.out.println("SQL: " + call2);
            ExecutionResult callResult2 = engine.executeSQL(call2);
            System.out.println("结果: " + (callResult2.isSuccess() ? "✅ " + callResult2.getMessage() : "❌ " + callResult2.getMessage()));
            
            System.out.println("\n📝 测试6: 调用带条件的持久化函数");
            String call3 = "CALL permanent_max(10, 7)";
            System.out.println("SQL: " + call3);
            ExecutionResult callResult3 = engine.executeSQL(call3);
            System.out.println("结果: " + (callResult3.isSuccess() ? "✅ " + callResult3.getMessage() : "❌ " + callResult3.getMessage()));
            
            System.out.println("\n📝 测试7: 再次调用相同参数验证一致性");
            String call4 = "CALL permanent_max(7, 10)";
            System.out.println("SQL: " + call4);
            ExecutionResult callResult4 = engine.executeSQL(call4);
            System.out.println("结果: " + (callResult4.isSuccess() ? "✅ " + callResult4.getMessage() : "❌ " + callResult4.getMessage()));
            
            System.out.println("\n📝 测试8: 查询系统函数表验证持久化");
            String querySystem = "SELECT * FROM __system_functions__";
            System.out.println("SQL: " + querySystem);
            ExecutionResult queryResult = engine.executeSQL(querySystem);
            System.out.println("结果: " + (queryResult.isSuccess() ? "✅ " + queryResult.getMessage() : "❌ " + queryResult.getMessage()));
            
            System.out.println("\n=== 持久化函数测试完成 ===");
            System.out.println("注意: 持久化函数应该保存在 data/__system_functions__.tbl 文件中");
            System.out.println("重启数据库后，持久化函数应该能够自动加载");
            
        } catch (Exception e) {
            System.err.println("❌ 测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
