package com.database;

import com.database.engine.*;
import com.sqlcompiler.execution.*;
import java.util.*;
import java.util.Scanner;

/**
 * SparrowDB 主应用程序
 * 提供命令行界面来演示整个数据库系统的功能
 */
public class SparrowDBApplication {
    private DatabaseEngine engine;
    private Scanner scanner;
    private boolean running;
    
    public static void main(String[] args) {
        System.out.println("=== 欢迎使用 SparrowDB 数据库系统 ===");
        
        SparrowDBApplication app = new SparrowDBApplication();
        app.start();
    }
    
    public void start() {
        scanner = new Scanner(System.in);
        running = true;
        
        // 初始化数据库引擎
        System.out.print("请输入数据库名称 (默认: sparrow_db): ");
        String dbName = scanner.nextLine().trim();
        if (dbName.isEmpty()) {
            dbName = "sparrow_db";
        }
        
        System.out.print("请输入数据目录路径 (默认: ./data): ");
        String dataDir = scanner.nextLine().trim();
        if (dataDir.isEmpty()) {
            dataDir = "./data";
        }
        
        engine = new DatabaseEngine(dbName, dataDir);
        
        if (!engine.initialize()) {
            System.err.println("数据库引擎初始化失败！");
            return;
        }
        
        System.out.println("数据库引擎初始化成功！");
        System.out.println("数据库: " + dbName + ", 数据目录: " + dataDir);
        
        // 创建示例数据
        setupExampleData();
        
        // 主循环
        mainLoop();
        
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
        }
    }
    
    private void mainLoop() {
        printHelp();
        
        while (running) {
            System.out.print("\nSparrowDB> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            handleCommand(input);
        }
    }
    
    private void handleCommand(String input) {
        String command = input.toLowerCase();
        
        try {
            if (command.equals("help") || command.equals("h")) {
                printHelp();
            } else if (command.equals("quit") || command.equals("exit") || command.equals("q")) {
                running = false;
                System.out.println("再见！");
            } else if (command.equals("tables") || command.equals("show tables")) {
                showTables();
            } else if (command.startsWith("desc ")) {
                String tableName = input.substring(5).trim();
                describeTable(tableName);
            } else if (command.equals("info") || command.equals("status")) {
                showDatabaseInfo();
            } else if (command.equals("examples")) {
                showExamples();
            } else if (command.startsWith("benchmark")) {
                runBenchmark();
            } else {
                // 执行SQL语句
                executeSQLCommand(input);
            }
        } catch (Exception e) {
            System.err.println("执行命令时发生错误: " + e.getMessage());
        }
    }
    
    private void printHelp() {
        System.out.println("\n=== SparrowDB 命令帮助 ===");
        System.out.println("SQL 命令:");
        System.out.println("  SELECT * FROM table_name          - 查询数据");
        System.out.println("  INSERT INTO table VALUES (...)    - 插入数据");
        System.out.println("  DELETE FROM table WHERE ...       - 删除数据");
        System.out.println("  CREATE TABLE table (columns...)   - 创建表");
        System.out.println("");
        System.out.println("系统命令:");
        System.out.println("  tables, show tables               - 显示所有表");
        System.out.println("  desc <table_name>                 - 描述表结构");
        System.out.println("  info, status                      - 显示数据库信息");
        System.out.println("  examples                          - 显示示例查询");
        System.out.println("  benchmark                         - 运行性能测试");
        System.out.println("  help, h                          - 显示此帮助");
        System.out.println("  quit, exit, q                    - 退出程序");
        System.out.println("");
        System.out.println("提示: 直接输入SQL语句来执行查询");
    }
    
    private void showTables() {
        List<String> tables = engine.listTables();
        System.out.println("\n数据库中的表:");
        if (tables.isEmpty()) {
            System.out.println("  (没有表)");
        } else {
            for (String table : tables) {
                System.out.println("  - " + table);
            }
        }
    }
    
    private void describeTable(String tableName) {
        Map<String, Object> tableInfo = engine.getTableInfo(tableName);
        if (tableInfo == null) {
            System.out.println("表 '" + tableName + "' 不存在");
            return;
        }
        
        System.out.println("\n表 '" + tableName + "' 的结构:");
        for (Map.Entry<String, Object> entry : tableInfo.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
    }
    
    private void showDatabaseInfo() {
        Map<String, Object> dbInfo = engine.getDatabaseInfo();
        System.out.println("\n数据库信息:");
        for (Map.Entry<String, Object> entry : dbInfo.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
    }
    
    private void showExamples() {
        System.out.println("\n=== 示例查询 ===");
        System.out.println("基本查询:");
        System.out.println("  SELECT * FROM users");
        System.out.println("  SELECT name, age FROM users WHERE age > 25");
        System.out.println("  SELECT * FROM products WHERE category = 'Electronics'");
        System.out.println("");
        System.out.println("插入数据:");
        System.out.println("  INSERT INTO users VALUES (6, 'Frank Miller', 'frank@example.com', 40)");
        System.out.println("  INSERT INTO products VALUES (6, 'Smartphone', 799.99, 'Electronics')");
        System.out.println("");
        System.out.println("删除数据:");
        System.out.println("  DELETE FROM users WHERE age < 25");
        System.out.println("  DELETE FROM products WHERE price > 1000");
        System.out.println("");
        System.out.println("创建新表:");
        System.out.println("  CREATE TABLE orders (id INT PRIMARY KEY, user_id INT, total DECIMAL)");
    }
    
    private void runBenchmark() {
        System.out.println("\n正在运行性能测试...");
        
        long startTime = System.currentTimeMillis();
        
        // 插入性能测试
        System.out.print("插入测试... ");
        int insertCount = 0;
        for (int i = 100; i < 200; i++) {
            String sql = String.format("INSERT INTO users VALUES (%d, 'TestUser%d', 'test%d@example.com', %d)",
                                     i, i, i, 20 + (i % 40));
            ExecutionResult result = engine.executeSQL(sql);
            if (result.isSuccess()) {
                insertCount++;
            }
        }
        System.out.println("成功插入 " + insertCount + " 条记录");
        
        // 查询性能测试
        System.out.print("查询测试... ");
        ExecutionResult result = engine.executeSQL("SELECT * FROM users");
        if (result.isSuccess()) {
            System.out.println("查询到 " + result.getData().size() + " 条记录");
        } else {
            System.out.println("查询失败");
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("总耗时: " + (endTime - startTime) + "ms");
    }
    
    private void executeSQLCommand(String sql) {
        System.out.println("执行SQL: " + sql);
        
        long startTime = System.currentTimeMillis();
        ExecutionResult result = engine.executeSQL(sql);
        long endTime = System.currentTimeMillis();
        
        if (result.isSuccess()) {
            System.out.println("✓ 执行成功 (耗时: " + (endTime - startTime) + "ms)");
            
            if (result.getData() != null && !result.getData().isEmpty()) {
                System.out.println("查询结果:");
                displayResults(result.getData());
            } else if (result.getMessage() != null && !result.getMessage().isEmpty()) {
                System.out.println("结果: " + result.getMessage());
            }
        } else {
            System.err.println("✗ 执行失败: " + result.getMessage());
        }
    }
    
    private void displayResults(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
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
            System.out.println("数据库引擎已关闭");
        }
        
        if (scanner != null) {
            scanner.close();
        }
    }
}
