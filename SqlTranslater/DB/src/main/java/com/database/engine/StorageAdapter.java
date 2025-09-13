package com.database.engine;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.sqlcompiler.catalog.*;

/**
 * 存储系统适配器 - 整合Java存储系统与数据库引擎
 * 将高级存储系统接口适配为数据库引擎所需的接口
 */
public class StorageAdapter {
    // Java存储系统组件（导入外部包）
    private Object bufferPoolManager;
    private Object diskManager;
    private final String dataDirectory;
    private final Map<String, TableStorageInfo> tableStorageMap;
    private final Map<String, Integer> nextPageIdMap;
    
    // 存储系统配置
    private static final int BUFFER_POOL_SIZE = 50;
    private static final String REPLACEMENT_POLICY = "LRU";
    
    public StorageAdapter(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.tableStorageMap = new HashMap<>();
        this.nextPageIdMap = new HashMap<>();
        
        // 确保数据目录存在
        File dir = new File(dataDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 初始化存储系统（如果Java存储系统类在classpath中）
        try {
            initializeStorageSystem();
        } catch (Exception e) {
            System.err.println("警告：无法初始化高级存储系统，使用简单文件存储: " + e.getMessage());
        }
        
        // 初始化系统表
        initializeSystemTables();
        
        // 自动发现并注册现有的表
        discoverAndRegisterExistingTables();
    }
    
    /**
     * 初始化存储系统
     */
    private void initializeStorageSystem() {
        try {
            // 动态加载Java存储系统类
            Class<?> replacementPolicyClass = Class.forName("ReplacementPolicy");
            Class<?> bufferPoolManagerClass = Class.forName("BufferPoolManager");
            
            // 获取LRU枚举值
            Object lruPolicy = Enum.valueOf((Class<Enum>) replacementPolicyClass, "LRU");
            
            // 创建BufferPoolManager实例
            String dbFilename = dataDirectory + File.separator + "storage_system.db";
            this.bufferPoolManager = bufferPoolManagerClass
                .getConstructor(int.class, String.class, replacementPolicyClass)
                .newInstance(BUFFER_POOL_SIZE, dbFilename, lruPolicy);
                
            System.out.println("高级存储系统初始化成功");
        } catch (Exception e) {
            System.out.println("使用简单文件存储系统");
            this.bufferPoolManager = null;
        }
    }
    
    /**
     * 创建表存储
     */
    public boolean createTable(String tableName, TableInfo tableInfo) {
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
            
            if (bufferPoolManager != null) {
                // 使用高级存储系统
                return insertRecordWithBufferPool(tableName, serializedRecord);
            } else {
                // 使用简单文件存储
                return insertRecordWithFileStorage(tableName, serializedRecord);
            }
            
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
            
            if (bufferPoolManager != null) {
                // 使用高级存储系统扫描
                records = scanTableWithBufferPool(tableName);
            } else {
                // 使用简单文件存储扫描
                records = scanTableWithFileStorage(tableName);
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
            
            if (bufferPoolManager != null) {
                return deleteRecordWithBufferPool(tableName, record);
            } else {
                return deleteRecordWithFileStorage(tableName, record);
            }
            
        } catch (Exception e) {
            System.err.println("删除记录失败: " + e.getMessage());
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
        
        List<Map<String, Object>> records = scanTable(tableName);
        totalRecords = records.size();
        
        return new TableStats(tableName, totalPages, totalRecords);
    }
    
    /**
     * 删除表
     */
    public boolean dropTable(String tableName) {
        try {
            // 从内存中移除表信息
            tableStorageMap.remove(tableName);
            nextPageIdMap.remove(tableName);
            
            // 删除表文件
            String tableFile = getTableFilePath(tableName);
            File file = new File(tableFile);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    System.out.println("表文件删除成功: " + tableName);
                    return true;
                } else {
                    System.err.println("表文件删除失败: " + tableName);
                    return false;
                }
            } else {
                System.out.println("表文件不存在: " + tableName);
                return true; // 文件不存在也算成功
            }
            
        } catch (Exception e) {
            System.err.println("删除表失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        if (bufferPoolManager != null) {
            try {
                // 反射调用getCacheStats方法
                Object stats = bufferPoolManager.getClass().getMethod("getStats").invoke(bufferPoolManager);
                return stats.toString();
            } catch (Exception e) {
                return "缓存统计信息获取失败: " + e.getMessage();
            }
        }
        return "简单文件存储系统 - 无缓存统计";
    }
    
    /**
     * 刷新所有页面
     */
    public boolean flushAllPages() {
        if (bufferPoolManager != null) {
            try {
                return (Boolean) bufferPoolManager.getClass().getMethod("flushAllPages").invoke(bufferPoolManager);
            } catch (Exception e) {
                System.err.println("刷新页面失败: " + e.getMessage());
                return false;
            }
        }
        return true; // 文件存储自动刷新
    }
    
    // ========== 私有辅助方法 ==========
    
    private void initializeSystemTables() {
        tableStorageMap.put("__system_tables__", new TableStorageInfo("__system_tables__"));
        nextPageIdMap.put("__system_tables__", 1);
    }
    
    /**
     * 自动发现并注册现有的表
     */
    private void discoverAndRegisterExistingTables() {
        try {
            File dataDir = new File(dataDirectory);
            if (!dataDir.exists() || !dataDir.isDirectory()) {
                return;
            }
            
            File[] tblFiles = dataDir.listFiles((dir, name) -> name.endsWith(".tbl"));
            if (tblFiles == null) {
                return;
            }
            
            for (File tblFile : tblFiles) {
                String fileName = tblFile.getName();
                String tableName = fileName.substring(0, fileName.lastIndexOf(".tbl"));
                
                // 跳过系统表
                if (tableName.startsWith("__system_")) {
                    continue;
                }
                
                // 注册表到存储适配器
                if (!tableStorageMap.containsKey(tableName)) {
                    tableStorageMap.put(tableName, new TableStorageInfo(tableName));
                    nextPageIdMap.put(tableName, 1);
                    System.out.println("StorageAdapter发现并注册表: " + tableName);
                }
            }
            
        } catch (Exception e) {
            System.err.println("发现现有表失败: " + e.getMessage());
        }
    }
    
    /**
     * 确保表已注册到存储适配器
     */
    private void ensureTableRegistered(String tableName) {
        if (!tableStorageMap.containsKey(tableName)) {
            // 检查表文件是否存在
            String tableFile = getTableFilePath(tableName);
            File file = new File(tableFile);
            if (file.exists()) {
                tableStorageMap.put(tableName, new TableStorageInfo(tableName));
                nextPageIdMap.put(tableName, 1);
                System.out.println("StorageAdapter动态注册表: " + tableName);
            }
        }
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
    
    // ========== 高级存储系统方法 ==========
    
    private boolean insertRecordWithBufferPool(String tableName, String serializedRecord) {
        try {
            // 查找合适的页面或分配新页面
            int pageId = findOrAllocatePageForRecord(tableName, serializedRecord);
            if (pageId == -1) {
                return false;
            }
            
            // 使用反射调用BufferPoolManager的方法
            Object page = bufferPoolManager.getClass().getMethod("getPage", int.class).invoke(bufferPoolManager, pageId);
            if (page == null) {
                return false;
            }
            
            // 写入记录到页面（简化实现）
            String existingData = (String) page.getClass().getMethod("readString").invoke(page);
            String newData = existingData + serializedRecord + "\n";
            
            page.getClass().getMethod("writeString", String.class).invoke(page, newData);
            bufferPoolManager.getClass().getMethod("unpinPage", int.class, boolean.class).invoke(bufferPoolManager, pageId, true);
            
            return true;
        } catch (Exception e) {
            System.err.println("使用缓冲池插入记录失败: " + e.getMessage());
            return false;
        }
    }
    
    private List<Map<String, Object>> scanTableWithBufferPool(String tableName) {
        List<Map<String, Object>> records = new ArrayList<>();
        
        try {
            // 扫描所有页面
            for (int pageId = 1; pageId < nextPageIdMap.get(tableName); pageId++) {
                Object page = bufferPoolManager.getClass().getMethod("getPage", int.class).invoke(bufferPoolManager, pageId);
                if (page != null) {
                    String data = (String) page.getClass().getMethod("readString").invoke(page);
                    String[] lines = data.split("\n");
                    
                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            Map<String, Object> record = deserializeRecord(line.trim());
                            if (record != null) {
                                records.add(record);
                            }
                        }
                    }
                    
                    bufferPoolManager.getClass().getMethod("unpinPage", int.class, boolean.class).invoke(bufferPoolManager, pageId, false);
                }
            }
        } catch (Exception e) {
            System.err.println("使用缓冲池扫描表失败: " + e.getMessage());
        }
        
        return records;
    }
    
    private boolean deleteRecordWithBufferPool(String tableName, Map<String, Object> record) {
        // 实现类似于文件存储的删除逻辑，但使用缓冲池管理器
        return deleteRecordWithFileStorage(tableName, record);
    }
    
    private int findOrAllocatePageForRecord(String tableName, String recordData) {
        try {
            // 查找现有页面是否有空间
            for (int pageId = 1; pageId < nextPageIdMap.get(tableName); pageId++) {
                Object page = bufferPoolManager.getClass().getMethod("getPage", int.class).invoke(bufferPoolManager, pageId);
                if (page != null) {
                    String existingData = (String) page.getClass().getMethod("readString").invoke(page);
                    bufferPoolManager.getClass().getMethod("unpinPage", int.class, boolean.class).invoke(bufferPoolManager, pageId, false);
                    
                    if (existingData.length() + recordData.length() + 1 < 4000) { // 4KB页面
                        return pageId;
                    }
                }
            }
            
            // 分配新页面
            int newPageId = nextPageIdMap.get(tableName);
            nextPageIdMap.put(tableName, newPageId + 1);
            
            // 初始化新页面
            Object page = bufferPoolManager.getClass().getMethod("getPage", int.class).invoke(bufferPoolManager, newPageId);
            if (page != null) {
                page.getClass().getMethod("writeString", String.class).invoke(page, "");
                bufferPoolManager.getClass().getMethod("unpinPage", int.class, boolean.class).invoke(bufferPoolManager, newPageId, true);
                return newPageId;
            }
            
        } catch (Exception e) {
            System.err.println("查找或分配页面失败: " + e.getMessage());
        }
        
        return -1;
    }
    
    // ========== 简单文件存储方法 ==========
    
    private boolean insertRecordWithFileStorage(String tableName, String serializedRecord) {
        try {
            String tableFile = getTableFilePath(tableName);
            
            // 追加记录到文件 - 使用UTF-8编码
            try (FileWriter writer = new FileWriter(tableFile, StandardCharsets.UTF_8, true)) {
                writer.write("RECORD:" + serializedRecord + "\n");
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("文件存储插入记录失败: " + e.getMessage());
            return false;
        }
    }
    
    private List<Map<String, Object>> scanTableWithFileStorage(String tableName) {
        List<Map<String, Object>> records = new ArrayList<>();
        
        try {
            String tableFile = getTableFilePath(tableName);
            File file = new File(tableFile);
            if (!file.exists()) {
                return records;
            }
            
            try (BufferedReader reader = new BufferedReader(new FileReader(tableFile, StandardCharsets.UTF_8))) {
                String line;
                boolean inDataSection = false;
                
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("# End Metadata")) {
                        inDataSection = true;
                        continue;
                    }
                    
                    if (inDataSection && line.startsWith("RECORD:")) {
                        String recordData = line.substring(7); // 移除"RECORD:"前缀
                        Map<String, Object> record = deserializeRecord(recordData);
                        if (record != null) {
                            records.add(record);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("文件存储扫描表失败: " + e.getMessage());
        }
        
        return records;
    }
    
    private boolean deleteRecordWithFileStorage(String tableName, Map<String, Object> targetRecord) {
        try {
            String tableFile = getTableFilePath(tableName);
            File file = new File(tableFile);
            if (!file.exists()) {
                return false;
            }
            
            // 读取所有内容
            List<String> allLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    allLines.add(line);
                }
            }
            
            // 重写文件，跳过要删除的记录
            boolean recordDeleted = false;
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                boolean inDataSection = false;
                
                for (String line : allLines) {
                    if (line.startsWith("# End Metadata")) {
                        writer.println(line);
                        inDataSection = true;
                        continue;
                    }
                    
                    if (inDataSection && line.startsWith("RECORD:")) {
                        String recordData = line.substring(7);
                        Map<String, Object> currentRecord = deserializeRecord(recordData);
                        
                        if (currentRecord != null && recordsEqual(currentRecord, targetRecord) && !recordDeleted) {
                            // 跳过此记录（删除）
                            recordDeleted = true;
                            continue;
                        }
                    }
                    
                    writer.println(line);
                }
            }
            
            return recordDeleted;
        } catch (IOException e) {
            System.err.println("文件存储删除记录失败: " + e.getMessage());
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
    
    // ========== 内部类 ==========
    
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