import com.database.engine.DatabaseEngine;
import java.io.File;
import java.util.List;
import java.util.Map;

public class test_complete_shard {
    public static void main(String[] args) {
        try {
            System.out.println("=== 完整分片功能测试 ===");
            
            // 测试两个数据库
            String[] databases = {"main", "smxx"};
            
            for (String dbName : databases) {
                System.out.println("\n" + "=".repeat(50));
                System.out.println("测试数据库: " + dbName);
                System.out.println("=".repeat(50));
                
                // 1. 初始化数据库引擎
                DatabaseEngine databaseEngine = new DatabaseEngine(dbName, "E:\\SQL实训\\data");
                
                // 2. 清理现有数据
                System.out.println("\n1. 清理现有数据...");
                try {
                    databaseEngine.executeSQL("DROP SHARD test_products");
                    System.out.println("✅ 删除现有分片");
                } catch (Exception e) {
                    System.out.println("分片不存在，继续");
                }
                
                try {
                    databaseEngine.executeSQL("DROP TABLE test_products");
                    System.out.println("✅ 删除现有表");
                } catch (Exception e) {
                    System.out.println("表不存在，继续");
                }
                
                // 3. 创建表
                System.out.println("\n2. 创建测试表...");
                var createTableResult = databaseEngine.executeSQL(
                    "CREATE TABLE test_products (id INT, name VARCHAR(50), supplier VARCHAR(50), price DECIMAL(10,2))"
                );
                if (createTableResult.isSuccess()) {
                    System.out.println("✅ 表创建成功");
                } else {
                    System.out.println("❌ 表创建失败: " + createTableResult.getMessage());
                    continue;
                }
                
                // 4. 插入测试数据
                System.out.println("\n3. 插入测试数据...");
                String[] suppliers = {"SupplierA", "SupplierB", "SupplierC", "SupplierD"};
                for (int i = 1; i <= 8; i++) {
                    String supplier = suppliers[(i-1) % suppliers.length];
                    String insertSQL = String.format(
                        "INSERT INTO test_products VALUES (%d, 'Product%d', '%s', %d.00)",
                        i, i, supplier, i * 100
                    );
                    var insertResult = databaseEngine.executeSQL(insertSQL);
                    if (insertResult.isSuccess()) {
                        System.out.println("✅ 插入成功: Product" + i + " -> " + supplier);
                    } else {
                        System.out.println("❌ 插入失败: " + insertResult.getMessage());
                    }
                }
                
                // 5. 测试CREATE SHARD语法
                System.out.println("\n4. 测试CREATE SHARD语法...");
                var createShardResult = databaseEngine.executeSQL("CREATE SHARD test_products BY supplier USING RANGE (4)");
                if (createShardResult.isSuccess()) {
                    System.out.println("✅ CREATE SHARD 语法支持正常");
                } else {
                    System.out.println("❌ CREATE SHARD 语法错误: " + createShardResult.getMessage());
                    continue;
                }
                
                // 6. 检查分片文件创建
                System.out.println("\n5. 检查分片文件创建...");
                String dbPath = "E:\\SQL实训\\data\\" + dbName;
                String[] expectedShardFiles = {
                    "test_products_shard_0.tbl",
                    "test_products_shard_1.tbl", 
                    "test_products_shard_2.tbl",
                    "test_products_shard_3.tbl"
                };
                
                boolean allShardFilesExist = true;
                for (String shardFile : expectedShardFiles) {
                    File file = new File(dbPath + "\\" + shardFile);
                    if (file.exists()) {
                        System.out.println("✅ 分片文件存在: " + shardFile + " (大小: " + file.length() + " 字节)");
                    } else {
                        System.out.println("❌ 分片文件不存在: " + shardFile);
                        allShardFilesExist = false;
                    }
                }
                
                if (!allShardFilesExist) {
                    System.out.println("❌ 分片文件创建不完整");
                    continue;
                }
                
                // 7. 测试分片数据插入
                System.out.println("\n6. 测试分片数据插入...");
                String[] newSuppliers = {"SupplierE", "SupplierF", "SupplierG", "SupplierH"};
                for (int i = 9; i <= 12; i++) {
                    String supplier = newSuppliers[(i-9) % newSuppliers.length];
                    String insertSQL = String.format(
                        "INSERT INTO test_products VALUES (%d, 'Product%d', '%s', %d.00)",
                        i, i, supplier, i * 100
                    );
                    var insertResult = databaseEngine.executeSQL(insertSQL);
                    if (insertResult.isSuccess()) {
                        System.out.println("✅ 分片插入成功: Product" + i + " -> " + supplier);
                    } else {
                        System.out.println("❌ 分片插入失败: " + insertResult.getMessage());
                    }
                }
                
                // 8. 测试查询功能
                System.out.println("\n7. 测试查询功能...");
                var selectResult = databaseEngine.executeSQL("SELECT * FROM test_products");
                if (selectResult.isSuccess()) {
                    List<Map<String, Object>> records = (List<Map<String, Object>>) selectResult.getData();
                    System.out.println("✅ 查询成功，返回 " + records.size() + " 条记录");
                    
                    // 显示前5条记录
                    for (int i = 0; i < Math.min(5, records.size()); i++) {
                        Map<String, Object> record = records.get(i);
                        System.out.println("  记录" + (i+1) + ": " + record);
                    }
                    if (records.size() > 5) {
                        System.out.println("  ... 还有 " + (records.size() - 5) + " 条记录");
                    }
                } else {
                    System.out.println("❌ 查询失败: " + selectResult.getMessage());
                }
                
                // 9. 测试DROP SHARD语法
                System.out.println("\n8. 测试DROP SHARD语法...");
                var dropShardResult = databaseEngine.executeSQL("DROP SHARD test_products");
                if (dropShardResult.isSuccess()) {
                    System.out.println("✅ DROP SHARD 语法支持正常");
                } else {
                    System.out.println("❌ DROP SHARD 语法错误: " + dropShardResult.getMessage());
                }
                
                // 10. 检查分片元数据
                System.out.println("\n9. 检查分片元数据...");
                File shardMetadataFile = new File(dbPath + "\\__system_shards__.tbl");
                if (shardMetadataFile.exists()) {
                    System.out.println("✅ 分片元数据文件存在: " + shardMetadataFile.getName());
                    System.out.println("   文件大小: " + shardMetadataFile.length() + " 字节");
                } else {
                    System.out.println("❌ 分片元数据文件不存在");
                }
                
                System.out.println("\n✅ 数据库 " + dbName + " 分片功能测试完成");
            }
            
            System.out.println("\n" + "=".repeat(50));
            System.out.println("🎉 所有数据库分片功能测试完成！");
            System.out.println("=".repeat(50));

        } catch (Exception e) {
            System.err.println("测试过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
