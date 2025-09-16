package com.sqlcompiler;

import com.sqlcompiler.ast.Statement;
import com.sqlcompiler.ast.DropTableStatement;

/**
 * 测试EnhancedSQLCompiler对DROP TABLE的支持
 */
public class TestEnhancedCompiler {
    public static void main(String[] args) {
        System.out.println("=== 测试EnhancedSQLCompiler对DROP TABLE的支持 ===\n");
        
        EnhancedSQLCompiler compiler = new EnhancedSQLCompiler();
        
        // 测试DROP TABLE语句
        String sql = "DROP TABLE user;";
        System.out.println("测试SQL: " + sql);
        System.out.println("----------------------------------------");
        
        EnhancedSQLCompiler.CompilationResult result = compiler.compile(sql);
        
        System.out.println("编译结果:");
        System.out.println("成功: " + result.isSuccess());
        
        if (result.isSuccess()) {
            System.out.println("✓ 编译成功");
            
            Statement statement = result.getStatement();
            if (statement instanceof DropTableStatement) {
                DropTableStatement dropStmt = (DropTableStatement) statement;
                System.out.println("✓ 语句类型: DropTableStatement");
                System.out.println("✓ 表名: " + dropStmt.getTableName());
                System.out.println("✓ IF EXISTS: " + dropStmt.isIfExists());
            } else {
                System.out.println("✗ 语句类型错误: " + statement.getClass().getSimpleName());
            }
            
            if (result.getExecutionPlan() != null) {
                System.out.println("✓ 执行计划生成成功");
                System.out.println("执行计划类型: " + result.getExecutionPlan().getClass().getSimpleName());
            }
        } else {
            System.out.println("✗ 编译失败");
            System.out.println("错误信息:");
            for (String error : result.getErrors()) {
                System.out.println("  " + error);
            }
        }
        
        System.out.println("\n词法分析结果:");
        for (com.sqlcompiler.lexer.Token token : result.getTokens()) {
            System.out.println("  " + token.toString());
        }
        
        System.out.println("\nAST结构:");
        System.out.println(result.getAstStructure());
    }
}
