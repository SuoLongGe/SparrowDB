package com.database.engine;

import com.sqlcompiler.execution.*;
import com.sqlcompiler.catalog.*;
import java.util.*;

/**
 * 数据库引擎测试类
 */
public class TestDatabaseEngine {
    public static void main(String[] args) {
        System.out.println("=== SparrowDB 数据库引擎测试 ===");
        
        // 创建数据库引擎
        DatabaseEngine engine = new DatabaseEngine("testdb", "./data");
        
        // 初始化数据库引擎
        if (!engine.initialize()) {
            System.err.println("数据库引擎初始化失败");
            return;
        }
        
        try {
            // 测试1: 创建表
            System.out.println("\n1. 测试创建表...");
            testCreateTable(engine);
            
            // 测试2: 插入数据
            System.out.println("\n2. 测试插入数据...");
            testInsertData(engine);
            
            // 测试3: 查询数据
            System.out.println("\n3. 测试查询数据...");
            testSelectData(engine);
            
            // 测试4: 删除数据
            System.out.println("\n4. 测试删除数据...");
            testDeleteData(engine);
            
            // 测试5: 获取统计信息
            System.out.println("\n5. 测试获取统计信息...");
            testStatistics(engine);
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭数据库引擎
            engine.shutdown();
            System.out.println("\n数据库引擎已关闭");
        }
    }
    
    private static void testCreateTable(DatabaseEngine engine) {
        // 创建学生表
        List<ColumnPlan> columns = new ArrayList<>();
        columns.add(new ColumnPlan("id", "INT", 4, true, true, false, null, true));
        columns.add(new ColumnPlan("name", "VARCHAR", 50, true, false, false, null, false));
        columns.add(new ColumnPlan("age", "INT", 4, false, false, false, null, false));
        columns.add(new ColumnPlan("grade", "VARCHAR", 10, false, false, false, null, false));
        
        List<ConstraintPlan> constraints = new ArrayList<>();
        
        ExecutionResult result = engine.createTable("students", columns, constraints);
        System.out.println("创建学生表: " + result);
        
        // 创建课程表
        List<ColumnPlan> courseColumns = new ArrayList<>();
        courseColumns.add(new ColumnPlan("course_id", "INT", 4, true, true, false, null, true));
        courseColumns.add(new ColumnPlan("course_name", "VARCHAR", 100, true, false, false, null, false));
        courseColumns.add(new ColumnPlan("credits", "INT", 4, false, false, false, "3", false));
        
        ExecutionResult courseResult = engine.createTable("courses", courseColumns, constraints);
        System.out.println("创建课程表: " + courseResult);
    }
    
    private static void testInsertData(DatabaseEngine engine) {
        // 插入学生数据
        List<List<ExpressionPlan>> studentValues = new ArrayList<>();
        
        // 学生1
        List<ExpressionPlan> student1 = new ArrayList<>();
        student1.add(new LiteralExpressionPlan("1", "INT"));
        student1.add(new LiteralExpressionPlan("张三", "VARCHAR"));
        student1.add(new LiteralExpressionPlan("20", "INT"));
        student1.add(new LiteralExpressionPlan("计算机科学", "VARCHAR"));
        studentValues.add(student1);
        
        // 学生2
        List<ExpressionPlan> student2 = new ArrayList<>();
        student2.add(new LiteralExpressionPlan("2", "INT"));
        student2.add(new LiteralExpressionPlan("李四", "VARCHAR"));
        student2.add(new LiteralExpressionPlan("19", "INT"));
        student2.add(new LiteralExpressionPlan("软件工程", "VARCHAR"));
        studentValues.add(student2);
        
        // 学生3
        List<ExpressionPlan> student3 = new ArrayList<>();
        student3.add(new LiteralExpressionPlan("3", "INT"));
        student3.add(new LiteralExpressionPlan("王五", "VARCHAR"));
        student3.add(new LiteralExpressionPlan("21", "INT"));
        student3.add(new LiteralExpressionPlan("计算机科学", "VARCHAR"));
        studentValues.add(student3);
        
        List<String> columns = Arrays.asList("id", "name", "age", "grade");
        ExecutionResult result = engine.insertData("students", studentValues);
        System.out.println("插入学生数据: " + result);
        
        // 插入课程数据
        List<List<ExpressionPlan>> courseValues = new ArrayList<>();
        
        // 课程1
        List<ExpressionPlan> course1 = new ArrayList<>();
        course1.add(new LiteralExpressionPlan("1", "INT"));
        course1.add(new LiteralExpressionPlan("数据库系统", "VARCHAR"));
        course1.add(new LiteralExpressionPlan("4", "INT"));
        courseValues.add(course1);
        
        // 课程2
        List<ExpressionPlan> course2 = new ArrayList<>();
        course2.add(new LiteralExpressionPlan("2", "INT"));
        course2.add(new LiteralExpressionPlan("数据结构", "VARCHAR"));
        course2.add(new LiteralExpressionPlan("3", "INT"));
        courseValues.add(course2);
        
        List<String> courseColumns = Arrays.asList("course_id", "course_name", "credits");
        ExecutionResult courseResult = engine.insertData("courses", courseValues);
        System.out.println("插入课程数据: " + courseResult);
    }
    
    private static void testSelectData(DatabaseEngine engine) {
        // 查询所有学生
        List<ExpressionPlan> selectAll = new ArrayList<>();
        selectAll.add(new IdentifierExpressionPlan("*"));
        
        ExecutionResult result = engine.selectData("students", selectAll, null, null, null);
        System.out.println("查询所有学生: " + result);
        if (result.getData() != null) {
            for (Map<String, Object> row : result.getData()) {
                System.out.println("  " + row);
            }
        }
        
        // 查询特定年级的学生
        List<ExpressionPlan> selectGrade = new ArrayList<>();
        selectGrade.add(new IdentifierExpressionPlan("name"));
        selectGrade.add(new IdentifierExpressionPlan("age"));
        
        ExpressionPlan whereClause = new BinaryExpressionPlan(
            new IdentifierExpressionPlan("grade"),
            "=",
            new LiteralExpressionPlan("计算机科学", "VARCHAR")
        );
        
        ExecutionResult gradeResult = engine.selectData("students", selectGrade, whereClause, null, null);
        System.out.println("查询计算机科学专业学生: " + gradeResult);
        if (gradeResult.getData() != null) {
            for (Map<String, Object> row : gradeResult.getData()) {
                System.out.println("  " + row);
            }
        }
        
        // 查询所有课程
        ExecutionResult courseResult = engine.selectData("courses", selectAll, null, null, null);
        System.out.println("查询所有课程: " + courseResult);
        if (courseResult.getData() != null) {
            for (Map<String, Object> row : courseResult.getData()) {
                System.out.println("  " + row);
            }
        }
    }
    
    private static void testDeleteData(DatabaseEngine engine) {
        // 删除年龄大于20的学生
        ExpressionPlan whereClause = new BinaryExpressionPlan(
            new IdentifierExpressionPlan("age"),
            ">",
            new LiteralExpressionPlan("20", "INT")
        );
        
        ExecutionResult result = engine.deleteData("students", whereClause);
        System.out.println("删除年龄大于20的学生: " + result);
        
        // 再次查询验证删除结果
        List<ExpressionPlan> selectAll = new ArrayList<>();
        selectAll.add(new IdentifierExpressionPlan("*"));
        
        ExecutionResult verifyResult = engine.selectData("students", selectAll, null, null, null);
        System.out.println("删除后查询所有学生: " + verifyResult);
        if (verifyResult.getData() != null) {
            for (Map<String, Object> row : verifyResult.getData()) {
                System.out.println("  " + row);
            }
        }
    }
    
    private static void testStatistics(DatabaseEngine engine) {
        // 获取数据库信息
        Map<String, Object> dbInfo = engine.getDatabaseInfo();
        System.out.println("数据库信息:");
        for (Map.Entry<String, Object> entry : dbInfo.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        
        // 获取表信息
        Map<String, Object> studentInfo = engine.getTableInfo("students");
        if (studentInfo != null) {
            System.out.println("\n学生表信息:");
            for (Map.Entry<String, Object> entry : studentInfo.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        Map<String, Object> courseInfo = engine.getTableInfo("courses");
        if (courseInfo != null) {
            System.out.println("\n课程表信息:");
            for (Map.Entry<String, Object> entry : courseInfo.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        // 获取所有表名
        List<String> tableNames = engine.listTables();
        System.out.println("\n所有表名: " + tableNames);
    }
}
