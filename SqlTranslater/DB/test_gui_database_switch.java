import com.database.MultiDatabaseManager;
import com.database.engine.DatabaseEngine;
import com.database.config.DatabaseConfig;
import java.util.Set;

public class test_gui_database_switch {
    public static void main(String[] args) {
        System.out.println("=== 测试GUI数据库切换问题 ===");
        
        try {
            // 1. 初始化多数据库管理器（模拟GUI初始化）
            String baseDataDirectory = "E:\\SQL实训\\data";
            MultiDatabaseManager databaseManager = new MultiDatabaseManager(baseDataDirectory);
            
            System.out.println("当前数据库: " + databaseManager.getCurrentDatabase());
            
            // 2. 检查main数据库的表
            DatabaseEngine mainEngine = databaseManager.getCurrentDatabaseEngine();
            if (mainEngine != null) {
                Set<String> mainTables = mainEngine.getCatalogManager().getAllTableNames();
                System.out.println("main数据库中的表: " + mainTables);
            } else {
                System.out.println("main数据库引擎为null");
            }
            
            // 3. 检查smxx数据库的表
            if (databaseManager.databaseExists("smxx")) {
                DatabaseEngine smxxEngine = databaseManager.getDatabaseEngine("smxx");
                if (smxxEngine != null) {
                    Set<String> smxxTables = smxxEngine.getCatalogManager().getAllTableNames();
                    System.out.println("smxx数据库中的表: " + smxxTables);
                } else {
                    System.out.println("smxx数据库引擎为null");
                }
            } else {
                System.out.println("smxx数据库不存在");
            }
            
            // 4. 切换到smxx数据库
            System.out.println("\n切换到smxx数据库...");
            databaseManager.switchDatabase("smxx");
            System.out.println("切换后当前数据库: " + databaseManager.getCurrentDatabase());
            
            // 5. 再次检查表
            DatabaseEngine currentEngine = databaseManager.getCurrentDatabaseEngine();
            if (currentEngine != null) {
                Set<String> currentTables = currentEngine.getCatalogManager().getAllTableNames();
                System.out.println("当前数据库中的表: " + currentTables);
            } else {
                System.out.println("当前数据库引擎为null");
            }
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
