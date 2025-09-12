import java.io.*;
import java.util.*;

public class SQLTestRunner {
    private List<String> successfulQueries = new ArrayList<>();
    private List<String> failedQueries = new ArrayList<>();
    private List<String> errorMessages = new ArrayList<>();

    public static void main(String[] args) {
        SQLTestRunner runner = new SQLTestRunner();
        runner.runAllTests();
    }

    public void runAllTests() {
        System.out.println("\n=== SparrowDB 自定义SQL语句后台测试 ===\n");
        
        // 定义所有测试SQL语句
        String[] testQueries = {
            // 基本SELECT查询
            "SELECT * FROM users",
            "SELECT * FROM products", 
            "SELECT name, email FROM users",
            "SELECT id, name, price FROM products",
            
            // CREATE TABLE语句
            "CREATE TABLE test_employees (id INT PRIMARY KEY, name VARCHAR(50), department VARCHAR(30), salary DECIMAL(10))",
            "CREATE TABLE test_orders (order_id INT PRIMARY KEY, user_id INT, product_id INT, quantity INT, order_date VARCHAR(20))",
            
            // INSERT INTO语句
            "INSERT INTO test_employees VALUES (1, 'John Doe', 'Engineering', 75000.00)",
            "INSERT INTO test_employees VALUES (2, 'Jane Smith', 'Marketing', 65000.00)",
            "INSERT INTO test_employees VALUES (3, 'Mike Johnson', 'Sales', 55000.00)",
            "INSERT INTO test_orders VALUES (101, 1, 1, 2, '2024-01-15')",
            "INSERT INTO test_orders VALUES (102, 2, 3, 1, '2024-01-16')",
            "INSERT INTO test_orders VALUES (103, 3, 2, 3, '2024-01-17')",
            
            // DELETE FROM语句
            "DELETE FROM test_employees WHERE salary < 60000",
            "DELETE FROM test_orders WHERE quantity > 2",
            
            // 复杂查询和WHERE条件
            "SELECT * FROM users WHERE age > 30",
            "SELECT * FROM products WHERE price < 50.00",
            "SELECT name FROM users WHERE email LIKE '%example.com'",
            
            // 错误处理测试
            "INSERT INTO non_existing_table VALUES (1, 'test')",
            "INSERT INTO users VALUES (1, 'Too Few Columns')",
            "SELECT * FROM another_non_existing_table",
            "CREATE TABLE users (duplicate_table VARCHAR(50))",
            "DELETE FROM non_existing_table WHERE id = 1"
        };

        // 执行所有测试
        for (String sql : testQueries) {
            executeSQL(sql);
        }
        
        // 输出测试结果
        printTestResults();
    }

    private void executeSQL(String sql) {
        try {
            System.out.println("执行: " + sql);
            
            // 模拟通过命令行执行数据库程序
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", "target/classes", "com.database.SparrowDBApplication");
            pb.directory(new File("."));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // 发送SQL命令
            PrintWriter writer = new PrintWriter(process.getOutputStream());
            writer.println("sparrow_db"); // 数据库名
            writer.println("./data");     // 数据目录
            writer.println(sql);          // SQL语句
            writer.println("quit");       // 退出
            writer.flush();
            writer.close();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            boolean foundResult = false;
            
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\\n");
                if (line.contains("SQL执行成功") || line.contains("执行成功")) {
                    foundResult = true;
                }
            }
            
            process.waitFor();
            
            if (foundResult || output.toString().contains("✓")) {
                successfulQueries.add(sql);
                System.out.println("✅ 成功");
            } else {
                failedQueries.add(sql);
                errorMessages.add("未找到成功标志");
                System.out.println("❌ 失败: 未找到成功标志");
            }
            
        } catch (Exception e) {
            failedQueries.add(sql);
            errorMessages.add(e.getMessage());
            System.out.println("❌ 失败: " + e.getMessage());
        }
        System.out.println();
    }

    private void printTestResults() {
        System.out.println("\\n" + "=".repeat(60));
        System.out.println("📋 测试结果汇总");
        System.out.println("=".repeat(60));
        
        System.out.println("✅ 成功执行的SQL语句 (" + successfulQueries.size() + "条):");
        for (int i = 0; i < successfulQueries.size(); i++) {
            System.out.println("  " + (i+1) + ". " + successfulQueries.get(i));
        }
        
        System.out.println("\\n❌ 执行失败的SQL语句 (" + failedQueries.size() + "条):");
        for (int i = 0; i < failedQueries.size(); i++) {
            System.out.println("  " + (i+1) + ". " + failedQueries.get(i));
            System.out.println("     错误: " + errorMessages.get(i));
        }
        
        System.out.println("\\n📊 总体统计:");
        System.out.println("  总计: " + (successfulQueries.size() + failedQueries.size()) + " 条SQL语句");
        System.out.println("  成功: " + successfulQueries.size() + " 条");
        System.out.println("  失败: " + failedQueries.size() + " 条");
        if (successfulQueries.size() + failedQueries.size() > 0) {
            System.out.println("  成功率: " + String.format("%.1f%%", 
                (double)successfulQueries.size() / (successfulQueries.size() + failedQueries.size()) * 100));
        }
        
        System.out.println("\\n=".repeat(60));
    }
}
