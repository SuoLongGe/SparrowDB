import com.database.engine.DatabaseEngine;
import com.database.engine.StorageAdapter;
import java.util.List;
import java.util.Map;

public class test_scan_columns {
    public static void main(String[] args) {
        try {
            System.out.println("=== 测试扫描系统列表 ===");
            
            // 1. 初始化数据库引擎
            DatabaseEngine databaseEngine = new DatabaseEngine("smxx", "E:\\SQL实训\\data");
            StorageAdapter storageAdapter = databaseEngine.getStorageAdapter();
            
            // 2. 扫描系统列表
            System.out.println("\n1. 扫描 __system_columns__.tbl...");
            List<Map<String, Object>> records = storageAdapter.scanTable("__system_columns__");
            System.out.println("找到 " + records.size() + " 条记录");
            
            // 3. 查找 demo_products 的列记录
            System.out.println("\n2. 查找 demo_products 的列记录...");
            int demoProductsCount = 0;
            for (Map<String, Object> record : records) {
                String tableName = (String) record.get("table_name");
                if ("demo_products".equals(tableName)) {
                    demoProductsCount++;
                    System.out.println("找到 demo_products 列: " + record.get("column_name") + " (" + record.get("data_type") + ")");
                }
            }
            System.out.println("demo_products 列记录总数: " + demoProductsCount);
            
            // 4. 显示所有表名
            System.out.println("\n3. 所有表名:");
            for (Map<String, Object> record : records) {
                String tableName = (String) record.get("table_name");
                if (tableName != null && !tableName.startsWith("__system_")) {
                    System.out.println("  - " + tableName);
                }
            }
            
        } catch (Exception e) {
            System.err.println("测试过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
