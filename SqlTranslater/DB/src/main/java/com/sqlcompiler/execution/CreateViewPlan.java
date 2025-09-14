package com.sqlcompiler.execution;

import com.sqlcompiler.ast.SelectStatement;

/**
 * CREATE VIEW执行计划
 */
public class CreateViewPlan extends ExecutionPlan {
    private final String viewName;
    private final SelectStatement selectStatement;
    private final String originalQuery;
    
    public CreateViewPlan(String viewName, SelectStatement selectStatement, String originalQuery) {
        super("CREATE_VIEW");
        this.viewName = viewName;
        this.selectStatement = selectStatement;
        this.originalQuery = originalQuery != null ? originalQuery : selectStatement.toString();
    }
    
    public String getViewName() {
        return viewName;
    }
    
    public SelectStatement getSelectStatement() {
        return selectStatement;
    }
    
    public String getOriginalQuery() {
        return originalQuery;
    }
    
    @Override
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"CREATE_VIEW\",\n");
        sb.append("  \"viewName\": \"").append(viewName).append("\",\n");
        sb.append("  \"originalQuery\": \"").append(originalQuery.replace("\"", "\\\"")).append("\"\n");
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(CREATE_VIEW \"").append(viewName).append("\" ");
        sb.append("\"").append(originalQuery.replace("\"", "\\\"")).append("\")");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("CreateViewPlan(viewName=%s, query=%s)", viewName, originalQuery);
    }
}
