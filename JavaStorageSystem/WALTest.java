/**
 * WAL (Write-Ahead Logging) 测试程序
 * 演示WAL机制的功能和优势
 */
public class WALTest {
    
    public static void main(String[] args) {
        System.out.println("=== WAL (Write-Ahead Logging) 测试 ===");
        
        // 创建带WAL支持的存储引擎
        StorageEngine engine = new StorageEngine(10, "wal_test.db", ReplacementPolicy.LRU);
        
        try {
            // 测试1: 基本WAL功能
            testBasicWALFunctionality(engine);
            
            // 测试2: 事务支持
            testTransactionSupport(engine);
            
            // 测试3: 故障恢复
            testCrashRecovery(engine);
            
            // 测试4: 检查点机制
            testCheckpointMechanism(engine);
            
            // 显示WAL统计信息
            engine.printWALStats();
            
        } finally {
            // 关闭存储引擎
            engine.close();
        }
        
        System.out.println("\n=== WAL测试完成 ===");
    }
    
    /**
     * 测试基本WAL功能
     */
    private static void testBasicWALFunctionality(StorageEngine engine) {
        System.out.println("\n=== 测试1: 基本WAL功能 ===");
        
        try {
            // 开始事务
            long transactionId = engine.beginTransaction();
            System.out.println("开始事务: " + transactionId);
            
            // 分配页面
            int[] pageId = new int[1];
            Page page = engine.allocateNewPage(pageId);
            System.out.println("分配页面: " + pageId[0]);
            
            // 使用WAL写入数据
            String testData = "WAL测试数据: " + System.currentTimeMillis();
            boolean success = engine.writeRecordWithWAL(transactionId, pageId[0], testData);
            System.out.println("WAL写入数据: " + (success ? "成功" : "失败"));
            
            // 提交事务
            boolean committed = engine.commitTransaction(transactionId);
            System.out.println("提交事务: " + (committed ? "成功" : "失败"));
            
            // 验证数据
            Page readPage = engine.getPage(pageId[0]);
            if (readPage != null) {
                String readData = readPage.readString();
                System.out.println("读取数据: " + readData);
                System.out.println("数据一致性: " + (testData.equals(readData) ? "正确" : "错误"));
                engine.releasePage(pageId[0], false);
            }
            
        } catch (Exception e) {
            System.err.println("基本WAL功能测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试事务支持
     */
    private static void testTransactionSupport(StorageEngine engine) {
        System.out.println("\n=== 测试2: 事务支持 ===");
        
        try {
            // 测试成功事务
            System.out.println("测试成功事务...");
            long successTxnId = engine.beginTransaction();
            
            int[] pageId = new int[1];
            Page page = engine.allocateNewPage(pageId);
            
            // 写入多条记录
            for (int i = 0; i < 3; i++) {
                String data = "事务记录 " + i + " - 时间戳: " + System.currentTimeMillis();
                engine.writeRecordWithWAL(successTxnId, pageId[0], data);
            }
            
            engine.commitTransaction(successTxnId);
            System.out.println("成功事务提交完成");
            
            // 测试回滚事务
            System.out.println("测试回滚事务...");
            long abortTxnId = engine.beginTransaction();
            
            int[] abortPageId = new int[1];
            Page abortPage = engine.allocateNewPage(abortPageId);
            
            // 写入数据
            String abortData = "这个数据应该被回滚";
            engine.writeRecordWithWAL(abortTxnId, abortPageId[0], abortData);
            
            // 回滚事务
            engine.abortTransaction(abortTxnId);
            System.out.println("回滚事务完成");
            
        } catch (Exception e) {
            System.err.println("事务支持测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试故障恢复
     */
    private static void testCrashRecovery(StorageEngine engine) {
        System.out.println("\n=== 测试3: 故障恢复 ===");
        
        try {
            // 模拟故障前的操作
            long transactionId = engine.beginTransaction();
            System.out.println("故障前事务: " + transactionId);
            
            int[] pageId = new int[1];
            Page page = engine.allocateNewPage(pageId);
            
            // 写入数据但不提交
            String crashData = "故障恢复测试数据";
            engine.writeRecordWithWAL(transactionId, pageId[0], crashData);
            System.out.println("写入数据但未提交");
            
            // 创建检查点
            engine.createCheckpoint();
            System.out.println("创建检查点");
            
            // 模拟故障恢复 - 重新创建存储引擎
            System.out.println("模拟故障恢复...");
            engine.close();
            
            // 重新打开存储引擎
            StorageEngine recoveryEngine = new StorageEngine(10, "wal_test.db", ReplacementPolicy.LRU);
            System.out.println("存储引擎恢复完成");
            
            // 验证数据是否恢复
            Page recoveryPage = recoveryEngine.getPage(pageId[0]);
            if (recoveryPage != null) {
                String recoveryData = recoveryPage.readString();
                System.out.println("恢复的数据: " + recoveryData);
                System.out.println("故障恢复: " + (crashData.equals(recoveryData) ? "成功" : "失败"));
                recoveryEngine.releasePage(pageId[0], false);
            }
            
            recoveryEngine.close();
            
        } catch (Exception e) {
            System.err.println("故障恢复测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试检查点机制
     */
    private static void testCheckpointMechanism(StorageEngine engine) {
        System.out.println("\n=== 测试4: 检查点机制 ===");
        
        try {
            // 执行多个事务
            for (int i = 0; i < 5; i++) {
                long transactionId = engine.beginTransaction();
                
                int[] pageId = new int[1];
                Page page = engine.allocateNewPage(pageId);
                
                String data = "检查点测试数据 " + i;
                engine.writeRecordWithWAL(transactionId, pageId[0], data);
                
                engine.commitTransaction(transactionId);
                System.out.println("完成事务 " + i);
            }
            
            // 创建检查点
            engine.createCheckpoint();
            System.out.println("检查点创建完成");
            
            // 清理WAL
            engine.cleanupWAL();
            System.out.println("WAL清理完成");
            
        } catch (Exception e) {
            System.err.println("检查点机制测试失败: " + e.getMessage());
        }
    }
}
