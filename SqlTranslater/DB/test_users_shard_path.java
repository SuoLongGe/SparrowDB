import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import java.io.File;

public class test_users_shard_path {
    public static void main(String[] args) {
        System.out.println("=== 测试users表分片文件路径修复 ===");
        
        try {
            // 1. 初始化数据库引擎
            DatabaseEngine engine = new DatabaseEngine("smxx", "E:\\SQL实训\\data\\smxx");
            System.out.println("数据库引擎初始化成功");
            
            // 2. 先删除可能存在的分片
            System.out.println("\n删除现有分片...");
            ExecutionResult dropResult = engine.executeSQL("DROP SHARD users");
            System.out.println("删除分片结果: " + dropResult.isSuccess());
            
            // 3. 创建新的分片
            System.out.println("\n创建新分片...");
            ExecutionResult createResult = engine.executeSQL("CREATE SHARD users BY id USING HASH (3)");
            System.out.println("创建分片结果: " + createResult.isSuccess() + " - " + createResult.getMessage());
            
            if (createResult.isSuccess()) {
                // 4. 检查分片文件是否在正确位置创建
                System.out.println("\n检查分片文件位置...");
                String baseDir = "E:\\SQL实训\\data\\smxx";
                
                for (int i = 0; i < 3; i++) {
                    String shardFile = baseDir + File.separator + "users_shard_" + i + ".tbl";
                    File file = new File(shardFile);
                    System.out.println("分片文件 " + i + ": " + shardFile);
                    System.out.println("  存在: " + file.exists());
                    if (file.exists()) {
                        System.out.println("  大小: " + file.length() + " 字节");
                    }
                }
                
                // 5. 检查分片元数据
                System.out.println("\n检查分片元数据...");
                String metadataFile = baseDir + File.separator + "__system_shards__.tbl";
                File metadata = new File(metadataFile);
                if (metadata.exists()) {
                    System.out.println("分片元数据文件存在: " + metadataFile);
                    System.out.println("文件大小: " + metadata.length() + " 字节");
                } else {
                    System.out.println("分片元数据文件不存在");
                }
                
                // 6. 测试数据插入
                System.out.println("\n测试数据插入...");
                ExecutionResult insertResult = engine.executeSQL(
                    "INSERT INTO users (id, name, email, age) " +
                    "VALUES (999, 'Path Test User', 'test@example.com', 25)"
                );
                System.out.println("插入结果: " + insertResult.isSuccess() + " - " + insertResult.getMessage());
                
            } else {
                System.out.println("分片创建失败，无法继续测试");
            }
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
