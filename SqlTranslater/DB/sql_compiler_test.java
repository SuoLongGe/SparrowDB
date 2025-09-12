import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

public class sql_compiler_test {
    public static void main(String[] args) {
        try {
            System.out.println("=== SQL编译器完整测试 ===\n");
            
            // 1. 初始化数据库引擎
            System.out.println("1. 初始化数据库引擎...");
            DatabaseEngine engine = new DatabaseEngine("compiler_test_db", "./compiler_test_data");
            
            boolean initResult = engine.initialize();
            if (!initResult) {
                System.out.println("数据库引擎初始化失败，退出测试");
                return;
            }
            System.out.println("✓ 数据库引擎初始化成功\n");
            
            // 2. 测试CREATE TABLE
            System.out.println("2. 测试CREATE TABLE语句...");
            ExecutionResult result1 = engine.executeSQL("CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(100), age INT, gpa DECIMAL(3,2))");
            System.out.println("CREATE TABLE结果: " + (result1.isSuccess() ? "✓ 成功" : "✗ 失败") + " - " + result1.getMessage());
            
            if (!result1.isSuccess()) {
                System.out.println("创建表失败，退出测试");
                return;
            }
            
            // 3. 测试INSERT
            System.out.println("\n3. 测试INSERT语句...");
            String[] insertSQLs = {
                "INSERT INTO students VALUES (1, 'Alice', 20, 3.5)",
                "INSERT INTO students VALUES (2, 'Bob', 21, 3.2)",
                "INSERT INTO students VALUES (3, 'Charlie', 19, 3.8)"
            };
            
            for (String sql : insertSQLs) {
                ExecutionResult result = engine.executeSQL(sql);
                System.out.println("INSERT结果: " + (result.isSuccess() ? "✓" : "✗") + " - " + result.getMessage());
            }
            
            // 4. 测试SELECT
            System.out.println("\n4. 测试SELECT语句...");
            ExecutionResult selectResult = engine.executeSQL("SELECT * FROM students");
            System.out.println("SELECT结果: " + (selectResult.isSuccess() ? "✓ 成功" : "✗ 失败") + " - " + selectResult.getMessage());
            
            if (selectResult.isSuccess() && selectResult.getData() != null) {
                System.out.println("查询到 " + selectResult.getData().size() + " 行数据:");
                for (java.util.Map<String, Object> row : selectResult.getData()) {
                    System.out.println("  " + row);
                }
            }
            
            // 5. 测试带WHERE子句的SELECT
            System.out.println("\n5. 测试带WHERE条件的SELECT...");
            ExecutionResult whereResult = engine.executeSQL("SELECT name, age FROM students WHERE age > 20");
            System.out.println("WHERE SELECT结果: " + (whereResult.isSuccess() ? "✓ 成功" : "✗ 失败") + " - " + whereResult.getMessage());
            
            if (whereResult.isSuccess() && whereResult.getData() != null) {
                System.out.println("符合条件的 " + whereResult.getData().size() + " 行数据:");
                for (java.util.Map<String, Object> row : whereResult.getData()) {
                    System.out.println("  " + row);
                }
            }
            
            // 6. 测试UPDATE
            System.out.println("\n6. 测试UPDATE语句...");
            ExecutionResult updateResult = engine.executeSQL("UPDATE students SET age = 22 WHERE name = 'Alice'");
            System.out.println("UPDATE结果: " + (updateResult.isSuccess() ? "✓ 成功" : "✗ 失败") + " - " + updateResult.getMessage());
            
            // 7. 测试DELETE
            System.out.println("\n7. 测试DELETE语句...");
            ExecutionResult deleteResult = engine.executeSQL("DELETE FROM students WHERE id = 3");
            System.out.println("DELETE结果: " + (deleteResult.isSuccess() ? "✓ 成功" : "✗ 失败") + " - " + deleteResult.getMessage());
            
            // 8. 最终验证
            System.out.println("\n8. 最终数据验证...");
            ExecutionResult finalResult = engine.executeSQL("SELECT * FROM students");
            if (finalResult.isSuccess() && finalResult.getData() != null) {
                System.out.println("最终剩余 " + finalResult.getData().size() + " 行数据:");
                for (java.util.Map<String, Object> row : finalResult.getData()) {
                    System.out.println("  " + row);
                }
            }
            
            System.out.println("\n=== SQL编译器测试完成 ===");
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
