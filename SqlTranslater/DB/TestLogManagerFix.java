import com.database.engine.DatabaseEngine;
import com.database.config.DatabaseConfig;

/**
 * 测试LogManager修复
 */
public class TestLogManagerFix {
    
    public static void main(String[] args) {
        System.out.println("=== 测试LogManager修复 ===");
        
        try {
            // 测试数据库引擎初始化
            String dataDirectory = DatabaseConfig.getAutoDetectedDataDirectory();
            System.out.println("数据目录: " + dataDirectory);
            
            DatabaseEngine engine = new DatabaseEngine("SparrowDB", dataDirectory);
            System.out.println("✓ DatabaseEngine创建成功");
            
            if (engine.initialize()) {
                System.out.println("✓ 数据库引擎初始化成功");
                
                // 测试日志统计
                engine.printLogStats();
                
                // 测试简单查询
                System.out.println("\n测试简单查询:");
                var result = engine.executeSQL("SELECT * FROM users LIMIT 1");
                if (result.isSuccess()) {
                    System.out.println("✓ 查询成功");
                } else {
                    System.out.println("✗ 查询失败: " + result.getMessage());
                }
                
                // 关闭引擎
                engine.shutdown();
                System.out.println("✓ 数据库引擎关闭成功");
                
            } else {
                System.out.println("✗ 数据库引擎初始化失败");
            }
            
        } catch (Exception e) {
            System.err.println("✗ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== 测试完成 ===");
    }
}
