package com.sqlcompiler.execution;

/**
 * DROP TABLE执行计划
 */
public class DropTablePlan extends ExecutionPlan {
    private final String tableName;
    private final boolean ifExists;
    
    public DropTablePlan(String tableName, boolean ifExists) {
        super("DROP_TABLE");
        this.tableName = tableName;
        this.ifExists = ifExists;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public boolean isIfExists() {
        return ifExists;
    }
    
    @Override
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DROP_TABLE\n");
        sb.append("├── 表名: ").append(tableName).append("\n");
        sb.append("└── IF EXISTS: ").append(ifExists);
        return sb.toString();
    }
    
    @Override
    public String toJSON() {
        return String.format("{\n" +
                "  \"type\": \"DROP_TABLE\",\n" +
                "  \"tableName\": \"%s\",\n" +
                "  \"ifExists\": %s\n" +
                "}", tableName, ifExists);
    }
    
    @Override
    public String toSExpression() {
        return String.format("(DROP_TABLE %s %s)", tableName, ifExists);
    }
}
