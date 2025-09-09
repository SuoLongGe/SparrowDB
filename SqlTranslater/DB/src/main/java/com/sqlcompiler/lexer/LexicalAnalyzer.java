package com.sqlcompiler.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL词法分析器
 * 负责将SQL源代码转换为Token序列
 */
public class LexicalAnalyzer {
    private final String source;
    private final List<Token> tokens;
    private int currentPos;
    private int currentLine;
    private int currentColumn;
    private final Map<String, TokenType> keywords;
    
    public LexicalAnalyzer(String source) {
        this.source = source;
        this.tokens = new ArrayList<>();
        this.currentPos = 0;
        this.currentLine = 1;
        this.currentColumn = 1;
        this.keywords = initializeKeywords();
    }
    
    /**
     * 初始化关键字映射表
     */
    private Map<String, TokenType> initializeKeywords() {
        Map<String, TokenType> keywordMap = new HashMap<>();
        
        // SQL关键字
        keywordMap.put("CREATE", TokenType.CREATE);
        keywordMap.put("TABLE", TokenType.TABLE);
        keywordMap.put("INSERT", TokenType.INSERT);
        keywordMap.put("INTO", TokenType.INTO);
        keywordMap.put("VALUES", TokenType.VALUES);
        keywordMap.put("SELECT", TokenType.SELECT);
        keywordMap.put("FROM", TokenType.FROM);
        keywordMap.put("WHERE", TokenType.WHERE);
        keywordMap.put("DELETE", TokenType.DELETE);
        keywordMap.put("DROP", TokenType.DROP);
        keywordMap.put("ALTER", TokenType.ALTER);
        keywordMap.put("UPDATE", TokenType.UPDATE);
        keywordMap.put("SET", TokenType.SET);
        keywordMap.put("AND", TokenType.AND);
        keywordMap.put("OR", TokenType.OR);
        keywordMap.put("NOT", TokenType.NOT);
        keywordMap.put("AS", TokenType.AS);
        keywordMap.put("DISTINCT", TokenType.DISTINCT);
        keywordMap.put("ORDER", TokenType.ORDER);
        keywordMap.put("BY", TokenType.BY);
        keywordMap.put("GROUP", TokenType.GROUP);
        keywordMap.put("HAVING", TokenType.HAVING);
        keywordMap.put("LIMIT", TokenType.LIMIT);
        keywordMap.put("OFFSET", TokenType.OFFSET);
        keywordMap.put("JOIN", TokenType.JOIN);
        keywordMap.put("INNER", TokenType.INNER);
        keywordMap.put("LEFT", TokenType.LEFT);
        keywordMap.put("RIGHT", TokenType.RIGHT);
        keywordMap.put("OUTER", TokenType.OUTER);
        keywordMap.put("ON", TokenType.ON);
        keywordMap.put("IS", TokenType.IS);
        keywordMap.put("NULL", TokenType.NULL);
        keywordMap.put("TRUE", TokenType.TRUE);
        keywordMap.put("FALSE", TokenType.FALSE);
        
        // 数据类型
        keywordMap.put("INT", TokenType.INT);
        keywordMap.put("INTEGER", TokenType.INTEGER);
        keywordMap.put("VARCHAR", TokenType.VARCHAR);
        keywordMap.put("CHAR", TokenType.CHAR);
        keywordMap.put("TEXT", TokenType.TEXT);
        keywordMap.put("DECIMAL", TokenType.DECIMAL);
        keywordMap.put("FLOAT", TokenType.FLOAT);
        keywordMap.put("DOUBLE", TokenType.DOUBLE);
        keywordMap.put("BOOLEAN", TokenType.BOOLEAN);
        keywordMap.put("DATE", TokenType.DATE);
        keywordMap.put("TIME", TokenType.TIME);
        keywordMap.put("TIMESTAMP", TokenType.TIMESTAMP);
        
        // 约束关键字
        keywordMap.put("PRIMARY", TokenType.PRIMARY);
        keywordMap.put("KEY", TokenType.KEY);
        keywordMap.put("FOREIGN", TokenType.FOREIGN);
        keywordMap.put("REFERENCES", TokenType.REFERENCES);
        keywordMap.put("UNIQUE", TokenType.UNIQUE);
        keywordMap.put("DEFAULT", TokenType.DEFAULT);
        keywordMap.put("AUTO_INCREMENT", TokenType.AUTO_INCREMENT);
        
        // 运算符关键字
        keywordMap.put("LIKE", TokenType.LIKE);
        keywordMap.put("IN", TokenType.IN);
        keywordMap.put("BETWEEN", TokenType.BETWEEN);
        
        return keywordMap;
    }
    
    /**
     * 执行词法分析，返回Token列表
     */
    public List<Token> tokenize() {
        tokens.clear();
        currentPos = 0;
        currentLine = 1;
        currentColumn = 1;
        
        while (currentPos < source.length()) {
            char currentChar = source.charAt(currentPos);
            
            if (Character.isWhitespace(currentChar)) {
                skipWhitespace();
            } else if (Character.isLetter(currentChar) || currentChar == '_') {
                readIdentifierOrKeyword();
            } else if (Character.isDigit(currentChar)) {
                readNumber();
            } else if (currentChar == '\'' || currentChar == '"') {
                readString(currentChar);
            } else if (currentChar == '`') {
                readBacktickIdentifier();
            } else {
                readOperatorOrDelimiter();
            }
        }
        
        // 添加EOF token
        tokens.add(new Token(TokenType.EOF, "", new Position(currentLine, currentColumn)));
        
        return tokens;
    }
    
    /**
     * 跳过空白字符
     */
    private void skipWhitespace() {
        while (currentPos < source.length() && Character.isWhitespace(source.charAt(currentPos))) {
            if (source.charAt(currentPos) == '\n') {
                currentLine++;
                currentColumn = 1;
            } else {
                currentColumn++;
            }
            currentPos++;
        }
    }
    
    /**
     * 读取标识符或关键字
     */
    private void readIdentifierOrKeyword() {
        int startPos = currentPos;
        int startLine = currentLine;
        int startColumn = currentColumn;
        
        while (currentPos < source.length() && 
               (Character.isLetterOrDigit(source.charAt(currentPos)) || 
                source.charAt(currentPos) == '_')) {
            currentPos++;
            currentColumn++;
        }
        
        String value = source.substring(startPos, currentPos);
        TokenType type = keywords.get(value.toUpperCase());
        
        if (type != null) {
            // 处理特殊关键字
            if (value.toUpperCase().equals("NOT") && 
                currentPos < source.length() && 
                source.substring(currentPos).trim().toUpperCase().startsWith("NULL")) {
                // 处理 NOT NULL
                skipWhitespace();
                if (currentPos + 4 <= source.length() && 
                    source.substring(currentPos, currentPos + 4).toUpperCase().equals("NULL")) {
                    currentPos += 4;
                    currentColumn += 4;
                    tokens.add(new Token(TokenType.NOT_NULL, "NOT NULL", 
                                       new Position(startLine, startColumn)));
                    return;
                }
            }
            tokens.add(new Token(type, value, new Position(startLine, startColumn)));
        } else {
            tokens.add(new Token(TokenType.IDENTIFIER, value, new Position(startLine, startColumn)));
        }
    }
    
    /**
     * 读取数字字面量
     */
    private void readNumber() {
        int startPos = currentPos;
        int startLine = currentLine;
        int startColumn = currentColumn;
        
        while (currentPos < source.length() && Character.isDigit(source.charAt(currentPos))) {
            currentPos++;
            currentColumn++;
        }
        
        // 检查是否有小数点
        if (currentPos < source.length() && source.charAt(currentPos) == '.') {
            currentPos++;
            currentColumn++;
            while (currentPos < source.length() && Character.isDigit(source.charAt(currentPos))) {
                currentPos++;
                currentColumn++;
            }
        }
        
        String value = source.substring(startPos, currentPos);
        tokens.add(new Token(TokenType.NUMBER_LITERAL, value, new Position(startLine, startColumn)));
    }
    
    /**
     * 读取字符串字面量
     */
    private void readString(char quote) {
        int startLine = currentLine;
        int startColumn = currentColumn;
        currentPos++; // 跳过开始引号
        currentColumn++;
        
        StringBuilder value = new StringBuilder();
        
        while (currentPos < source.length()) {
            char currentChar = source.charAt(currentPos);
            
            if (currentChar == quote) {
                currentPos++;
                currentColumn++;
                break;
            } else if (currentChar == '\\' && currentPos + 1 < source.length()) {
                // 处理转义字符
                currentPos++;
                currentColumn++;
                char nextChar = source.charAt(currentPos);
                switch (nextChar) {
                    case 'n': value.append('\n'); break;
                    case 't': value.append('\t'); break;
                    case 'r': value.append('\r'); break;
                    case '\\': value.append('\\'); break;
                    case '\'': value.append('\''); break;
                    case '"': value.append('"'); break;
                    default: value.append(nextChar); break;
                }
                currentPos++;
                currentColumn++;
            } else {
                value.append(currentChar);
                currentPos++;
                currentColumn++;
            }
        }
        
        tokens.add(new Token(TokenType.STRING_LITERAL, value.toString(), 
                           new Position(startLine, startColumn)));
    }
    
    /**
     * 读取反引号标识符
     */
    private void readBacktickIdentifier() {
        int startLine = currentLine;
        int startColumn = currentColumn;
        currentPos++; // 跳过开始反引号
        currentColumn++;
        
        StringBuilder value = new StringBuilder();
        
        while (currentPos < source.length() && source.charAt(currentPos) != '`') {
            value.append(source.charAt(currentPos));
            currentPos++;
            currentColumn++;
        }
        
        if (currentPos < source.length()) {
            currentPos++; // 跳过结束反引号
            currentColumn++;
        }
        
        tokens.add(new Token(TokenType.IDENTIFIER, value.toString(), 
                           new Position(startLine, startColumn)));
    }
    
    /**
     * 读取运算符或分隔符
     */
    private void readOperatorOrDelimiter() {
        char currentChar = source.charAt(currentPos);
        int startLine = currentLine;
        int startColumn = currentColumn;
        
        TokenType type = null;
        String value = String.valueOf(currentChar);
        
        // 检查双字符运算符
        if (currentPos + 1 < source.length()) {
            char nextChar = source.charAt(currentPos + 1);
            String twoChar = currentChar + String.valueOf(nextChar);
            
            switch (twoChar) {
                case "!=": type = TokenType.NOT_EQUALS; break;
                case "<=": type = TokenType.LESS_EQUAL; break;
                case ">=": type = TokenType.GREATER_EQUAL; break;
            }
        }
        
        if (type != null) {
            currentPos += 2;
            currentColumn += 2;
            tokens.add(new Token(type, value + source.charAt(currentPos - 1), 
                               new Position(startLine, startColumn)));
        } else {
            // 单字符运算符或分隔符
            switch (currentChar) {
                case '=': type = TokenType.EQUALS; break;
                case '<': type = TokenType.LESS_THAN; break;
                case '>': type = TokenType.GREATER_THAN; break;
                case '+': type = TokenType.PLUS; break;
                case '-': type = TokenType.MINUS; break;
                case '*': type = TokenType.MULTIPLY; break;
                case '/': type = TokenType.DIVIDE; break;
                case '%': type = TokenType.MODULO; break;
                case ';': type = TokenType.SEMICOLON; break;
                case ',': type = TokenType.COMMA; break;
                case '.': type = TokenType.DOT; break;
                case '(': type = TokenType.LEFT_PAREN; break;
                case ')': type = TokenType.RIGHT_PAREN; break;
                case '[': type = TokenType.LEFT_BRACKET; break;
                case ']': type = TokenType.RIGHT_BRACKET; break;
                case '{': type = TokenType.LEFT_BRACE; break;
                case '}': type = TokenType.RIGHT_BRACE; break;
                default:
                    type = TokenType.UNKNOWN;
                    break;
            }
            
            currentPos++;
            currentColumn++;
            tokens.add(new Token(type, value, new Position(startLine, startColumn)));
        }
    }
    
    /**
     * 获取Token列表
     */
    public List<Token> getTokens() {
        return tokens;
    }
}
