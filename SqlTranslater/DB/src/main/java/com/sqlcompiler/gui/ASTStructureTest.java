package com.sqlcompiler.gui;

import com.sqlcompiler.SQLCompiler;
import com.sqlcompiler.ast.Statement;
import com.sqlcompiler.ast.SelectStatement;
import com.sqlcompiler.ast.SelectListClause;
import com.sqlcompiler.ast.FromClause;
import com.sqlcompiler.ast.IdentifierExpression;
import com.sqlcompiler.ast.TableReference;
import com.sqlcompiler.exception.CompilationException;
import com.sqlcompiler.lexer.Position;

/**
 * AST结构测试程序
 * 验证改进后的AST层次结构
 */
public class ASTStructureTest {
    
    public static void main(String[] args) {
        System.out.println("=== AST层次结构测试 ===\n");
        
        // 测试简单的SELECT语句
        testSelectStatement("SELECT * FROM user");
        testSelectStatement("SELECT name, age FROM students WHERE age > 18");
        testSelectStatement("SELECT * FROM users ORDER BY name");
    }
    
    private static void testSelectStatement(String sql) {
        System.out.println("测试SQL: " + sql);
        System.out.println("----------------------------------------");
        
        try {
            // 解析SQL语句
            SQLCompiler compiler = new SQLCompiler();
            SQLCompiler.CompilationResult result = compiler.compile(sql);
            
            if (!result.isSuccess()) {
                System.out.println("❌ SQL编译失败: " + result.getErrors());
                return;
            }
            
            Statement ast = result.getStatement();
            
            if (ast instanceof SelectStatement) {
                SelectStatement selectStmt = (SelectStatement) ast;
                System.out.println("✅ 成功解析为SelectStatement");
                
                // 检查层次结构
                System.out.println("\n📊 AST层次结构分析:");
                System.out.println("第1层: SELECT (根节点)");
                
                // 检查SELECT_LIST
                if (selectStmt.getSelectList() != null && !selectStmt.getSelectList().isEmpty()) {
                    System.out.println("第2层: SELECT_LIST (中间层节点)");
                    System.out.println("第3层: " + selectStmt.getSelectList().size() + " 个表达式");
                    for (int i = 0; i < selectStmt.getSelectList().size(); i++) {
                        var expr = selectStmt.getSelectList().get(i);
                        if (expr instanceof IdentifierExpression) {
                            System.out.println("  - " + ((IdentifierExpression) expr).getName());
                        } else {
                            System.out.println("  - " + expr.getClass().getSimpleName());
                        }
                    }
                }
                
                // 检查FROM_CLAUSE
                if (selectStmt.getFromClause() != null && !selectStmt.getFromClause().isEmpty()) {
                    System.out.println("第2层: FROM_CLAUSE (中间层节点)");
                    System.out.println("第3层: " + selectStmt.getFromClause().size() + " 个表引用");
                    for (TableReference tableRef : selectStmt.getFromClause()) {
                        System.out.println("  - " + tableRef.getTableName());
                    }
                }
                
                // 检查其他子句
                if (selectStmt.getWhereClause() != null) {
                    System.out.println("第2层: WHERE (子句节点)");
                }
                if (selectStmt.getOrderByClause() != null) {
                    System.out.println("第2层: ORDER BY (子句节点)");
                }
                if (selectStmt.getGroupByClause() != null) {
                    System.out.println("第2层: GROUP BY (子句节点)");
                }
                if (selectStmt.getHavingClause() != null) {
                    System.out.println("第2层: HAVING (子句节点)");
                }
                if (selectStmt.getLimitClause() != null) {
                    System.out.println("第2层: LIMIT (子句节点)");
                }
                
                System.out.println("\n✅ 层次结构验证完成！");
                System.out.println("现在可以看到完整的3层结构：");
                System.out.println("  1. SELECT (根节点)");
                System.out.println("  2. SELECT_LIST, FROM_CLAUSE (中间层节点)");
                System.out.println("  3. 具体表达式和表名 (叶子节点)");
                
            } else {
                System.out.println("❌ 不是SelectStatement类型: " + ast.getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n" + "=".repeat(60) + "\n");
    }
}
