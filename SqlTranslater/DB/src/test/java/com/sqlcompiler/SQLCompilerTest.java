package com.sqlcompiler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL编译器测试类
 */
public class SQLCompilerTest {
    private SQLCompiler compiler;
    
    @BeforeEach
    public void setUp() {
        compiler = new SQLCompiler();
    }
    
    @Test
    public void testCreateTable() {
        String sql = "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50) NOT NULL, email VARCHAR(100));";
        
        SQLCompiler.CompilationResult result = compiler.compile(sql);
        
        assertTrue(result.isSuccess(), "CREATE TABLE语句应该编译成功");
        assertNotNull(result.getStatement(), "应该生成AST");
        assertNotNull(result.getExecutionPlan(), "应该生成执行计划");
    }
    
    @Test
    public void testInsertStatement() {
        // 先创建表
        String createTable = "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50), email VARCHAR(100));";
        compiler.compile(createTable);
        
        // 测试INSERT语句
        String sql = "INSERT INTO users (id, name, email) VALUES (1, 'John', 'john@example.com');";
        
        SQLCompiler.CompilationResult result = compiler.compile(sql);
        
        assertTrue(result.isSuccess(), "INSERT语句应该编译成功");
    }
    
    @Test
    public void testSelectStatement() {
        // 先创建表
        String createTable = "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50), email VARCHAR(100));";
        compiler.compile(createTable);
        
        // 测试SELECT语句
        String sql = "SELECT id, name FROM users WHERE id > 0 ORDER BY name ASC LIMIT 10;";
        
        SQLCompiler.CompilationResult result = compiler.compile(sql);
        
        assertTrue(result.isSuccess(), "SELECT语句应该编译成功");
    }
    
    @Test
    public void testDeleteStatement() {
        // 先创建表
        String createTable = "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50), email VARCHAR(100));";
        compiler.compile(createTable);
        
        // 测试DELETE语句
        String sql = "DELETE FROM users WHERE id = 1;";
        
        SQLCompiler.CompilationResult result = compiler.compile(sql);
        
        assertTrue(result.isSuccess(), "DELETE语句应该编译成功");
    }
    
    @Test
    public void testInvalidSyntax() {
        String sql = "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50);"; // 缺少右括号
        
        SQLCompiler.CompilationResult result = compiler.compile(sql);
        
        assertFalse(result.isSuccess(), "语法错误的语句应该编译失败");
        assertNotNull(result.getErrors(), "应该有错误信息");
    }
    
    @Test
    public void testSemanticError() {
        // 测试引用不存在的表
        String sql = "SELECT * FROM nonexistent_table;";
        
        SQLCompiler.CompilationResult result = compiler.compile(sql);
        
        assertFalse(result.isSuccess(), "引用不存在表的语句应该编译失败");
    }
    
    @Test
    public void testComplexQuery() {
        // 创建两个表
        String createUsers = "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50), department_id INT);";
        String createDepts = "CREATE TABLE departments (id INT PRIMARY KEY, name VARCHAR(50));";
        
        compiler.compile(createUsers);
        compiler.compile(createDepts);
        
        // 复杂查询
        String sql = "SELECT u.name, d.name FROM users u JOIN departments d ON u.department_id = d.id WHERE u.id > 0 ORDER BY u.name;";
        
        SQLCompiler.CompilationResult result = compiler.compile(sql);
        
        assertTrue(result.isSuccess(), "复杂查询应该编译成功");
    }
}
