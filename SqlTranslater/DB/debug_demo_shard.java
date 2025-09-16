import com.database.engine.DatabaseEngine;
import java.io.File;

public class debug_demo_shard {
    public static void main(String[] args) {
        try {
            System.out.println("=== 调试 demo_products 分片问题 ===");
            
            // 1. 初始化数据库引擎
            DatabaseEngine databaseEngine = new DatabaseEngine("smxx", "E:\\SQL实训\\data");
            
            // 2. 检查分片元数据
            System.out.println("\n1. 检查分片元数据...");
            var shardResult = databaseEngine.executeSQL("SHOW SHARDS");
            if (shardResult.isSuccess()) {
                System.out.println("✅ 分片元数据查询成功");
                System.out.println("结果: " + shardResult.getData());
            } else {
                System.out.println("❌ 分片元数据查询失败: " + shardResult.getMessage());
            }
            
            // 3. 检查分片文件是否存在
            System.out.println("\n2. 检查分片文件...");
            String[] expectedFiles = {
                "E:\\SQL实训\\data\\smxx\\demo_products_shard_0.tbl",
                "E:\\SQL实训\\data\\smxx\\demo_products_shard_1.tbl", 
                "E:\\SQL实训\\data\\smxx\\demo_products_shard_2.tbl",
                "E:\\SQL实训\\data\\smxx\\demo_products_shard_3.tbl"
            };
            
            for (String filePath : expectedFiles) {
                File file = new File(filePath);
                if (file.exists()) {
                    System.out.println("✅ 分片文件存在: " + filePath + " (大小: " + file.length() + " 字节)");
                } else {
                    System.out.println("❌ 分片文件不存在: " + filePath);
                }
            }
            
            // 4. 检查实际创建的分片文件
            System.out.println("\n3. 检查实际创建的分片文件...");
            File dataDir = new File("E:\\SQL实训\\data\\smxx");
            if (dataDir.exists()) {
                File[] files = dataDir.listFiles((dir, name) -> name.startsWith("demo_products_shard_"));
                if (files != null && files.length > 0) {
                    System.out.println("找到 " + files.length + " 个 demo_products 分片文件:");
                    for (File file : files) {
                        System.out.println("  - " + file.getName() + " (大小: " + file.length() + " 字节)");
                    }
                } else {
                    System.out.println("❌ 没有找到 demo_products 分片文件");
                }
            } else {
                System.out.println("❌ 数据目录不存在: " + dataDir.getAbsolutePath());
            }
            
            // 5. 尝试重新创建分片
            System.out.println("\n4. 尝试重新创建分片...");
            var dropResult = databaseEngine.executeSQL("DROP SHARD demo_products");
            if (dropResult.isSuccess()) {
                System.out.println("✅ 删除现有分片成功");
            } else {
                System.out.println("⚠️ 删除现有分片失败: " + dropResult.getMessage());
            }
            
            var createResult = databaseEngine.executeSQL("CREATE SHARD demo_products BY supplier USING RANGE (4)");
            if (createResult.isSuccess()) {
                System.out.println("✅ 重新创建分片成功");
                
                // 再次检查分片文件
                System.out.println("\n5. 重新检查分片文件...");
                for (String filePath : expectedFiles) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        System.out.println("✅ 分片文件存在: " + filePath + " (大小: " + file.length() + " 字节)");
                    } else {
                        System.out.println("❌ 分片文件不存在: " + filePath);
                    }
                }
            } else {
                System.out.println("❌ 重新创建分片失败: " + createResult.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("调试过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
