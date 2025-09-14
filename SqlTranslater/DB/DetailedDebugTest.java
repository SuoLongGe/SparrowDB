import com.database.engine.StorageAdapter;
import java.util.HashMap;
import java.util.Map;

/**
 * 详细调试删除记录问题
 */
public class DetailedDebugTest {
    public static void main(String[] args) {
        System.out.println("🔧 详细调试删除记录问题");
        System.out.println("=======================================");
        
        try {
            StorageAdapter adapter = new StorageAdapter("data");
            
            System.out.println("\n📝 测试1: 查询现有记录");
            var records = adapter.scanTable("__system_functions__");
            if (records != null && !records.isEmpty()) {
                System.out.println("找到 " + records.size() + " 条记录:");
                for (int i = 0; i < records.size(); i++) {
                    Map<String, Object> record = records.get(i);
                    System.out.println("记录 " + (i+1) + ":");
                    for (Map.Entry<String, Object> entry : record.entrySet()) {
                        System.out.println("  " + entry.getKey() + " = " + entry.getValue() + " (" + entry.getValue().getClass().getSimpleName() + ")");
                    }
                }
                
                System.out.println("\n📝 测试2: 尝试精确匹配删除第一条记录");
                Map<String, Object> firstRecord = records.get(0);
                
                // 创建完全匹配的删除条件
                Map<String, Object> deleteCondition = new HashMap<>();
                for (Map.Entry<String, Object> entry : firstRecord.entrySet()) {
                    deleteCondition.put(entry.getKey(), entry.getValue());
                }
                
                System.out.println("删除条件:");
                for (Map.Entry<String, Object> entry : deleteCondition.entrySet()) {
                    System.out.println("  " + entry.getKey() + " = " + entry.getValue() + " (" + entry.getValue().getClass().getSimpleName() + ")");
                }
                
                boolean success = adapter.deleteRecord("__system_functions__", deleteCondition);
                System.out.println("删除结果: " + success);
                
                System.out.println("\n📝 测试3: 验证删除效果");
                var afterRecords = adapter.scanTable("__system_functions__");
                if (afterRecords != null) {
                    System.out.println("删除后记录数: " + afterRecords.size());
                } else {
                    System.out.println("删除后查询失败");
                }
                
                System.out.println("\n📝 测试4: 尝试只用function_name删除");
                if (afterRecords != null && !afterRecords.isEmpty()) {
                    Map<String, Object> simpleCondition = new HashMap<>();
                    simpleCondition.put("function_name", afterRecords.get(0).get("function_name"));
                    
                    System.out.println("简单删除条件: function_name = " + simpleCondition.get("function_name"));
                    boolean simpleSuccess = adapter.deleteRecord("__system_functions__", simpleCondition);
                    System.out.println("简单删除结果: " + simpleSuccess);
                }
                
            } else {
                System.out.println("没有找到记录");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
