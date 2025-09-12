package com.sqlcompiler.gui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * SQL语法高亮组件
 * 为SQL语句提供语法高亮功能，包括关键字、字符串、数字、注释等
 */
public class SQLSyntaxHighlighter {
    
    // 语法高亮样式
    public static class SyntaxStyle {
        private final Color color;
        private final boolean bold;
        private final boolean italic;
        
        public SyntaxStyle(Color color, boolean bold, boolean italic) {
            this.color = color;
            this.bold = bold;
            this.italic = italic;
        }
        
        public Color getColor() { return color; }
        public boolean isBold() { return bold; }
        public boolean isItalic() { return italic; }
    }
    
    // 预定义的语法样式
    public static final SyntaxStyle KEYWORD_STYLE = new SyntaxStyle(new Color(0, 0, 255), true, false);      // 蓝色粗体
    public static final SyntaxStyle STRING_STYLE = new SyntaxStyle(new Color(0, 128, 0), false, false);      // 绿色
    public static final SyntaxStyle NUMBER_STYLE = new SyntaxStyle(new Color(255, 0, 0), false, false);      // 红色
    public static final SyntaxStyle COMMENT_STYLE = new SyntaxStyle(new Color(128, 128, 128), false, true);  // 灰色斜体
    public static final SyntaxStyle OPERATOR_STYLE = new SyntaxStyle(new Color(128, 0, 128), false, false);  // 紫色
    public static final SyntaxStyle FUNCTION_STYLE = new SyntaxStyle(new Color(0, 128, 128), false, false);  // 青色
    public static final SyntaxStyle DEFAULT_STYLE = new SyntaxStyle(Color.BLACK, false, false);              // 黑色
    
    // SQL关键字
    private static final Set<String> SQL_KEYWORDS = new HashSet<>(Arrays.asList(
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
        "CREATE", "TABLE", "DROP", "ALTER", "INDEX", "VIEW", "DATABASE", "SCHEMA",
        "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE", "IS", "NULL",
        "ORDER", "BY", "GROUP", "HAVING", "LIMIT", "OFFSET", "DISTINCT",
        "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON", "AS",
        "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "DEFAULT", "AUTO_INCREMENT",
        "INT", "INTEGER", "VARCHAR", "CHAR", "TEXT", "DECIMAL", "FLOAT", "DOUBLE", "BOOLEAN",
        "DATE", "TIME", "TIMESTAMP", "DATETIME",
        "TRUE", "FALSE", "ASC", "DESC", "COUNT", "SUM", "AVG", "MAX", "MIN"
    ));
    
    // SQL函数
    private static final Set<String> SQL_FUNCTIONS = new HashSet<>(Arrays.asList(
        "COUNT", "SUM", "AVG", "MAX", "MIN", "UPPER", "LOWER", "LENGTH", "SUBSTRING",
        "CONCAT", "TRIM", "REPLACE", "ROUND", "FLOOR", "CEIL", "ABS", "MOD"
    ));
    
    // 正则表达式模式
    private static final Pattern STRING_PATTERN = Pattern.compile("'([^'\\\\]|\\\\.)*'|\"([^\"\\\\]|\\\\.)*\"");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\.?\\d*\\b");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("--.*$|/\\*.*?\\*/", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern OPERATOR_PATTERN = Pattern.compile("[=<>!]+|[+\\-*/%]|\\b(AND|OR|NOT)\\b");
    
    private final JTextPane textPane;
    private final StyledDocument document;
    private final StyleContext styleContext;
    private javax.swing.Timer highlightTimer;
    private boolean isHighlighting = false;
    private boolean syntaxHighlightingEnabled = true;
    
    public SQLSyntaxHighlighter(JTextPane textPane) {
        this.textPane = textPane;
        this.document = textPane.getStyledDocument();
        this.styleContext = new StyleContext();
        
        setupStyles();
        setupDocumentListener();
        setupHighlightTimer();
    }
    
    /**
     * 设置语法高亮样式
     */
    private void setupStyles() {
        // 关键字样式
        Style keywordStyle = styleContext.addStyle("keyword", null);
        StyleConstants.setForeground(keywordStyle, KEYWORD_STYLE.getColor());
        StyleConstants.setBold(keywordStyle, KEYWORD_STYLE.isBold());
        StyleConstants.setItalic(keywordStyle, KEYWORD_STYLE.isItalic());
        
        // 字符串样式
        Style stringStyle = styleContext.addStyle("string", null);
        StyleConstants.setForeground(stringStyle, STRING_STYLE.getColor());
        StyleConstants.setBold(stringStyle, STRING_STYLE.isBold());
        StyleConstants.setItalic(stringStyle, STRING_STYLE.isItalic());
        
        // 数字样式
        Style numberStyle = styleContext.addStyle("number", null);
        StyleConstants.setForeground(numberStyle, NUMBER_STYLE.getColor());
        StyleConstants.setBold(numberStyle, NUMBER_STYLE.isBold());
        StyleConstants.setItalic(numberStyle, NUMBER_STYLE.isItalic());
        
        // 注释样式
        Style commentStyle = styleContext.addStyle("comment", null);
        StyleConstants.setForeground(commentStyle, COMMENT_STYLE.getColor());
        StyleConstants.setBold(commentStyle, COMMENT_STYLE.isBold());
        StyleConstants.setItalic(commentStyle, COMMENT_STYLE.isItalic());
        
        // 运算符样式
        Style operatorStyle = styleContext.addStyle("operator", null);
        StyleConstants.setForeground(operatorStyle, OPERATOR_STYLE.getColor());
        StyleConstants.setBold(operatorStyle, OPERATOR_STYLE.isBold());
        StyleConstants.setItalic(operatorStyle, OPERATOR_STYLE.isItalic());
        
        // 函数样式
        Style functionStyle = styleContext.addStyle("function", null);
        StyleConstants.setForeground(functionStyle, FUNCTION_STYLE.getColor());
        StyleConstants.setBold(functionStyle, FUNCTION_STYLE.isBold());
        StyleConstants.setItalic(functionStyle, FUNCTION_STYLE.isItalic());
        
        // 默认样式
        Style defaultStyle = styleContext.addStyle("default", null);
        StyleConstants.setForeground(defaultStyle, DEFAULT_STYLE.getColor());
        StyleConstants.setBold(defaultStyle, DEFAULT_STYLE.isBold());
        StyleConstants.setItalic(defaultStyle, DEFAULT_STYLE.isItalic());
    }
    
    /**
     * 设置文档监听器
     */
    private void setupDocumentListener() {
        document.addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                scheduleHighlighting();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                scheduleHighlighting();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                scheduleHighlighting();
            }
        });
    }
    
    /**
     * 设置高亮定时器
     */
    private void setupHighlightTimer() {
        highlightTimer = new javax.swing.Timer(300, e -> {
            if (!isHighlighting) {
                highlightSyntax();
            }
        });
        highlightTimer.setRepeats(false);
    }
    
    /**
     * 调度语法高亮
     */
    private void scheduleHighlighting() {
        if (!syntaxHighlightingEnabled) return;
        
        if (highlightTimer.isRunning()) {
            highlightTimer.stop();
        }
        highlightTimer.start();
    }
    
    /**
     * 执行语法高亮
     */
    public void highlightSyntax() {
        if (isHighlighting || !syntaxHighlightingEnabled) return;
        
        isHighlighting = true;
        try {
            String text = document.getText(0, document.getLength());
            if (text.isEmpty()) {
                isHighlighting = false;
                return;
            }
            
            // 限制文本长度，避免处理过长的文档
            if (text.length() > 10000) {
                isHighlighting = false;
                return;
            }
            
            // 在后台线程中执行高亮
            SwingUtilities.invokeLater(() -> {
                try {
                    // 清除所有样式
                    document.setCharacterAttributes(0, document.getLength(), 
                        styleContext.getStyle("default"), true);
                    
                    // 应用语法高亮
                    highlightComments(text);
                    highlightStrings(text);
                    highlightNumbers(text);
                    highlightKeywords(text);
                    highlightFunctions(text);
                    highlightOperators(text);
                    
                } catch (BadLocationException e) {
                    e.printStackTrace();
                } finally {
                    isHighlighting = false;
                }
            });
            
        } catch (BadLocationException e) {
            e.printStackTrace();
            isHighlighting = false;
        }
    }
    
    /**
     * 高亮注释
     */
    private void highlightComments(String text) throws BadLocationException {
        java.util.regex.Matcher matcher = COMMENT_PATTERN.matcher(text);
        while (matcher.find()) {
            document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(),
                styleContext.getStyle("comment"), true);
        }
    }
    
    /**
     * 高亮字符串
     */
    private void highlightStrings(String text) throws BadLocationException {
        java.util.regex.Matcher matcher = STRING_PATTERN.matcher(text);
        while (matcher.find()) {
            document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(),
                styleContext.getStyle("string"), true);
        }
    }
    
    /**
     * 高亮数字
     */
    private void highlightNumbers(String text) throws BadLocationException {
        java.util.regex.Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(),
                styleContext.getStyle("number"), true);
        }
    }
    
    /**
     * 高亮关键字
     */
    private void highlightKeywords(String text) throws BadLocationException {
        // 使用正则表达式一次性匹配所有关键字
        String keywordPattern = "\\b(" + String.join("|", SQL_KEYWORDS) + ")\\b";
        Pattern pattern = Pattern.compile(keywordPattern, Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(),
                styleContext.getStyle("keyword"), true);
        }
    }
    
    /**
     * 高亮函数
     */
    private void highlightFunctions(String text) throws BadLocationException {
        // 使用正则表达式匹配函数调用
        String functionPattern = "\\b(" + String.join("|", SQL_FUNCTIONS) + ")\\s*\\(";
        Pattern pattern = Pattern.compile(functionPattern, Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start() - 1,
                styleContext.getStyle("function"), true);
        }
    }
    
    /**
     * 高亮运算符
     */
    private void highlightOperators(String text) throws BadLocationException {
        java.util.regex.Matcher matcher = OPERATOR_PATTERN.matcher(text);
        while (matcher.find()) {
            document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(),
                styleContext.getStyle("operator"), true);
        }
    }
    
    /**
     * 手动触发语法高亮
     */
    public void refreshHighlighting() {
        highlightSyntax();
    }
    
    /**
     * 获取样式上下文
     */
    public StyleContext getStyleContext() {
        return styleContext;
    }
    
    /**
     * 启用或禁用语法高亮
     */
    public void setSyntaxHighlightingEnabled(boolean enabled) {
        this.syntaxHighlightingEnabled = enabled;
        if (!enabled) {
            // 如果禁用语法高亮，清除所有样式
            SwingUtilities.invokeLater(() -> {
                try {
                    int length = document.getLength();
                    if (length > 0) {
                        document.setCharacterAttributes(0, length, 
                            styleContext.getStyle("default"), true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            // 如果启用语法高亮，重新应用高亮
            highlightSyntax();
        }
    }
    
    /**
     * 检查语法高亮是否启用
     */
    public boolean isSyntaxHighlightingEnabled() {
        return syntaxHighlightingEnabled;
    }
}
