import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

/**
 * 视图查询功能测试
 * 验证 SELECT FROM view_name 功能是否正常工作
 */
public class ViewQueryTest {
    
    public static void main(String[] args) {
        System.out.println("=== SparrowDB 视图查询功能测试 ===\n");
        
        try {
            // 初始化数据库引擎
            DatabaseEngine engine = new DatabaseEngine("viewquerytest", "data");
            engine.initialize();
            System.out.println("✅ 数据库引擎初始化成功\n");
            
            // 创建测试表和数据
            setupTestData(engine);
            
            // 创建测试视图
            createTestViews(engine);
            
            // 测试视图查询功能
            testViewQueries(engine);
            
            // 关闭数据库
            engine.shutdown();
            System.out.println("\n✅ 数据库已关闭");
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 设置测试数据
     */
    private static void setupTestData(DatabaseEngine engine) {
        System.out.println("--- 设置测试数据 ---");
        
        // 创建用户表
        executeSQL(engine, "CREATE TABLE test_users (id INT PRIMARY KEY, name VARCHAR(50), age INT, department VARCHAR(30))", 
                  "创建用户表");
        
        // 插入测试数据
        executeSQL(engine, "INSERT INTO test_users VALUES (1, 'Alice Johnson', 28, 'Engineering')", 
                  "插入用户1");
        executeSQL(engine, "INSERT INTO test_users VALUES (2, 'Bob Smith', 35, 'Marketing')", 
                  "插入用户2");
        executeSQL(engine, "INSERT INTO test_users VALUES (3, 'Charlie Brown', 22, 'Engineering')", 
                  "插入用户3");
        executeSQL(engine, "INSERT INTO test_users VALUES (4, 'Diana Prince', 30, 'HR')", 
                  "插入用户4");
        executeSQL(engine, "INSERT INTO test_users VALUES (5, 'Eve Wilson', 26, 'Engineering')", 
                  "插入用户5");
        
        // 创建产品表
        executeSQL(engine, "CREATE TABLE test_products (id INT PRIMARY KEY, name VARCHAR(50), price DECIMAL, category VARCHAR(30))", 
                  "创建产品表");
        
        executeSQL(engine, "INSERT INTO test_products VALUES (1, 'Laptop', 1299.99, 'Electronics')", 
                  "插入产品1");
        executeSQL(engine, "INSERT INTO test_products VALUES (2, 'Mouse', 25.50, 'Electronics')", 
                  "插入产品2");
        executeSQL(engine, "INSERT INTO test_products VALUES (3, 'Book', 15.99, 'Education')", 
                  "插入产品3");
        executeSQL(engine, "INSERT INTO test_products VALUES (4, 'Chair', 89.99, 'Furniture')", 
                  "插入产品4");
        executeSQL(engine, "INSERT INTO test_products VALUES (5, 'Monitor', 299.99, 'Electronics')", 
                  "插入产品5");
        
        System.out.println("✅ 测试数据设置完成\n");
    }
    
    /**
     * 创建测试视图
     */
    private static void createTestViews(DatabaseEngine engine) {
        System.out.println("--- 创建测试视图 ---");
        
        // 创建年轻用户视图
        executeSQL(engine, "CREATE VIEW young_users_view AS SELECT id, name, age FROM test_users WHERE age < 30", 
                  "创建年轻用户视图");
        
        // 创建工程部门视图
        executeSQL(engine, "CREATE VIEW engineering_users_view AS SELECT id, name, age, department FROM test_users WHERE department = 'Engineering'", 
                  "创建工程部门视图");
        
        // 创建电子产品视图
        executeSQL(engine, "CREATE VIEW electronics_view AS SELECT id, name, price FROM test_products WHERE category = 'Electronics'", 
                  "创建电子产品视图");
        
        // 创建高价产品视图
        executeSQL(engine, "CREATE VIEW expensive_products_view AS SELECT id, name, price, category FROM test_products WHERE price > 100", 
                  "创建高价产品视图");
        
        System.out.println("✅ 测试视图创建完成\n");
    }
    
    /**
     * 测试视图查询功能
     */
    private static void testViewQueries(DatabaseEngine engine) {
        System.out.println("--- 测试视图查询功能 ---");
        
        // 测试1: 查询年轻用户视图
        System.out.println("🔍 测试1: 查询年轻用户视图");
        executeQuerySQL(engine, "SELECT * FROM young_users_view", "查询年轻用户视图");
        
        // 测试2: 查询工程部门视图
        System.out.println("\n🔍 测试2: 查询工程部门视图");
        executeQuerySQL(engine, "SELECT * FROM engineering_users_view", "查询工程部门视图");
        
        // 测试3: 查询电子产品视图
        System.out.println("\n🔍 测试3: 查询电子产品视图");
        executeQuerySQL(engine, "SELECT * FROM electronics_view", "查询电子产品视图");
        
        // 测试4: 查询高价产品视图
        System.out.println("\n🔍 测试4: 查询高价产品视图");
        executeQuerySQL(engine, "SELECT * FROM expensive_products_view", "查询高价产品视图");
        
        // 测试5: 查询不存在的视图（应该失败）
        System.out.println("\n🔍 测试5: 查询不存在的视图（预期失败）");
        executeQuerySQL(engine, "SELECT * FROM nonexistent_view", "查询不存在的视图");
        
        System.out.println("\n✅ 视图查询功能测试完成");
    }
    
    /**
     * 执行SQL并显示结果
     */
    private static void executeSQL(DatabaseEngine engine, String sql, String description) {
        System.out.print("   " + description + " ... ");
        try {
            ExecutionResult result = engine.executeSQL(sql);
            if (result.isSuccess()) {
                System.out.println("✅ 成功");
            } else {
                System.out.println("❌ 失败: " + result.getMessage());
            }
        } catch (Exception e) {
            System.out.println("❌ 异常: " + e.getMessage());
        }
    }
    
    /**
     * 执行查询SQL并显示详细结果
     */
    private static void executeQuerySQL(DatabaseEngine engine, String sql, String description) {
        System.out.println("   SQL: " + sql);
        try {
            ExecutionResult result = engine.executeSQL(sql);
            if (result.isSuccess()) {
                System.out.println("   ✅ 查询成功: " + result.getMessage());
                if (result.getData() != null && !result.getData().isEmpty()) {
                    System.out.println("   📊 查询结果:");
                    int count = 0;
                    for (var row : result.getData()) {
                        if (count < 5) { // 只显示前5行
                            System.out.println("      " + row);
                        }
                        count++;
                    }
                    if (count > 5) {
                        System.out.println("      ... (" + (count - 5) + " 行未显示)");
                    }
                } else {
                    System.out.println("   📊 无查询结果");
                }
            } else {
                System.out.println("   ❌ 查询失败: " + result.getMessage());
            }
        } catch (Exception e) {
            System.out.println("   ❌ 查询异常: " + e.getMessage());
        }
    }
}
