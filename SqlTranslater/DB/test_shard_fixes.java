import com.database.engine.DatabaseEngine;
import com.database.engine.StorageAdapter;
import com.database.engine.sharding.ShardManager;
import com.database.engine.CatalogManager;
import com.sqlcompiler.SQLCompiler;
import com.database.engine.ExecutionResult;
import java.io.File;
import java.util.List;
import java.util.Map;

public class test_shard_fixes {
    public static void main(String[] args) {
        System.out.println("=== 测试分片功能修复 ===");
        
        try {
            // 1. 初始化数据库引擎
            System.out.println("\n1. 初始化数据库引擎...");
            DatabaseEngine engine = new DatabaseEngine("smxx", "E:\\SQL实训\\data");
            System.out.println("✅ 数据库引擎初始化成功");
            
            // 2. 创建分片
            System.out.println("\n2. 创建分片...");
            String createSQL = "CREATE SHARD demo_products BY supplier USING RANGE (4)";
            ExecutionResult createResult = engine.executeSQL(createSQL);
            System.out.println("创建分片结果: " + createResult.isSuccess() + " - " + createResult.getMessage());
            
            if (!createResult.isSuccess()) {
                System.out.println("❌ 分片创建失败，退出测试");
                return;
            }
            
            // 3. 测试数据路由 - 插入新数据
            System.out.println("\n3. 测试数据路由...");
            String[] insertSQLs = {
                "INSERT INTO demo_products (product_id, product_name, category, price, stock_quantity, supplier, created_date) VALUES (201, 'Shard Test A', 'Electronics', 199.99, 5, 'SupplierA', '2024-01-01')",
                "INSERT INTO demo_products (product_id, product_name, category, price, stock_quantity, supplier, created_date) VALUES (202, 'Shard Test B', 'Books', 29.99, 10, 'SupplierB', '2024-01-02')",
                "INSERT INTO demo_products (product_id, product_name, category, price, stock_quantity, supplier, created_date) VALUES (203, 'Shard Test C', 'Electronics', 399.99, 3, 'SupplierC', '2024-01-03')"
            };
            
            for (String sql : insertSQLs) {
                ExecutionResult result = engine.executeSQL(sql);
                System.out.println("插入结果: " + result.isSuccess() + " - " + result.getMessage());
            }
            
            // 4. 检查分片文件
            System.out.println("\n4. 检查分片文件...");
            checkShardFiles();
            
            // 5. 测试查询
            System.out.println("\n5. 测试查询...");
            String selectSQL = "SELECT * FROM demo_products WHERE supplier = 'SupplierA'";
            ExecutionResult selectResult = engine.executeSQL(selectSQL);
            System.out.println("查询结果: " + selectResult.isSuccess());
            if (selectResult.isSuccess() && selectResult.getData() != null) {
                System.out.println("返回记录数: " + selectResult.getData().size());
                for (Object record : selectResult.getData()) {
                    System.out.println("记录: " + record);
                }
            }
            
            // 6. 测试DROP SHARD
            System.out.println("\n6. 测试DROP SHARD...");
            String dropSQL = "DROP SHARD demo_products";
            ExecutionResult dropResult = engine.executeSQL(dropSQL);
            System.out.println("删除分片结果: " + dropResult.isSuccess() + " - " + dropResult.getMessage());
            
            // 7. 验证分片文件是否被删除
            System.out.println("\n7. 验证分片文件删除...");
            checkShardFilesAfterDrop();
            
            System.out.println("\n=== 分片功能修复测试完成 ===");
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void checkShardFiles() {
        String baseDir = "E:\\SQL实训\\data\\smxx";
        System.out.println("检查分片文件状态...");
        
        for (int i = 0; i < 4; i++) {
            String shardFile = baseDir + File.separator + "demo_products_shard_" + i + ".tbl";
            File file = new File(shardFile);
            if (file.exists()) {
                System.out.println("✅ 分片文件存在: demo_products_shard_" + i + ".tbl (大小: " + file.length() + " 字节)");
                
                // 检查文件内容
                if (file.length() > 300) {
                    System.out.println("  - 包含数据记录");
                } else {
                    System.out.println("  - 仅包含元数据");
                }
            } else {
                System.out.println("❌ 分片文件不存在: demo_products_shard_" + i + ".tbl");
            }
        }
    }
    
    private static void checkShardFilesAfterDrop() {
        String baseDir = "E:\\SQL实训\\data\\smxx";
        System.out.println("检查分片文件删除状态...");
        
        boolean allDeleted = true;
        for (int i = 0; i < 4; i++) {
            String shardFile = baseDir + File.separator + "demo_products_shard_" + i + ".tbl";
            File file = new File(shardFile);
            if (file.exists()) {
                System.out.println("❌ 分片文件仍存在: demo_products_shard_" + i + ".tbl");
                allDeleted = false;
            } else {
                System.out.println("✅ 分片文件已删除: demo_products_shard_" + i + ".tbl");
            }
        }
        
        if (allDeleted) {
            System.out.println("✅ 所有分片文件已成功删除");
        } else {
            System.out.println("❌ 部分分片文件未删除");
        }
    }
}
