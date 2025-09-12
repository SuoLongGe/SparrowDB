import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import com.database.config.DatabaseConfig;
import java.util.List;
import java.util.Map;

/**
 * 测试统一数据目录功能
 */
public class TestUnifiedDataDirectory {
    
    public static void main(String[] args) {
        System.out.println("=== 测试统一数据目录功能 ===");
        
        // 显示配置信息
        System.out.println("\n1. 配置信息:");
        System.out.println("当前工作目录: " + System.getProperty("user.dir"));
        System.out.println("GUI数据目录: " + DatabaseConfig.getGUIDataDirectory());
        System.out.println("CLI数据目录: " + DatabaseConfig.getCLIDataDirectory());
        System.out.println("自动检测的数据目录: " + DatabaseConfig.getAutoDetectedDataDirectory());
        
        // 测试数据库引擎
        System.out.println("\n2. 测试数据库引擎:");
        String dataDirectory = DatabaseConfig.getAutoDetectedDataDirectory();
        DatabaseEngine engine = new DatabaseEngine("SparrowDB", dataDirectory);
        
        if (engine.initialize()) {
            System.out.println("✓ 数据库引擎初始化成功");
            System.out.println("数据目录: " + dataDirectory);
            
            // 列出所有表
            System.out.println("\n3. 数据库中的表:");
            List<String> tables = engine.listTables();
            if (tables.isEmpty()) {
                System.out.println("  (没有表)");
            } else {
                for (String table : tables) {
                    System.out.println("  - " + table);
                }
            }
            
            // 测试查询
            System.out.println("\n4. 测试查询功能:");
            ExecutionResult result = engine.executeSQL("SELECT * FROM users LIMIT 3");
            if (result.isSuccess()) {
                System.out.println("✓ 查询成功");
                if (result.getData() != null && !result.getData().isEmpty()) {
                    System.out.println("查询到 " + result.getData().size() + " 条记录");
                    // 显示前3条记录
                    for (int i = 0; i < Math.min(3, result.getData().size()); i++) {
                        Map<String, Object> row = result.getData().get(i);
                        System.out.println("  记录 " + (i+1) + ": " + row);
                    }
                }
            } else {
                System.out.println("✗ 查询失败: " + result.getMessage());
            }
            
            // 测试创建新表
            System.out.println("\n5. 测试创建新表:");
            String createTableSQL = "CREATE TABLE test_unified (id INT PRIMARY KEY, name VARCHAR(50))";
            ExecutionResult createResult = engine.executeSQL(createTableSQL);
            if (createResult.isSuccess()) {
                System.out.println("✓ 新表创建成功");
                
                // 测试插入数据
                String insertSQL = "INSERT INTO test_unified VALUES (1, 'Test User')";
                ExecutionResult insertResult = engine.executeSQL(insertSQL);
                if (insertResult.isSuccess()) {
                    System.out.println("✓ 数据插入成功");
                } else {
                    System.out.println("✗ 数据插入失败: " + insertResult.getMessage());
                }
                
                // 验证新表是否在统一目录中
                List<String> updatedTables = engine.listTables();
                if (updatedTables.contains("test_unified")) {
                    System.out.println("✓ 新表已添加到目录中");
                } else {
                    System.out.println("✗ 新表未添加到目录中");
                }
            } else {
                System.out.println("✗ 新表创建失败: " + createResult.getMessage());
            }
            
            // 显示数据库信息
            System.out.println("\n6. 数据库信息:");
            Map<String, Object> dbInfo = engine.getDatabaseInfo();
            for (Map.Entry<String, Object> entry : dbInfo.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
            
            // 关闭数据库引擎
            engine.shutdown();
            System.out.println("\n✓ 数据库引擎已关闭");
            
        } else {
            System.out.println("✗ 数据库引擎初始化失败");
        }
        
        System.out.println("\n=== 测试完成 ===");
    }
}
