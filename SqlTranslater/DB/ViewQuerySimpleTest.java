import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

/**
 * 简单的视图查询功能测试
 * 只测试视图创建和查询功能
 */
public class ViewQuerySimpleTest {
    
    public static void main(String[] args) {
        System.out.println("=== 简单视图查询测试 ===\n");
        
        try {
            // 初始化数据库引擎
            DatabaseEngine engine = new DatabaseEngine("simpletest", "data");
            engine.initialize();
            System.out.println("✅ 数据库引擎初始化成功\n");
            
            // 清理旧数据
            cleanupOldData(engine);
            
            // 创建简单测试表
            createSimpleTestTable(engine);
            
            // 创建简单视图
            createSimpleView(engine);
            
            // 测试视图查询
            testSimpleViewQuery(engine);
            
            // 关闭数据库
            engine.shutdown();
            System.out.println("\n✅ 测试完成");
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清理旧数据
     */
    private static void cleanupOldData(DatabaseEngine engine) {
        System.out.println("--- 清理旧数据 ---");
        
        // 删除旧视图
        executeSQL(engine, "DROP VIEW IF EXISTS simple_view", "删除旧视图");
        
        // 删除旧表（这个可能会失败，没关系）
        executeSQL(engine, "DROP TABLE IF EXISTS simple_table", "删除旧表");
        
        System.out.println("✅ 清理完成\n");
    }
    
    /**
     * 创建简单测试表
     */
    private static void createSimpleTestTable(DatabaseEngine engine) {
        System.out.println("--- 创建简单测试表 ---");
        
        // 创建表
        executeSQL(engine, "CREATE TABLE simple_table (id INT PRIMARY KEY, name VARCHAR(50), value INT)", 
                  "创建表");
        
        // 插入数据
        executeSQL(engine, "INSERT INTO simple_table VALUES (1, 'Item1', 100)", "插入数据1");
        executeSQL(engine, "INSERT INTO simple_table VALUES (2, 'Item2', 200)", "插入数据2");
        executeSQL(engine, "INSERT INTO simple_table VALUES (3, 'Item3', 50)", "插入数据3");
        
        System.out.println("✅ 测试表创建完成\n");
    }
    
    /**
     * 创建简单视图
     */
    private static void createSimpleView(DatabaseEngine engine) {
        System.out.println("--- 创建简单视图 ---");
        
        // 创建视图
        executeSQL(engine, "CREATE VIEW simple_view AS SELECT id, name FROM simple_table WHERE value > 75", 
                  "创建简单视图");
        
        System.out.println("✅ 视图创建完成\n");
    }
    
    /**
     * 测试视图查询
     */
    private static void testSimpleViewQuery(DatabaseEngine engine) {
        System.out.println("--- 测试视图查询 ---");
        
        // 查询视图
        System.out.println("🔍 测试: 查询简单视图");
        executeQuerySQL(engine, "SELECT * FROM simple_view", "查询简单视图");
        
        System.out.println("\n✅ 视图查询测试完成");
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
                    for (var row : result.getData()) {
                        System.out.println("      " + row);
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
