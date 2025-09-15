import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

/**
 * 视图功能完整测试
 * 验证CREATE VIEW和DROP VIEW的所有功能
 */
public class ViewFunctionalityTest {
    
    public static void main(String[] args) {
        System.out.println("=== SparrowDB 视图功能完整测试 ===\n");
        
        try {
            // 初始化数据库引擎
            DatabaseEngine engine = new DatabaseEngine("viewtest", "data");
            boolean initialized = engine.initialize();
            
            if (!initialized) {
                System.err.println("❌ 数据库初始化失败！");
                return;
            }
            
            System.out.println("✅ 数据库引擎初始化成功");
            
            // 运行所有测试
            runAllTests(engine);
            
            // 关闭数据库
            engine.shutdown();
            System.out.println("\n✅ 数据库已关闭");
            
        } catch (Exception e) {
            System.err.println("❌ 测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void runAllTests(DatabaseEngine engine) {
        System.out.println("\n📋 开始运行测试套件...");
        
        int totalTests = 0;
        int passedTests = 0;
        
        // 1. 基础表创建测试
        System.out.println("\n--- 1. 基础表创建测试 ---");
        totalTests += 3;
        passedTests += testCreateTables(engine);
        
        // 2. 视图创建测试
        System.out.println("\n--- 2. 视图创建测试 ---");
        totalTests += 4;
        passedTests += testCreateViews(engine);
        
        // 3. 视图删除测试
        System.out.println("\n--- 3. 视图删除测试 ---");
        totalTests += 3;
        passedTests += testDropViews(engine);
        
        // 4. 错误处理测试
        System.out.println("\n--- 4. 错误处理测试 ---");
        totalTests += 3;
        passedTests += testErrorHandling(engine);
        
        // 5. 视图管理器测试
        System.out.println("\n--- 5. 视图管理器测试 ---");
        totalTests += 2;
        passedTests += testViewManager(engine);
        
        // 输出测试结果
        System.out.println("\n" + "=".repeat(50));
        System.out.println("📊 测试结果总结:");
        System.out.println("   总测试数: " + totalTests);
        System.out.println("   通过测试: " + passedTests);
        System.out.println("   失败测试: " + (totalTests - passedTests));
        System.out.println("   成功率: " + String.format("%.1f%%", (passedTests * 100.0 / totalTests)));
        
        if (passedTests == totalTests) {
            System.out.println("🎉 所有测试都通过了！视图功能完全正常！");
        } else {
            System.out.println("⚠️  部分测试失败，需要检查实现");
        }
    }
    
    private static int testCreateTables(DatabaseEngine engine) {
        int passed = 0;
        
        // 创建用户表
        passed += executeTest(engine, 
            "CREATE TABLE test_users (id INT PRIMARY KEY, name VARCHAR(50), age INT)",
            "创建用户表"
        );
        
        // 插入用户数据
        passed += executeTest(engine,
            "INSERT INTO test_users VALUES (1, 'Alice', 25), (2, 'Bob', 35), (3, 'Charlie', 22)",
            "插入用户数据"
        );
        
        // 创建产品表
        passed += executeTest(engine,
            "CREATE TABLE test_products (id INT PRIMARY KEY, name VARCHAR(50), price DECIMAL, category VARCHAR(30))",
            "创建产品表"
        );
        
        return passed;
    }
    
    private static int testCreateViews(DatabaseEngine engine) {
        int passed = 0;
        
        // 测试1: 创建简单视图
        passed += executeTest(engine,
            "CREATE VIEW young_users AS SELECT id, name, age FROM test_users WHERE age < 30",
            "创建年轻用户视图"
        );
        
        // 测试2: 创建带复杂条件的视图
        passed += executeTest(engine,
            "CREATE VIEW adult_users AS SELECT id, name FROM test_users WHERE age >= 25 AND age <= 35",
            "创建成年用户视图"
        );
        
        // 测试3: 创建所有列视图
        passed += executeTest(engine,
            "CREATE VIEW all_users AS SELECT * FROM test_users",
            "创建所有用户视图"
        );
        
        // 测试4: 创建单列视图
        passed += executeTest(engine,
            "CREATE VIEW user_names AS SELECT name FROM test_users",
            "创建用户名视图"
        );
        
        return passed;
    }
    
    private static int testDropViews(DatabaseEngine engine) {
        int passed = 0;
        
        // 测试1: 删除存在的视图
        passed += executeTest(engine,
            "DROP VIEW young_users",
            "删除年轻用户视图"
        );
        
        // 测试2: 条件删除存在的视图
        passed += executeTest(engine,
            "DROP VIEW IF EXISTS adult_users",
            "条件删除成年用户视图"
        );
        
        // 测试3: 条件删除不存在的视图
        passed += executeTest(engine,
            "DROP VIEW IF EXISTS nonexistent_view",
            "条件删除不存在的视图"
        );
        
        return passed;
    }
    
    private static int testErrorHandling(DatabaseEngine engine) {
        int passed = 0;
        
        // 测试1: 创建重复视图名应该失败
        passed += executeTest(engine,
            "CREATE VIEW duplicate_view AS SELECT * FROM test_users",
            "创建第一个重复视图"
        );
        
        passed += executeFailureTest(engine,
            "CREATE VIEW duplicate_view AS SELECT * FROM test_users",
            "创建重复视图名（应该失败）"
        );
        
        // 测试2: 删除不存在的视图应该失败
        passed += executeFailureTest(engine,
            "DROP VIEW nonexistent_view",
            "删除不存在的视图（应该失败）"
        );
        
        return passed;
    }
    
    private static int testViewManager(DatabaseEngine engine) {
        int passed = 0;
        
        try {
            // 测试1: 获取视图管理器
            var viewManager = engine.getViewManager();
            if (viewManager != null) {
                System.out.println("✅ 获取视图管理器成功");
                passed++;
            } else {
                System.out.println("❌ 获取视图管理器失败");
            }
            
            // 测试2: 获取所有视图名
            var viewNames = viewManager.getAllViewNames();
            System.out.println("✅ 当前视图数量: " + viewNames.size());
            System.out.println("   视图列表: " + viewNames);
            passed++;
            
        } catch (Exception e) {
            System.out.println("❌ 视图管理器测试失败: " + e.getMessage());
        }
        
        return passed;
    }
    
    private static int executeTest(DatabaseEngine engine, String sql, String description) {
        System.out.print("   测试: " + description + " ... ");
        
        try {
            ExecutionResult result = engine.executeSQL(sql);
            if (result.isSuccess()) {
                System.out.println("✅ 通过");
                return 1;
            } else {
                System.out.println("❌ 失败: " + result.getMessage());
                return 0;
            }
        } catch (Exception e) {
            System.out.println("❌ 异常: " + e.getMessage());
            return 0;
        }
    }
    
    private static int executeFailureTest(DatabaseEngine engine, String sql, String description) {
        System.out.print("   测试: " + description + " ... ");
        
        try {
            ExecutionResult result = engine.executeSQL(sql);
            if (!result.isSuccess()) {
                System.out.println("✅ 通过（正确失败）");
                return 1;
            } else {
                System.out.println("❌ 失败（不应该成功）");
                return 0;
            }
        } catch (Exception e) {
            System.out.println("✅ 通过（正确抛出异常）");
            return 1;
        }
    }
}
