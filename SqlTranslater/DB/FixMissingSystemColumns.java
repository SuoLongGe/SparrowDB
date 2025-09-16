import java.io.*;
import java.util.*;

/**
 * 修复缺失的 __system_columns__.tbl 文件
 * 这个工具会为指定数据库重新生成缺失的系统列表
 */
public class FixMissingSystemColumns {
    
    public static void main(String[] args) {
        String databasePath = "../../data/column_expe";  // 目标数据库路径
        
        try {
            fixSystemColumnsTable(databasePath);
            System.out.println("✅ 成功修复 __system_columns__.tbl 文件");
        } catch (Exception e) {
            System.err.println("❌ 修复失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 修复系统列表
     */
    public static void fixSystemColumnsTable(String databasePath) throws IOException {
        File dbDir = new File(databasePath);
        if (!dbDir.exists() || !dbDir.isDirectory()) {
            throw new IllegalArgumentException("数据库目录不存在: " + databasePath);
        }
        
        // 1. 创建 __system_columns__.tbl 文件
        File systemColumnsFile = new File(dbDir, "__system_columns__.tbl");
        
        // 2. 写入表结构元数据
        try (PrintWriter writer = new PrintWriter(new FileWriter(systemColumnsFile))) {
            // 写入元数据头部
            writer.println("# Table Metadata");
            writer.println("TABLE_NAME=__system_columns__");
            writer.println("COLUMN_COUNT=9");
            writer.println("COLUMN=table_name:VARCHAR:255");
            writer.println("COLUMN=column_name:VARCHAR:255");
            writer.println("COLUMN=data_type:VARCHAR:50");
            writer.println("COLUMN=length:INT:4");
            writer.println("COLUMN=not_null:BOOLEAN:1");
            writer.println("COLUMN=primary_key:BOOLEAN:1");
            writer.println("COLUMN=unique:BOOLEAN:1");
            writer.println("COLUMN=default_value:VARCHAR:255");
            writer.println("COLUMN=auto_increment:BOOLEAN:1");
            writer.println("# End Metadata");
            writer.println();
            
            // 3. 扫描现有表文件并提取列信息
            Map<String, List<ColumnInfo>> tablesInfo = extractTableColumnInfo(dbDir);
            
            // 4. 写入数据页
            writer.println("PAGE:1");
            
            // 为每个表的每一列写入记录
            for (Map.Entry<String, List<ColumnInfo>> entry : tablesInfo.entrySet()) {
                String tableName = entry.getKey();
                List<ColumnInfo> columns = entry.getValue();
                
                for (ColumnInfo column : columns) {
                    // 按照现有格式写入记录
                    writer.println(String.format(
                        "auto_increment=%s|not_null=%s|unique=%s|column_name=%s|data_type=%s|length=%d|default_value=%s|table_name=%s|primary_key=%s",
                        column.autoIncrement ? "true" : "false",
                        column.notNull ? "true" : "false", 
                        column.unique ? "true" : "false",
                        column.name,
                        column.dataType,
                        column.length,
                        column.defaultValue != null ? column.defaultValue : "null",
                        tableName,
                        column.primaryKey ? "true" : "false"
                    ));
                }
            }
            
            writer.println(); // 空行结束
        }
        
        System.out.println("✅ 已创建 " + systemColumnsFile.getAbsolutePath());
        System.out.println("📊 处理了 " + extractTableColumnInfo(dbDir).size() + " 个表");
    }
    
    /**
     * 从数据库目录中提取所有表的列信息
     */
    private static Map<String, List<ColumnInfo>> extractTableColumnInfo(File dbDir) throws IOException {
        Map<String, List<ColumnInfo>> result = new HashMap<>();
        
        // 扫描所有 .tbl 文件
        File[] tblFiles = dbDir.listFiles((dir, name) -> 
            name.endsWith(".tbl") && !name.startsWith("__system_"));
            
        if (tblFiles != null) {
            for (File tblFile : tblFiles) {
                String tableName = tblFile.getName().replace(".tbl", "");
                List<ColumnInfo> columns = parseTableMetadata(tblFile);
                if (!columns.isEmpty()) {
                    result.put(tableName, columns);
                    System.out.println("📋 解析表: " + tableName + " (" + columns.size() + " 列)");
                }
            }
        }
        
        return result;
    }
    
    /**
     * 解析表文件的元数据部分提取列信息
     */
    private static List<ColumnInfo> parseTableMetadata(File tblFile) throws IOException {
        List<ColumnInfo> columns = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(tblFile))) {
            String line;
            boolean inMetadata = false;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.equals("# Table Metadata")) {
                    inMetadata = true;
                    continue;
                }
                
                if (line.equals("# End Metadata")) {
                    break;
                }
                
                if (inMetadata && line.startsWith("COLUMN=")) {
                    // 解析列定义: COLUMN=column_name:data_type:length
                    String columnDef = line.substring(7); // 去掉 "COLUMN="
                    String[] parts = columnDef.split(":");
                    
                    if (parts.length >= 2) {
                        String columnName = parts[0];
                        String dataType = parts[1];
                        int length = parts.length > 2 ? parseLength(parts[2]) : getDefaultLength(dataType);
                        
                        columns.add(new ColumnInfo(columnName, dataType, length));
                    }
                }
            }
        }
        
        return columns;
    }
    
    /**
     * 解析长度值
     */
    private static int parseLength(String lengthStr) {
        try {
            return Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 获取数据类型的默认长度
     */
    private static int getDefaultLength(String dataType) {
        switch (dataType.toUpperCase()) {
            case "INT": return 4;
            case "BIGINT": return 8;
            case "BOOLEAN": return 1;
            case "DATE": return 10;
            case "DECIMAL": return 10;
            case "VARCHAR": 
            default: return 50;
        }
    }
    
    /**
     * 列信息类
     */
    private static class ColumnInfo {
        String name;
        String dataType;
        int length;
        boolean notNull = false;
        boolean primaryKey = false;
        boolean unique = false;
        String defaultValue = null;
        boolean autoIncrement = false;
        
        public ColumnInfo(String name, String dataType, int length) {
            this.name = name;
            this.dataType = dataType;
            this.length = length;
        }
    }
}
