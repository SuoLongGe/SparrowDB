package com.database.engine;

import com.sqlcompiler.catalog.*;
import java.util.*;
import java.io.*;

/**
 * 存储引擎 - 负责数据的物理存储和检索
 * 实现行与页的映射、磁盘组织
 */
public class StorageEngine {
    private final String dataDirectory;
    private final Map<String, TableStorageInfo> tableStorageMap;
    private final Map<String, Integer> nextPageIdMap;
    
    public StorageEngine(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.tableStorageMap = new HashMap<>();
        this.nextPageIdMap = new HashMap<>();
        
        // 确保数据目录存在
        File dir = new File(dataDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 初始化系统表
        initializeSystemTables();
    }
    
    /**
     * 获取数据目录路径
     */
    public String getDataDirectory() {
        return dataDirectory;
    }
    
    /**
     * 创建表存储
     */
    public boolean createTableStorage(String tableName, TableInfo tableInfo) {
        try {
            // 创建表存储信息
            TableStorageInfo storageInfo = new TableStorageInfo(tableName);
            tableStorageMap.put(tableName, storageInfo);
            nextPageIdMap.put(tableName, 1);
            
            // 创建表文件
            String tableFile = getTableFilePath(tableName);
            File file = new File(tableFile);
            if (!file.exists()) {
                file.createNewFile();
            }
            
            // 写入表元数据到文件头
            writeTableMetadata(tableFile, tableInfo);
            
            return true;
        } catch (Exception e) {
            System.err.println("创建表存储失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 插入记录
     */
    public boolean insertRecord(String tableName, Map<String, Object> record) {
        try {
            TableStorageInfo storageInfo = tableStorageMap.get(tableName);
            if (storageInfo == null) {
                return false;
            }
            
            // 序列化记录
            String serializedRecord = serializeRecord(record);
            
            // 查找合适的页面
            int pageId = findPageForRecord(tableName, serializedRecord.length());
            if (pageId == -1) {
                // 分配新页面
                pageId = allocateNewPage(tableName);
                if (pageId == -1) {
                    return false;
                }
            }
            
            // 写入记录到页面
            return writeRecordToPage(tableName, pageId, serializedRecord);
            
        } catch (Exception e) {
            System.err.println("插入记录失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 扫描表记录
     */
    public List<Map<String, Object>> scanTable(String tableName) {
        List<Map<String, Object>> records = new ArrayList<>();
        
        try {
            // 确保表已注册
            ensureTableRegistered(tableName);
            
            TableStorageInfo storageInfo = tableStorageMap.get(tableName);
            if (storageInfo == null) {
                return records;
            }
            
            // 首先尝试读取RECORD:格式的数据（兼容现有数据）
            List<Map<String, Object>> recordFormatData = readRecordFormatData(tableName);
            if (!recordFormatData.isEmpty()) {
                return recordFormatData;
            }
            
            // 如果没有RECORD:格式数据，扫描PAGE:格式
            Integer nextPageId = nextPageIdMap.get(tableName);
            if (nextPageId == null) {
                nextPageId = 1;
            }
            
            for (int pageId = 1; pageId < nextPageId; pageId++) {
                List<String> pageRecords = readPageRecords(tableName, pageId);
                for (String recordData : pageRecords) {
                    Map<String, Object> record = deserializeRecord(recordData);
                    if (record != null) {
                        records.add(record);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("扫描表失败: " + e.getMessage());
        }
        
        return records;
    }
    
    /**
     * 删除记录
     */
    public boolean deleteRecord(String tableName, Map<String, Object> record) {
        try {
            TableStorageInfo storageInfo = tableStorageMap.get(tableName);
            if (storageInfo == null) {
                return false;
            }
            
            // 扫描所有页面查找记录
            for (int pageId = 1; pageId < nextPageIdMap.get(tableName); pageId++) {
                List<String> pageRecords = readPageRecords(tableName, pageId);
                List<String> updatedRecords = new ArrayList<>();
                boolean found = false;
                
                for (String recordData : pageRecords) {
                    Map<String, Object> currentRecord = deserializeRecord(recordData);
                    if (currentRecord != null && recordsEqual(currentRecord, record)) {
                        found = true;
                        // 跳过此记录（删除）
                    } else {
                        updatedRecords.add(recordData);
                    }
                }
                
                if (found) {
                    // 重写页面
                    writePageRecords(tableName, pageId, updatedRecords);
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("删除记录失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 更新记录
     */
    public boolean updateRecord(String tableName, Map<String, Object> oldRecord, Map<String, Object> newRecord) {
        try {
            TableStorageInfo storageInfo = tableStorageMap.get(tableName);
            if (storageInfo == null) {
                return false;
            }
            
            // 扫描所有页面查找记录
            for (int pageId = 1; pageId < nextPageIdMap.get(tableName); pageId++) {
                List<String> pageRecords = readPageRecords(tableName, pageId);
                List<String> updatedRecords = new ArrayList<>();
                boolean found = false;
                
                for (String recordData : pageRecords) {
                    Map<String, Object> currentRecord = deserializeRecord(recordData);
                    if (currentRecord != null && recordsEqual(currentRecord, oldRecord)) {
                        // 更新记录
                        String newRecordData = serializeRecord(newRecord);
                        updatedRecords.add(newRecordData);
                        found = true;
                    } else {
                        updatedRecords.add(recordData);
                    }
                }
                
                if (found) {
                    // 重写页面
                    writePageRecords(tableName, pageId, updatedRecords);
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("更新记录失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取表统计信息
     */
    public TableStats getTableStats(String tableName) {
        TableStorageInfo storageInfo = tableStorageMap.get(tableName);
        if (storageInfo == null) {
            return null;
        }
        
        int totalPages = nextPageIdMap.get(tableName) - 1;
        int totalRecords = 0;
        
        for (int pageId = 1; pageId < nextPageIdMap.get(tableName); pageId++) {
            List<String> pageRecords = readPageRecords(tableName, pageId);
            totalRecords += pageRecords.size();
        }
        
        return new TableStats(tableName, totalPages, totalRecords);
    }
    
    // 私有辅助方法
    
    private void initializeSystemTables() {
        // 初始化系统表存储
        tableStorageMap.put("__system_tables__", new TableStorageInfo("__system_tables__"));
        nextPageIdMap.put("__system_tables__", 1);
    }
    
    /**
     * 确保表已注册到存储引擎
     */
    private void ensureTableRegistered(String tableName) {
        if (!tableStorageMap.containsKey(tableName)) {
            tableStorageMap.put(tableName, new TableStorageInfo(tableName));
            nextPageIdMap.put(tableName, 1);
        }
    }
    
    /**
     * 读取RECORD:格式的数据（兼容现有数据文件）
     */
    private List<Map<String, Object>> readRecordFormatData(String tableName) {
        List<Map<String, Object>> records = new ArrayList<>();
        
        try {
            String tableFile = getTableFilePath(tableName);
            File file = new File(tableFile);
            if (!file.exists()) {
                return records;
            }
            
            try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
                String line;
                boolean inDataSection = false;
                
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("# End Metadata")) {
                        inDataSection = true;
                        continue;
                    }
                    
                    if (inDataSection && line.startsWith("RECORD:")) {
                        // 解析RECORD:格式的数据
                        String recordData = line.substring(7); // 移除"RECORD:"前缀
                        Map<String, Object> record = deserializeRecord(recordData);
                        if (record != null) {
                            records.add(record);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("读取RECORD格式数据失败: " + e.getMessage());
        }
        
        return records;
    }
    
    private String getTableFilePath(String tableName) {
        return dataDirectory + File.separator + tableName + ".tbl";
    }
    
    private void writeTableMetadata(String tableFile, TableInfo tableInfo) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(tableFile))) {
            writer.println("# Table Metadata");
            writer.println("TABLE_NAME=" + tableInfo.getName());
            writer.println("COLUMN_COUNT=" + tableInfo.getColumns().size());
            
            for (ColumnInfo column : tableInfo.getColumns()) {
                writer.println("COLUMN=" + column.getName() + ":" + column.getDataType() + ":" + column.getLength());
            }
            
            writer.println("# End Metadata");
            writer.println();
        } catch (IOException e) {
            System.err.println("写入表元数据失败: " + e.getMessage());
        }
    }
    
    private String serializeRecord(Map<String, Object> record) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            if (!first) {
                sb.append("|");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        
        return sb.toString();
    }
    
    private Map<String, Object> deserializeRecord(String recordData) {
        Map<String, Object> record = new HashMap<>();
        
        try {
            String[] pairs = recordData.split("\\|");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    record.put(keyValue[0], keyValue[1]);
                }
            }
        } catch (Exception e) {
            return null;
        }
        
        return record;
    }
    
    private int findPageForRecord(String tableName, int recordSize) {
        // 简单的页面查找策略
        for (int pageId = 1; pageId < nextPageIdMap.get(tableName); pageId++) {
            List<String> pageRecords = readPageRecords(tableName, pageId);
            int currentSize = 0;
            for (String record : pageRecords) {
                currentSize += record.length();
            }
            
            if (currentSize + recordSize < 4000) { // 假设页面大小4000字节
                return pageId;
            }
        }
        return -1; // 需要新页面
    }
    
    private int allocateNewPage(String tableName) {
        int pageId = nextPageIdMap.get(tableName);
        nextPageIdMap.put(tableName, pageId + 1);
        return pageId;
    }
    
    
    private boolean writeRecordToPage(String tableName, int pageId, String recordData) {
        try {
            List<String> pageRecords = readPageRecords(tableName, pageId);
            pageRecords.add(recordData);
            return writePageRecords(tableName, pageId, pageRecords);
        } catch (Exception e) {
            return false;
        }
    }
    
    private List<String> readPageRecords(String tableName, int pageId) {
        List<String> records = new ArrayList<>();
        
        try {
            String tableFile = getTableFilePath(tableName);
            File file = new File(tableFile);
            if (!file.exists()) {
                return records;
            }
            
            try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
                String line;
                boolean inDataSection = false;
                int currentPage = 0;
                
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("# End Metadata")) {
                        inDataSection = true;
                        continue;
                    }
                    
                    if (inDataSection) {
                        if (line.startsWith("PAGE:" + pageId)) {
                            currentPage = pageId;
                            continue;
                        } else if (line.startsWith("PAGE:")) {
                            currentPage = 0;
                            continue;
                        }
                        
                        if (currentPage == pageId && !line.isEmpty()) {
                            records.add(line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("读取页面记录失败: " + e.getMessage());
        }
        
        return records;
    }
    
    private boolean writePageRecords(String tableName, int pageId, List<String> records) {
        try {
            String tableFile = getTableFilePath(tableName);
            File file = new File(tableFile);
            
            // 读取现有内容
            List<String> allLines = new ArrayList<>();
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        allLines.add(line);
                    }
                }
            }
            
            // 重写文件
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                boolean inDataSection = false;
                boolean pageWritten = false;
                
                for (String line : allLines) {
                    if (line.startsWith("# End Metadata")) {
                        writer.println(line);
                        inDataSection = true;
                        continue;
                    }
                    
                    if (inDataSection && line.startsWith("PAGE:" + pageId)) {
                        // 跳过旧页面数据
                        continue;
                    } else if (inDataSection && line.startsWith("PAGE:")) {
                        if (!pageWritten) {
                            // 写入新页面数据
                            writer.println("PAGE:" + pageId);
                            for (String record : records) {
                                writer.println(record);
                            }
                            writer.println();
                            pageWritten = true;
                        }
                        writer.println(line);
                    } else if (!inDataSection) {
                        writer.println(line);
                    }
                }
                
                if (!pageWritten) {
                    // 如果数据部分为空，添加页面数据
                    writer.println("PAGE:" + pageId);
                    for (String record : records) {
                        writer.println(record);
                    }
                    writer.println();
                }
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("写入页面记录失败: " + e.getMessage());
            return false;
        }
    }
    
    private boolean recordsEqual(Map<String, Object> record1, Map<String, Object> record2) {
        if (record1.size() != record2.size()) {
            return false;
        }
        
        for (Map.Entry<String, Object> entry : record1.entrySet()) {
            if (!entry.getValue().equals(record2.get(entry.getKey()))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 表存储信息
     */
    private static class TableStorageInfo {
        private final String tableName;
        private final long createTime;
        
        public TableStorageInfo(String tableName) {
            this.tableName = tableName;
            this.createTime = System.currentTimeMillis();
        }
        
        public String getTableName() {
            return tableName;
        }
        
        public long getCreateTime() {
            return createTime;
        }
    }
    
    /**
     * 表统计信息
     */
    public static class TableStats {
        private final String tableName;
        private final int pageCount;
        private final int recordCount;
        
        public TableStats(String tableName, int pageCount, int recordCount) {
            this.tableName = tableName;
            this.pageCount = pageCount;
            this.recordCount = recordCount;
        }
        
        public String getTableName() {
            return tableName;
        }
        
        public int getPageCount() {
            return pageCount;
        }
        
        public int getRecordCount() {
            return recordCount;
        }
        
        @Override
        public String toString() {
            return String.format("表 %s: %d 页, %d 条记录", tableName, pageCount, recordCount);
        }
    }
}
