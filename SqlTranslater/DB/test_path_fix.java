import com.database.MultiDatabaseManager;
import com.database.engine.DatabaseEngine;
import java.io.File;
import java.util.Set;

public class test_path_fix {
    public static void main(String[] args) {
        System.out.println("=== 测试数据库路径修复 ===");
        
        try {
            // 1. 初始化多数据库管理器
            String baseDataDirectory = "E:\\SQL实训\\data";
            MultiDatabaseManager databaseManager = new MultiDatabaseManager(baseDataDirectory);
            
            System.out.println("基础数据目录: " + baseDataDirectory);
            
            // 2. 检查smxx数据库
            if (databaseManager.databaseExists("smxx")) {
                System.out.println("\n检查smxx数据库...");
                
                // 检查正确的路径
                String correctPath = baseDataDirectory + File.separator + "smxx";
                File correctDir = new File(correctPath);
                System.out.println("正确路径: " + correctPath);
                System.out.println("正确路径存在: " + correctDir.exists());
                
                // 检查错误的嵌套路径
                String wrongPath = correctPath + File.separator + "smxx";
                File wrongDir = new File(wrongPath);
                System.out.println("错误嵌套路径: " + wrongPath);
                System.out.println("错误嵌套路径存在: " + wrongDir.exists());
                
                // 3. 加载数据库引擎
                DatabaseEngine engine = databaseManager.getDatabaseEngine("smxx");
                if (engine != null) {
                    System.out.println("\n数据库引擎加载成功");
                    System.out.println("数据库引擎加载成功");
                    
                    // 4. 检查表
                    Set<String> tables = engine.getCatalogManager().getAllTableNames();
                    System.out.println("表数量: " + tables.size());
                    System.out.println("表列表: " + tables);
                    
                    // 5. 检查demo_products表的数据
                    if (tables.contains("demo_products")) {
                        System.out.println("\n检查demo_products表...");
                        var records = engine.getStorageAdapter().scanTable("demo_products");
                        System.out.println("demo_products表记录数: " + records.size());
                        if (!records.isEmpty()) {
                            System.out.println("第一条记录: " + records.get(0));
                        }
                    }
                } else {
                    System.out.println("数据库引擎加载失败");
                }
            } else {
                System.out.println("smxx数据库不存在");
            }
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
