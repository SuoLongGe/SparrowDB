package com.sqlcompiler.catalog;

import com.sqlcompiler.ast.SelectStatement;

/**
 * 视图信息
 */
public class ViewInfo {
    private final String name;
    private final SelectStatement selectStatement;
    private final String originalQuery;
    private final long createTime;
    
    public ViewInfo(String name, SelectStatement selectStatement, String originalQuery) {
        this.name = name;
        this.selectStatement = selectStatement;
        this.originalQuery = originalQuery;
        this.createTime = System.currentTimeMillis();
    }
    
    public String getName() {
        return name;
    }
    
    public SelectStatement getSelectStatement() {
        return selectStatement;
    }
    
    public String getOriginalQuery() {
        return originalQuery;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    @Override
    public String toString() {
        return String.format("View(name=%s, query=%s)", name, originalQuery);
    }
}
