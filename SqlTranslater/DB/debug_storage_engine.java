import com.database.engine.DatabaseEngine;
import com.database.engine.StorageEngine;
import java.lang.reflect.Field;
import java.util.Map;

public class debug_storage_engine {
    public static void main(String[] args) {
        try {
            System.out.println("调试StorageEngine...");
            
            // 创建数据库引擎
            DatabaseEngine engine = new DatabaseEngine("main", "data/main");
            System.out.println("数据库引擎创建成功");
            
            // 获取StorageEngine（通过反射）
            Field storageEngineField = DatabaseEngine.class.getDeclaredField("storageEngine");
            storageEngineField.setAccessible(true);
            StorageEngine storageEngine = (StorageEngine) storageEngineField.get(engine);
            System.out.println("获取StorageEngine成功");
            
            // 使用反射检查tableStorageMap
            try {
                Field tableStorageMapField = StorageEngine.class.getDeclaredField("tableStorageMap");
                tableStorageMapField.setAccessible(true);
                Map<?, ?> tableStorageMap = (Map<?, ?>) tableStorageMapField.get(storageEngine);
                
                System.out.println("StorageEngine中已注册的表数量: " + tableStorageMap.size());
                System.out.println("StorageEngine中已注册的表:");
                for (Object key : tableStorageMap.keySet()) {
                    System.out.println("  - " + key);
                }
                
                // 检查系统表是否已注册
                if (tableStorageMap.containsKey("__system_tables__")) {
                    System.out.println("__system_tables__ 已注册");
                } else {
                    System.out.println("__system_tables__ 未注册");
                }
                
                if (tableStorageMap.containsKey("__system_columns__")) {
                    System.out.println("__system_columns__ 已注册");
                } else {
                    System.out.println("__system_columns__ 未注册");
                }
                
                if (tableStorageMap.containsKey("__system_constraints__")) {
                    System.out.println("__system_constraints__ 已注册");
                } else {
                    System.out.println("__system_constraints__ 未注册");
                }
                
            } catch (Exception e) {
                System.err.println("反射访问失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("调试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
