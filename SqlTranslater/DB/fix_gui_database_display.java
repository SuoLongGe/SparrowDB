import com.database.MultiDatabaseManager;
import com.database.engine.DatabaseEngine;
import java.util.Set;

public class fix_gui_database_display {
    public static void main(String[] args) {
        System.out.println("=== 修复GUI数据库显示问题 ===");
        
        try {
            // 1. 初始化多数据库管理器
            String baseDataDirectory = "E:\\SQL实训\\data";
            MultiDatabaseManager databaseManager = new MultiDatabaseManager(baseDataDirectory);
            
            System.out.println("当前数据库: " + databaseManager.getCurrentDatabase());
            
            // 2. 检查所有数据库，找到有用户表的数据库
            Set<String> databaseNames = databaseManager.getAllDatabaseNames();
            String databaseWithUserTables = null;
            
            for (String dbName : databaseNames) {
                try {
                    DatabaseEngine engine = databaseManager.getDatabaseEngine(dbName);
                    if (engine != null) {
                        Set<String> tables = engine.getCatalogManager().getAllTableNames();
                        // 过滤掉系统表，检查是否有用户表
                        long userTableCount = tables.stream()
                            .filter(tableName -> !tableName.startsWith("__system_"))
                            .count();
                        
                        System.out.println("数据库 " + dbName + " 有 " + userTableCount + " 个用户表");
                        
                        if (userTableCount > 0 && databaseWithUserTables == null) {
                            databaseWithUserTables = dbName;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("检查数据库 " + dbName + " 失败: " + e.getMessage());
                }
            }
            
            // 3. 如果有用户表的数据库，切换到该数据库
            if (databaseWithUserTables != null) {
                System.out.println("\n发现用户表在数据库: " + databaseWithUserTables);
                
                if (databaseManager.useDatabase(databaseWithUserTables)) {
                    System.out.println("成功切换到数据库: " + databaseWithUserTables);
                    
                    // 4. 验证切换后的表列表
                    DatabaseEngine currentEngine = databaseManager.getCurrentDatabaseEngine();
                    if (currentEngine != null) {
                        Set<String> currentTables = currentEngine.getCatalogManager().getAllTableNames();
                        System.out.println("当前数据库中的表: " + currentTables);
                        
                        // 过滤用户表
                        Set<String> userTables = currentTables.stream()
                            .filter(tableName -> !tableName.startsWith("__system_"))
                            .collect(java.util.stream.Collectors.toSet());
                        System.out.println("用户表: " + userTables);
                    }
                } else {
                    System.out.println("切换数据库失败");
                }
            } else {
                System.out.println("没有找到包含用户表的数据库");
            }
            
        } catch (Exception e) {
            System.err.println("修复失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
