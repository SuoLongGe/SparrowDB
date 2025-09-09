package com.sqlcompiler.lexer;

/**
 * 表示词法分析器识别出的Token
 * 包含种别码、词素值、位置信息
 */
public class Token {
    private final TokenType type;
    private final String value;
    private final Position position;
    
    public Token(TokenType type, String value, Position position) {
        this.type = type;
        this.value = value;
        this.position = position;
    }
    
    public TokenType getType() {
        return type;
    }
    
    public String getValue() {
        return value;
    }
    
    public Position getPosition() {
        return position;
    }
    
    /**
     * 获取种别码（Token类型的字符串表示）
     */
    public String getCategoryCode() {
        return type.getValue();
    }
    
    /**
     * 获取词素值
     */
    public String getLexeme() {
        return value;
    }
    
    /**
     * 获取行号
     */
    public int getLine() {
        return position.getLine();
    }
    
    /**
     * 获取列号
     */
    public int getColumn() {
        return position.getColumn();
    }
    
    @Override
    public String toString() {
        return String.format("[%s, %s, %d, %d]", 
                           getCategoryCode(), getLexeme(), getLine(), getColumn());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Token token = (Token) obj;
        return type == token.type && 
               value.equals(token.value) && 
               position.equals(token.position);
    }
    
    @Override
    public int hashCode() {
        return type.hashCode() * 31 + value.hashCode() * 17 + position.hashCode();
    }
}
