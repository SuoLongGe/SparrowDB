import com.database.engine.Executor;

public class TestSystemTableFix {
    public static void main(String[] args) {
        try {
            System.out.println("🔍 测试系统函数表修复");
            System.out.println("创建数据库引擎...");
            
            Executor executor = new Executor("SystemTableTest");
            System.out.println("数据库引擎初始化成功！");
            
            System.out.println("\n📋 尝试查询系统函数表:");
            String result = executor.execute("SELECT * FROM __system_functions__");
            System.out.println("✅ 查询成功！");
            System.out.println("查询结果: " + result);
            
        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
