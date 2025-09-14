import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

/**
 * SQL功能扩展测试类
 * 测试WHERE子句、DELETE、UPDATE和LIKE操作符的功能
 */
public class TestSQLExtensions {
    
    public static void main(String[] args) {
        System.out.println("=== SparrowDB SQL功能扩展测试 ===\n");
        
        // 创建数据库引擎
        DatabaseEngine engine = new DatabaseEngine("TestDB", "./test_data");
        
        if (!engine.initialize()) {
            System.err.println("数据库引擎初始化失败");
            return;
        }
        
        try {
            // 1. 创建测试表
            System.out.println("1. 创建测试表...");
            testCreateTable(engine);
            
            // 2. 插入测试数据
            System.out.println("\n2. 插入测试数据...");
            testInsertData(engine);
            
            // 3. 测试WHERE子句的数值比较
            System.out.println("\n3. 测试WHERE子句数值比较...");
            testWhereNumericComparison(engine);
            
            // 4. 测试LIKE操作符
            System.out.println("\n4. 测试LIKE操作符...");
            testLikeOperator(engine);
            
            // 5. 测试DELETE语句
            System.out.println("\n5. 测试DELETE语句...");
            testDeleteStatement(engine);
            
            // 6. 测试UPDATE语句
            System.out.println("\n6. 测试UPDATE语句...");
            testUpdateStatement(engine);
            
            // 7. 验证最终结果
            System.out.println("\n7. 验证最终结果...");
            testFinalResults(engine);
            
        } finally {
            engine.shutdown();
        }
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    private static void testCreateTable(DatabaseEngine engine) {
        String sql = "CREATE TABLE test_users (" +
                    "id INT PRIMARY KEY, " +
                    "name VARCHAR(50), " +
                    "email VARCHAR(100), " +
                    "age INT, " +
                    "salary DECIMAL(10,2)" +
                    ")";
        
        ExecutionResult result = engine.executeSQL(sql);
        printResult("CREATE TABLE", result);
    }
    
    private static void testInsertData(DatabaseEngine engine) {
        String[] insertSQLs = {
            "INSERT INTO test_users VALUES (1, 'Alice', 'alice@example.com', 25, 50000.00)",
            "INSERT INTO test_users VALUES (2, 'Bob', 'bob@example.com', 30, 60000.00)",
            "INSERT INTO test_users VALUES (3, 'Charlie', 'charlie@example.com', 35, 70000.00)",
            "INSERT INTO test_users VALUES (4, 'Diana', 'diana@test.org', 28, 55000.00)",
            "INSERT INTO test_users VALUES (5, 'Eve', 'eve@example.com', 22, 45000.00)"
        };
        
        for (String sql : insertSQLs) {
            ExecutionResult result = engine.executeSQL(sql);
            printResult("INSERT", result);
        }
    }
    
    private static void testWhereNumericComparison(DatabaseEngine engine) {
        String[] testSQLs = {
            "SELECT * FROM test_users WHERE age > 25",
            "SELECT * FROM test_users WHERE age < 30", 
            "SELECT * FROM test_users WHERE salary >= 55000",
            "SELECT * FROM test_users WHERE age = 28"
        };
        
        for (String sql : testSQLs) {
            ExecutionResult result = engine.executeSQL(sql);
            printResult("WHERE数值比较: " + sql, result);
        }
    }
    
    private static void testLikeOperator(DatabaseEngine engine) {
        String[] testSQLs = {
            "SELECT * FROM test_users WHERE email LIKE '%example.com'",
            "SELECT * FROM test_users WHERE name LIKE 'A%'",
            "SELECT * FROM test_users WHERE email LIKE '%test%'"
        };
        
        for (String sql : testSQLs) {
            ExecutionResult result = engine.executeSQL(sql);
            printResult("LIKE操作符: " + sql, result);
        }
    }
    
    private static void testDeleteStatement(DatabaseEngine engine) {
        String[] testSQLs = {
            "DELETE FROM test_users WHERE age < 25",
            "DELETE FROM test_users WHERE email LIKE '%test%'"
        };
        
        for (String sql : testSQLs) {
            ExecutionResult result = engine.executeSQL(sql);
            printResult("DELETE: " + sql, result);
        }
    }
    
    private static void testUpdateStatement(DatabaseEngine engine) {
        String[] testSQLs = {
            "UPDATE test_users SET salary = 65000 WHERE name = 'Bob'",
            "UPDATE test_users SET age = 26 WHERE age = 25"
        };
        
        for (String sql : testSQLs) {
            ExecutionResult result = engine.executeSQL(sql);
            printResult("UPDATE: " + sql, result);
        }
    }
    
    private static void testFinalResults(DatabaseEngine engine) {
        ExecutionResult result = engine.executeSQL("SELECT * FROM test_users");
        printResult("最终查询结果", result);
    }
    
    private static void printResult(String operation, ExecutionResult result) {
        System.out.println("[" + operation + "] " + 
                          (result.isSuccess() ? "✅ 成功" : "❌ 失败") + 
                          ": " + result.getMessage());
        
        if (result.getData() != null && !result.getData().isEmpty()) {
            System.out.println("  返回 " + result.getData().size() + " 条记录");
            // 显示前3条记录作为示例
            for (int i = 0; i < Math.min(3, result.getData().size()); i++) {
                System.out.println("  " + result.getData().get(i));
            }
            if (result.getData().size() > 3) {
                System.out.println("  ... 还有 " + (result.getData().size() - 3) + " 条记录");
            }
        }
        
        if (!result.isSuccess()) {
            System.err.println("  错误详情: " + result.getMessage());
        }
        
        System.out.println();
    }
}




