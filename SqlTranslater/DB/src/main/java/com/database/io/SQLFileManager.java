package com.database.io;

import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import com.database.engine.CatalogManager;
import com.sqlcompiler.catalog.TableInfo;
import com.sqlcompiler.catalog.ColumnInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * SQL文件管理器 - 负责SQL文件的导入、执行和转储功能
 */
public class SQLFileManager {
    private final DatabaseEngine databaseEngine;
    private final CatalogManager catalogManager;
    
    // SQL语句分隔符模式
    private static final Pattern SQL_DELIMITER_PATTERN = Pattern.compile(";\\s*(?:\\r?\\n|$)");
    
    public SQLFileManager(DatabaseEngine databaseEngine, CatalogManager catalogManager) {
        this.databaseEngine = databaseEngine;
        this.catalogManager = catalogManager;
    }
    
    /**
     * 导入并执行SQL文件
     * @param filePath SQL文件路径
     * @return 执行结果
     */
    public ExecutionResult importAndExecuteSQLFile(String filePath) {
        return importAndExecuteSQLFile(filePath, false);
    }
    
    /**
     * 导入并执行SQL文件
     * @param filePath SQL文件路径
     * @param continueOnError 是否在遇到错误时继续执行
     * @return 执行结果
     */
    public ExecutionResult importAndExecuteSQLFile(String filePath, boolean continueOnError) {
        try {
            System.out.println("=== 开始导入SQL文件: " + filePath + " ===");
            
            // 检查文件是否存在
            File file = new File(filePath);
            if (!file.exists()) {
                return new ExecutionResult(false, "SQL文件不存在: " + filePath, null);
            }
            
            // 读取文件内容
            String sqlContent = readSQLFile(filePath);
            if (sqlContent.trim().isEmpty()) {
                return new ExecutionResult(false, "SQL文件为空", null);
            }
            
            // 解析SQL语句
            List<String> sqlStatements = parseSQLStatements(sqlContent);
            if (sqlStatements.isEmpty()) {
                return new ExecutionResult(false, "未找到有效的SQL语句", null);
            }
            
            System.out.println("解析到 " + sqlStatements.size() + " 条SQL语句");
            
            // 执行SQL语句
            return executeSQLStatements(sqlStatements, continueOnError);
            
        } catch (Exception e) {
            return new ExecutionResult(false, "导入SQL文件失败: " + e.getMessage(), null);
        }
    }
    
    /**
     * 导出数据库为SQL文件
     * @param outputPath 输出文件路径
     * @return 执行结果
     */
    public ExecutionResult exportDatabaseToSQL(String outputPath) {
        return exportDatabaseToSQL(outputPath, null, true, true);
    }
    
    /**
     * 导出数据库为SQL文件
     * @param outputPath 输出文件路径
     * @param tableNames 要导出的表名列表，null表示导出所有表
     * @param includeStructure 是否包含表结构
     * @param includeData 是否包含数据
     * @return 执行结果
     */
    public ExecutionResult exportDatabaseToSQL(String outputPath, List<String> tableNames, 
                                              boolean includeStructure, boolean includeData) {
        try {
            System.out.println("=== 开始导出数据库为SQL文件: " + outputPath + " ===");
            
            // 创建输出目录
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // 获取要导出的表列表
            Set<String> tables = new HashSet<>();
            if (tableNames != null && !tableNames.isEmpty()) {
                tables.addAll(tableNames);
            } else {
                // 导出所有表
                tables.addAll(catalogManager.getAllTableNames());
            }
            
            // 生成SQL内容
            StringBuilder sqlContent = new StringBuilder();
            
            // 添加文件头注释
            addFileHeader(sqlContent);
            
            int exportedTables = 0;
            int exportedRecords = 0;
            
            // 导出每个表
            for (String tableName : tables) {
                if (!catalogManager.tableExists(tableName)) {
                    System.out.println("警告: 表 " + tableName + " 不存在，跳过");
                    continue;
                }
                
                TableInfo tableInfo = catalogManager.getTable(tableName);
                
                // 添加表注释
                sqlContent.append("\n-- ").append("表: ").append(tableName).append("\n");
                sqlContent.append("-- ").append("导出时间: ")
                         .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                         .append("\n\n");
                
                // 导出表结构
                if (includeStructure) {
                    String createTableSQL = generateCreateTableSQL(tableInfo);
                    sqlContent.append(createTableSQL).append("\n\n");
                }
                
                // 导出数据
                if (includeData) {
                    List<String> insertStatements = generateInsertStatements(tableName, tableInfo);
                    exportedRecords += insertStatements.size();
                    
                    if (!insertStatements.isEmpty()) {
                        sqlContent.append("-- ").append(tableName).append(" 数据\n");
                        for (String insertSQL : insertStatements) {
                            sqlContent.append(insertSQL).append("\n");
                        }
                        sqlContent.append("\n");
                    }
                }
                
                exportedTables++;
            }
            
            // 写入文件
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath), StandardCharsets.UTF_8)) {
                writer.write(sqlContent.toString());
            }
            
            String message = String.format("数据库导出成功！导出了 %d 个表，%d 条记录", exportedTables, exportedRecords);
            System.out.println(message);
            
            return new ExecutionResult(true, message, null);
            
        } catch (Exception e) {
            return new ExecutionResult(false, "导出数据库失败: " + e.getMessage(), null);
        }
    }
    
    /**
     * 导出单个表为SQL文件
     * @param tableName 表名
     * @param outputPath 输出文件路径
     * @return 执行结果
     */
    public ExecutionResult exportTableToSQL(String tableName, String outputPath) {
        return exportDatabaseToSQL(outputPath, Arrays.asList(tableName), true, true);
    }
    
    /**
     * 读取SQL文件内容
     */
    private String readSQLFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 解析SQL语句
     */
    private List<String> parseSQLStatements(String sqlContent) {
        List<String> statements = new ArrayList<>();
        
        // 移除注释
        String cleanedContent = removeComments(sqlContent);
        
        // 按分号分割SQL语句
        String[] parts = SQL_DELIMITER_PATTERN.split(cleanedContent);
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                // 确保语句以分号结尾
                if (!trimmed.endsWith(";")) {
                    trimmed += ";";
                }
                statements.add(trimmed);
            }
        }
        
        return statements;
    }
    
    /**
     * 移除SQL注释
     */
    private String removeComments(String sql) {
        StringBuilder result = new StringBuilder();
        String[] lines = sql.split("\n");
        
        for (String line : lines) {
            // 移除单行注释 (-- 注释)
            int commentIndex = line.indexOf("--");
            if (commentIndex >= 0) {
                line = line.substring(0, commentIndex);
            }
            
            // 保留非空行
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                result.append(line).append("\n");
            }
        }
        
        // 移除多行注释 (/* ... */)
        String content = result.toString();
        content = content.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        
        return content;
    }
    
    /**
     * 执行SQL语句列表
     */
    private ExecutionResult executeSQLStatements(List<String> sqlStatements, boolean continueOnError) {
        List<ExecutionResult> results = new ArrayList<>();
        int successCount = 0;
        int totalCount = sqlStatements.size();
        
        for (int i = 0; i < sqlStatements.size(); i++) {
            String sql = sqlStatements.get(i);
            System.out.println("执行语句 " + (i + 1) + "/" + totalCount + ": " + 
                             (sql.length() > 100 ? sql.substring(0, 100) + "..." : sql));
            
            try {
                ExecutionResult result = databaseEngine.executeSQL(sql);
                results.add(result);
                
                if (result.isSuccess()) {
                    successCount++;
                    System.out.println("✓ 执行成功: " + result.getMessage());
                } else {
                    System.err.println("✗ 执行失败: " + result.getMessage());
                    if (!continueOnError) {
                        return new ExecutionResult(false, 
                            String.format("SQL文件执行失败在第 %d 条语句: %s", i + 1, result.getMessage()), 
                            null);
                    }
                }
            } catch (Exception e) {
                String errorMsg = "执行SQL语句时发生异常: " + e.getMessage();
                System.err.println("✗ " + errorMsg);
                results.add(new ExecutionResult(false, errorMsg, null));
                
                if (!continueOnError) {
                    return new ExecutionResult(false, 
                        String.format("SQL文件执行失败在第 %d 条语句: %s", i + 1, errorMsg), 
                        null);
                }
            }
        }
        
        String message = String.format("SQL文件执行完成: %d/%d 语句执行成功", successCount, totalCount);
        boolean success = (successCount == totalCount) || continueOnError;
        
        // 返回包含批处理结果的ExecutionResult
        return new ExecutionResult(success, message, results, true);
    }
    
    /**
     * 添加文件头注释
     */
    private void addFileHeader(StringBuilder sqlContent) {
        sqlContent.append("-- ================================================\n");
        sqlContent.append("-- SparrowDB 数据库导出文件\n");
        sqlContent.append("-- 导出时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sqlContent.append("-- 生成工具: SparrowDB SQLFileManager\n");
        sqlContent.append("-- ================================================\n\n");
    }
    
    /**
     * 生成CREATE TABLE语句
     */
    private String generateCreateTableSQL(TableInfo tableInfo) {
        StringBuilder sql = new StringBuilder();
        
        sql.append("-- 删除表（如果存在）\n");
        sql.append("DROP TABLE IF EXISTS ").append(tableInfo.getName()).append(";\n\n");
        
        sql.append("-- 创建表 ").append(tableInfo.getName()).append("\n");
        sql.append("CREATE TABLE ").append(tableInfo.getName()).append(" (\n");
        
        List<ColumnInfo> columns = new ArrayList<>(tableInfo.getColumns());
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo column = columns.get(i);
            
            sql.append("    ").append(column.getName()).append(" ").append(column.getDataType());
            
            // 长度
            if (column.getLength() > 0 && needsLength(column.getDataType())) {
                sql.append("(").append(column.getLength()).append(")");
            }
            
            // 约束
            if (column.isPrimaryKey()) {
                sql.append(" PRIMARY KEY");
            }
            if (column.isAutoIncrement()) {
                sql.append(" AUTO_INCREMENT");
            }
            if (!column.isNullable()) {
                sql.append(" NOT NULL");
            }
            if (column.isUnique() && !column.isPrimaryKey()) {
                sql.append(" UNIQUE");
            }
            if (column.getDefaultValue() != null) {
                sql.append(" DEFAULT ").append(formatDefaultValue(column.getDefaultValue(), column.getDataType()));
            }
            
            if (i < columns.size() - 1) {
                sql.append(",");
            }
            sql.append("\n");
        }
        
        sql.append(");");
        
        return sql.toString();
    }
    
    /**
     * 判断数据类型是否需要长度参数
     */
    private boolean needsLength(String dataType) {
        String type = dataType.toUpperCase();
        return type.equals("VARCHAR") || type.equals("CHAR") || type.equals("TEXT");
    }
    
    /**
     * 格式化默认值
     */
    private String formatDefaultValue(String defaultValue, String dataType) {
        if (defaultValue == null) {
            return "NULL";
        }
        
        String type = dataType.toUpperCase();
        if (type.equals("VARCHAR") || type.equals("CHAR") || type.equals("TEXT")) {
            return "'" + defaultValue.replace("'", "''") + "'";
        }
        
        return defaultValue;
    }
    
    /**
     * 生成INSERT语句
     */
    private List<String> generateInsertStatements(String tableName, TableInfo tableInfo) {
        List<String> insertStatements = new ArrayList<>();
        
        try {
            // 获取表数据
            List<Map<String, Object>> records = databaseEngine.getStorageAdapter().scanTable(tableName);
            
            if (records.isEmpty()) {
                return insertStatements;
            }
            
            // 获取列名
            List<String> columnNames = tableInfo.getColumnNames();
            
            // 生成INSERT语句
            for (Map<String, Object> record : records) {
                StringBuilder sql = new StringBuilder();
                sql.append("INSERT INTO ").append(tableName).append(" (");
                
                // 列名
                for (int i = 0; i < columnNames.size(); i++) {
                    sql.append(columnNames.get(i));
                    if (i < columnNames.size() - 1) {
                        sql.append(", ");
                    }
                }
                
                sql.append(") VALUES (");
                
                // 值
                for (int i = 0; i < columnNames.size(); i++) {
                    String columnName = columnNames.get(i);
                    Object value = record.get(columnName);
                    
                    sql.append(formatValue(value, tableInfo.getColumn(columnName)));
                    
                    if (i < columnNames.size() - 1) {
                        sql.append(", ");
                    }
                }
                
                sql.append(");");
                insertStatements.add(sql.toString());
            }
            
        } catch (Exception e) {
            System.err.println("生成INSERT语句失败: " + e.getMessage());
        }
        
        return insertStatements;
    }
    
    /**
     * 格式化值用于SQL语句
     */
    private String formatValue(Object value, ColumnInfo columnInfo) {
        if (value == null) {
            return "NULL";
        }
        
        String dataType = columnInfo.getDataType().toUpperCase();
        String valueStr = value.toString();
        
        // 字符串类型需要加引号并转义
        if (dataType.equals("VARCHAR") || dataType.equals("CHAR") || dataType.equals("TEXT")) {
            return "'" + valueStr.replace("'", "''") + "'";
        }
        
        // 数字类型直接返回
        if (dataType.equals("INT") || dataType.equals("INTEGER") || 
            dataType.equals("BIGINT") || dataType.equals("FLOAT") || 
            dataType.equals("DOUBLE") || dataType.equals("DECIMAL")) {
            return valueStr;
        }
        
        // 日期时间类型加引号
        if (dataType.equals("DATE") || dataType.equals("TIME") || 
            dataType.equals("DATETIME") || dataType.equals("TIMESTAMP")) {
            return "'" + valueStr + "'";
        }
        
        // 布尔类型
        if (dataType.equals("BOOLEAN") || dataType.equals("BOOL")) {
            return valueStr;
        }
        
        // 默认加引号
        return "'" + valueStr.replace("'", "''") + "'";
    }
    
    /**
     * 批量导入SQL文件目录
     * @param directoryPath 目录路径
     * @param filePattern 文件名模式（如 "*.sql"）
     * @param continueOnError 是否在遇到错误时继续
     * @return 执行结果
     */
    public ExecutionResult importSQLDirectory(String directoryPath, String filePattern, boolean continueOnError) {
        try {
            File directory = new File(directoryPath);
            if (!directory.exists() || !directory.isDirectory()) {
                return new ExecutionResult(false, "目录不存在: " + directoryPath, null);
            }
            
            // 查找匹配的SQL文件
            List<File> sqlFiles = new ArrayList<>();
            Pattern pattern = Pattern.compile(filePattern.replace("*", ".*"));
            
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && pattern.matcher(file.getName()).matches()) {
                        sqlFiles.add(file);
                    }
                }
            }
            
            if (sqlFiles.isEmpty()) {
                return new ExecutionResult(false, "未找到匹配的SQL文件", null);
            }
            
            // 按文件名排序
            sqlFiles.sort(Comparator.comparing(File::getName));
            
            System.out.println("找到 " + sqlFiles.size() + " 个SQL文件");
            
            List<ExecutionResult> results = new ArrayList<>();
            int successCount = 0;
            
            // 逐个导入文件
            for (File file : sqlFiles) {
                System.out.println("导入文件: " + file.getName());
                ExecutionResult result = importAndExecuteSQLFile(file.getAbsolutePath(), continueOnError);
                results.add(result);
                
                if (result.isSuccess()) {
                    successCount++;
                } else if (!continueOnError) {
                    return new ExecutionResult(false, 
                        "批量导入失败在文件: " + file.getName() + ", 错误: " + result.getMessage(), 
                        null);
                }
            }
            
            String message = String.format("批量导入完成: %d/%d 文件导入成功", successCount, sqlFiles.size());
            boolean overallSuccess = (successCount == sqlFiles.size()) || continueOnError;
            return new ExecutionResult(overallSuccess, message, results, true);
            
        } catch (Exception e) {
            return new ExecutionResult(false, "批量导入失败: " + e.getMessage(), null);
        }
    }
}
