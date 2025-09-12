public class SimpleTest {
    public static void main(String[] args) {
        System.out.println("=== SparrowDB 简单存储测试 ===");
        
        try {
            // 创建存储引擎 (缓冲池大小=10, 文件名="test.db", 策略=LRU)
            StorageEngine engine = new StorageEngine(10, "test.db", ReplacementPolicy.LRU);
            
            // 分配一个新页面
            int[] pageId = new int[1];
            Page page = engine.allocateNewPage(pageId);
            
            if (page != null) {
                System.out.println("分配新页面成功, 页面ID: " + pageId[0]);
                
                // 写入一些测试数据
                String testData = "Hello SparrowDB! This is test data.";
                page.writeString(testData);
                
                System.out.println("写入数据: " + testData);
                
                // 强制刷新到磁盘
                engine.flushAllPages();
                System.out.println("数据已刷新到磁盘");
                
                // 释放页面
                engine.releasePage(pageId[0], true);
                
                // 重新读取页面验证数据
                Page readPage = engine.getPage(pageId[0]);
                if (readPage != null) {
                    String readData = readPage.readString();
                    System.out.println("读取数据: " + readData);
                    
                    if (testData.equals(readData)) {
                        System.out.println("✓ 数据验证成功!");
                    } else {
                        System.out.println("✗ 数据验证失败!");
                    }
                    
                    engine.releasePage(pageId[0], false);
                } else {
                    System.out.println("重新读取页面失败");
                }
                
            } else {
                System.out.println("分配页面失败");
            }
            
            // 关闭引擎
            engine.close();
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
