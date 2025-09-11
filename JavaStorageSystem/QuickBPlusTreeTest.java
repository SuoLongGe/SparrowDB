/**
 * Quick B+ Tree Index Test
 */
public class QuickBPlusTreeTest {
    public static void main(String[] args) {
        System.out.println("=== B+树索引快速测试 ===");
        
        // 清理之前的测试文件
        java.io.File testFile = new java.io.File("quick_test.db");
        if (testFile.exists()) {
            testFile.delete();
        }
        
        try {
            // 创建存储引擎
            StorageEngine engine = new StorageEngine(5, "quick_test.db", ReplacementPolicy.LRU);
            
            // 测试1: 创建索引
            System.out.println("\n--- 测试1: 创建索引 ---");
            boolean created = engine.createIntegerIndex("test_index", 5);
            System.out.println("创建整数索引: " + (created ? "成功" : "失败"));
            
            // 测试2: 插入数据
            System.out.println("\n--- 测试2: 插入数据 ---");
            int[] keys = {10, 20, 5, 15, 25, 30, 1, 8};
            for (int i = 0; i < keys.length; i++) {
                boolean inserted = engine.insertToIndex("test_index", keys[i], i + 100);
                System.out.println("插入键 " + keys[i] + " -> 页面 " + (i + 100) + ": " + (inserted ? "成功" : "失败"));
            }
            
            // 测试3: 查找数据
            System.out.println("\n--- 测试3: 查找数据 ---");
            for (int key : keys) {
                int pageId = engine.searchIndex("test_index", key);
                System.out.println("查找键 " + key + ": " + (pageId != -1 ? "找到页面 " + pageId : "未找到"));
            }
            
            // 测试4: 查找不存在的键
            System.out.println("\n--- 测试4: 查找不存在的键 ---");
            int notFound = engine.searchIndex("test_index", 999);
            System.out.println("查找键 999: " + (notFound == -1 ? "正确返回-1" : "错误"));
            
            // 测试5: 范围查询
            System.out.println("\n--- 测试5: 范围查询 ---");
            java.util.List<Integer> results = engine.rangeSearchIndex("test_index", 10, 25);
            System.out.println("范围查询 [10, 25] 结果: " + results);
            
            // 测试6: 索引信息
            System.out.println("\n--- 测试6: 索引信息 ---");
            engine.printIndexInfo("test_index");
            
            // 测试7: 索引结构
            System.out.println("\n--- 测试7: 索引结构 ---");
            engine.printIndexStructure("test_index");
            
            // 测试8: 字符串索引
            System.out.println("\n--- 测试8: 字符串索引 ---");
            engine.createStringIndex("string_index", 5);
            String[] strings = {"apple", "banana", "cherry"};
            for (int i = 0; i < strings.length; i++) {
                engine.insertToIndex("string_index", strings[i], i + 200);
                System.out.println("插入字符串键 " + strings[i] + " -> 页面 " + (i + 200));
            }
            
            for (String str : strings) {
                int pageId = engine.searchIndex("string_index", str);
                System.out.println("查找字符串键 " + str + ": " + (pageId != -1 ? "找到页面 " + pageId : "未找到"));
            }
            
            // 刷新所有页面
            engine.flushAllPages();
            
            System.out.println("\n=== 测试完成 ===");
            
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
}
