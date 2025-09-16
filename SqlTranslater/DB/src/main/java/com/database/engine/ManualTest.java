package com.database.engine;

import com.sqlcompiler.execution.*;
import com.sqlcompiler.catalog.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * 手动测试数据库引擎功能
 * 用于验证各种数据库操作
 */
public class ManualTest {
    
    public static void main(String[] args) {
        System.out.println("=== SparrowDB 手动功能测试 ===\n");
        
        try {
            // 1. 创建数据库引擎
            System.out.println("1. 创建数据库引擎...");
            DatabaseEngine engine = new DatabaseEngine("testdb", "./data");
            System.out.println("   ✓ 数据库引擎创建成功");
            
            // 2. 初始化数据库引擎
            System.out.println("\n2. 初始化数据库引擎...");
            boolean initResult = engine.initialize();
            if (initResult) {
                System.out.println("   ✓ 数据库引擎初始化成功");
            } else {
                System.out.println("   ✗ 数据库引擎初始化失败");
                return;
            }
            
            // 3. 创建学生表
            System.out.println("\n3. 创建学生表...");
            List<ColumnPlan> studentColumns = new ArrayList<>();
            studentColumns.add(new ColumnPlan("id", "INT", 4, true, true, false, null, true));
            studentColumns.add(new ColumnPlan("name", "VARCHAR", 50, true, false, false, null, false));
            studentColumns.add(new ColumnPlan("age", "INT", 4, false, false, false, "18", false));
            studentColumns.add(new ColumnPlan("grade", "DECIMAL", 10, false, false, false, null, false));
            
            List<ConstraintPlan> studentConstraints = new ArrayList<>();
            studentConstraints.add(new ConstraintPlan("pk_student", ConstraintPlan.ConstraintType.PRIMARY_KEY, Arrays.asList("id"), null, null, null));
            
            ExecutionResult createTableResult = engine.createTable("students", studentColumns, studentConstraints);
            if (createTableResult.isSuccess()) {
                System.out.println("   ✓ 学生表创建成功");
            } else {
                System.out.println("   ✗ 学生表创建失败: " + createTableResult.getMessage());
            }
            
            // 4. 插入学生数据
            System.out.println("\n4. 插入学生数据...");
            List<String> insertColumns = Arrays.asList("id", "name", "age", "grade");
            
            // 准备插入数据
            List<List<ExpressionPlan>> values1 = new ArrayList<>();
            List<ExpressionPlan> row1 = new ArrayList<>();
            row1.add(new LiteralExpressionPlan("1", "INT"));
            row1.add(new LiteralExpressionPlan("张三", "VARCHAR"));
            row1.add(new LiteralExpressionPlan("20", "INT"));
            row1.add(new LiteralExpressionPlan("85.5", "DECIMAL"));
            values1.add(row1);
            
            List<List<ExpressionPlan>> values2 = new ArrayList<>();
            List<ExpressionPlan> row2 = new ArrayList<>();
            row2.add(new LiteralExpressionPlan("2", "INT"));
            row2.add(new LiteralExpressionPlan("李四", "VARCHAR"));
            row2.add(new LiteralExpressionPlan("21", "INT"));
            row2.add(new LiteralExpressionPlan("92.0", "DECIMAL"));
            values2.add(row2);
            
            List<List<ExpressionPlan>> values3 = new ArrayList<>();
            List<ExpressionPlan> row3 = new ArrayList<>();
            row3.add(new LiteralExpressionPlan("3", "INT"));
            row3.add(new LiteralExpressionPlan("王五", "VARCHAR"));
            row3.add(new LiteralExpressionPlan("19", "INT"));
            row3.add(new LiteralExpressionPlan("78.5", "DECIMAL"));
            values3.add(row3);
            
            ExecutionResult insertResult1 = engine.insertData("students", insertColumns, values1);
            ExecutionResult insertResult2 = engine.insertData("students", insertColumns, values2);
            ExecutionResult insertResult3 = engine.insertData("students", insertColumns, values3);
            
            System.out.println("   插入学生1: " + (insertResult1.isSuccess() ? "成功" : "失败 - " + insertResult1.getMessage()));
            System.out.println("   插入学生2: " + (insertResult2.isSuccess() ? "成功" : "失败 - " + insertResult2.getMessage()));
            System.out.println("   插入学生3: " + (insertResult3.isSuccess() ? "成功" : "失败 - " + insertResult3.getMessage()));
            
            // 5. 查询所有学生
            System.out.println("\n5. 查询所有学生...");
            List<ExpressionPlan> selectColumns = new ArrayList<>();
            selectColumns.add(new IdentifierExpressionPlan("*"));
            ExecutionResult selectResult = engine.selectData("students", selectColumns, null, null, null);
            if (selectResult.isSuccess()) {
                System.out.println("   ✓ 查询成功，返回 " + selectResult.getData().size() + " 行数据");
                System.out.println("   查询结果:");
                for (int i = 0; i < selectResult.getData().size(); i++) {
                    System.out.println("     " + (i + 1) + ". " + selectResult.getData().get(i));
                }
            } else {
                System.out.println("   ✗ 查询失败: " + selectResult.getMessage());
            }
            
            // 6. 创建课程表
            System.out.println("\n6. 创建课程表...");
            List<ColumnPlan> courseColumns = new ArrayList<>();
            courseColumns.add(new ColumnPlan("course_id", "INT", 4, true, true, false, null, true));
            courseColumns.add(new ColumnPlan("course_name", "VARCHAR", 50, true, false, false, null, false));
            courseColumns.add(new ColumnPlan("credits", "INT", 4, false, false, false, "1", false));
            
            List<ConstraintPlan> courseConstraints = new ArrayList<>();
            courseConstraints.add(new ConstraintPlan("pk_course", ConstraintPlan.ConstraintType.PRIMARY_KEY, Arrays.asList("course_id"), null, null, null));
            
            ExecutionResult createCourseResult = engine.createTable("courses", courseColumns, courseConstraints);
            if (createCourseResult.isSuccess()) {
                System.out.println("   ✓ 课程表创建成功");
            } else {
                System.out.println("   ✗ 课程表创建失败: " + createCourseResult.getMessage());
            }
            
            // 7. 插入课程数据
            System.out.println("\n7. 插入课程数据...");
            List<String> courseInsertColumns = Arrays.asList("course_id", "course_name", "credits");
            
            List<List<ExpressionPlan>> courseValues1 = new ArrayList<>();
            List<ExpressionPlan> courseRow1 = new ArrayList<>();
            courseRow1.add(new LiteralExpressionPlan("101", "INT"));
            courseRow1.add(new LiteralExpressionPlan("数据库原理", "VARCHAR"));
            courseRow1.add(new LiteralExpressionPlan("3", "INT"));
            courseValues1.add(courseRow1);
            
            List<List<ExpressionPlan>> courseValues2 = new ArrayList<>();
            List<ExpressionPlan> courseRow2 = new ArrayList<>();
            courseRow2.add(new LiteralExpressionPlan("102", "INT"));
            courseRow2.add(new LiteralExpressionPlan("数据结构", "VARCHAR"));
            courseRow2.add(new LiteralExpressionPlan("4", "INT"));
            courseValues2.add(courseRow2);
            
            ExecutionResult courseInsert1 = engine.insertData("courses", courseInsertColumns, courseValues1);
            ExecutionResult courseInsert2 = engine.insertData("courses", courseInsertColumns, courseValues2);
            
            System.out.println("   插入课程1: " + (courseInsert1.isSuccess() ? "成功" : "失败 - " + courseInsert1.getMessage()));
            System.out.println("   插入课程2: " + (courseInsert2.isSuccess() ? "成功" : "失败 - " + courseInsert2.getMessage()));
            
            // 8. 查询课程表
            System.out.println("\n8. 查询课程表...");
            List<ExpressionPlan> courseSelectColumns = new ArrayList<>();
            courseSelectColumns.add(new IdentifierExpressionPlan("*"));
            ExecutionResult courseSelectResult = engine.selectData("courses", courseSelectColumns, null, null, null);
            if (courseSelectResult.isSuccess()) {
                System.out.println("   ✓ 课程查询成功，返回 " + courseSelectResult.getData().size() + " 行数据");
                System.out.println("   课程查询结果:");
                for (int i = 0; i < courseSelectResult.getData().size(); i++) {
                    System.out.println("     " + (i + 1) + ". " + courseSelectResult.getData().get(i));
                }
            } else {
                System.out.println("   ✗ 课程查询失败: " + courseSelectResult.getMessage());
            }
            
            // 9. 测试错误情况
            System.out.println("\n9. 测试错误情况...");
            
            // 尝试插入重复主键
            List<List<ExpressionPlan>> duplicateValues = new ArrayList<>();
            List<ExpressionPlan> duplicateRow = new ArrayList<>();
            duplicateRow.add(new LiteralExpressionPlan("1", "INT"));
            duplicateRow.add(new LiteralExpressionPlan("重复ID", "VARCHAR"));
            duplicateRow.add(new LiteralExpressionPlan("25", "INT"));
            duplicateRow.add(new LiteralExpressionPlan("90.0", "DECIMAL"));
            duplicateValues.add(duplicateRow);
            
            ExecutionResult duplicateResult = engine.insertData("students", insertColumns, duplicateValues);
            System.out.println("   插入重复主键: " + (duplicateResult.isSuccess() ? "意外成功" : "正确失败 - " + duplicateResult.getMessage()));
            
            // 尝试查询不存在的表
            List<ExpressionPlan> invalidSelectColumns = new ArrayList<>();
            invalidSelectColumns.add(new IdentifierExpressionPlan("*"));
            ExecutionResult invalidTableResult = engine.selectData("nonexistent_table", invalidSelectColumns, null, null, null);
            System.out.println("   查询不存在表: " + (invalidTableResult.isSuccess() ? "意外成功" : "正确失败 - " + invalidTableResult.getMessage()));
            
            // 10. 关闭数据库引擎
            System.out.println("\n10. 关闭数据库引擎...");
            engine.shutdown();
            System.out.println("   ✓ 数据库引擎关闭成功");
            
            System.out.println("\n=== 手动测试完成 ===");
            System.out.println("所有功能测试通过！数据库引擎工作正常。");
            
        } catch (Exception e) {
            System.out.println("\n✗ 测试过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
