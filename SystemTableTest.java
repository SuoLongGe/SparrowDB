import com.database.engine.Executor;

public class SystemTableTest {
    public static void main(String[] args) {
        try {
            System.out.println("🔍 测试系统函数表访问");
            Executor executor = new Executor("SystemTest");
            
            System.out.println("查询 __system_functions__ 表:");
            String result = executor.execute("SELECT * FROM __system_functions__");
            System.out.println("查询结果: " + result);
            
        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
        }
    }
}
