import com.database.engine.DatabaseEngine;
import com.database.engine.StorageAdapter;
import com.database.engine.CatalogManager;
import com.database.engine.sharding.ShardManager;
import com.database.engine.sharding.RangeShardStrategy;
import java.io.File;

public class test_demo_shard_simple {
    public static void main(String[] args) {
        try {
            System.out.println("=== 简单测试 demo_products 分片 ===");
            
            // 1. 初始化数据库引擎
            DatabaseEngine databaseEngine = new DatabaseEngine("smxx", "E:\\SQL实训\\data");
            
            // 2. 检查表是否存在
            System.out.println("\n1. 检查表是否存在...");
            CatalogManager catalogManager = databaseEngine.getCatalogManager();
            boolean tableExists = catalogManager.tableExists("demo_products");
            System.out.println("demo_products 表存在: " + tableExists);
            
            if (tableExists) {
                System.out.println("表信息: " + catalogManager.getTable("demo_products"));
            }
            
            // 3. 直接测试分片创建
            System.out.println("\n2. 直接测试分片创建...");
            StorageAdapter storageAdapter = databaseEngine.getStorageAdapter();
            ShardManager shardManager = storageAdapter.getShardManager();
            
            if (shardManager != null) {
                System.out.println("分片管理器已初始化");
                
                // 检查是否已有分片
                boolean hasShards = shardManager.hasShards("demo_products");
                System.out.println("demo_products 已有分片: " + hasShards);
                
                if (hasShards) {
                    System.out.println("删除现有分片...");
                    boolean dropResult = shardManager.dropTableShards("demo_products");
                    System.out.println("删除结果: " + dropResult);
                }
                
                // 创建分片
                System.out.println("创建分片...");
                RangeShardStrategy strategy = new RangeShardStrategy();
                boolean createResult = shardManager.createTableShards("demo_products", "supplier", strategy, 4);
                System.out.println("创建结果: " + createResult);
                
            } else {
                System.out.println("❌ 分片管理器未初始化");
            }
            
            // 4. 检查分片文件
            System.out.println("\n3. 检查分片文件...");
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
            
        } catch (Exception e) {
            System.err.println("测试过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
