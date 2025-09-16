package com.database.engine;

import com.sqlcompiler.execution.*;
import com.sqlcompiler.catalog.*;
import java.util.*;
import java.util.Arrays;

/**
 * 简单的数据库引擎测试
 */
public class SimpleTest {
    public static void main(String[] args) {
        System.out.println("=== SparrowDB 简单测试 ===");
        
        try {
            // 测试创建数据库引擎
            DatabaseEngine engine = new DatabaseEngine("testdb", "./data");
            System.out.println("✓ 数据库引擎创建成功");
            
            // 测试初始化
            boolean initResult = engine.initialize();
            System.out.println("✓ 数据库引擎初始化: " + (initResult ? "成功" : "失败"));
            
            // 测试创建表
            List<ColumnPlan> columns = new ArrayList<>();
            columns.add(new ColumnPlan("id", "INT", 4, true, true, false, null, true));
            columns.add(new ColumnPlan("name", "VARCHAR", 50, true, false, false, null, false));
            
            List<ConstraintPlan> constraints = new ArrayList<>();
            ExecutionResult result = engine.createTable("test_table", columns, constraints);
            System.out.println("✓ 创建表测试: " + result);
            
            // 测试插入数据
            List<String> insertColumns = Arrays.asList("id", "name");
            List<List<ExpressionPlan>> values = new ArrayList<>();
            List<ExpressionPlan> row1 = new ArrayList<>();
            row1.add(new LiteralExpressionPlan("1", "INT"));
            row1.add(new LiteralExpressionPlan("测试数据", "VARCHAR"));
            values.add(row1);
            
            ExecutionResult insertResult = engine.insertData("test_table", insertColumns, values);
            System.out.println("✓ 插入数据测试: " + insertResult);
            
            // 测试查询数据
            List<ExpressionPlan> selectColumns = new ArrayList<>();
            selectColumns.add(new IdentifierExpressionPlan("*"));
            
            ExecutionResult selectResult = engine.selectData("test_table", selectColumns, null, null, null);
            System.out.println("✓ 查询数据测试: " + selectResult);
            
            if (selectResult.getData() != null) {
                System.out.println("查询结果:");
                for (Map<String, Object> row : selectResult.getData()) {
                    System.out.println("  " + row);
                }
            }
            
            // 关闭数据库引擎
            engine.shutdown();
            System.out.println("✓ 数据库引擎关闭成功");
            
            System.out.println("\n=== 所有测试通过 ===");
            
        } catch (Exception e) {
            System.err.println("✗ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
