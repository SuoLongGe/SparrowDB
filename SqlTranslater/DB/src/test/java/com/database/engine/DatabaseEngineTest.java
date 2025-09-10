package com.database.engine;

import com.sqlcompiler.execution.*;
import com.sqlcompiler.catalog.*;
import org.junit.jupiter.api.*;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库引擎JUnit测试类
 * 可以在IDEA中直接运行测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseEngineTest {
    
    private static DatabaseEngine engine;
    
    @BeforeAll
    static void setUp() {
        System.out.println("=== 开始数据库引擎测试 ===");
        engine = new DatabaseEngine("test_db", "./test_data");
        engine.initialize();
        System.out.println("数据库引擎初始化完成");
    }
    
    @AfterAll
    static void tearDown() {
        if (engine != null) {
            engine.shutdown();
            System.out.println("数据库引擎已关闭");
        }
        System.out.println("=== 数据库引擎测试完成 ===");
    }
    
    @Test
    @Order(1)
    @DisplayName("测试创建表")
    void testCreateTable() {
        System.out.println("\n测试1: 创建表");
        
        List<ColumnPlan> columns = new ArrayList<>();
        columns.add(new ColumnPlan("id", "INT", null, false, true, false, null, false));
        columns.add(new ColumnPlan("name", "VARCHAR", 50, false, false, false, null, false));
        columns.add(new ColumnPlan("age", "INT", null, false, false, false, null, false));
        
        ExecutionResult result = engine.createTable("junit_test_table", columns, new ArrayList<>());
        
        assertTrue(result.isSuccess(), "表创建应该成功: " + result.getMessage());
        System.out.println("   ✓ 表创建成功");
    }
    
    @Test
    @Order(2)
    @DisplayName("测试插入数据")
    void testInsertData() {
        System.out.println("\n测试2: 插入数据");
        
        List<String> insertColumns = List.of("id", "name", "age");
        List<List<ExpressionPlan>> insertValues = new ArrayList<>();
        
        // 插入测试数据
        List<ExpressionPlan> row1 = new ArrayList<>();
        row1.add(new LiteralExpressionPlan("1", "INT"));
        row1.add(new LiteralExpressionPlan("JUnit测试用户", "VARCHAR"));
        row1.add(new LiteralExpressionPlan("25", "INT"));
        insertValues.add(row1);
        
        ExecutionResult result = engine.insertData("junit_test_table", insertColumns, insertValues);
        
        assertTrue(result.isSuccess(), "数据插入应该成功: " + result.getMessage());
        System.out.println("   ✓ 数据插入成功");
    }
    
    @Test
    @Order(3)
    @DisplayName("测试查询数据")
    void testSelectData() {
        System.out.println("\n测试3: 查询数据");
        
        List<ExpressionPlan> selectColumns = new ArrayList<>();
        selectColumns.add(new IdentifierExpressionPlan("*"));
        
        ExecutionResult result = engine.selectData("junit_test_table", selectColumns, null, null, null);
        
        assertTrue(result.isSuccess(), "数据查询应该成功: " + result.getMessage());
        assertFalse(result.getData().isEmpty(), "查询结果不应该为空");
        assertEquals(1, result.getData().size(), "应该返回1行数据");
        
        System.out.println("   ✓ 数据查询成功，返回 " + result.getData().size() + " 行数据");
        System.out.println("   查询结果: " + result.getData().get(0));
    }
    
    @Test
    @Order(4)
    @DisplayName("测试错误处理")
    void testErrorHandling() {
        System.out.println("\n测试4: 错误处理");
        
        // 测试查询不存在的表
        List<ExpressionPlan> selectColumns = new ArrayList<>();
        selectColumns.add(new IdentifierExpressionPlan("*"));
        
        ExecutionResult result = engine.selectData("nonexistent_table", selectColumns, null, null, null);
        
        assertFalse(result.isSuccess(), "查询不存在的表应该失败");
        assertTrue(result.getMessage().contains("不存在"), "错误信息应该包含'不存在'");
        
        System.out.println("   ✓ 错误处理正常: " + result.getMessage());
    }
}