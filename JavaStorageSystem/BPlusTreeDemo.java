import java.util.List;
import java.util.Scanner;

/**
 * B+树索引演示程序
 */
public class BPlusTreeDemo {
    private static StorageEngine engine;
    private static Scanner scanner;
    
    public static void main(String[] args) {
        System.out.println("=== B+树索引演示程序 ===");
        
        // 初始化存储引擎
        engine = new StorageEngine(10, "demo_bplus.db", ReplacementPolicy.LRU);
        scanner = new Scanner(System.in);
        
        try {
            showMainMenu();
        } catch (Exception e) {
            System.err.println("程序运行出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
            // 清理演示文件
            java.io.File demoFile = new java.io.File("demo_bplus.db");
            if (demoFile.exists()) {
                demoFile.delete();
            }
        }
    }
    
    private static void showMainMenu() {
        while (true) {
            System.out.println("\n=== 主菜单 ===");
            System.out.println("1. 创建索引");
            System.out.println("2. 插入数据");
            System.out.println("3. 查找数据");
            System.out.println("4. 范围查询");
            System.out.println("5. 删除数据");
            System.out.println("6. 显示索引信息");
            System.out.println("7. 显示索引结构");
            System.out.println("8. 批量操作演示");
            System.out.println("9. 性能测试");
            System.out.println("0. 退出");
            System.out.print("请选择操作 (0-9): ");
            
            int choice = getIntInput();
            switch (choice) {
                case 1:
                    createIndex();
                    break;
                case 2:
                    insertData();
                    break;
                case 3:
                    searchData();
                    break;
                case 4:
                    rangeSearch();
                    break;
                case 5:
                    deleteData();
                    break;
                case 6:
                    showIndexInfo();
                    break;
                case 7:
                    showIndexStructure();
                    break;
                case 8:
                    batchOperationsDemo();
                    break;
                case 9:
                    performanceTest();
                    break;
                case 0:
                    System.out.println("感谢使用B+树索引演示程序！");
                    return;
                default:
                    System.out.println("无效选择，请重新输入");
            }
        }
    }
    
    private static void createIndex() {
        System.out.println("\n--- 创建索引 ---");
        System.out.print("请输入索引名称: ");
        String indexName = scanner.nextLine();
        
        System.out.println("选择键类型:");
        System.out.println("1. 整数");
        System.out.println("2. 字符串");
        System.out.print("请选择 (1-2): ");
        
        int typeChoice = getIntInput();
        System.out.print("请输入最大键值数量 (建议5-10): ");
        int maxKeys = getIntInput();
        
        boolean success = false;
        if (typeChoice == 1) {
            success = engine.createIntegerIndex(indexName, maxKeys);
        } else if (typeChoice == 2) {
            success = engine.createStringIndex(indexName, maxKeys);
        } else {
            System.out.println("无效的键类型选择");
            return;
        }
        
        System.out.println("创建索引: " + (success ? "成功" : "失败"));
    }
    
    private static void insertData() {
        System.out.println("\n--- 插入数据 ---");
        System.out.print("请输入索引名称: ");
        String indexName = scanner.nextLine();
        
        if (!engine.hasIndex(indexName)) {
            System.out.println("索引不存在: " + indexName);
            return;
        }
        
        System.out.print("请输入键值: ");
        String keyStr = scanner.nextLine();
        System.out.print("请输入记录页面ID: ");
        int pageId = getIntInput();
        
        boolean success = engine.insertToIndex(indexName, keyStr, pageId);
        System.out.println("插入数据: " + (success ? "成功" : "失败"));
    }
    
    private static void searchData() {
        System.out.println("\n--- 查找数据 ---");
        System.out.print("请输入索引名称: ");
        String indexName = scanner.nextLine();
        
        if (!engine.hasIndex(indexName)) {
            System.out.println("索引不存在: " + indexName);
            return;
        }
        
        System.out.print("请输入要查找的键值: ");
        String keyStr = scanner.nextLine();
        
        int pageId = engine.searchIndex(indexName, keyStr);
        if (pageId != -1) {
            System.out.println("找到数据，页面ID: " + pageId);
        } else {
            System.out.println("未找到数据");
        }
    }
    
    private static void rangeSearch() {
        System.out.println("\n--- 范围查询 ---");
        System.out.print("请输入索引名称: ");
        String indexName = scanner.nextLine();
        
        if (!engine.hasIndex(indexName)) {
            System.out.println("索引不存在: " + indexName);
            return;
        }
        
        System.out.print("请输入起始键值: ");
        String startKey = scanner.nextLine();
        System.out.print("请输入结束键值: ");
        String endKey = scanner.nextLine();
        
        List<Integer> results = engine.rangeSearchIndex(indexName, startKey, endKey);
        System.out.println("找到 " + results.size() + " 个结果:");
        for (int i = 0; i < results.size(); i++) {
            System.out.print(results.get(i));
            if (i < results.size() - 1) {
                System.out.print(", ");
            }
        }
        System.out.println();
    }
    
    private static void deleteData() {
        System.out.println("\n--- 删除数据 ---");
        System.out.print("请输入索引名称: ");
        String indexName = scanner.nextLine();
        
        if (!engine.hasIndex(indexName)) {
            System.out.println("索引不存在: " + indexName);
            return;
        }
        
        System.out.print("请输入要删除的键值: ");
        String keyStr = scanner.nextLine();
        
        boolean success = engine.deleteFromIndex(indexName, keyStr);
        System.out.println("删除数据: " + (success ? "成功" : "失败"));
    }
    
    private static void showIndexInfo() {
        System.out.println("\n--- 索引信息 ---");
        engine.printAllIndexes();
    }
    
    private static void showIndexStructure() {
        System.out.println("\n--- 索引结构 ---");
        System.out.print("请输入索引名称: ");
        String indexName = scanner.nextLine();
        
        if (!engine.hasIndex(indexName)) {
            System.out.println("索引不存在: " + indexName);
            return;
        }
        
        engine.printIndexStructure(indexName);
    }
    
    private static void batchOperationsDemo() {
        System.out.println("\n--- 批量操作演示 ---");
        
        // 创建演示索引
        String indexName = "demo_index";
        System.out.println("创建演示索引: " + indexName);
        engine.createIntegerIndex(indexName, 5);
        
        // 插入演示数据
        System.out.println("插入演示数据...");
        int[] demoKeys = {10, 20, 5, 15, 25, 30, 1, 8, 12, 18, 22, 28, 3, 7, 14, 16, 24, 26, 2, 9};
        for (int i = 0; i < demoKeys.length; i++) {
            engine.insertToIndex(indexName, demoKeys[i], i + 1000);
        }
        
        System.out.println("插入完成，共插入 " + demoKeys.length + " 个数据");
        
        // 显示索引信息
        engine.printIndexInfo(indexName);
        
        // 显示索引结构
        System.out.println("\n索引结构:");
        engine.printIndexStructure(indexName);
        
        // 测试查找
        System.out.println("\n测试查找:");
        for (int key : demoKeys) {
            int pageId = engine.searchIndex(indexName, key);
            System.out.println("键 " + key + ": " + (pageId != -1 ? "找到页面 " + pageId : "未找到"));
        }
        
        // 测试范围查询
        System.out.println("\n范围查询 [10, 20]:");
        List<Integer> results = engine.rangeSearchIndex(indexName, 10, 20);
        System.out.println("找到 " + results.size() + " 个结果: " + results);
    }
    
    private static void performanceTest() {
        System.out.println("\n--- 性能测试 ---");
        
        String indexName = "perf_test_index";
        System.out.println("创建性能测试索引: " + indexName);
        engine.createIntegerIndex(indexName, 10);
        
        // 测试插入性能
        int testSize = 500;
        System.out.println("测试插入 " + testSize + " 个随机数据的性能...");
        
        long startTime = System.currentTimeMillis();
        java.util.Random random = new java.util.Random(42);
        
        int successCount = 0;
        for (int i = 0; i < testSize; i++) {
            int key = random.nextInt(1000);
            if (engine.insertToIndex(indexName, key, i + 2000)) {
                successCount++;
            }
        }
        
        long insertTime = System.currentTimeMillis() - startTime;
        System.out.println("插入完成: 成功 " + successCount + "/" + testSize + 
                          ", 耗时 " + insertTime + "ms");
        
        // 测试查找性能
        System.out.println("测试查找性能...");
        startTime = System.currentTimeMillis();
        
        int foundCount = 0;
        for (int i = 0; i < testSize; i++) {
            int key = random.nextInt(1000);
            if (engine.searchIndex(indexName, key) != -1) {
                foundCount++;
            }
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        System.out.println("查找完成: 找到 " + foundCount + "/" + testSize + 
                          ", 耗时 " + searchTime + "ms");
        
        // 显示性能统计
        System.out.println("\n性能统计:");
        engine.printCacheStats();
        engine.printIndexInfo(indexName);
    }
    
    private static int getIntInput() {
        while (true) {
            try {
                String input = scanner.nextLine();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.print("请输入有效的整数: ");
            }
        }
    }
}
