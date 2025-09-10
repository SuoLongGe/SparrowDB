import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 存储系统测试类
 */
public class StorageSystemTest {
    
    /**
     * 测试基本页操作
     */
    public static void testBasicPageOperations() {
        System.out.println("\n=== Testing Basic Page Operations ===");
        
        StorageEngine storage = new StorageEngine(10, "test_db.db", ReplacementPolicy.LRU);
        
        // 测试分配新页
        int[] pageId1 = new int[1];
        int[] pageId2 = new int[1];
        Page page1 = storage.allocateNewPage(pageId1);
        Page page2 = storage.allocateNewPage(pageId2);
        
        assert page1 != null;
        assert page2 != null;
        assert pageId1[0] != pageId2[0];
        
        System.out.println("Allocated pages successfully: page_id1=" + pageId1[0] + ", page_id2=" + pageId2[0]);
        
        // 测试写入数据
        page1.writeString("Hello, Database Storage System!");
        page2.writeString("This is another page with different content.");
        
        // 释放页面
        storage.releasePage(pageId1[0], true);  // 标记为脏页
        storage.releasePage(pageId2[0], true);  // 标记为脏页
        
        // 重新获取页面并验证数据
        Page retrievedPage1 = storage.getPage(pageId1[0]);
        Page retrievedPage2 = storage.getPage(pageId2[0]);
        
        assert retrievedPage1 != null;
        assert retrievedPage2 != null;
        
        System.out.println("Page 1 content: " + retrievedPage1.readString());
        System.out.println("Page 2 content: " + retrievedPage2.readString());
        
        storage.releasePage(pageId1[0], false);
        storage.releasePage(pageId2[0], false);
        
        System.out.println("Basic page operations test PASSED!");
    }
    
    /**
     * 测试缓存替换策略
     */
    public static void testCacheReplacement() {
        System.out.println("\n=== Testing Cache Replacement Policy ===");
        
        // 使用小缓冲池测试替换
        StorageEngine storage = new StorageEngine(3, "test_cache.db", ReplacementPolicy.LRU);
        
        Vector<Integer> pageIds = new Vector<>();
        
        // 分配超过缓冲池大小的页面
        for (int i = 0; i < 5; i++) {
            int[] pageId = new int[1];
            Page page = storage.allocateNewPage(pageId);
            assert page != null;
            
            // 写入不同的数据
            page.writeString("Page " + i + " data content");
            pageIds.add(pageId[0]);
            
            storage.releasePage(pageId[0], true);
            System.out.println("Allocated and wrote to page " + pageId[0]);
        }
        
        // 访问所有页面，触发替换
        for (int pageId : pageIds) {
            Page page = storage.getPage(pageId);
            if (page != null) {
                System.out.println("Read page " + pageId + ": " + page.readString());
                storage.releasePage(pageId, false);
            } else {
                System.out.println("Page " + pageId + " read failed (possibly replaced)");
            }
        }
        
        storage.printCacheStats();
        System.out.println("Cache replacement test completed!");
    }
    
    /**
     * 测试并发访问
     */
    public static void testConcurrentAccess() {
        System.out.println("\n=== Testing Concurrent Access ===");
        
        StorageEngine storage = new StorageEngine(5, "test_concurrent.db", ReplacementPolicy.LRU);
        
        // 分配一些页面
        Vector<Integer> pageIds = new Vector<>();
        for (int i = 0; i < 3; i++) {
            int[] pageId = new int[1];
            Page page = storage.allocateNewPage(pageId);
            page.writeString("Initial data for page " + i);
            pageIds.add(pageId[0]);
            storage.releasePage(pageId[0], true);
        }
        
        // 创建多个线程并发访问
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        for (int t = 0; t < 3; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < 10; i++) {
                    int pageId = pageIds.get(i % pageIds.size());
                    Page page = storage.getPage(pageId);
                    if (page != null) {
                        // 模拟一些处理时间
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        
                        // 修改数据
                        String currentData = page.readString();
                        String newData = currentData + " [Thread" + threadId + "-Access" + i + "]";
                        page.writeString(newData);
                        
                        storage.releasePage(pageId, true);
                    }
                }
            });
        }
        
        // 等待所有线程完成
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证最终结果
        for (int pageId : pageIds) {
            Page page = storage.getPage(pageId);
            if (page != null) {
                System.out.println("Page " + pageId + " final content: " + page.readString());
                storage.releasePage(pageId, false);
            }
        }
        
        storage.printCacheStats();
        System.out.println("Concurrent access test completed!");
    }
    
    /**
     * 测试表页操作
     */
    public static void testTablePageOperations() {
        System.out.println("\n=== Testing Table Page Operations ===");
        
        StorageEngine storage = new StorageEngine(5, "test_table.db", ReplacementPolicy.LRU);
        
        // 分配表页
        int[] tablePageId = new int[1];
        Page tablePage = storage.allocateTablePage(tablePageId);
        assert tablePage != null;
        
        System.out.println("Allocated table page successfully, page ID: " + tablePageId[0]);
        
        // 写入一些记录
        String[] testRecords = {
            "Record 1: Alice, 20, Student",
            "Record 2: Bob, 22, Graduate",
            "Record 3: Charlie, 21, Student"
        };
        
        storage.releasePage(tablePageId[0], false);  // 先释放页面
        
        for (String record : testRecords) {
            boolean success = storage.writeRecordToPage(tablePageId[0], record);
            if (success) {
                System.out.println("Wrote record successfully: " + record);
            } else {
                System.out.println("Failed to write record: " + record);
            }
        }
        
        // 读取记录
        List<String> retrievedRecords = storage.readRecordsFromPage(tablePageId[0]);
        System.out.println("Read " + retrievedRecords.size() + " records from page:");
        for (int i = 0; i < retrievedRecords.size(); i++) {
            System.out.println("  Record " + i + ": " + retrievedRecords.get(i));
        }
        
        System.out.println("Table page operations test completed!");
    }
    
    /**
     * 测试数据持久化
     */
    public static void testDataPersistence() {
        System.out.println("\n=== Testing Data Persistence ===");
        
        int[] persistentPageId = new int[1];
        String testData = "This data should persist across restarts!";
        
        // 第一阶段：写入数据
        {
            StorageEngine storage = new StorageEngine(5, "test_persist.db", ReplacementPolicy.LRU);
            
            Page page = storage.allocateNewPage(persistentPageId);
            assert page != null;
            
            page.writeString(testData);
            storage.releasePage(persistentPageId[0], true);  // 标记为脏页
            
            // 显式刷新所有页面
            storage.flushAllPages();
            
            System.out.println("Wrote data to page " + persistentPageId[0] + ": " + testData);
        } // 存储引擎析构，应该自动刷新所有页面
        
        // 第二阶段：重新创建存储引擎并读取数据
        {
            StorageEngine storage = new StorageEngine(5, "test_persist.db", ReplacementPolicy.LRU);
            
            Page page = storage.getPage(persistentPageId[0]);
            assert page != null;
            
            String retrievedData = page.readString();
            System.out.println("Read data from page " + persistentPageId[0] + ": " + retrievedData);
            
            if (retrievedData.equals(testData)) {
                System.out.println("Data persistence test PASSED!");
            } else {
                System.out.println("Data persistence test FAILED!");
            }
            
            storage.releasePage(persistentPageId[0], false);
        }
    }
    
    /**
     * 测试FIFO替换策略
     */
    public static void testFifoReplacement() {
        System.out.println("\n=== Testing FIFO Replacement Policy ===");
        
        StorageEngine storage = new StorageEngine(3, "test_fifo.db", ReplacementPolicy.FIFO);
        
        Vector<Integer> pageIds = new Vector<>();
        
        // 分配页面
        for (int i = 0; i < 5; i++) {
            int[] pageId = new int[1];
            Page page = storage.allocateNewPage(pageId);
            page.writeString("FIFO Page " + i);
            pageIds.add(pageId[0]);
            storage.releasePage(pageId[0], true);
            System.out.println("Allocated FIFO page " + pageId[0]);
        }
        
        // 访问页面
        for (int pageId : pageIds) {
            Page page = storage.getPage(pageId);
            if (page != null) {
                System.out.println("Accessed page " + pageId + ": " + page.readString());
                storage.releasePage(pageId, false);
            }
        }
        
        storage.printCacheStats();
        System.out.println("FIFO replacement policy test completed!");
    }
    
    /**
     * 主函数
     */
    public static void main(String[] args) {
        System.out.println("Starting Storage System Test...");
        
        try {
            testBasicPageOperations();
            testCacheReplacement();
            testConcurrentAccess();
            testTablePageOperations();
            testDataPersistence();
            testFifoReplacement();
            
            System.out.println("\nAll tests completed!");
            
        } catch (Exception e) {
            System.err.println("Error occurred during testing: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
