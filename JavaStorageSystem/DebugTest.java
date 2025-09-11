public class DebugTest {
    public static void main(String[] args) {
        System.out.println("=== SparrowDB 调试测试 ===");
        
        try {
            // 创建存储引擎 (缓冲池大小=10, 文件名="debug.db", 策略=LRU)
            System.out.println("1. 创建存储引擎...");
            StorageEngine engine = new StorageEngine(10, "debug.db", ReplacementPolicy.LRU);
            
            // 分配一个新页面
            System.out.println("2. 分配新页面...");
            int[] pageId = new int[1];
            Page page = engine.allocateNewPage(pageId);
            
            if (page != null) {
                System.out.println("3. 分配新页面成功, 页面ID: " + pageId[0]);
                
                // 写入一些测试数据
                System.out.println("4. 写入测试数据...");
                String testData = "Hello SparrowDB! This is test data.";
                page.writeString(testData);
                
                System.out.println("5. 数据写入完成: " + testData);
                
                // 强制刷新到磁盘
                System.out.println("6. 刷新数据到磁盘...");
                engine.flushAllPages();
                System.out.println("7. 数据已刷新到磁盘");
                
                // 释放页面
                System.out.println("8. 释放页面...");
                engine.releasePage(pageId[0], true);
                System.out.println("9. 页面已释放");
                
                // 重新获取页面验证数据持久化
                System.out.println("10. 重新获取页面...");
                Page readPage = engine.getPage(pageId[0]);
                if (readPage != null) {
                    System.out.println("11. 页面获取完成");
                    System.out.println("12. 读取页面数据...");
                    String readData = readPage.readString();
                    System.out.println("13. 读取数据完成: " + readData);
                    
                    if (testData.equals(readData)) {
                        System.out.println("✓ 数据验证成功!");
                    } else {
                        System.out.println("✗ 数据验证失败!");
                        System.out.println("期望: " + testData);
                        System.out.println("实际: " + readData);
                    }
                    
                    System.out.println("14. 最终释放页面...");
                    engine.releasePage(pageId[0], false);
                    System.out.println("15. 页面释放完成");
                } else {
                    System.out.println("重新读取页面失败");
                }
                
            } else {
                System.out.println("分配页面失败");
            }
            
            // 关闭引擎
            System.out.println("16. 关闭存储引擎...");
            engine.close();
            System.out.println("17. 存储引擎关闭完成");
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== 测试完成 ===");
    }
}
