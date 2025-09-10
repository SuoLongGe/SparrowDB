package com.sqlcompiler.lexer;

/**
 * 表示Token在源代码中的位置信息
 * 包含行号和列号
 */
public class Position {
    private final int line;
    private final int column;
    
    public Position(int line, int column) {
        this.line = line;
        this.column = column;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    @Override
    public String toString() {
        return "(" + line + ", " + column + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position position = (Position) obj;
        return line == position.line && column == position.column;
    }
    
    @Override
    public int hashCode() {
        return line * 31 + column;
    }
}
