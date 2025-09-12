package com.sqlcompiler;

import com.sqlcompiler.ast.Statement;
import com.sqlcompiler.ast.ASTPrinter;
import com.sqlcompiler.catalog.Catalog;
import com.sqlcompiler.exception.CompilationException;
import com.sqlcompiler.execution.ExecutionPlan;
import com.sqlcompiler.execution.ExecutionPlanGenerator;
import com.sqlcompiler.lexer.LexicalAnalyzer;
import com.sqlcompiler.lexer.Token;
import com.sqlcompiler.parser.SyntaxAnalyzer;
import com.sqlcompiler.semantic.SemanticAnalyzer;
import com.sqlcompiler.semantic.SemanticAnalysisResult;

import java.util.List;

/**
 * SQL编译器主类
 * 整合词法分析、语法分析、语义分析和执行计划生成
 */
public class SQLCompiler {
    private final Catalog catalog;
    private final LexicalAnalyzer lexicalAnalyzer;
    private final SyntaxAnalyzer syntaxAnalyzer;
    private final SemanticAnalyzer semanticAnalyzer;
    private final ExecutionPlanGenerator executionPlanGenerator;
    
    public SQLCompiler() {
        this.catalog = new Catalog();
        this.lexicalAnalyzer = null; // 将在编译时创建
        this.syntaxAnalyzer = null; // 将在编译时创建
        this.semanticAnalyzer = new SemanticAnalyzer(catalog);
        this.executionPlanGenerator = new ExecutionPlanGenerator();
    }
    
    public SQLCompiler(Catalog catalog) {
        this.catalog = catalog;
        this.lexicalAnalyzer = null; // 将在编译时创建
        this.syntaxAnalyzer = null; // 将在编译时创建
        this.semanticAnalyzer = new SemanticAnalyzer(catalog);
        this.executionPlanGenerator = new ExecutionPlanGenerator();
    }
    
    /**
     * 编译SQL语句
     */
    public CompilationResult compile(String sql) {
        try {
            // 1. 词法分析
            System.out.println("=== 词法分析 ===");
            LexicalAnalyzer lexer = new LexicalAnalyzer(sql);
            List<Token> tokens = lexer.tokenize();
            
            System.out.println("Token列表:");
            for (Token token : tokens) {
                if (token.getType() != com.sqlcompiler.lexer.TokenType.EOF) {
                    System.out.println("  " + token.toString());
                }
            }
            
            // 2. 语法分析
            System.out.println("\n=== 语法分析 ===");
            SyntaxAnalyzer parser = new SyntaxAnalyzer(tokens);
            Statement statement = parser.parse();
            System.out.println("语法分析成功，生成AST");
            
            // 显示AST结构
            System.out.println("\n=== AST结构 ===");
            ASTPrinter astPrinter = new ASTPrinter();
            String astStructure = statement.accept(astPrinter);
            System.out.println(astStructure);
            
            // 3. 语义分析
            System.out.println("\n=== 语义分析 ===");
            SemanticAnalysisResult semanticResult = semanticAnalyzer.analyze(statement);
            System.out.println(semanticResult.toString());
            
            if (!semanticResult.isSuccess()) {
                return new CompilationResult(false, null, null, semanticResult.getErrors());
            }
            
            // 4. 执行计划生成
            System.out.println("\n=== 执行计划生成 ===");
            ExecutionPlan executionPlan = statement.accept(executionPlanGenerator);
            
            System.out.println("执行计划生成成功");
            System.out.println("树形结构:");
            System.out.println(executionPlan.toTreeString());
            
            System.out.println("\nJSON格式:");
            System.out.println(executionPlan.toJSON());
            
            System.out.println("\nS表达式格式:");
            System.out.println(executionPlan.toSExpression());
            
            return new CompilationResult(true, statement, executionPlan, null);
            
        } catch (CompilationException e) {
            System.err.println("编译错误: " + e.toString());
            return new CompilationResult(false, null, null, List.of(e.toString()));
        } catch (Exception e) {
            System.err.println("编译过程中发生未知错误: " + e.getMessage());
            e.printStackTrace();
            return new CompilationResult(false, null, null, List.of("未知错误: " + e.getMessage()));
        }
    }
    
    /**
     * 获取目录信息
     */
    public String getCatalogInfo() {
        return catalog.getSummary();
    }
    
    /**
     * 清空目录
     */
    public void clearCatalog() {
        catalog.clear();
    }
    
    /**
     * 编译结果类
     */
    public static class CompilationResult {
        private final boolean success;
        private final Statement statement;
        private final ExecutionPlan executionPlan;
        private final List<String> errors;
        
        public CompilationResult(boolean success, Statement statement, 
                               ExecutionPlan executionPlan, List<String> errors) {
            this.success = success;
            this.statement = statement;
            this.executionPlan = executionPlan;
            this.errors = errors;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public Statement getStatement() {
            return statement;
        }
        
        public ExecutionPlan getExecutionPlan() {
            return executionPlan;
        }
        
        public List<String> getErrors() {
            return errors;
        }
    }
    
    /**
     * 获取目录对象
     */
    public Catalog getCatalog() {
        return catalog;
    }
}
