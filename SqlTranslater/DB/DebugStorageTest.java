import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import com.database.engine.StorageAdapter;
import java.util.HashMap;
import java.util.Map;

/**
 * 调试存储系统测试
 */
public class DebugStorageTest {
    public static void main(String[] args) {
        System.out.println("🔧 存储系统调试测试");
        System.out.println("=======================================");
        
        try {
            // 直接测试StorageAdapter
            StorageAdapter adapter = new StorageAdapter("data");
            
            System.out.println("\n📝 测试1: 检查系统函数表是否存在");
            boolean exists = adapter.tableExists("__system_functions__");
            System.out.println("系统函数表存在: " + exists);
            
            System.out.println("\n📝 测试2: 直接插入记录到系统函数表");
            Map<String, Object> record = new HashMap<>();
            record.put("function_name", "debug_function");
            record.put("signature", "INT,INT");
            record.put("return_type", "INT");
            record.put("body", "RETURN a + b");
            record.put("is_permanent", true);
            record.put("create_time", System.currentTimeMillis());
            
            boolean success = adapter.insertRecord("__system_functions__", record);
            System.out.println("插入记录结果: " + success);
            
            System.out.println("\n📝 测试3: 查询系统函数表");
            var records = adapter.scanTable("__system_functions__");
            if (records != null) {
                System.out.println("查询到记录数: " + records.size());
                for (var r : records) {
                    System.out.println("记录: " + r);
                }
            } else {
                System.out.println("查询失败或表为空");
            }
            
            System.out.println("\n📝 测试4: 删除记录");
            Map<String, Object> condition = new HashMap<>();
            condition.put("function_name", "debug_function");
            boolean deleteSuccess = adapter.deleteRecord("__system_functions__", condition);
            System.out.println("删除记录结果: " + deleteSuccess);
            
            System.out.println("\n📝 测试5: 再次查询验证删除");
            records = adapter.scanTable("__system_functions__");
            if (records != null) {
                System.out.println("删除后记录数: " + records.size());
            } else {
                System.out.println("删除后查询失败或表为空");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
