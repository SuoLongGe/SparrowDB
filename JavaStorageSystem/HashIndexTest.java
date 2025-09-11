import java.util.*;

/**
 * 哈希索引测试程序
 */
public class HashIndexTest {
    private static final String TEST_DB_FILE = "hash_index_test.db";
    private static final int BUFFER_POOL_SIZE = 20;
    
    public static void main(String[] args) {
        System.out.println("=== 哈希索引测试程序 ===");
        
        // 清理之前的测试文件
        java.io.File testFile = new java.io.File(TEST_DB_FILE);
        if (testFile.exists()) {
            testFile.delete();
        }
        
        try {
            // 创建存储引擎
            StorageEngine engine = new StorageEngine(
                BUFFER_POOL_SIZE, 
                TEST_DB_FILE, 
                ReplacementPolicy.LRU
            );
            
            // 创建扩展的索引管理器
            ExtendedIndexManager indexManager = new ExtendedIndexManager(engine);
            
            // 测试整数哈希索引
            testIntegerHashIndex(indexManager);
            
            // 测试字符串哈希索引
            testStringHashIndex(indexManager);
            
            // 测试索引类型对比
            testIndexComparison(indexManager);
            
            // 清理测试文件
            if (testFile.exists()) {
                testFile.delete();
            }
            
            System.out.println("\n=== 测试完成 ===");
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试整数哈希索引
     */
    private static void testIntegerHashIndex(ExtendedIndexManager indexManager) {
        System.out.println("\n=== 测试整数哈希索引 ===");
        
        String indexName = "int_hash_index";
        int bucketCount = 10;
        
        // 创建索引
        indexManager.createIntegerHashIndex(indexName, bucketCount);
        
        // 插入测试数据
        System.out.println("\n--- 插入数据 ---");
        Random random = new Random(42);
        for (int i = 0; i < 20; i++) {
            int key = random.nextInt(100);
            int pageId = 1000 + i;
            boolean success = indexManager.insert(indexName, key, pageId);
            System.out.println("插入 (" + key + ", " + pageId + "): " + success);
        }
        
        // 查询测试
        System.out.println("\n--- 查询测试 ---");
        int[] searchKeys = {15, 25, 35, 45, 55};
        for (int key : searchKeys) {
            int pageId = indexManager.search(indexName, key);
            System.out.println("查询 " + key + ": " + (pageId != -1 ? "找到页面ID " + pageId : "未找到"));
        }
        
        // 删除测试
        System.out.println("\n--- 删除测试 ---");
        int deleteKey = 15;
        boolean deleted = indexManager.delete(indexName, deleteKey);
        System.out.println("删除 " + deleteKey + ": " + deleted);
        
        // 再次查询
        int pageId = indexManager.search(indexName, deleteKey);
        System.out.println("删除后查询 " + deleteKey + ": " + (pageId != -1 ? "找到页面ID " + pageId : "未找到"));
        
        // 打印索引信息
        indexManager.printIndexInfo(indexName);
        indexManager.printIndexStructure(indexName);
    }
    
    /**
     * 测试字符串哈希索引
     */
    private static void testStringHashIndex(ExtendedIndexManager indexManager) {
        System.out.println("\n=== 测试字符串哈希索引 ===");
        
        String indexName = "str_hash_index";
        int bucketCount = 5;
        
        // 创建索引
        indexManager.createStringHashIndex(indexName, bucketCount);
        
        // 插入测试数据
        System.out.println("\n--- 插入数据 ---");
        String[] keys = {"apple", "banana", "cherry", "date", "elderberry", "fig", "grape"};
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            int pageId = 2000 + i;
            boolean success = indexManager.insert(indexName, key, pageId);
            System.out.println("插入 (" + key + ", " + pageId + "): " + success);
        }
        
        // 查询测试
        System.out.println("\n--- 查询测试 ---");
        String[] searchKeys = {"apple", "banana", "orange", "grape"};
        for (String key : searchKeys) {
            int pageId = indexManager.search(indexName, key);
            System.out.println("查询 " + key + ": " + (pageId != -1 ? "找到页面ID " + pageId : "未找到"));
        }
        
        // 打印索引信息
        indexManager.printIndexInfo(indexName);
        indexManager.printIndexStructure(indexName);
    }
    
    /**
     * 测试索引类型对比
     */
    private static void testIndexComparison(ExtendedIndexManager indexManager) {
        System.out.println("\n=== 索引类型对比测试 ===");
        
        // 创建B+树索引和哈希索引
        String bplusIndexName = "bplus_comparison";
        String hashIndexName = "hash_comparison";
        
        indexManager.createIntegerBPlusIndex(bplusIndexName, 3);
        indexManager.createIntegerHashIndex(hashIndexName, 10);
        
        // 插入相同的数据
        System.out.println("\n--- 插入相同数据 ---");
        int[] testKeys = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        for (int i = 0; i < testKeys.length; i++) {
            int key = testKeys[i];
            int pageId = 3000 + i;
            
            indexManager.insert(bplusIndexName, key, pageId);
            indexManager.insert(hashIndexName, key, pageId);
            
            System.out.println("插入键 " + key + " 到两个索引");
        }
        
        // 查询性能对比
        System.out.println("\n--- 查询性能对比 ---");
        int queryKey = 50;
        
        // B+树查询
        long startTime = System.nanoTime();
        int bplusResult = indexManager.search(bplusIndexName, queryKey);
        long bplusTime = System.nanoTime() - startTime;
        
        // 哈希索引查询
        startTime = System.nanoTime();
        int hashResult = indexManager.search(hashIndexName, queryKey);
        long hashTime = System.nanoTime() - startTime;
        
        System.out.println("B+树查询 " + queryKey + ": " + (bplusResult != -1 ? "找到页面ID " + bplusResult : "未找到") + 
                          " (耗时: " + bplusTime + " ns)");
        System.out.println("哈希索引查询 " + queryKey + ": " + (hashResult != -1 ? "找到页面ID " + hashResult : "未找到") + 
                          " (耗时: " + hashTime + " ns)");
        
        // 范围查询测试
        System.out.println("\n--- 范围查询测试 ---");
        List<Integer> bplusRange = indexManager.rangeSearch(bplusIndexName, 30, 70);
        List<Integer> hashRange = indexManager.rangeSearch(hashIndexName, 30, 70);
        
        System.out.println("B+树范围查询 [30, 70]: " + bplusRange.size() + " 个结果");
        System.out.println("哈希索引范围查询 [30, 70]: " + hashRange.size() + " 个结果");
        
        // 打印所有索引信息
        indexManager.printAllIndexes();
        indexManager.printStatistics();
    }
}
