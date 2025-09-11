package com.database;

import com.database.engine.*;
import com.sqlcompiler.execution.*;
import java.util.*;

/**
 * SparrowDB 简化版本 - 直接运行演示
 * 不需要用户输入，使用默认配置
 */
public class SimpleSparrowDB {
    private DatabaseEngine engine;
    
    public static void main(String[] args) {
        System.out.println("=== SparrowDB 数据库系统演示 ===");
        
        SimpleSparrowDB app = new SimpleSparrowDB();
        app.start();
    }
    
    public void start() {
        // 使用默认配置
        String dbName = "sparrow_db";
        String dataDir = "./data";
        
        System.out.println("初始化数据库引擎...");
        System.out.println("数据库: " + dbName + ", 数据目录: " + dataDir);
        
        engine = new DatabaseEngine(dbName, dataDir);
        
        if (!engine.initialize()) {
            System.err.println("数据库引擎初始化失败！");
            return;
        }
        
        System.out.println("✓ 数据库引擎初始化成功！");
        
        // 创建示例数据
        setupExampleData();
        
        // 运行演示
        runDemo();
        
        // 清理
        cleanup();
    }
    
    private void setupExampleData() {
        System.out.println("\n正在创建示例数据...");
        
        try {
            // 创建用户表
            List<ColumnPlan> userColumns = Arrays.asList(
                new ColumnPlan("id", "INT", 0, false, true, false, null, false),
                new ColumnPlan("name", "VARCHAR", 50, false, false, false, null, false),
                new ColumnPlan("email", "VARCHAR", 100, false, false, false, null, false),
                new ColumnPlan("age", "INT", 0, false, false, false, null, false)
            );
            
            ExecutionResult result = engine.createTable("users", userColumns, new ArrayList<>());
            if (result.isSuccess()) {
                System.out.println("✓ 用户表创建成功");
                
                // 插入示例数据
                String[] sampleUsers = {
                    "INSERT INTO users VALUES (1, 'Alice Johnson', 'alice@example.com', 28)",
                    "INSERT INTO users VALUES (2, 'Bob Smith', 'bob@example.com', 32)",
                    "INSERT INTO users VALUES (3, 'Charlie Brown', 'charlie@example.com', 25)",
                    "INSERT INTO users VALUES (4, 'Diana Wilson', 'diana@example.com', 29)",
                    "INSERT INTO users VALUES (5, 'Eve Davis', 'eve@example.com', 35)"
                };
                
                for (String sql : sampleUsers) {
                    ExecutionResult insertResult = engine.executeSQL(sql);
                    if (!insertResult.isSuccess()) {
                        System.out.println("⚠ 插入数据失败: " + sql);
                    }
                }
                System.out.println("✓ 示例用户数据插入完成");
            } else {
                System.out.println("⚠ 用户表可能已存在: " + result.getMessage());
            }
            
            // 创建产品表
            List<ColumnPlan> productColumns = Arrays.asList(
                new ColumnPlan("id", "INT", 0, false, true, false, null, false),
                new ColumnPlan("name", "VARCHAR", 100, false, false, false, null, false),
                new ColumnPlan("price", "DECIMAL", 10, false, false, false, null, false),
                new ColumnPlan("category", "VARCHAR", 50, false, false, false, null, false)
            );
            
            result = engine.createTable("products", productColumns, new ArrayList<>());
            if (result.isSuccess()) {
                System.out.println("✓ 产品表创建成功");
                
                // 插入示例产品
                String[] sampleProducts = {
                    "INSERT INTO products VALUES (1, 'Laptop', 1299.99, 'Electronics')",
                    "INSERT INTO products VALUES (2, 'Coffee Mug', 12.50, 'Home')",
                    "INSERT INTO products VALUES (3, 'Book: Database Systems', 89.99, 'Books')",
                    "INSERT INTO products VALUES (4, 'Wireless Mouse', 29.99, 'Electronics')",
                    "INSERT INTO products VALUES (5, 'Desk Lamp', 45.00, 'Home')"
                };
                
                for (String sql : sampleProducts) {
                    ExecutionResult insertResult = engine.executeSQL(sql);
                    if (!insertResult.isSuccess()) {
                        System.out.println("⚠ 插入产品数据失败: " + sql);
                    }
                }
                System.out.println("✓ 示例产品数据插入完成");
            } else {
                System.out.println("⚠ 产品表可能已存在: " + result.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("创建示例数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void runDemo() {
        System.out.println("\n=== 开始演示 SparrowDB 功能 ===");
        
        // 1. 显示所有表
        System.out.println("\n1. 显示所有表:");
        List<String> tables = engine.listTables();
        for (String table : tables) {
            System.out.println("  - " + table);
        }
        
        // 2. 查询用户表
        System.out.println("\n2. 查询用户表:");
        ExecutionResult result = engine.executeSQL("SELECT * FROM users");
        if (result.isSuccess()) {
            displayResults(result.getData());
        } else {
            System.out.println("查询失败: " + result.getMessage());
        }
        
        // 3. 查询产品表
        System.out.println("\n3. 查询产品表:");
        result = engine.executeSQL("SELECT * FROM products");
        if (result.isSuccess()) {
            displayResults(result.getData());
        } else {
            System.out.println("查询失败: " + result.getMessage());
        }
        
        // 4. 条件查询
        System.out.println("\n4. 条件查询 - 年龄大于25的用户:");
        result = engine.executeSQL("SELECT name, age FROM users WHERE age > 25");
        if (result.isSuccess()) {
            displayResults(result.getData());
        } else {
            System.out.println("查询失败: " + result.getMessage());
        }
        
        // 5. 插入新数据
        System.out.println("\n5. 插入新用户:");
        result = engine.executeSQL("INSERT INTO users VALUES (6, 'Frank Miller', 'frank@example.com', 40)");
        if (result.isSuccess()) {
            System.out.println("✓ 新用户插入成功");
        } else {
            System.out.println("插入失败: " + result.getMessage());
        }
        
        // 6. 验证插入
        System.out.println("\n6. 验证插入结果:");
        result = engine.executeSQL("SELECT * FROM users WHERE id = 6");
        if (result.isSuccess()) {
            displayResults(result.getData());
        } else {
            System.out.println("查询失败: " + result.getMessage());
        }
        
        // 7. 显示数据库信息
        System.out.println("\n7. 数据库信息:");
        Map<String, Object> dbInfo = engine.getDatabaseInfo();
        for (Map.Entry<String, Object> entry : dbInfo.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        
        System.out.println("\n=== 演示完成 ===");
    }
    
    private void displayResults(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            System.out.println("  (没有结果)");
            return;
        }
        
        // 显示前10条记录
        int displayCount = Math.min(results.size(), 10);
        
        // 获取列名
        Set<String> columns = results.get(0).keySet();
        
        // 打印表头
        System.out.print("  ");
        for (String column : columns) {
            System.out.printf("%-15s", column);
        }
        System.out.println();
        
        // 打印分隔线
        System.out.print("  ");
        for (int i = 0; i < columns.size(); i++) {
            System.out.print("---------------");
        }
        System.out.println();
        
        // 打印数据
        for (int i = 0; i < displayCount; i++) {
            Map<String, Object> row = results.get(i);
            System.out.print("  ");
            for (String column : columns) {
                Object value = row.get(column);
                String displayValue = value != null ? value.toString() : "NULL";
                System.out.printf("%-15s", displayValue);
            }
            System.out.println();
        }
        
        if (results.size() > displayCount) {
            System.out.println("  ... 还有 " + (results.size() - displayCount) + " 条记录");
        }
        
        System.out.println("总计: " + results.size() + " 条记录");
    }
    
    private void cleanup() {
        if (engine != null) {
            engine.shutdown();
            System.out.println("\n数据库引擎已关闭");
        }
    }
}
