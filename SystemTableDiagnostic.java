import com.database.engine.Executor;
import com.database.engine.StorageAdapter;

public class SystemTableDiagnostic {
    public static void main(String[] args) {
        try {
            System.out.println("🔍 系统表诊断");
            System.out.println("===============");
            
            // 创建引擎
            Executor executor = new Executor("SystemTableTest");
            
            System.out.println("\n📋 检查所有已注册的表:");
            StorageAdapter storageAdapter = executor.getStorageAdapter();
            
            // 检查系统函数表是否存在
            boolean systemFunctionsExists = storageAdapter.tableExists("__system_functions__");
            System.out.println("__system_functions__ 表存在: " + systemFunctionsExists);
            
            // 尝试列出所有表
            System.out.println("\n📊 尝试查询 SHOW TABLES:");
            String showTablesResult = executor.execute("SHOW TABLES");
            System.out.println("结果: " + showTablesResult);
            
            // 直接尝试查询系统函数表
            System.out.println("\n🔍 尝试查询 __system_functions__:");
            String result = executor.execute("SELECT * FROM __system_functions__");
            System.out.println("结果: " + result);
            
        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
