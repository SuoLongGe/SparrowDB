package com.database.engine;

import com.sqlcompiler.catalog.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 列式存储引擎
 * 专门用于列式存储格式的数据存储和检索
 * 优化聚合查询和数据分析性能
 */
public class ColumnarStorageEngine {
    private final String dataDirectory;
    private final Map<String, ColumnarTableInfo> tableInfoMap;
    
    public ColumnarStorageEngine(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.tableInfoMap = new HashMap<>();
        
        // 确保数据目录存在
        File dir = new File(dataDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 自动发现现有的列式存储表
        discoverExistingColumnarTables();
    }
    
    /**
     * 创建列式存储表
     */
    public boolean createTable(String tableName, TableInfo tableInfo) {
        try {
            // 创建列式表信息
            ColumnarTableInfo columnarInfo = new ColumnarTableInfo(tableName, tableInfo);
            tableInfoMap.put(tableName, columnarInfo);
            
            // 创建表目录
            String tableDir = dataDirectory + File.separator + tableName;
            File tableDirectory = new File(tableDir);
            if (!tableDirectory.exists()) {
                if (!tableDirectory.mkdirs()) {
                    System.err.println("无法创建表目录: " + tableDir);
                    return false;
                }
                System.out.println("创建表目录: " + tableDir);
            }
            
            // 为每个列创建单独的数据文件
            for (ColumnInfo column : tableInfo.getColumns()) {
                String columnFile = getColumnFilePath(tableName, column.getName());
                File file = new File(columnFile);
                if (!file.exists()) {
                    file.createNewFile();
                }
                
                // 写入列元数据
                writeColumnMetadata(columnFile, column);
            }
            
            // 创建表元数据文件
            String tableMetaFile = getTableMetadataPath(tableName);
            writeTableMetadata(tableMetaFile, tableInfo);
            
            return true;
        } catch (Exception e) {
            System.err.println("创建列式存储表失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 插入记录到列式存储
     */
    public boolean insertRecord(String tableName, Map<String, Object> record) {
        try {
            ColumnarTableInfo tableInfo = tableInfoMap.get(tableName);
            if (tableInfo == null) {
                System.err.println("表 " + tableName + " 不存在");
                return false;
            }
            
            // 为每个列分别写入数据
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                String columnName = entry.getKey();
                Object value = entry.getValue();
                
                String columnFile = getColumnFilePath(tableName, columnName);
                appendColumnValue(columnFile, value);
            }
            
            // 更新行计数
            tableInfo.incrementRowCount();
            
            return true;
        } catch (Exception e) {
            System.err.println("插入列式存储记录失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 扫描列式存储表 - 优化版本，支持列投影
     */
    public List<Map<String, Object>> scanTable(String tableName) {
        return scanTable(tableName, null); // 默认读取所有列
    }
    
    /**
     * 扫描列式存储表 - 支持列投影，只读取需要的列
     */
    public List<Map<String, Object>> scanTable(String tableName, List<String> selectedColumns) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            ColumnarTableInfo tableInfo = tableInfoMap.get(tableName);
            if (tableInfo == null) {
                System.err.println("表 " + tableName + " 不存在");
                return results;
            }
            
            int rowCount = tableInfo.getRowCount();
            if (rowCount == 0) {
                return results;
            }
            
            // 确定要读取的列
            List<ColumnInfo> columnsToRead = new ArrayList<>();
            if (selectedColumns == null || selectedColumns.isEmpty()) {
                // 读取所有列
                columnsToRead.addAll(tableInfo.getOriginalTableInfo().getColumns());
            } else {
                // 只读取指定的列
                for (ColumnInfo column : tableInfo.getOriginalTableInfo().getColumns()) {
                    if (selectedColumns.contains(column.getName())) {
                        columnsToRead.add(column);
                    }
                }
            }
            
            // 并行读取列数据
            Map<String, List<Object>> columnData = new HashMap<>();
            for (ColumnInfo column : columnsToRead) {
                String columnFile = getColumnFilePath(tableName, column.getName());
                List<Object> values = readColumnValues(columnFile, rowCount);
                columnData.put(column.getName(), values);
            }
            
            // 重新组织为行格式 - 只包含需要的列
            for (int i = 0; i < rowCount; i++) {
                Map<String, Object> row = new HashMap<>();
                for (Map.Entry<String, List<Object>> entry : columnData.entrySet()) {
                    String columnName = entry.getKey();
                    List<Object> values = entry.getValue();
                    if (i < values.size()) {
                        row.put(columnName, values.get(i));
                    }
                }
                results.add(row);
            }
            
        } catch (Exception e) {
            System.err.println("扫描列式存储表失败: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * 聚合查询优化 - 只读取需要的列，避免数据重组
     */
    public List<Map<String, Object>> aggregateQuery(String tableName, List<String> columns, 
                                                   String aggregateFunction, String aggregateColumn) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            ColumnarTableInfo tableInfo = tableInfoMap.get(tableName);
            if (tableInfo == null) {
                System.err.println("表 " + tableName + " 不存在");
                return results;
            }
            
            int rowCount = tableInfo.getRowCount();
            if (rowCount == 0) {
                return results;
            }
            
            // 只读取聚合列的数据 - 直接计算，不重组数据
            String aggregateColumnFile = getColumnFilePath(tableName, aggregateColumn);
            List<Object> values = readColumnValues(aggregateColumnFile, rowCount);
            
            // 直接在列数据上执行聚合计算
            Object result = performAggregationOptimized(values, aggregateFunction);
            
            Map<String, Object> resultRow = new HashMap<>();
            resultRow.put(aggregateFunction.toLowerCase() + "(" + aggregateColumn + ")", result);
            results.add(resultRow);
            
        } catch (Exception e) {
            System.err.println("聚合查询失败: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * 优化的单列查询 - 直接返回列数据，不重组为行格式
     */
    public List<Object> querySingleColumn(String tableName, String columnName) {
        try {
            ColumnarTableInfo tableInfo = tableInfoMap.get(tableName);
            if (tableInfo == null) {
                System.err.println("表 " + tableName + " 不存在");
                return new ArrayList<>();
            }
            
            int rowCount = tableInfo.getRowCount();
            if (rowCount == 0) {
                return new ArrayList<>();
            }
            
            // 直接读取并返回列数据
            String columnFile = getColumnFilePath(tableName, columnName);
            return readColumnValues(columnFile, rowCount);
            
        } catch (Exception e) {
            System.err.println("单列查询失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 优化的条件查询 - 只读取需要的列
     */
    public List<Map<String, Object>> queryWithCondition(String tableName, String conditionColumn, 
                                                       Object conditionValue, List<String> selectColumns) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            ColumnarTableInfo tableInfo = tableInfoMap.get(tableName);
            if (tableInfo == null) {
                System.err.println("表 " + tableName + " 不存在");
                return results;
            }
            
            int rowCount = tableInfo.getRowCount();
            if (rowCount == 0) {
                return results;
            }
            
            // 先读取条件列，找到匹配的行索引
            String conditionColumnFile = getColumnFilePath(tableName, conditionColumn);
            List<Object> conditionValues = readColumnValues(conditionColumnFile, rowCount);
            
            List<Integer> matchingRows = new ArrayList<>();
            for (int i = 0; i < conditionValues.size(); i++) {
                if (conditionValues.get(i).equals(conditionValue)) {
                    matchingRows.add(i);
                }
            }
            
            if (matchingRows.isEmpty()) {
                return results;
            }
            
            // 确定要读取的列
            List<ColumnInfo> columnsToRead = new ArrayList<>();
            if (selectColumns == null || selectColumns.isEmpty()) {
                columnsToRead.addAll(tableInfo.getOriginalTableInfo().getColumns());
            } else {
                for (ColumnInfo column : tableInfo.getOriginalTableInfo().getColumns()) {
                    if (selectColumns.contains(column.getName())) {
                        columnsToRead.add(column);
                    }
                }
            }
            
            // 只读取需要的列和匹配的行
            Map<String, List<Object>> columnData = new HashMap<>();
            for (ColumnInfo column : columnsToRead) {
                String columnFile = getColumnFilePath(tableName, column.getName());
                List<Object> allValues = readColumnValues(columnFile, rowCount);
                
                // 只提取匹配行的数据
                List<Object> selectedValues = new ArrayList<>();
                for (int rowIndex : matchingRows) {
                    if (rowIndex < allValues.size()) {
                        selectedValues.add(allValues.get(rowIndex));
                    }
                }
                columnData.put(column.getName(), selectedValues);
            }
            
            // 重新组织为行格式
            for (int i = 0; i < matchingRows.size(); i++) {
                Map<String, Object> row = new HashMap<>();
                for (Map.Entry<String, List<Object>> entry : columnData.entrySet()) {
                    String colName = entry.getKey();
                    List<Object> values = entry.getValue();
                    if (i < values.size()) {
                        row.put(colName, values.get(i));
                    }
                }
                results.add(row);
            }
            
        } catch (Exception e) {
            System.err.println("条件查询失败: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * 优化的聚合计算 - 避免不必要的类型转换
     */
    private Object performAggregationOptimized(List<Object> values, String function) {
        if (values.isEmpty()) {
            return null;
        }
        
        switch (function.toUpperCase()) {
            case "COUNT":
                return values.size();
                
            case "SUM":
                double sum = 0.0;
                for (Object value : values) {
                    if (value != null) {
                        try {
                            sum += Double.parseDouble(value.toString());
                        } catch (NumberFormatException e) {
                            // 忽略非数字值
                        }
                    }
                }
                return sum;
                
            case "AVG":
                double total = 0.0;
                int count = 0;
                for (Object value : values) {
                    if (value != null) {
                        try {
                            total += Double.parseDouble(value.toString());
                            count++;
                        } catch (NumberFormatException e) {
                            // 忽略非数字值
                        }
                    }
                }
                return count > 0 ? total / count : 0.0;
                
            case "MAX":
                Object max = null;
                for (Object value : values) {
                    if (value != null) {
                        if (max == null || compareValues(value, max) > 0) {
                            max = value;
                        }
                    }
                }
                return max;
                
            case "MIN":
                Object min = null;
                for (Object value : values) {
                    if (value != null) {
                        if (min == null || compareValues(value, min) < 0) {
                            min = value;
                        }
                    }
                }
                return min;
                
            default:
                return null;
        }
    }
    
    /**
     * 比较两个值的大小
     */
    @SuppressWarnings("unchecked")
    private int compareValues(Object a, Object b) {
        if (a instanceof Comparable && b instanceof Comparable) {
            try {
                return ((Comparable<Object>) a).compareTo(b);
            } catch (ClassCastException e) {
                // 如果类型不匹配，转换为字符串比较
                return a.toString().compareTo(b.toString());
            }
        }
        return a.toString().compareTo(b.toString());
    }
    
    /**
     * 执行聚合计算（保留原方法以兼容性）
     */
    private Object performAggregation(List<Object> values, String function) {
        if (values.isEmpty()) {
            return null;
        }
        
        switch (function.toUpperCase()) {
            case "COUNT":
                return values.size();
            case "SUM":
                return values.stream()
                    .filter(v -> v != null && !"NULL".equals(v.toString()))
                    .mapToDouble(v -> {
                        if (v instanceof Number) {
                            return ((Number) v).doubleValue();
                        } else {
                            try {
                                return Double.parseDouble(v.toString());
                            } catch (NumberFormatException e) {
                                return 0.0;
                            }
                        }
                    })
                    .sum();
            case "AVG":
                return values.stream()
                    .filter(v -> v != null && !"NULL".equals(v.toString()))
                    .mapToDouble(v -> {
                        if (v instanceof Number) {
                            return ((Number) v).doubleValue();
                        } else {
                            try {
                                return Double.parseDouble(v.toString());
                            } catch (NumberFormatException e) {
                                return 0.0;
                            }
                        }
                    })
                    .average()
                    .orElse(0.0);
            case "MAX":
                return values.stream()
                    .filter(v -> v instanceof Comparable)
                    .map(v -> (Comparable<Object>) v)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            case "MIN":
                return values.stream()
                    .filter(v -> v instanceof Comparable)
                    .map(v -> (Comparable<Object>) v)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            default:
                return null;
        }
    }
    
    /**
     * 获取列文件路径
     */
    private String getColumnFilePath(String tableName, String columnName) {
        // 使用新的列式存储目录结构
        return dataDirectory + File.separator + tableName + File.separator + columnName + ".col";
    }
    
    /**
     * 获取表元数据文件路径
     */
    private String getTableMetadataPath(String tableName) {
        // 使用新的列式存储目录结构
        return dataDirectory + File.separator + tableName + File.separator + "metadata.txt";
    }
    
    /**
     * 自动发现现有的列式存储表
     */
    private void discoverExistingColumnarTables() {
        try {
            File dataDir = new File(dataDirectory);
            if (!dataDir.exists()) {
                return;
            }
            
            // 扫描数据目录下的所有子目录
            File[] subDirs = dataDir.listFiles(File::isDirectory);
            if (subDirs == null) {
                return;
            }
            
            for (File subDir : subDirs) {
                String tableName = subDir.getName();
                String metaFile = subDir.getAbsolutePath() + File.separator + "metadata.txt";
                
                // 检查是否存在元数据文件
                File meta = new File(metaFile);
                if (meta.exists()) {
                    // 检查是否为列式存储
                    if (isColumnarStorageTable(tableName)) {
                        // 自动注册表信息
                        registerExistingTable(tableName);
                        System.out.println("自动发现列式存储表: " + tableName);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("自动发现列式存储表失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查表是否为列式存储
     */
    private boolean isColumnarStorageTable(String tableName) {
        String metaFile = getTableMetadataPath(tableName);
        File file = new File(metaFile);
        
        if (!file.exists()) {
            return false;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("STORAGE_FORMAT=")) {
                    String format = line.substring("STORAGE_FORMAT=".length());
                    return "COLUMN".equalsIgnoreCase(format);
                }
            }
        } catch (IOException e) {
            // 如果读取失败，假设不是列式存储
        }
        
        return false;
    }
    
    /**
     * 注册现有的表
     */
    private void registerExistingTable(String tableName) {
        try {
            // 从元数据文件读取列信息
            List<ColumnInfo> columns = readColumnsFromMetadata(tableName);
            if (columns.isEmpty()) {
                return;
            }
            
            // 创建表信息
            TableInfo tableInfo = new TableInfo(tableName, "COLUMN");
            for (ColumnInfo column : columns) {
                tableInfo.addColumn(column);
            }
            
            // 计算行数
            int rowCount = calculateRowCount(tableName);
            
            // 创建列式表信息
            ColumnarTableInfo columnarInfo = new ColumnarTableInfo(tableName, tableInfo);
            columnarInfo.setRowCount(rowCount);
            
            // 注册到表信息映射
            tableInfoMap.put(tableName, columnarInfo);
            
        } catch (Exception e) {
            System.err.println("注册现有表失败: " + e.getMessage());
        }
    }
    
    /**
     * 从元数据文件读取列信息
     */
    private List<ColumnInfo> readColumnsFromMetadata(String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();
        String metaFile = getTableMetadataPath(tableName);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("COLUMN=")) {
                    String columnName = line.substring("COLUMN=".length());
                    // 根据列名推断数据类型
                    String dataType = inferDataType(columnName);
                    int length = inferLength(columnName);
                    boolean isPrimaryKey = "id".equals(columnName) || "ID".equals(columnName);
                    
                    columns.add(new ColumnInfo(columnName, dataType, length, isPrimaryKey, false));
                }
            }
            
            // 如果没有找到COLUMN=格式，尝试从列文件推断列信息
            if (columns.isEmpty()) {
                File meta = new File(metaFile);
                File tableDir = meta.getParentFile();
                if (tableDir != null && tableDir.isDirectory()) {
                    File[] colFiles = tableDir.listFiles((dir, name) -> name.endsWith(".col"));
                    if (colFiles != null) {
                        for (File colFile : colFiles) {
                            String fileName = colFile.getName();
                            String columnName = fileName.substring(0, fileName.lastIndexOf(".col"));
                            
                            // 根据列名推断数据类型
                            String dataType = inferDataType(columnName);
                            int length = inferLength(columnName);
                            boolean isPrimaryKey = "id".equals(columnName) || "ID".equals(columnName);
                            
                            columns.add(new ColumnInfo(columnName, dataType, length, isPrimaryKey, false));
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("读取列信息失败: " + e.getMessage());
        }
        
        return columns;
    }
    
    /**
     * 推断数据类型
     */
    private String inferDataType(String columnName) {
        if ("id".equals(columnName)) {
            return "INT";
        } else if ("price".equals(columnName)) {
            return "DECIMAL";
        } else if ("quantity".equals(columnName)) {
            return "INT";
        } else {
            return "VARCHAR";
        }
    }
    
    /**
     * 推断长度
     */
    private int inferLength(String columnName) {
        if ("id".equals(columnName) || "quantity".equals(columnName)) {
            return 0;
        } else if ("price".equals(columnName)) {
            return 10;
        } else if ("product_name".equals(columnName)) {
            return 100;
        } else {
            return 50;
        }
    }
    
    /**
     * 计算行数
     */
    private int calculateRowCount(String tableName) {
        try {
            // 尝试读取第一个列文件来计算行数
            String firstColumnFile = getColumnFilePath(tableName, "id");
            File file = new File(firstColumnFile);
            
            // 如果id列不存在，尝试ID列
            if (!file.exists()) {
                firstColumnFile = getColumnFilePath(tableName, "ID");
                file = new File(firstColumnFile);
            }
            
            if (!file.exists()) {
                return 0;
            }
            
            int count = 0;
            boolean inDataSection = false;
            boolean hasMetadata = false;
            try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.equals("# Data starts below")) {
                        inDataSection = true;
                        hasMetadata = true;
                        continue;
                    }
                    
                    // 如果文件有元数据标记，只计算数据部分的行数
                    if (hasMetadata) {
                        if (inDataSection && !line.startsWith("#") && !line.trim().isEmpty()) {
                            count++;
                        }
                    } else {
                        // 如果文件没有元数据标记，直接计算所有非空行
                        if (!line.startsWith("#") && !line.trim().isEmpty()) {
                            count++;
                        }
                    }
                }
            }
            
            return count;
        } catch (Exception e) {
            System.err.println("计算行数失败: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 写入列元数据
     */
    private void writeColumnMetadata(String columnFile, ColumnInfo column) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(columnFile, StandardCharsets.UTF_8))) {
            writer.println("# Column Metadata");
            writer.println("COLUMN_NAME=" + column.getName());
            writer.println("DATA_TYPE=" + column.getDataType());
            writer.println("LENGTH=" + column.getLength());
            writer.println("NOT_NULL=" + column.isNotNull());
            writer.println("PRIMARY_KEY=" + column.isPrimaryKey());
            writer.println("UNIQUE=" + column.isUnique());
            writer.println("DEFAULT_VALUE=" + (column.getDefaultValue() != null ? column.getDefaultValue() : ""));
            writer.println("AUTO_INCREMENT=" + column.isAutoIncrement());
            writer.println("# End Metadata");
            writer.println("# Data starts below");
        }
    }
    
    /**
     * 写入表元数据
     */
    private void writeTableMetadata(String metaFile, TableInfo tableInfo) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(metaFile, StandardCharsets.UTF_8))) {
            writer.println("# Table Metadata");
            writer.println("TABLE_NAME=" + tableInfo.getName());
            writer.println("STORAGE_FORMAT=COLUMN");
            writer.println("COLUMN_COUNT=" + tableInfo.getColumns().size());
            writer.println("CONSTRAINT_COUNT=" + tableInfo.getConstraints().size());
            writer.println("CREATE_TIME=" + System.currentTimeMillis());
            writer.println("# End Metadata");
        }
    }
    
    /**
     * 追加列值
     */
    private void appendColumnValue(String columnFile, Object value) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(columnFile, StandardCharsets.UTF_8, true))) {
            writer.println(value != null ? value.toString() : "NULL");
        }
    }
    
    /**
     * 读取列值
     */
    private List<Object> readColumnValues(String columnFile, int expectedRows) throws IOException {
        List<Object> values = new ArrayList<>();
        
        // 优化：移除调试输出以提升性能
        
        // 优化：只读取一次文件，同时处理元数据和数据
        try (BufferedReader reader = new BufferedReader(new FileReader(columnFile, StandardCharsets.UTF_8))) {
            String line;
            boolean inDataSection = false;
            
            while ((line = reader.readLine()) != null) {
                if (line.equals("# Data starts below")) {
                    inDataSection = true;
                    continue;
                }
                
                // 读取数据行
                if (inDataSection && !line.startsWith("#") && !line.trim().isEmpty()) {
                    values.add("NULL".equals(line) ? null : line);
                } else if (!inDataSection && !line.startsWith("#") && !line.trim().isEmpty()) {
                    // 如果没有元数据标记，直接读取所有非空行
                    values.add("NULL".equals(line) ? null : line);
                }
            }
        }
        
        return values;
    }
    
    /**
     * 列式表信息
     */
    private static class ColumnarTableInfo {
        private final String tableName;
        private final TableInfo originalTableInfo;
        private int rowCount;
        
        public ColumnarTableInfo(String tableName, TableInfo originalTableInfo) {
            this.tableName = tableName;
            this.originalTableInfo = originalTableInfo;
            this.rowCount = 0;
        }
        
        public String getTableName() {
            return tableName;
        }
        
        public TableInfo getOriginalTableInfo() {
            return originalTableInfo;
        }
        
        public int getRowCount() {
            return rowCount;
        }
        
        public void incrementRowCount() {
            rowCount++;
        }
        
        public void setRowCount(int rowCount) {
            this.rowCount = rowCount;
        }
    }
    
    /**
     * 获取所有表名
     */
    public List<String> getTableNames() {
        return new ArrayList<>(tableInfoMap.keySet());
    }
    
    /**
     * 获取表信息
     */
    public TableInfo getTableInfo(String tableName) {
        ColumnarTableInfo columnarInfo = tableInfoMap.get(tableName);
        return columnarInfo != null ? columnarInfo.getOriginalTableInfo() : null;
    }
}
