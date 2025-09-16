package com.sqlcompiler;

import com.sqlcompiler.SQLCompiler;

/**
 * UPDATE功能测试程序
 */
public class UpdateTest {
    public static void main(String[] args) {
        try {
            // 初始化SQL编译器
            SQLCompiler compiler = new SQLCompiler();
            
            System.out.println("=== UPDATE功能测试 ===");
            
            // 1. 创建测试表
            System.out.println("\n1. 创建测试表...");
            String createTableSQL = "CREATE TABLE users (id INT, name VARCHAR(50), age INT);";
            executeSQL(createTableSQL, compiler);
            
            // 2. 插入测试数据
            System.out.println("\n2. 插入测试数据...");
            String insertSQL1 = "INSERT INTO users VALUES (1, 'Alice', 25);";
            String insertSQL2 = "INSERT INTO users VALUES (2, 'Bob', 30);";
            String insertSQL3 = "INSERT INTO users VALUES (3, 'Charlie', 35);";
            executeSQL(insertSQL1, compiler);
            executeSQL(insertSQL2, compiler);
            executeSQL(insertSQL3, compiler);
            
            // 3. 查看插入后的数据
            System.out.println("\n3. 查看插入后的数据...");
            String selectSQL1 = "SELECT * FROM users;";
            executeSQL(selectSQL1, compiler);
            
            // 4. 测试UPDATE语句
            System.out.println("\n4. 测试UPDATE语句...");
            String updateSQL = "UPDATE users SET name = 'Tom', age = 20 WHERE id = 1;";
            executeSQL(updateSQL, compiler);
            
            // 5. 查看UPDATE后的数据
            System.out.println("\n5. 查看UPDATE后的数据...");
            String selectSQL2 = "SELECT * FROM users;";
            executeSQL(selectSQL2, compiler);
            
            // 6. 测试更多UPDATE语句
            System.out.println("\n6. 测试更多UPDATE语句...");
            String updateSQL2 = "UPDATE users SET age = age + 5 WHERE id = 2;";
            executeSQL(updateSQL2, compiler);
            
            // 7. 查看最终数据
            System.out.println("\n7. 查看最终数据...");
            String selectSQL3 = "SELECT * FROM users;";
            executeSQL(selectSQL3, compiler);
            
            System.out.println("\n=== UPDATE功能测试完成 ===");
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void executeSQL(String sql, SQLCompiler compiler) {
        try {
            System.out.println("执行SQL: " + sql);
            
            // 编译SQL
            SQLCompiler.CompilationResult result = compiler.compile(sql);
            
            if (result.isSuccess()) {
                System.out.println("✓ 编译成功");
                if (result.getStatement() != null) {
                    System.out.println("AST: " + result.getStatement().getClass().getSimpleName());
                }
            } else {
                System.out.println("✗ 编译失败: " + result.getErrors());
            }
            
        } catch (Exception e) {
            System.out.println("✗ 执行错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
