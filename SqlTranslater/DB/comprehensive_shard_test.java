import com.database.engine.DatabaseEngine;
import com.database.engine.StorageAdapter;
import com.database.engine.sharding.ShardManager;
import com.database.engine.CatalogManager;
import com.sqlcompiler.SQLCompiler;
import com.database.engine.ExecutionResult;
import java.io.File;
import java.util.List;
import java.util.Map;

public class comprehensive_shard_test {
    public static void main(String[] args) {
        System.out.println("=== 数据分片功能完整测试 ===");
        
        try {
            // 1. 初始化数据库引擎
            System.out.println("\n1. 初始化数据库引擎...");
            DatabaseEngine engine = new DatabaseEngine("smxx", "E:\\SQL实训\\data");
            System.out.println("✅ 数据库引擎初始化成功");
            
            // 2. 测试语法解析
            System.out.println("\n2. 测试CREATE SHARD语法解析...");
            testSyntaxParsing(engine);
            
            // 3. 测试语义验证
            System.out.println("\n3. 测试分片语义验证...");
            testSemanticValidation(engine);
            
            // 4. 测试分片创建
            System.out.println("\n4. 测试分片创建...");
            testShardCreation(engine);
            
            // 5. 测试数据路由
            System.out.println("\n5. 测试数据路由...");
            testDataRouting(engine);
            
            // 6. 测试查询执行
            System.out.println("\n6. 测试分片表查询...");
            testQueryExecution(engine);
            
            // 7. 验证分片文件结构
            System.out.println("\n7. 验证分片文件结构...");
            verifyShardFiles();
            
            // 8. 测试DROP SHARD
            System.out.println("\n8. 测试DROP SHARD...");
            testDropShard(engine);
            
            System.out.println("\n=== 分片功能测试完成 ===");
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testSyntaxParsing(DatabaseEngine engine) {
        try {
            // 测试CREATE SHARD语法
            String createShardSQL = "CREATE SHARD demo_products BY supplier USING RANGE (4)";
            System.out.println("测试SQL: " + createShardSQL);
            
            // 这里我们直接测试编译器
            SQLCompiler compiler = engine.getSQLCompiler();
            if (compiler != null) {
                System.out.println("✅ SQL编译器可用");
            } else {
                System.out.println("❌ SQL编译器不可用");
            }
            
        } catch (Exception e) {
            System.out.println("❌ 语法解析测试失败: " + e.getMessage());
        }
    }
    
    private static void testSemanticValidation(DatabaseEngine engine) {
        try {
            // 检查表是否存在
            CatalogManager catalog = engine.getCatalogManager();
            boolean tableExists = catalog.tableExists("demo_products");
            System.out.println("demo_products表存在: " + tableExists);
            
            if (tableExists) {
                // 检查列是否存在
                var tableInfo = catalog.getTable("demo_products");
                boolean hasSupplierColumn = false;
                for (var column : tableInfo.getColumns()) {
                    if ("supplier".equalsIgnoreCase(column.getName())) {
                        hasSupplierColumn = true;
                        System.out.println("✅ 找到supplier列: " + column.getDataType());
                        break;
                    }
                }
                if (!hasSupplierColumn) {
                    System.out.println("❌ 未找到supplier列");
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ 语义验证测试失败: " + e.getMessage());
        }
    }
    
    private static void testShardCreation(DatabaseEngine engine) {
        try {
            // 先删除可能存在的分片
            String dropSQL = "DROP SHARD demo_products";
            System.out.println("执行: " + dropSQL);
            ExecutionResult dropResult = engine.executeSQL(dropSQL);
            System.out.println("删除结果: " + dropResult.isSuccess());
            
            // 创建分片
            String createSQL = "CREATE SHARD demo_products BY supplier USING RANGE (4)";
            System.out.println("执行: " + createSQL);
            ExecutionResult createResult = engine.executeSQL(createSQL);
            System.out.println("创建结果: " + createResult.isSuccess() + " - " + createResult.getMessage());
            
            if (createResult.isSuccess()) {
                // 检查分片元数据
                ShardManager shardManager = engine.getStorageAdapter().getShardManager();
                if (shardManager != null) {
                    boolean hasShards = shardManager.hasShards("demo_products");
                    System.out.println("分片存在: " + hasShards);
                    
                    if (hasShards) {
                        var shardMetadata = shardManager.getShardMetadata("demo_products");
                        System.out.println("分片数量: " + shardMetadata.getShards().size());
                        System.out.println("分片策略: " + shardMetadata.getStrategy().getClass().getSimpleName());
                        System.out.println("分片键: " + shardMetadata.getShardKeyColumn());
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ 分片创建测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testDataRouting(DatabaseEngine engine) {
        try {
            // 插入一些测试数据
            String[] insertSQLs = {
                "INSERT INTO demo_products (product_id, product_name, category, price, stock_quantity, supplier, created_date) VALUES (101, 'Test Product A', 'Electronics', 99.99, 10, 'SupplierA', '2024-01-01')",
                "INSERT INTO demo_products (product_id, product_name, category, price, stock_quantity, supplier, created_date) VALUES (102, 'Test Product B', 'Books', 29.99, 20, 'SupplierB', '2024-01-02')",
                "INSERT INTO demo_products (product_id, product_name, category, price, stock_quantity, supplier, created_date) VALUES (103, 'Test Product C', 'Electronics', 199.99, 5, 'SupplierC', '2024-01-03')"
            };
            
            System.out.println("测试数据插入...");
            for (String sql : insertSQLs) {
                ExecutionResult result = engine.executeSQL(sql);
                System.out.println("插入结果: " + result.isSuccess() + " - " + result.getMessage());
            }
            
        } catch (Exception e) {
            System.out.println("❌ 数据路由测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testQueryExecution(DatabaseEngine engine) {
        try {
            // 测试查询分片表
            String selectSQL = "SELECT * FROM demo_products WHERE supplier = 'SupplierA'";
            System.out.println("执行查询: " + selectSQL);
            ExecutionResult result = engine.executeSQL(selectSQL);
            System.out.println("查询结果: " + result.isSuccess());
            if (result.isSuccess() && result.getData() != null) {
                System.out.println("返回记录数: " + result.getData().size());
                for (Object record : result.getData()) {
                    System.out.println("记录: " + record);
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ 查询执行测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void verifyShardFiles() {
        try {
            String baseDir = "E:\\SQL实训\\data\\smxx";
            System.out.println("检查分片文件...");
            
            for (int i = 0; i < 4; i++) {
                String shardFile = baseDir + File.separator + "demo_products_shard_" + i + ".tbl";
                File file = new File(shardFile);
                if (file.exists()) {
                    System.out.println("✅ 分片文件存在: demo_products_shard_" + i + ".tbl (大小: " + file.length() + " 字节)");
                    
                    // 检查文件内容
                    if (file.length() > 300) { // 有数据的文件应该更大
                        System.out.println("  - 包含数据记录");
                    } else {
                        System.out.println("  - 仅包含元数据");
                    }
                } else {
                    System.out.println("❌ 分片文件不存在: demo_products_shard_" + i + ".tbl");
                }
            }
            
            // 检查分片元数据文件
            String shardMetadataFile = baseDir + File.separator + "__system_shards__.tbl";
            File metadataFile = new File(shardMetadataFile);
            if (metadataFile.exists()) {
                System.out.println("✅ 分片元数据文件存在: __system_shards__.tbl (大小: " + metadataFile.length() + " 字节)");
            } else {
                System.out.println("❌ 分片元数据文件不存在");
            }
            
        } catch (Exception e) {
            System.out.println("❌ 文件验证失败: " + e.getMessage());
        }
    }
    
    private static void testDropShard(DatabaseEngine engine) {
        try {
            String dropSQL = "DROP SHARD demo_products";
            System.out.println("执行: " + dropSQL);
            ExecutionResult result = engine.executeSQL(dropSQL);
            System.out.println("删除分片结果: " + result.isSuccess() + " - " + result.getMessage());
            
            if (result.isSuccess()) {
                // 验证分片文件是否被删除
                String baseDir = "E:\\SQL实训\\data\\smxx";
                boolean allFilesDeleted = true;
                for (int i = 0; i < 4; i++) {
                    String shardFile = baseDir + File.separator + "demo_products_shard_" + i + ".tbl";
                    File file = new File(shardFile);
                    if (file.exists()) {
                        System.out.println("❌ 分片文件仍存在: demo_products_shard_" + i + ".tbl");
                        allFilesDeleted = false;
                    }
                }
                if (allFilesDeleted) {
                    System.out.println("✅ 所有分片文件已删除");
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ DROP SHARD测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
