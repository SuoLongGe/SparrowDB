package com.database.engine;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.sqlcompiler.catalog.*;
import com.database.logging.RollbackCallback;

/**
 * 存储系统适配器 - 整合Java存储系统与数据库引擎
 * 将高级存储系统接口适配为数据库引擎所需的接口
 */
public class StorageAdapter implements RollbackCallback {
    // Java存储系统组件（导入外部包）
    private Object bufferPoolManager;
    private Object diskManager;
    private final String dataDirectory;
    private final Map<String, TableStorageInfo> tableStorageMap;
    private final Map<String, Integer> nextPageIdMap;
    
    // 列式存储引擎
    private final ColumnarStorageEngine columnarStorageEngine;

    // 存储系统配置
    private static final int BUFFER_POOL_SIZE = 50;
    private static final String REPLACEMENT_POLICY = "LRU";
    
    public StorageAdapter(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.tableStorageMap = new HashMap<>();
        this.nextPageIdMap = new HashMap<>();
        this.columnarStorageEngine = new ColumnarStorageEngine(dataDirectory);

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
     * 获取列式存储引擎
     */
    public ColumnarStorageEngine getColumnarStorageEngine() {
        return columnarStorageEngine;
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
            // 根据存储格式选择存储引擎
            if (tableInfo.isColumnarStorage()) {
                // 使用列式存储引擎
                return columnarStorageEngine.createTable(tableName, tableInfo);
            } else {
                // 使用行式存储引擎
                return createRowStorageTable(tableName, tableInfo);
            }
        } catch (Exception e) {
            System.err.println("创建表存储失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 创建行式存储表
     */
    private boolean createRowStorageTable(String tableName, TableInfo tableInfo) {
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
            System.err.println("创建行式存储表失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 插入记录
     */
    public boolean insertRecord(String tableName, Map<String, Object> record) {
        try {
            // 检查是否为列式存储表
            if (isColumnarStorageTable(tableName)) {
                return columnarStorageEngine.insertRecord(tableName, record);
            }

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
            // 检查是否为列式存储表
            if (isColumnarStorageTable(tableName)) {
                return columnarStorageEngine.scanTable(tableName);
            }

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
     * 更新记录
     */
    public boolean updateRecord(String tableName, Map<String, Object> oldRecord, Map<String, Object> newRecord) {
        try {
            TableStorageInfo storageInfo = tableStorageMap.get(tableName);
            if (storageInfo == null) {
                return false;
            }

            if (bufferPoolManager != null) {
                return updateRecordWithBufferPool(tableName, oldRecord, newRecord);
            } else {
                return updateRecordWithFileStorage(tableName, oldRecord, newRecord);
            }

        } catch (Exception e) {
            System.err.println("更新记录失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 使用缓冲池更新记录
     */
    private boolean updateRecordWithBufferPool(String tableName, Map<String, Object> oldRecord, Map<String, Object> newRecord) {
        try {
            // 先删除旧记录
            if (!deleteRecordWithBufferPool(tableName, oldRecord)) {
                return false;
            }
            // 再插入新记录
            return insertRecordWithBufferPool(tableName, serializeRecord(newRecord));
        } catch (Exception e) {
            System.err.println("使用缓冲池更新记录失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 使用文件存储更新记录
     */
    private boolean updateRecordWithFileStorage(String tableName, Map<String, Object> oldRecord, Map<String, Object> newRecord) {
        try {
            // 先删除旧记录
            if (!deleteRecordWithFileStorage(tableName, oldRecord)) {
                return false;
            }
            // 再插入新记录
            return insertRecordWithFileStorage(tableName, serializeRecord(newRecord));
        } catch (Exception e) {
            System.err.println("使用文件存储更新记录失败: " + e.getMessage());
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
        // 注册所有系统表
        String[] systemTables = {
            "__system_tables__",
            "__system_columns__",
            "__system_functions__",
            "__system_constraints__"
        };

        for (String systemTable : systemTables) {
            tableStorageMap.put(systemTable, new TableStorageInfo(systemTable));
            nextPageIdMap.put(systemTable, 1);

            // 检查文件是否存在，如果存在则动态注册
            String tableFile = getTableFilePath(systemTable);
            File file = new File(tableFile);
            if (file.exists()) {
                System.out.println("StorageAdapter发现并注册系统表: " + systemTable);
            }
        }
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
    
    /**
     * 检查表是否为列式存储
     */
    public boolean isColumnarStorageTable(String tableName) {
        // 检查是否存在列式存储目录和元数据文件
        String columnarDir = dataDirectory + File.separator + tableName;
        String metaFile = columnarDir + File.separator + "metadata.txt";
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
            // 如果读取失败，假设是行式存储
        }

        return false;
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
            System.out.println("🔍 准备写入文件: " + tableFile);
            System.out.println("🔍 序列化记录: " + serializedRecord);
            
            // 追加记录到文件 - 使用UTF-8编码
            try (FileWriter writer = new FileWriter(tableFile, StandardCharsets.UTF_8, true)) {
                String recordLine = "RECORD:" + serializedRecord + "\n";
                System.out.println("🔍 写入记录行: " + recordLine);
                writer.write(recordLine);
                writer.flush(); // 强制刷新缓冲区
            }
            
            System.out.println("🔍 文件写入完成");
            return true;
        } catch (IOException e) {
            System.err.println("文件存储插入记录失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private List<Map<String, Object>> scanTableWithFileStorage(String tableName) {
        List<Map<String, Object>> records = new ArrayList<>();
        
        try {
            String tableFile = getTableFilePath(tableName);
            System.out.println("🔍 扫描文件: " + tableFile);
            File file = new File(tableFile);
            if (!file.exists()) {
                System.out.println("🔍 文件不存在: " + tableFile);
                return records;
            }
            
            System.out.println("🔍 文件存在，开始读取");
            try (BufferedReader reader = new BufferedReader(new FileReader(tableFile, StandardCharsets.UTF_8))) {
                String line;
                boolean inDataSection = false;
                int lineNum = 0;
                
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    if (line.startsWith("# End Metadata")) {
                        inDataSection = true;
                        System.out.println("🔍 找到数据段开始，行号: " + lineNum);
                        continue;
                    }
                    
                    if (inDataSection) {
                        if (line.startsWith("RECORD:")) {
                            // 处理RECORD:格式
                            String recordData = line.substring(7); // 移除"RECORD:"前缀
                            System.out.println("🔍 找到RECORD记录，行号: " + lineNum + ", 数据: " + recordData);
                            Map<String, Object> record = deserializeRecord(recordData);
                            if (record != null) {
                                records.add(record);
                                System.out.println("🔍 成功解析RECORD记录: " + record);
                            } else {
                                System.out.println("🔍 RECORD记录解析失败");
                            }
                        } else if (line.startsWith("PAGE:")) {
                            // 跳过PAGE:行
                            System.out.println("🔍 跳过PAGE行: " + line);
                        } else if (!line.isEmpty() && !line.startsWith("#")) {
                            // 处理PAGE:后面的数据行
                            System.out.println("🔍 找到PAGE数据记录，行号: " + lineNum + ", 数据: " + line);
                            Map<String, Object> record = deserializeRecord(line);
                            if (record != null) {
                                records.add(record);
                                System.out.println("🔍 成功解析PAGE记录: " + record);
                            } else {
                                System.out.println("🔍 PAGE记录解析失败");
                            }
                        }
                    }
                }
            }
            System.out.println("🔍 扫描完成，共找到 " + records.size() + " 条记录");
        } catch (IOException e) {
            System.err.println("文件存储扫描表失败: " + e.getMessage());
            e.printStackTrace();
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

    /**
     * 检查表是否存在
     */
    public boolean tableExists(String tableName) {
        return tableStorageMap.containsKey(tableName.toLowerCase()) ||
               new File(dataDirectory, tableName.toLowerCase() + ".tbl").exists();
    }

    /**
     * 创建系统表
     */
    public boolean createSystemTable(String tableName, List<String> columnDefinitions) {
        try {
            // 构建简单的表信息
            TableInfo tableInfo = new TableInfo(tableName);
            for (String columnDef : columnDefinitions) {
                String[] parts = columnDef.split("\\s+");
                if (parts.length >= 2) {
                    ColumnInfo columnInfo = new ColumnInfo(parts[0], parts[1], 255);
                    tableInfo.addColumn(columnInfo);
                }
            }

            return createTable(tableName, tableInfo);
        } catch (Exception e) {
            System.err.println("创建系统表失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 向系统表插入数据
     */
    public boolean insertIntoSystemTable(String tableName, Map<String, Object> data) {
        return insertRecord(tableName, data);
    }

    /**
     * 从系统表删除数据
     */
    public boolean deleteFromSystemTable(String tableName, Map<String, Object> condition) {
        try {
            List<Map<String, Object>> records = scanTable(tableName);
            for (Map<String, Object> record : records) {
                boolean matches = true;
                for (Map.Entry<String, Object> entry : condition.entrySet()) {
                    if (!Objects.equals(record.get(entry.getKey()), entry.getValue())) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return deleteRecord(tableName, record);
                }
            }
            return true; // 没有匹配的记录也认为删除成功
        } catch (Exception e) {
            System.err.println("从系统表删除数据失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 查询系统表所有数据
     */
    public List<Map<String, Object>> selectAll(String tableName) {
        return scanTable(tableName);
    }

    // ==================== RollbackCallback 接口实现 ====================

    @Override
    public void rollbackInsert(String tableName, Map<String, Object> record) throws IOException {
        System.out.println("回滚INSERT操作 - 删除记录从表: " + tableName + ", 记录: " + record);
        // 对于INSERT回滚，我们需要删除插入的记录
        // 这里简化处理，通过主键查找并删除记录
        deleteRecordByKey(tableName, record);
    }

    @Override
    public void rollbackUpdate(String tableName, Map<String, Object> record) throws IOException {
        System.out.println("回滚UPDATE操作 - 恢复记录到表: " + tableName + ", 记录: " + record);
        // 对于UPDATE回滚，我们需要恢复更新前的数据
        // 这里简化处理，直接更新记录
        updateRecord(tableName, record);
    }

    @Override
    public void rollbackDelete(String tableName, Map<String, Object> record) throws IOException {
        System.out.println("回滚DELETE操作 - 重新插入记录到表: " + tableName + ", 记录: " + record);
        // 对于DELETE回滚，我们需要重新插入被删除的记录
        insertRecord(tableName, record);
    }

    @Override
    public void rollbackCreateTable(String tableName, String metadata) throws IOException {
        System.out.println("回滚CREATE TABLE操作 - 删除表: " + tableName);
        // 对于CREATE TABLE回滚，我们需要删除创建的表
        dropTable(tableName);
    }

    @Override
    public void rollbackDropTable(String tableName) throws IOException {
        System.out.println("回滚删除表: " + tableName);

        // 删除行式存储表文件
        String tableFile = dataDirectory + File.separator + tableName + ".tbl";
        File file = new File(tableFile);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("成功删除表文件: " + tableFile);
            } else {
                System.err.println("删除表文件失败: " + tableFile);
            }
        }

        // 删除列式存储表目录
        String columnarDir = dataDirectory + File.separator + tableName;
        File dir = new File(columnarDir);
        if (dir.exists() && dir.isDirectory()) {
            deleteDirectory(dir);
            System.out.println("成功删除列式存储目录: " + columnarDir);
        }

        // 从内存中移除表信息
        tableStorageMap.remove(tableName);
        nextPageIdMap.remove(tableName);
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    /**
     * 根据主键删除记录（用于回滚INSERT操作）
     */
    private void deleteRecordByKey(String tableName, Map<String, Object> record) throws IOException {
        // 简化实现：扫描表找到匹配的记录并删除
        List<Map<String, Object>> allRecords = scanTable(tableName);
        List<Map<String, Object>> filteredRecords = new ArrayList<>();

        // 假设第一个字段是主键
        String primaryKey = record.keySet().iterator().next();
        Object primaryKeyValue = record.get(primaryKey);

        for (Map<String, Object> existingRecord : allRecords) {
            if (!primaryKeyValue.equals(existingRecord.get(primaryKey))) {
                filteredRecords.add(existingRecord);
            }
        }

        // 重写表文件
        if (isColumnarStorageTable(tableName)) {
            // 列式存储：重写所有列文件
            columnarStorageEngine.scanTable(tableName).clear();
            for (Map<String, Object> rec : filteredRecords) {
                columnarStorageEngine.insertRecord(tableName, rec);
            }
        } else {
            // 行式存储：重写表文件
            String tableFile = dataDirectory + File.separator + tableName + ".tbl";
            try (PrintWriter writer = new PrintWriter(new FileWriter(tableFile, StandardCharsets.UTF_8))) {
                for (Map<String, Object> rec : filteredRecords) {
                    writer.println(serializeRecord(rec));
                }
            }
        }

        System.out.println("成功删除记录，主键: " + primaryKey + "=" + primaryKeyValue);
    }

    /**
     * 更新记录（用于回滚UPDATE操作）
     */
    private void updateRecord(String tableName, Map<String, Object> record) throws IOException {
        // 简化实现：删除旧记录，插入新记录
        deleteRecordByKey(tableName, record);
        insertRecord(tableName, record);
        System.out.println("成功更新记录: " + record);
    }
}