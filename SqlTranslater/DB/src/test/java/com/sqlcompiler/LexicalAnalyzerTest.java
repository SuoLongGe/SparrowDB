package com.sqlcompiler;

import com.sqlcompiler.lexer.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * 词法分析器测试类
 */
public class LexicalAnalyzerTest {
    
    @Test
    public void testBasicTokens() {
        String sql = "SELECT * FROM users WHERE id = 1;";
        LexicalAnalyzer analyzer = new LexicalAnalyzer(sql);
        List<Token> tokens = analyzer.tokenize();
        
        assertFalse(tokens.isEmpty(), "应该生成token");
        
        // 检查第一个token
        Token firstToken = tokens.get(0);
        assertEquals(TokenType.SELECT, firstToken.getType());
        assertEquals("SELECT", firstToken.getValue());
        assertEquals(1, firstToken.getLine());
        assertEquals(1, firstToken.getColumn());
    }
    
    @Test
    public void testKeywords() {
        String sql = "CREATE TABLE INSERT INTO VALUES SELECT FROM WHERE DELETE";
        LexicalAnalyzer analyzer = new LexicalAnalyzer(sql);
        List<Token> tokens = analyzer.tokenize();
        
        assertEquals(TokenType.CREATE, tokens.get(0).getType());
        assertEquals(TokenType.TABLE, tokens.get(1).getType());
        assertEquals(TokenType.INSERT, tokens.get(2).getType());
        assertEquals(TokenType.INTO, tokens.get(3).getType());
        assertEquals(TokenType.VALUES, tokens.get(4).getType());
        assertEquals(TokenType.SELECT, tokens.get(5).getType());
        assertEquals(TokenType.FROM, tokens.get(6).getType());
        assertEquals(TokenType.WHERE, tokens.get(7).getType());
        assertEquals(TokenType.DELETE, tokens.get(8).getType());
    }
    
    @Test
    public void testIdentifiers() {
        String sql = "table_name column_name _underscore";
        LexicalAnalyzer analyzer = new LexicalAnalyzer(sql);
        List<Token> tokens = analyzer.tokenize();
        
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
        assertEquals("table_name", tokens.get(0).getValue());
        
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType());
        assertEquals("column_name", tokens.get(1).getValue());
        
        assertEquals(TokenType.IDENTIFIER, tokens.get(2).getType());
        assertEquals("_underscore", tokens.get(2).getValue());
    }
    
    @Test
    public void testStringLiterals() {
        String sql = "'hello world' \"double quotes\" 'escaped\\'quote'";
        LexicalAnalyzer analyzer = new LexicalAnalyzer(sql);
        List<Token> tokens = analyzer.tokenize();
        
        assertEquals(TokenType.STRING_LITERAL, tokens.get(0).getType());
        assertEquals("hello world", tokens.get(0).getValue());
        
        assertEquals(TokenType.STRING_LITERAL, tokens.get(1).getType());
        assertEquals("double quotes", tokens.get(1).getValue());
        
        assertEquals(TokenType.STRING_LITERAL, tokens.get(2).getType());
        assertEquals("escaped'quote", tokens.get(2).getValue());
    }
    
    @Test
    public void testNumberLiterals() {
        String sql = "123 45.67 0.5";
        LexicalAnalyzer analyzer = new LexicalAnalyzer(sql);
        List<Token> tokens = analyzer.tokenize();
        
        assertEquals(TokenType.NUMBER_LITERAL, tokens.get(0).getType());
        assertEquals("123", tokens.get(0).getValue());
        
        assertEquals(TokenType.NUMBER_LITERAL, tokens.get(1).getType());
        assertEquals("45.67", tokens.get(1).getValue());
        
        assertEquals(TokenType.NUMBER_LITERAL, tokens.get(2).getType());
        assertEquals("0.5", tokens.get(2).getValue());
    }
    
    @Test
    public void testOperators() {
        String sql = "= != < > <= >= + - * / %";
        LexicalAnalyzer analyzer = new LexicalAnalyzer(sql);
        List<Token> tokens = analyzer.tokenize();
        
        assertEquals(TokenType.EQUALS, tokens.get(0).getType());
        assertEquals(TokenType.NOT_EQUALS, tokens.get(1).getType());
        assertEquals(TokenType.LESS_THAN, tokens.get(2).getType());
        assertEquals(TokenType.GREATER_THAN, tokens.get(3).getType());
        assertEquals(TokenType.LESS_EQUAL, tokens.get(4).getType());
        assertEquals(TokenType.GREATER_EQUAL, tokens.get(5).getType());
        assertEquals(TokenType.PLUS, tokens.get(6).getType());
        assertEquals(TokenType.MINUS, tokens.get(7).getType());
        assertEquals(TokenType.MULTIPLY, tokens.get(8).getType());
        assertEquals(TokenType.DIVIDE, tokens.get(9).getType());
        assertEquals(TokenType.MODULO, tokens.get(10).getType());
    }
    
    @Test
    public void testDelimiters() {
        String sql = "();,.";
        LexicalAnalyzer analyzer = new LexicalAnalyzer(sql);
        List<Token> tokens = analyzer.tokenize();
        
        assertEquals(TokenType.LEFT_PAREN, tokens.get(0).getType());
        assertEquals(TokenType.RIGHT_PAREN, tokens.get(1).getType());
        assertEquals(TokenType.SEMICOLON, tokens.get(2).getType());
        assertEquals(TokenType.COMMA, tokens.get(3).getType());
        assertEquals(TokenType.DOT, tokens.get(4).getType());
    }
    
    @Test
    public void testPositionTracking() {
        String sql = "SELECT\n  id,\n  name\nFROM users;";
        LexicalAnalyzer analyzer = new LexicalAnalyzer(sql);
        List<Token> tokens = analyzer.tokenize();
        
        // SELECT在第一行第一列
        assertEquals(1, tokens.get(0).getLine());
        assertEquals(1, tokens.get(0).getColumn());
        
        // id在第二行第三列
        assertEquals(2, tokens.get(1).getLine());
        assertEquals(3, tokens.get(1).getColumn());
        
        // name在第三行第三列
        assertEquals(3, tokens.get(3).getLine());
        assertEquals(3, tokens.get(3).getColumn());
        
        // FROM在第四行第一列
        assertEquals(4, tokens.get(5).getLine());
        assertEquals(1, tokens.get(5).getColumn());
    }
}
