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
 * 增强版SQL编译器
 * 专门为GUI界面设计，返回详细的编译信息而不直接打印到控制台
 */
public class EnhancedSQLCompiler {
    private final Catalog catalog;
    private final SemanticAnalyzer semanticAnalyzer;
    private final ExecutionPlanGenerator executionPlanGenerator;
    
    public EnhancedSQLCompiler() {
        this.catalog = new Catalog();
        this.semanticAnalyzer = new SemanticAnalyzer(catalog);
        this.executionPlanGenerator = new ExecutionPlanGenerator();
    }
    
    /**
     * 编译SQL语句并返回详细信息
     */
    public CompilationResult compile(String sql) {
        try {
            // 1. 词法分析
            LexicalAnalyzer lexer = new LexicalAnalyzer(sql);
            List<Token> tokens = lexer.tokenize();
            
            // 2. 语法分析
            SyntaxAnalyzer parser = new SyntaxAnalyzer(tokens);
            Statement statement = parser.parse();
            
            // 3. 语义分析
            SemanticAnalysisResult semanticResult = semanticAnalyzer.analyze(statement);
            
            if (!semanticResult.isSuccess()) {
                return new CompilationResult(false, tokens, null, null, null, semanticResult.getErrors());
            }
            
            // 4. 执行计划生成
            ExecutionPlan executionPlan = statement.accept(executionPlanGenerator);
            
            // 5. 生成AST字符串
            ASTPrinter astPrinter = new ASTPrinter();
            String astStructure = statement.accept(astPrinter);
            
            return new CompilationResult(true, tokens, statement, executionPlan, astStructure, null);
            
        } catch (CompilationException e) {
            return new CompilationResult(false, null, null, null, null, List.of(e.toString()));
        } catch (Exception e) {
            return new CompilationResult(false, null, null, null, null, List.of("未知错误: " + e.getMessage()));
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
     * 增强版编译结果类
     */
    public static class CompilationResult {
        private final boolean success;
        private final List<Token> tokens;
        private final Statement statement;
        private final ExecutionPlan executionPlan;
        private final String astStructure;
        private final List<String> errors;
        
        public CompilationResult(boolean success, List<Token> tokens, Statement statement, 
                               ExecutionPlan executionPlan, String astStructure, List<String> errors) {
            this.success = success;
            this.tokens = tokens;
            this.statement = statement;
            this.executionPlan = executionPlan;
            this.astStructure = astStructure;
            this.errors = errors;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public List<Token> getTokens() {
            return tokens;
        }
        
        public Statement getStatement() {
            return statement;
        }
        
        public ExecutionPlan getExecutionPlan() {
            return executionPlan;
        }
        
        public String getAstStructure() {
            return astStructure;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        /**
         * 获取格式化的Token列表字符串
         */
        public String getFormattedTokens() {
            if (tokens == null) {
                return "Token信息不可用";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== Token列表 ===\n");
            for (Token token : tokens) {
                if (token.getType() != com.sqlcompiler.lexer.TokenType.EOF) {
                    sb.append(String.format("%-15s %-20s %s\n", 
                        token.getType().toString(), 
                        token.getValue(), 
                        token.getPosition()));
                }
            }
            return sb.toString();
        }
        
        /**
         * 获取格式化的AST字符串
         */
        public String getFormattedAST() {
            if (astStructure == null) {
                return "AST信息不可用";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("=== AST结构 ===\n");
            sb.append(astStructure);
            
            if (executionPlan != null) {
                sb.append("\n\n=== 执行计划 ===\n");
                sb.append("树形结构:\n");
                sb.append(executionPlan.toTreeString());
                sb.append("\n\nJSON格式:\n");
                sb.append(executionPlan.toJSON());
                sb.append("\n\nS表达式格式:\n");
                sb.append(executionPlan.toSExpression());
            }
            
            return sb.toString();
        }
    }
}
