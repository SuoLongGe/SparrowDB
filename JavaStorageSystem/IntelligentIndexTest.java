import java.util.*;

/**
 * 智能索引选择器测试程序
 * 演示如何根据查询模式和数据特征自动选择最优索引类型
 */
public class IntelligentIndexTest {
    private static final String TEST_DB_FILE = "intelligent_index_test.db";
    private static final int BUFFER_POOL_SIZE = 50;
    
    public static void main(String[] args) {
        System.out.println("=== 智能索引选择器测试 ===");
        
        // 清理之前的测试文件
        java.io.File testFile = new java.io.File(TEST_DB_FILE);
        if (testFile.exists()) {
            testFile.delete();
        }
        
        try {
            // 创建存储引擎和索引管理器
            StorageEngine engine = new StorageEngine(
                BUFFER_POOL_SIZE, 
                TEST_DB_FILE, 
                ReplacementPolicy.LRU
            );
            
            ExtendedIndexManager indexManager = new ExtendedIndexManager(engine);
            IndexSelector selector = new IndexSelector(indexManager);
            
            // 测试场景1：等值查询为主的小数据集
            testScenario1(selector, indexManager);
            
            // 测试场景2：范围查询为主的大数据集
            testScenario2(selector, indexManager);
            
            // 测试场景3：混合查询模式
            testScenario3(selector, indexManager);
            
            // 测试场景4：极小数据集
            testScenario4(selector, indexManager);
            
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
     * 测试场景1：等值查询为主的小数据集
     * 预期结果：选择哈希索引
     */
    private static void testScenario1(IndexSelector selector, ExtendedIndexManager indexManager) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("场景1：等值查询为主的小数据集");
        System.out.println("=".repeat(60));
        
        // 生成测试数据
        List<Object> sampleData = generateSampleData(200, true);
        
        // 定义查询模式：90%等值查询，10%范围查询
        IndexSelector.QueryPattern pattern = new IndexSelector.QueryPattern(
            900,  // 等值查询
            50,   // 范围查询
            50,   // 排序查询
            1000, // 总查询
            1,    // 1小时时间窗口
            5     // 最大并发5
        );
        
        // 定义数据特征
        IndexSelector.DataCharacteristics characteristics = new IndexSelector.DataCharacteristics(
            true,  // 数值类型
            IndexSelector.GrowthPattern.STABLE, // 稳定增长
            1024 * 1024 * 10 // 10MB内存限制
        );
        
        // 自动选择索引
        String indexName = "scenario1_index";
        boolean created = selector.createOptimalIndex(indexName, sampleData, pattern, characteristics);
        
        if (created) {
            // 测试索引性能
            testIndexPerformance(indexManager, indexName, sampleData, "等值查询测试");
        }
    }
    
    /**
     * 测试场景2：范围查询为主的大数据集
     * 预期结果：选择B+树索引
     */
    private static void testScenario2(IndexSelector selector, ExtendedIndexManager indexManager) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("场景2：范围查询为主的大数据集");
        System.out.println("=".repeat(60));
        
        // 生成测试数据
        List<Object> sampleData = generateSampleData(2000, true);
        
        // 定义查询模式：20%等值查询，70%范围查询，10%排序查询
        IndexSelector.QueryPattern pattern = new IndexSelector.QueryPattern(
            200,  // 等值查询
            700,  // 范围查询
            100,  // 排序查询
            1000, // 总查询
            1,    // 1小时时间窗口
            3     // 最大并发3
        );
        
        // 定义数据特征
        IndexSelector.DataCharacteristics characteristics = new IndexSelector.DataCharacteristics(
            true,  // 数值类型
            IndexSelector.GrowthPattern.LINEAR, // 线性增长
            1024 * 1024 * 50 // 50MB内存限制
        );
        
        // 自动选择索引
        String indexName = "scenario2_index";
        boolean created = selector.createOptimalIndex(indexName, sampleData, pattern, characteristics);
        
        if (created) {
            // 测试索引性能
            testIndexPerformance(indexManager, indexName, sampleData, "范围查询测试");
        }
    }
    
    /**
     * 测试场景3：混合查询模式
     * 预期结果：根据具体分数选择
     */
    private static void testScenario3(IndexSelector selector, ExtendedIndexManager indexManager) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("场景3：混合查询模式");
        System.out.println("=".repeat(60));
        
        // 生成测试数据
        List<Object> sampleData = generateSampleData(1000, true);
        
        // 定义查询模式：50%等值查询，30%范围查询，20%排序查询
        IndexSelector.QueryPattern pattern = new IndexSelector.QueryPattern(
            500,  // 等值查询
            300,  // 范围查询
            200,  // 排序查询
            1000, // 总查询
            1,    // 1小时时间窗口
            8     // 最大并发8
        );
        
        // 定义数据特征
        IndexSelector.DataCharacteristics characteristics = new IndexSelector.DataCharacteristics(
            true,  // 数值类型
            IndexSelector.GrowthPattern.STABLE, // 稳定增长
            1024 * 1024 * 20 // 20MB内存限制
        );
        
        // 自动选择索引
        String indexName = "scenario3_index";
        boolean created = selector.createOptimalIndex(indexName, sampleData, pattern, characteristics);
        
        if (created) {
            // 测试索引性能
            testIndexPerformance(indexManager, indexName, sampleData, "混合查询测试");
        }
    }
    
    /**
     * 测试场景4：极小数据集
     * 预期结果：建议使用线性查找
     */
    private static void testScenario4(IndexSelector selector, ExtendedIndexManager indexManager) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("场景4：极小数据集");
        System.out.println("=".repeat(60));
        
        // 生成测试数据
        List<Object> sampleData = generateSampleData(30, true);
        
        // 定义查询模式：80%等值查询，20%范围查询
        IndexSelector.QueryPattern pattern = new IndexSelector.QueryPattern(
            80,   // 等值查询
            20,   // 范围查询
            0,    // 排序查询
            100,  // 总查询
            1,    // 1小时时间窗口
            2     // 最大并发2
        );
        
        // 定义数据特征
        IndexSelector.DataCharacteristics characteristics = new IndexSelector.DataCharacteristics(
            true,  // 数值类型
            IndexSelector.GrowthPattern.STABLE, // 稳定增长
            1024 * 1024 * 5 // 5MB内存限制
        );
        
        // 自动选择索引
        String indexName = "scenario4_index";
        boolean created = selector.createOptimalIndex(indexName, sampleData, pattern, characteristics);
        
        if (created) {
            // 测试索引性能
            testIndexPerformance(indexManager, indexName, sampleData, "小数据集测试");
        } else {
            System.out.println("建议使用线性查找，测试线性查找性能...");
            testLinearSearchPerformance(sampleData);
        }
    }
    
    /**
     * 生成样本数据
     */
    private static List<Object> generateSampleData(int size, boolean isNumeric) {
        List<Object> data = new ArrayList<>();
        Random random = new Random(42);
        
        if (isNumeric) {
            for (int i = 0; i < size; i++) {
                data.add(random.nextInt(size * 2));
            }
        } else {
            String[] names = {"Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Henry"};
            for (int i = 0; i < size; i++) {
                data.add(names[random.nextInt(names.length)] + "_" + i);
            }
        }
        
        return data;
    }
    
    /**
     * 测试索引性能
     */
    private static void testIndexPerformance(ExtendedIndexManager indexManager, String indexName, 
                                           List<Object> sampleData, String testName) {
        System.out.println("\n--- " + testName + " ---");
        
        // 插入数据
        long startInsert = System.nanoTime();
        int successCount = 0;
        for (int i = 0; i < sampleData.size(); i++) {
            if (indexManager.insert(indexName, sampleData.get(i), i + 1000)) {
                successCount++;
            }
        }
        long insertTime = System.nanoTime() - startInsert;
        
        System.out.println("插入 " + successCount + "/" + sampleData.size() + " 条记录，耗时: " + formatTime(insertTime));
        
        // 查询测试
        Random random = new Random(24);
        int queryCount = Math.min(50, sampleData.size());
        int found = 0;
        
        long startSearch = System.nanoTime();
        for (int i = 0; i < queryCount; i++) {
            Object queryKey = sampleData.get(random.nextInt(sampleData.size()));
            if (indexManager.search(indexName, queryKey) != -1) {
                found++;
            }
        }
        long searchTime = System.nanoTime() - startSearch;
        
        System.out.println("查询 " + queryCount + " 次，找到 " + found + " 条记录，耗时: " + formatTime(searchTime));
        System.out.println("平均每次查询: " + formatTime(searchTime / queryCount));
        
        // 打印索引信息
        indexManager.printIndexInfo(indexName);
    }
    
    /**
     * 测试线性查找性能
     */
    private static void testLinearSearchPerformance(List<Object> sampleData) {
        System.out.println("\n--- 线性查找性能测试 ---");
        
        // 创建线性存储
        List<KeyValuePair> storage = new ArrayList<>();
        
        // 插入数据
        long startInsert = System.nanoTime();
        for (int i = 0; i < sampleData.size(); i++) {
            storage.add(new KeyValuePair(sampleData.get(i), i + 1000));
        }
        long insertTime = System.nanoTime() - startInsert;
        
        System.out.println("插入 " + sampleData.size() + " 条记录，耗时: " + formatTime(insertTime));
        
        // 查询测试
        Random random = new Random(24);
        int queryCount = Math.min(50, sampleData.size());
        int found = 0;
        
        long startSearch = System.nanoTime();
        for (int i = 0; i < queryCount; i++) {
            Object queryKey = sampleData.get(random.nextInt(sampleData.size()));
            for (KeyValuePair pair : storage) {
                if (pair.key.equals(queryKey)) {
                    found++;
                    break;
                }
            }
        }
        long searchTime = System.nanoTime() - startSearch;
        
        System.out.println("查询 " + queryCount + " 次，找到 " + found + " 条记录，耗时: " + formatTime(searchTime));
        System.out.println("平均每次查询: " + formatTime(searchTime / queryCount));
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
     * 键值对类
     */
    private static class KeyValuePair {
        Object key;
        Object value;
        
        public KeyValuePair(Object key, Object value) {
            this.key = key;
            this.value = value;
        }
    }
}
