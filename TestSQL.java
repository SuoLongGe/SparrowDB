import com.sqlcompiler.SQLCompiler;

public class TestSQL {
    public static void main(String[] args) {
        System.out.println("=== SQL编译器功能测试 ===");
        
        try {
            SQLCompiler compiler = new SQLCompiler();
            
            // 测试1: CREATE TABLE
            System.out.println("\n1. 测试CREATE TABLE语句:");
            String createSQL = "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50), age INT);";
            System.out.println("SQL: " + createSQL);
            
            var result1 = compiler.compile(createSQL);
            System.out.println("编译结果: " + (result1.isSuccess() ? "成功" : "失败"));
            if (!result1.isSuccess()) {
                System.out.println("错误: " + result1.getErrorMessage());
            }
            
            // 测试2: INSERT
            System.out.println("\n2. 测试INSERT语句:");
            String insertSQL = "INSERT INTO users VALUES (1, 'Alice', 25);";
            System.out.println("SQL: " + insertSQL);
            
            var result2 = compiler.compile(insertSQL);
            System.out.println("编译结果: " + (result2.isSuccess() ? "成功" : "失败"));
            if (!result2.isSuccess()) {
                System.out.println("错误: " + result2.getErrorMessage());
            }
            
            // 测试3: SELECT
            System.out.println("\n3. 测试SELECT语句:");
            String selectSQL = "SELECT * FROM users;";
            System.out.println("SQL: " + selectSQL);
            
            var result3 = compiler.compile(selectSQL);
            System.out.println("编译结果: " + (result3.isSuccess() ? "成功" : "失败"));
            if (!result3.isSuccess()) {
                System.out.println("错误: " + result3.getErrorMessage());
            } else {
                System.out.println("查询结果: " + result3.getExecutionResult());
            }
            
        } catch (Exception e) {
            System.out.println("测试异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n测试完成!");
    }
}
