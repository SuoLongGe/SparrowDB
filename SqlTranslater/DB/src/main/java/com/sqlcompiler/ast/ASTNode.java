package com.sqlcompiler.ast;

import com.sqlcompiler.lexer.Position;

/**
 * 抽象语法树节点基类
 */
public abstract class ASTNode {
    protected Position position;
    
    public ASTNode(Position position) {
        this.position = position;
    }
    
    public Position getPosition() {
        return position;
    }
    
    public void setPosition(Position position) {
        this.position = position;
    }
    
    /**
     * 接受访问者模式
     */
    public abstract <T> T accept(ASTVisitor<T> visitor) throws com.sqlcompiler.exception.CompilationException;
}
