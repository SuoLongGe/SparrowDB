import java.util.*;
import java.io.*;

/**
 * 三种索引效率比较测试
 * 1. 线性查找（基准）
 * 2. B+树索引
 * 3. 哈希索引
 */
public class IndexEfficiencyComparison {
    private static final String TEST_DB_FILE = "index_comparison_test.db";
    private static final int BUFFER_POOL_SIZE = 50;
    
    // 测试配置
    private static final int[] DATA_SIZES = {100, 500, 1000, 2000};
    private static final int QUERY_COUNT = 100;
    private static final int WARMUP_ROUNDS = 3;
    
    public static void main(String[] args) {
        System.out.println("=== 三种索引效率比较测试 ===");
        
        // 清理之前的测试文件
        File testFile = new File(TEST_DB_FILE);
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
            
            // 执行比较测试
            runComparisonTest(indexManager);
            
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
     * 执行比较测试
     */
    private static void runComparisonTest(ExtendedIndexManager indexManager) {
        System.out.println("\n测试配置:");
        System.out.println("- 数据量: " + Arrays.toString(DATA_SIZES));
        System.out.println("- 查询次数: " + QUERY_COUNT);
        System.out.println("- 预热轮数: " + WARMUP_ROUNDS);
        
        // 测试结果存储
        List<TestResult> results = new ArrayList<>();
        
        for (int dataSize : DATA_SIZES) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("测试数据量: " + dataSize + " 条记录");
            System.out.println("=".repeat(60));
            
            TestResult result = testWithDataSize(indexManager, dataSize);
            results.add(result);
            
            // 打印当前测试结果
            printTestResult(result);
        }
        
        // 打印综合比较结果
        printComparisonSummary(results);
    }
    
    /**
     * 测试指定数据量的性能
     */
    private static TestResult testWithDataSize(ExtendedIndexManager indexManager, int dataSize) {
        TestResult result = new TestResult(dataSize);
        
        // 生成测试数据
        List<Integer> keys = generateTestData(dataSize);
        List<Integer> queryKeys = generateQueryKeys(keys, QUERY_COUNT);
        
        // 1. 线性查找测试
        System.out.println("\n--- 1. 线性查找测试 ---");
        LinearSearchResult linearResult = testLinearSearch(keys, queryKeys);
        result.linearSearch = linearResult;
        
        // 2. B+树索引测试
        System.out.println("\n--- 2. B+树索引测试 ---");
        IndexTestResult bplusResult = testBPlusTreeIndex(indexManager, keys, queryKeys, dataSize);
        result.bplusTree = bplusResult;
        
        // 3. 哈希索引测试
        System.out.println("\n--- 3. 哈希索引测试 ---");
        IndexTestResult hashResult = testHashIndex(indexManager, keys, queryKeys, dataSize);
        result.hashIndex = hashResult;
        
        return result;
    }
    
    /**
     * 生成测试数据
     */
    private static List<Integer> generateTestData(int size) {
        List<Integer> keys = new ArrayList<>(size);
        Random random = new Random(42); // 固定种子
        
        for (int i = 0; i < size; i++) {
            keys.add(random.nextInt(size * 2)); // 键值范围是数据量的2倍
        }
        
        return keys;
    }
    
    /**
     * 生成查询键
     */
    private static List<Integer> generateQueryKeys(List<Integer> dataKeys, int count) {
        List<Integer> queryKeys = new ArrayList<>(count);
        Random random = new Random(24); // 固定种子
        
        for (int i = 0; i < count; i++) {
            queryKeys.add(dataKeys.get(random.nextInt(dataKeys.size())));
        }
        
        return queryKeys;
    }
    
    /**
     * 线性查找测试
     */
    private static LinearSearchResult testLinearSearch(List<Integer> keys, List<Integer> queryKeys) {
        // 创建线性存储结构
        List<KeyValuePair> storage = new ArrayList<>();
        
        // 插入数据
        long startInsert = System.nanoTime();
        for (int i = 0; i < keys.size(); i++) {
            storage.add(new KeyValuePair(keys.get(i), i + 1000));
        }
        long insertTime = System.nanoTime() - startInsert;
        
        // 预热
        for (int round = 0; round < WARMUP_ROUNDS; round++) {
            for (Integer queryKey : queryKeys) {
                for (KeyValuePair pair : storage) {
                    if (pair.key.equals(queryKey)) {
                        break;
                    }
                }
            }
        }
        
        // 正式测试
        int found = 0;
        long startSearch = System.nanoTime();
        for (Integer queryKey : queryKeys) {
            for (KeyValuePair pair : storage) {
                if (pair.key.equals(queryKey)) {
                    found++;
                    break;
                }
            }
        }
        long searchTime = System.nanoTime() - startSearch;
        
        System.out.println("插入时间: " + formatTime(insertTime));
        System.out.println("查询时间: " + formatTime(searchTime));
        System.out.println("平均查询时间: " + formatTime(searchTime / queryKeys.size()));
        System.out.println("找到记录数: " + found + "/" + queryKeys.size());
        
        return new LinearSearchResult(insertTime, searchTime, found, queryKeys.size());
    }
    
    /**
     * B+树索引测试
     */
    private static IndexTestResult testBPlusTreeIndex(ExtendedIndexManager indexManager, 
                                                     List<Integer> keys, 
                                                     List<Integer> queryKeys, 
                                                     int dataSize) {
        String indexName = "bplus_test_" + dataSize;
        
        // 创建索引
        long startCreate = System.nanoTime();
        indexManager.createIntegerBPlusIndex(indexName, 5);
        long createTime = System.nanoTime() - startCreate;
        
        // 插入数据
        long startInsert = System.nanoTime();
        int successCount = 0;
        for (int i = 0; i < keys.size(); i++) {
            if (indexManager.insert(indexName, keys.get(i), i + 1000)) {
                successCount++;
            }
        }
        long insertTime = System.nanoTime() - startInsert;
        
        // 预热
        for (int round = 0; round < WARMUP_ROUNDS; round++) {
            for (Integer queryKey : queryKeys) {
                indexManager.search(indexName, queryKey);
            }
        }
        
        // 正式测试
        int found = 0;
        long startSearch = System.nanoTime();
        for (Integer queryKey : queryKeys) {
            if (indexManager.search(indexName, queryKey) != -1) {
                found++;
            }
        }
        long searchTime = System.nanoTime() - startSearch;
        
        System.out.println("创建索引时间: " + formatTime(createTime));
        System.out.println("插入时间: " + formatTime(insertTime));
        System.out.println("查询时间: " + formatTime(searchTime));
        System.out.println("平均查询时间: " + formatTime(searchTime / queryKeys.size()));
        System.out.println("成功插入: " + successCount + "/" + keys.size());
        System.out.println("找到记录数: " + found + "/" + queryKeys.size());
        
        return new IndexTestResult(createTime, insertTime, searchTime, found, queryKeys.size(), successCount);
    }
    
    /**
     * 哈希索引测试
     */
    private static IndexTestResult testHashIndex(ExtendedIndexManager indexManager, 
                                                List<Integer> keys, 
                                                List<Integer> queryKeys, 
                                                int dataSize) {
        String indexName = "hash_test_" + dataSize;
        int bucketCount = Math.max(10, keys.size() / 10); // 动态计算桶数量
        
        // 创建索引
        long startCreate = System.nanoTime();
        indexManager.createIntegerHashIndex(indexName, bucketCount);
        long createTime = System.nanoTime() - startCreate;
        
        // 插入数据
        long startInsert = System.nanoTime();
        int successCount = 0;
        for (int i = 0; i < keys.size(); i++) {
            if (indexManager.insert(indexName, keys.get(i), i + 1000)) {
                successCount++;
            }
        }
        long insertTime = System.nanoTime() - startInsert;
        
        // 预热
        for (int round = 0; round < WARMUP_ROUNDS; round++) {
            for (Integer queryKey : queryKeys) {
                indexManager.search(indexName, queryKey);
            }
        }
        
        // 正式测试
        int found = 0;
        long startSearch = System.nanoTime();
        for (Integer queryKey : queryKeys) {
            if (indexManager.search(indexName, queryKey) != -1) {
                found++;
            }
        }
        long searchTime = System.nanoTime() - startSearch;
        
        System.out.println("创建索引时间: " + formatTime(createTime));
        System.out.println("插入时间: " + formatTime(insertTime));
        System.out.println("查询时间: " + formatTime(searchTime));
        System.out.println("平均查询时间: " + formatTime(searchTime / queryKeys.size()));
        System.out.println("成功插入: " + successCount + "/" + keys.size());
        System.out.println("找到记录数: " + found + "/" + queryKeys.size());
        
        return new IndexTestResult(createTime, insertTime, searchTime, found, queryKeys.size(), successCount);
    }
    
    /**
     * 打印单个测试结果
     */
    private static void printTestResult(TestResult result) {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("数据量 " + result.dataSize + " 的测试结果");
        System.out.println("=".repeat(40));
        
        System.out.println("\n插入性能对比:");
        System.out.printf("线性查找: %s\n", formatTime(result.linearSearch.insertTime));
        System.out.printf("B+树索引: %s\n", formatTime(result.bplusTree.insertTime));
        System.out.printf("哈希索引: %s\n", formatTime(result.hashIndex.insertTime));
        
        System.out.println("\n查询性能对比:");
        System.out.printf("线性查找: %s (平均: %s)\n", 
            formatTime(result.linearSearch.searchTime),
            formatTime(result.linearSearch.searchTime / result.linearSearch.queryCount));
        System.out.printf("B+树索引: %s (平均: %s)\n", 
            formatTime(result.bplusTree.searchTime),
            formatTime(result.bplusTree.searchTime / result.bplusTree.queryCount));
        System.out.printf("哈希索引: %s (平均: %s)\n", 
            formatTime(result.hashIndex.searchTime),
            formatTime(result.hashIndex.searchTime / result.hashIndex.queryCount));
        
        // 计算性能提升倍数
        double bplusSpeedup = (double) result.linearSearch.searchTime / result.bplusTree.searchTime;
        double hashSpeedup = (double) result.linearSearch.searchTime / result.hashIndex.searchTime;
        double hashVsBplus = (double) result.bplusTree.searchTime / result.hashIndex.searchTime;
        
        System.out.println("\n性能提升倍数:");
        System.out.printf("B+树 vs 线性查找: %.2fx\n", bplusSpeedup);
        System.out.printf("哈希索引 vs 线性查找: %.2fx\n", hashSpeedup);
        System.out.printf("哈希索引 vs B+树: %.2fx\n", hashVsBplus);
    }
    
    /**
     * 打印综合比较结果
     */
    private static void printComparisonSummary(List<TestResult> results) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("综合性能比较总结");
        System.out.println("=".repeat(80));
        
        System.out.println("\n查询性能趋势 (平均每次查询时间):");
        System.out.printf("%-10s %-15s %-15s %-15s\n", "数据量", "线性查找", "B+树索引", "哈希索引");
        System.out.println("-".repeat(60));
        
        for (TestResult result : results) {
            long linearAvg = result.linearSearch.searchTime / result.linearSearch.queryCount;
            long bplusAvg = result.bplusTree.searchTime / result.bplusTree.queryCount;
            long hashAvg = result.hashIndex.searchTime / result.hashIndex.queryCount;
            
            System.out.printf("%-10d %-15s %-15s %-15s\n", 
                result.dataSize,
                formatTime(linearAvg),
                formatTime(bplusAvg),
                formatTime(hashAvg));
        }
        
        System.out.println("\n性能提升倍数总结:");
        System.out.printf("%-10s %-20s %-20s %-20s\n", "数据量", "B+树提升倍数", "哈希索引提升倍数", "哈希vsB+树");
        System.out.println("-".repeat(80));
        
        for (TestResult result : results) {
            double bplusSpeedup = (double) result.linearSearch.searchTime / result.bplusTree.searchTime;
            double hashSpeedup = (double) result.linearSearch.searchTime / result.hashIndex.searchTime;
            double hashVsBplus = (double) result.bplusTree.searchTime / result.hashIndex.searchTime;
            
            System.out.printf("%-10d %-20.2f %-20.2f %-20.2f\n", 
                result.dataSize, bplusSpeedup, hashSpeedup, hashVsBplus);
        }
        
        // 推荐使用场景
        System.out.println("\n推荐使用场景:");
        System.out.println("1. 小数据集 (< 500): 线性查找或哈希索引");
        System.out.println("2. 中等数据集 (500-1000): 哈希索引（等值查询）或B+树（范围查询）");
        System.out.println("3. 大数据集 (> 1000): B+树索引（通用）或哈希索引（纯等值查询）");
        System.out.println("4. 需要范围查询: 只能选择B+树索引");
        System.out.println("5. 对查询性能要求极高: 优先选择哈希索引");
    }
    
    /**
     * 格式化时间显示
     */
    private static String formatTime(long nanoseconds) {
        if (nanoseconds < 1000) {
            return nanoseconds + " ns";
        } else if (nanoseconds < 1_000_000) {
            return String.format("%.2f μs", nanoseconds / 1000.0);
        } else if (nanoseconds < 1_000_000_000) {
            return String.format("%.2f ms", nanoseconds / 1_000_000.0);
        } else {
            return String.format("%.2f s", nanoseconds / 1_000_000_000.0);
        }
    }
    
    /**
     * 测试结果类
     */
    private static class TestResult {
        int dataSize;
        LinearSearchResult linearSearch;
        IndexTestResult bplusTree;
        IndexTestResult hashIndex;
        
        public TestResult(int dataSize) {
            this.dataSize = dataSize;
        }
    }
    
    /**
     * 线性查找结果
     */
    private static class LinearSearchResult {
        long insertTime;
        long searchTime;
        int found;
        int queryCount;
        
        public LinearSearchResult(long insertTime, long searchTime, int found, int queryCount) {
            this.insertTime = insertTime;
            this.searchTime = searchTime;
            this.found = found;
            this.queryCount = queryCount;
        }
    }
    
    /**
     * 索引测试结果
     */
    private static class IndexTestResult {
        long createTime;
        long insertTime;
        long searchTime;
        int found;
        int queryCount;
        int successCount;
        
        public IndexTestResult(long createTime, long insertTime, long searchTime, 
                              int found, int queryCount, int successCount) {
            this.createTime = createTime;
            this.insertTime = insertTime;
            this.searchTime = searchTime;
            this.found = found;
            this.queryCount = queryCount;
            this.successCount = successCount;
        }
    }
    
    /**
     * 键值对类
     */
    private static class KeyValuePair {
        Integer key;
        Integer value;
        
        public KeyValuePair(Integer key, Integer value) {
            this.key = key;
            this.value = value;
        }
    }
}
