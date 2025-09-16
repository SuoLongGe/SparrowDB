import com.database.MultiDatabaseManager;
import com.database.engine.DatabaseEngine;
import java.io.File;
import java.util.Set;

public class test_database_discovery {
    public static void main(String[] args) {
        System.out.println("=== 测试数据库发现功能 ===");
        
        try {
            // 1. 检查数据目录
            String baseDataDirectory = "E:\\SQL实训\\data";
            File baseDir = new File(baseDataDirectory);
            System.out.println("数据目录存在: " + baseDir.exists());
            System.out.println("数据目录路径: " + baseDir.getAbsolutePath());
            
            if (baseDir.exists()) {
                File[] subdirs = baseDir.listFiles(File::isDirectory);
                System.out.println("子目录数量: " + (subdirs != null ? subdirs.length : 0));
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        String dbName = subdir.getName();
                        System.out.println("发现目录: " + dbName + " (路径: " + subdir.getAbsolutePath() + ")");
                    }
                }
            }
            
            // 2. 初始化多数据库管理器
            MultiDatabaseManager databaseManager = new MultiDatabaseManager(baseDataDirectory);
            
            // 3. 获取所有数据库名
            Set<String> databaseNames = databaseManager.getAllDatabaseNames();
            System.out.println("\n发现的数据库: " + databaseNames);
            
            // 4. 检查每个数据库的表
            for (String dbName : databaseNames) {
                System.out.println("\n检查数据库: " + dbName);
                try {
                    DatabaseEngine engine = databaseManager.getDatabaseEngine(dbName);
                    if (engine != null) {
                        Set<String> tables = engine.getCatalogManager().getAllTableNames();
                        System.out.println("  表数量: " + tables.size());
                        System.out.println("  表列表: " + tables);
                    } else {
                        System.out.println("  数据库引擎为null");
                    }
                } catch (Exception e) {
                    System.out.println("  加载数据库失败: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
