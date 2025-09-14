import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

/**
 * 测试DROP FUNCTION修复 - 验证持久化函数能否正确删除
 */
public class TestDropFunctionFix {
    public static void main(String[] args) {
        System.out.println("🔧 DROP FUNCTION 修复测试");
        System.out.println("=======================================");
        
        try {
            DatabaseEngine engine = new DatabaseEngine("TestDB", "data");
            engine.initialize();
            
            System.out.println("\n📝 测试1: 创建持久化函数");
            String createSQL = "CREATE PERMANENT FUNCTION test_function(a INT, b INT) RETURNS INT BEGIN RETURN a + b; END";
            System.out.println("SQL: " + createSQL);
            ExecutionResult result = engine.executeSQL(createSQL);
            System.out.println("结果: " + result.getMessage());
            
            System.out.println("\n📝 测试2: 查询系统函数表确认创建");
            String querySQL = "SELECT * FROM __system_functions__";
            System.out.println("SQL: " + querySQL);
            result = engine.executeSQL(querySQL);
            System.out.println("结果: " + result.getMessage());
            if (result.getData() != null) {
                System.out.println("函数记录: " + result.getData());
            }
            
            System.out.println("\n📝 测试3: 删除持久化函数");
            String dropSQL = "DROP FUNCTION test_function";
            System.out.println("SQL: " + dropSQL);
            result = engine.executeSQL(dropSQL);
            System.out.println("结果: " + result.getMessage());
            
            System.out.println("\n📝 测试4: 再次查询系统函数表验证删除");
            System.out.println("SQL: " + querySQL);
            result = engine.executeSQL(querySQL);
            System.out.println("结果: " + result.getMessage());
            if (result.getData() != null) {
                System.out.println("函数记录: " + result.getData());
            }
            
            System.out.println("\n📝 测试5: 尝试再次删除（应该失败）");
            System.out.println("SQL: " + dropSQL);
            result = engine.executeSQL(dropSQL);
            System.out.println("结果: " + result.getMessage());
            
            System.out.println("\n📝 测试6: 使用IF EXISTS删除（应该成功）");
            String dropIfExistsSQL = "DROP FUNCTION IF EXISTS test_function";
            System.out.println("SQL: " + dropIfExistsSQL);
            result = engine.executeSQL(dropIfExistsSQL);
            System.out.println("结果: " + result.getMessage());
            
            System.out.println("\n=== DROP FUNCTION 修复测试完成 ===");
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
