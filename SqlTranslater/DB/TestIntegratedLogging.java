import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import com.database.config.DatabaseConfig;
import com.database.logging.LogManager;
import java.util.List;
import java.util.Map;

/**
 * 测试集成后的日志功能
 */
public class TestIntegratedLogging {
    
    public static void main(String[] args) {
        System.out.println("=== 测试集成后的日志功能 ===");
        
        // 显示配置信息
        System.out.println("\n1. 配置信息:");
        System.out.println("数据目录: " + DatabaseConfig.getAutoDetectedDataDirectory());
        
        // 测试数据库引擎
        System.out.println("\n2. 测试数据库引擎:");
        String dataDirectory = DatabaseConfig.getAutoDetectedDataDirectory();
        DatabaseEngine engine = new DatabaseEngine("SparrowDB", dataDirectory);
        
        if (engine.initialize()) {
            System.out.println("✓ 数据库引擎初始化成功");
            
            // 显示日志统计信息
            System.out.println("\n3. 初始日志统计:");
            engine.printLogStats();
            
            // 测试各种SQL操作
            System.out.println("\n4. 测试SQL操作和日志记录:");
            
            // 测试查询操作
            System.out.println("\n4.1 测试查询操作:");
            ExecutionResult selectResult = engine.executeSQL("SELECT * FROM users LIMIT 2");
            if (selectResult.isSuccess()) {
                System.out.println("✓ 查询成功，返回 " + (selectResult.getData() != null ? selectResult.getData().size() : 0) + " 条记录");
            } else {
                System.out.println("✗ 查询失败: " + selectResult.getMessage());
            }
            
            // 测试创建表操作
            System.out.println("\n4.2 测试创建表操作:");
            String createTableSQL = "CREATE TABLE test_logging (id INT PRIMARY KEY, name VARCHAR(50), created_at TIMESTAMP)";
            ExecutionResult createResult = engine.executeSQL(createTableSQL);
            if (createResult.isSuccess()) {
                System.out.println("✓ 表创建成功");
            } else {
                System.out.println("✗ 表创建失败: " + createResult.getMessage());
            }
            
            // 测试插入操作
            System.out.println("\n4.3 测试插入操作:");
            String insertSQL = "INSERT INTO test_logging VALUES (1, 'Test User 1', '2025-01-12 10:00:00')";
            ExecutionResult insertResult = engine.executeSQL(insertSQL);
            if (insertResult.isSuccess()) {
                System.out.println("✓ 数据插入成功");
            } else {
                System.out.println("✗ 数据插入失败: " + insertResult.getMessage());
            }
            
            // 测试更新操作
            System.out.println("\n4.4 测试更新操作:");
            String updateSQL = "UPDATE test_logging SET name = 'Updated User' WHERE id = 1";
            ExecutionResult updateResult = engine.executeSQL(updateSQL);
            if (updateResult.isSuccess()) {
                System.out.println("✓ 数据更新成功");
            } else {
                System.out.println("✗ 数据更新失败: " + updateResult.getMessage());
            }
            
            // 测试删除操作
            System.out.println("\n4.5 测试删除操作:");
            String deleteSQL = "DELETE FROM test_logging WHERE id = 1";
            ExecutionResult deleteResult = engine.executeSQL(deleteSQL);
            if (deleteResult.isSuccess()) {
                System.out.println("✓ 数据删除成功");
            } else {
                System.out.println("✗ 数据删除失败: " + deleteResult.getMessage());
            }
            
            // 显示操作后的日志统计
            System.out.println("\n5. 操作后的日志统计:");
            engine.printLogStats();
            
            // 创建检查点
            System.out.println("\n6. 创建检查点:");
            engine.createCheckpoint();
            
            // 清理日志
            System.out.println("\n7. 清理已提交的事务日志:");
            engine.cleanupLogs();
            
            // 最终日志统计
            System.out.println("\n8. 最终日志统计:");
            engine.printLogStats();
            
            // 显示日志文件信息
            System.out.println("\n9. 日志文件信息:");
            try {
                LogManager logManager = engine.getLogManager();
                List<String> logFiles = logManager.getLogFiles();
                System.out.println("日志文件列表:");
                for (String logFile : logFiles) {
                    System.out.println("  - " + logFile);
                }
            } catch (Exception e) {
                System.err.println("获取日志文件信息失败: " + e.getMessage());
            }
            
            // 关闭数据库引擎
            engine.shutdown();
            System.out.println("\n✓ 数据库引擎已关闭");
            
        } else {
            System.out.println("✗ 数据库引擎初始化失败");
        }
        
        System.out.println("\n=== 测试完成 ===");
        System.out.println("\n请检查以下位置查看日志文件:");
        System.out.println("日志目录: " + dataDirectory + "/log/");
        System.out.println("日志文件格式: sparrow_yyyy-MM-dd.log");
    }
}
