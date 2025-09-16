package com.sqlcompiler.execution;

/**
 * DROP VIEW执行计划
 */
public class DropViewPlan extends ExecutionPlan {
    private final String viewName;
    private final boolean ifExists;
    
    public DropViewPlan(String viewName, boolean ifExists) {
        super("DROP_VIEW");
        this.viewName = viewName;
        this.ifExists = ifExists;
    }
    
    public String getViewName() {
        return viewName;
    }
    
    public boolean isIfExists() {
        return ifExists;
    }
    
    @Override
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"DROP_VIEW\",\n");
        sb.append("  \"viewName\": \"").append(viewName).append("\",\n");
        sb.append("  \"ifExists\": ").append(ifExists).append("\n");
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public String toSExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append("(DROP_VIEW \"").append(viewName).append("\"");
        if (ifExists) {
            sb.append(" IF_EXISTS");
        }
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("DropViewPlan(viewName=%s, ifExists=%s)", viewName, ifExists);
    }
}
