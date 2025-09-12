import java.util.List;
import java.util.Random;

/**
 * B+树索引测试类
 */
public class BPlusTreeTest {
    private static final String TEST_DB_FILE = "test_bplus.db";
    private static final int BUFFER_POOL_SIZE = 10;
    private static final int MAX_KEYS = 5; // 较小的值便于测试分裂
    
    public static void main(String[] args) {
        System.out.println("=== B+树索引测试开始 ===");
        
        // 清理之前的测试文件
        java.io.File testFile = new java.io.File(TEST_DB_FILE);
        if (testFile.exists()) {
            testFile.delete();
        }
        
        try {
            // 测试基本功能
            testBasicOperations();
            
            // 测试大量数据插入
            testBulkInsert();
            
            // 测试范围查询
            testRangeSearch();
            
            // 测试删除操作
            testDeleteOperations();
            
            // 测试性能
            testPerformance();
            
            // 测试字符串索引
            testStringIndex();
            
            System.out.println("\n=== B+树索引测试完成 ===");
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理测试文件
            if (testFile.exists()) {
                testFile.delete();
            }
        }
    }
    
    /**
     * 测试基本操作
     */
    private static void testBasicOperations() {
        System.out.println("\n--- 测试基本操作 ---");
        
        StorageEngine engine = new StorageEngine(BUFFER_POOL_SIZE, TEST_DB_FILE, ReplacementPolicy.LRU);
        
        // 创建整数索引
        boolean created = engine.createIntegerIndex("test_int_index", MAX_KEYS);
        System.out.println("创建整数索引: " + (created ? "成功" : "失败"));
        
        // 插入一些数据
        System.out.println("\n插入数据:");
        int[] testKeys = {10, 20, 5, 15, 25, 30, 1, 8, 12, 18};
        for (int i = 0; i < testKeys.length; i++) {
            boolean inserted = engine.insertToIndex("test_int_index", testKeys[i], i + 100);
            System.out.println("插入键 " + testKeys[i] + " -> 页面 " + (i + 100) + ": " + (inserted ? "成功" : "失败"));
        }
        
        // 打印索引信息
        engine.printIndexInfo("test_int_index");
        engine.printIndexStructure("test_int_index");
        
        // 测试查找
        System.out.println("\n测试查找:");
        for (int key : testKeys) {
            int pageId = engine.searchIndex("test_int_index", key);
            System.out.println("查找键 " + key + ": " + (pageId != -1 ? "找到页面 " + pageId : "未找到"));
        }
        
        // 测试查找不存在的键
        int notFound = engine.searchIndex("test_int_index", 999);
        System.out.println("查找不存在的键 999: " + (notFound == -1 ? "正确返回-1" : "错误"));
        
        engine.flushAllPages();
    }
    
    /**
     * 测试大量数据插入
     */
    private static void testBulkInsert() {
        System.out.println("\n--- 测试大量数据插入 ---");
        
        StorageEngine engine = new StorageEngine(BUFFER_POOL_SIZE, TEST_DB_FILE, ReplacementPolicy.LRU);
        
        // 创建索引
        engine.createIntegerIndex("bulk_index", MAX_KEYS);
        
        // 插入100个随机数据
        Random random = new Random(42); // 固定种子以便重现
        int insertCount = 0;
        int failCount = 0;
        
        System.out.println("插入100个随机数据...");
        for (int i = 0; i < 100; i++) {
            int key = random.nextInt(1000);
            boolean inserted = engine.insertToIndex("bulk_index", key, i + 200);
            if (inserted) {
                insertCount++;
            } else {
                failCount++;
            }
        }
        
        System.out.println("插入结果: 成功 " + insertCount + ", 失败 " + failCount);
        
        // 打印索引信息
        engine.printIndexInfo("bulk_index");
        
        // 验证一些数据
        System.out.println("\n验证插入的数据:");
        for (int i = 0; i < 10; i++) {
            int key = random.nextInt(1000);
            int pageId = engine.searchIndex("bulk_index", key);
            System.out.println("键 " + key + ": " + (pageId != -1 ? "找到页面 " + pageId : "未找到"));
        }
        
        engine.flushAllPages();
    }
    
    /**
     * 测试范围查询
     */
    private static void testRangeSearch() {
        System.out.println("\n--- 测试范围查询 ---");
        
        StorageEngine engine = new StorageEngine(BUFFER_POOL_SIZE, TEST_DB_FILE, ReplacementPolicy.LRU);
        
        // 创建索引并插入有序数据
        engine.createIntegerIndex("range_index", MAX_KEYS);
        
        System.out.println("插入有序数据 1-20:");
        for (int i = 1; i <= 20; i++) {
            engine.insertToIndex("range_index", i, i + 300);
        }
        
        // 测试范围查询
        System.out.println("\n范围查询 [5, 15]:");
        List<Integer> results = engine.rangeSearchIndex("range_index", 5, 15);
        System.out.println("找到 " + results.size() + " 个结果:");
        for (int pageId : results) {
            System.out.print(pageId + " ");
        }
        System.out.println();
        
        // 测试边界情况
        System.out.println("\n范围查询 [0, 5]:");
        results = engine.rangeSearchIndex("range_index", 0, 5);
        System.out.println("找到 " + results.size() + " 个结果");
        
        System.out.println("\n范围查询 [15, 25]:");
        results = engine.rangeSearchIndex("range_index", 15, 25);
        System.out.println("找到 " + results.size() + " 个结果");
        
        engine.flushAllPages();
    }
    
    /**
     * 测试删除操作
     */
    private static void testDeleteOperations() {
        System.out.println("\n--- 测试删除操作 ---");
        
        StorageEngine engine = new StorageEngine(BUFFER_POOL_SIZE, TEST_DB_FILE, ReplacementPolicy.LRU);
        
        // 创建索引并插入数据
        engine.createIntegerIndex("delete_index", MAX_KEYS);
        
        int[] keys = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        System.out.println("插入数据:");
        for (int i = 0; i < keys.length; i++) {
            engine.insertToIndex("delete_index", keys[i], i + 400);
        }
        
        // 打印删除前的结构
        System.out.println("\n删除前的索引结构:");
        engine.printIndexStructure("delete_index");
        
        // 删除一些数据
        System.out.println("\n删除数据:");
        int[] deleteKeys = {3, 7, 1, 9};
        for (int key : deleteKeys) {
            boolean deleted = engine.deleteFromIndex("delete_index", key);
            System.out.println("删除键 " + key + ": " + (deleted ? "成功" : "失败"));
        }
        
        // 验证删除结果
        System.out.println("\n验证删除结果:");
        for (int key : keys) {
            int pageId = engine.searchIndex("delete_index", key);
            System.out.println("键 " + key + ": " + (pageId != -1 ? "存在页面 " + pageId : "已删除"));
        }
        
        // 打印删除后的结构
        System.out.println("\n删除后的索引结构:");
        engine.printIndexStructure("delete_index");
        
        engine.flushAllPages();
    }
    
    /**
     * 测试性能
     */
    private static void testPerformance() {
        System.out.println("\n--- 测试性能 ---");
        
        StorageEngine engine = new StorageEngine(BUFFER_POOL_SIZE, TEST_DB_FILE, ReplacementPolicy.LRU);
        
        // 创建索引
        engine.createIntegerIndex("perf_index", MAX_KEYS);
        
        // 测试插入性能
        int testSize = 1000;
        System.out.println("测试插入 " + testSize + " 个数据的性能...");
        
        long startTime = System.currentTimeMillis();
        Random random = new Random(123);
        
        for (int i = 0; i < testSize; i++) {
            int key = random.nextInt(10000);
            engine.insertToIndex("perf_index", key, i + 500);
        }
        
        long insertTime = System.currentTimeMillis() - startTime;
        System.out.println("插入 " + testSize + " 个数据耗时: " + insertTime + "ms");
        
        // 测试查找性能
        System.out.println("测试查找性能...");
        startTime = System.currentTimeMillis();
        
        int foundCount = 0;
        for (int i = 0; i < testSize; i++) {
            int key = random.nextInt(10000);
            int pageId = engine.searchIndex("perf_index", key);
            if (pageId != -1) {
                foundCount++;
            }
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        System.out.println("查找 " + testSize + " 次耗时: " + searchTime + "ms, 找到 " + foundCount + " 个");
        
        // 打印性能统计
        System.out.println("\n性能统计:");
        engine.printCacheStats();
        engine.printIndexInfo("perf_index");
        
        engine.flushAllPages();
    }
    
    /**
     * 测试字符串索引
     */
    private static void testStringIndex() {
        System.out.println("\n--- 测试字符串索引 ---");
        
        StorageEngine engine = new StorageEngine(BUFFER_POOL_SIZE, TEST_DB_FILE, ReplacementPolicy.LRU);
        
        // 创建字符串索引
        boolean created = engine.createStringIndex("string_index", MAX_KEYS);
        System.out.println("创建字符串索引: " + (created ? "成功" : "失败"));
        
        // 插入字符串数据
        String[] testStrings = {"apple", "banana", "cherry", "date", "elderberry", "fig", "grape"};
        System.out.println("\n插入字符串数据:");
        for (int i = 0; i < testStrings.length; i++) {
            boolean inserted = engine.insertToIndex("string_index", testStrings[i], i + 600);
            System.out.println("插入键 '" + testStrings[i] + "' -> 页面 " + (i + 600) + ": " + (inserted ? "成功" : "失败"));
        }
        
        // 测试字符串查找
        System.out.println("\n测试字符串查找:");
        for (String str : testStrings) {
            int pageId = engine.searchIndex("string_index", str);
            System.out.println("查找键 '" + str + "': " + (pageId != -1 ? "找到页面 " + pageId : "未找到"));
        }
        
        // 测试字符串范围查询
        System.out.println("\n字符串范围查询 ['banana', 'date']:");
        List<Integer> results = engine.rangeSearchIndex("string_index", "banana", "date");
        System.out.println("找到 " + results.size() + " 个结果:");
        for (int pageId : results) {
            System.out.print(pageId + " ");
        }
        System.out.println();
        
        engine.printIndexStructure("string_index");
        engine.flushAllPages();
    }
}
