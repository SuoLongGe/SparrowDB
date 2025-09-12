import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * 存储引擎类 - 对外统一接口
 */
public class StorageEngine {
    private final BufferPoolManager bufferPoolManager;
    private final IndexManager indexManager;
    private final WALManager walManager;
    private final String dbFilePath;
    
    /**
     * 构造函数
     */
    public StorageEngine(int bufferPoolSize, String dbFilename, ReplacementPolicy policy) {
        this.dbFilePath = dbFilename;
        this.bufferPoolManager = new BufferPoolManager(bufferPoolSize, dbFilename, policy);
        this.indexManager = new IndexManager(this);
        
        try {
            this.walManager = new WALManager(dbFilename);
            System.out.println("Storage engine initialized with WAL support");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize WAL Manager: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取页面
     */
    public Page getPage(int pageId) {
        return bufferPoolManager.getPage(pageId);
    }
    
    /**
     * 释放页面
     */
    public boolean releasePage(int pageId, boolean isDirty) {
        return bufferPoolManager.unpinPage(pageId, isDirty);
    }
    
    /**
     * 分配新页面
     */
    public Page allocateNewPage(int[] pageId) {
        return bufferPoolManager.newPage(pageId);
    }
    
    /**
     * 释放页面
     */
    public boolean deallocatePage(int pageId) {
        return bufferPoolManager.deletePage(pageId);
    }
    
    /**
     * 刷新页面
     */
    public boolean flushPage(int pageId) {
        return bufferPoolManager.flushPage(pageId);
    }
    
    /**
     * 刷新所有页面
     */
    public boolean flushAllPages() {
        return bufferPoolManager.flushAllPages();
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        return bufferPoolManager.getStats();
    }
    
    /**
     * 打印缓存统计信息
     */
    public void printCacheStats() {
        bufferPoolManager.printStats();
    }
    
    /**
     * 重置缓存统计信息
     */
    public void resetCacheStats() {
        bufferPoolManager.resetStats();
    }
    
    /**
     * 获取表页面
     */
    public Page getTablePage(int pageId) {
        return getPage(pageId);
    }
    
    /**
     * 分配表页面
     */
    public Page allocateTablePage(int[] pageId) {
        return allocateNewPage(pageId);
    }
    
    /**
     * 将记录写入页面
     */
    public boolean writeRecordToPage(int pageId, String recordData) {
        Page page = getPage(pageId);
        if (page == null) {
            return false;
        }
        
        // 简单的记录存储 - 追加到页面
        // 在实际实现中，这会更加复杂
        if (recordData.length() >= Page.PAGE_SIZE - 100) { // 留一些空间给元数据
            return false;
        }
        
        // 找到现有数据的末尾
        String existingData = page.readString();
        String newData = existingData + recordData;
        
        if (newData.length() >= Page.PAGE_SIZE) {
            return false;
        }
        
        // 写入新数据
        page.writeString(newData);
        releasePage(pageId, true); // 标记为脏页
        return true;
    }
    
    /**
     * 从页面读取记录
     */
    public List<String> readRecordsFromPage(int pageId) {
        Page page = getPage(pageId);
        if (page == null) {
            return new ArrayList<>();
        }
        
        List<String> records = new ArrayList<>();
        String data = page.readString();
        
        if (!data.isEmpty()) {
            // 简单的记录解析 - 按null分隔符分割
            // 在实际实现中，这会更加复杂
            String[] parts = data.split("\0");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    records.add(part);
                }
            }
        }
        
        releasePage(pageId, false);
        return records;
    }
    
    // ========== 索引管理方法 ==========
    
    /**
     * 获取索引管理器
     */
    public IndexManager getIndexManager() {
        return indexManager;
    }
    
    /**
     * 创建整数索引
     */
    public boolean createIntegerIndex(String indexName, int maxKeys) {
        return indexManager.createIntegerIndex(indexName, maxKeys);
    }
    
    /**
     * 创建字符串索引
     */
    public boolean createStringIndex(String indexName, int maxKeys) {
        return indexManager.createStringIndex(indexName, maxKeys);
    }
    
    /**
     * 删除索引
     */
    public boolean dropIndex(String indexName) {
        return indexManager.dropIndex(indexName);
    }
    
    /**
     * 插入键值对到指定索引
     */
    public boolean insertToIndex(String indexName, Object key, int recordPageId) {
        return indexManager.insert(indexName, key, recordPageId);
    }
    
    /**
     * 从指定索引删除键值
     */
    public boolean deleteFromIndex(String indexName, Object key) {
        return indexManager.delete(indexName, key);
    }
    
    /**
     * 在指定索引中查找键值
     */
    public int searchIndex(String indexName, Object key) {
        return indexManager.search(indexName, key);
    }
    
    /**
     * 范围查询
     */
    public List<Integer> rangeSearchIndex(String indexName, Object startKey, Object endKey) {
        return indexManager.rangeSearch(indexName, startKey, endKey);
    }
    
    /**
     * 检查索引是否存在
     */
    public boolean hasIndex(String indexName) {
        return indexManager.hasIndex(indexName);
    }
    
    /**
     * 打印所有索引信息
     */
    public void printAllIndexes() {
        indexManager.printAllIndexes();
    }
    
    /**
     * 打印指定索引信息
     */
    public void printIndexInfo(String indexName) {
        indexManager.printIndexInfo(indexName);
    }
    
    /**
     * 打印指定索引结构
     */
    public void printIndexStructure(String indexName) {
        indexManager.printIndexStructure(indexName);
    }
    
    /**
     * 关闭存储引擎
     * 刷新所有页面并释放资源
     */
    public void close() {
        try {
            // 创建检查点
            walManager.createCheckpoint();
            
            // 刷新所有页面到磁盘
            flushAllPages();
            
            // 关闭WAL管理器
            walManager.close();
            
            System.out.println("Storage engine closed successfully");
        } catch (Exception e) {
            System.err.println("Error closing storage engine: " + e.getMessage());
        }
    }
    
    // ========== WAL相关方法 ==========
    
    /**
     * 开始事务
     */
    public long beginTransaction() {
        try {
            return walManager.beginTransaction();
        } catch (Exception e) {
            System.err.println("Error beginning transaction: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * 提交事务
     */
    public boolean commitTransaction(long transactionId) {
        try {
            walManager.commitTransaction(transactionId);
            return true;
        } catch (Exception e) {
            System.err.println("Error committing transaction: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 回滚事务
     */
    public boolean abortTransaction(long transactionId) {
        try {
            walManager.abortTransaction(transactionId);
            return true;
        } catch (Exception e) {
            System.err.println("Error aborting transaction: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 带WAL的页面写入
     */
    public boolean writePageWithWAL(long transactionId, int pageId, int offset, byte[] newData) {
        try {
            // 获取旧数据用于WAL记录
            Page page = getPage(pageId);
            byte[] oldData = null;
            if (page != null) {
                byte[] pageData = page.getData();
                if (offset + newData.length <= pageData.length) {
                    oldData = new byte[newData.length];
                    System.arraycopy(pageData, offset, oldData, 0, newData.length);
                }
            }
            
            // 先写WAL日志
            walManager.logPageModification(transactionId, pageId, offset, oldData, newData);
            
            // 再写页面数据
            if (page != null) {
                System.arraycopy(newData, 0, page.getData(), offset, newData.length);
                page.setDirty(true);
                releasePage(pageId, true);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error writing page with WAL: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 带WAL的记录写入
     */
    public boolean writeRecordWithWAL(long transactionId, int pageId, String recordData) {
        try {
            // 获取旧数据
            Page page = getPage(pageId);
            String oldData = page != null ? page.readString() : "";
            
            // 先写WAL日志
            walManager.logPageModification(transactionId, pageId, 0, 
                                         oldData.getBytes(), recordData.getBytes());
            
            // 再写页面数据
            if (page != null) {
                page.writeString(recordData);
                releasePage(pageId, true);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error writing record with WAL: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建检查点
     */
    public void createCheckpoint() {
        try {
            walManager.createCheckpoint();
            System.out.println("Checkpoint created successfully");
        } catch (Exception e) {
            System.err.println("Error creating checkpoint: " + e.getMessage());
        }
    }
    
    /**
     * 清理已提交的事务日志
     */
    public void cleanupWAL() {
        try {
            walManager.cleanupCommittedTransactions();
            System.out.println("WAL cleanup completed");
        } catch (Exception e) {
            System.err.println("Error cleaning up WAL: " + e.getMessage());
        }
    }
    
    /**
     * 获取WAL统计信息
     */
    public void printWALStats() {
        try {
            Map<String, Object> stats = walManager.getWALStats();
            System.out.println("\n=== WAL Statistics ===");
            System.out.println("Next LSN: " + stats.get("nextLsn"));
            System.out.println("Current Position: " + stats.get("currentPosition"));
            System.out.println("Active Transactions: " + stats.get("activeTransactions"));
            System.out.println("Total Log Entries: " + stats.get("totalLogEntries"));
            System.out.println("WAL File Size: " + stats.get("walFileSize") + " bytes");
        } catch (Exception e) {
            System.err.println("Error getting WAL stats: " + e.getMessage());
        }
    }
    
    /**
     * 获取WAL管理器
     */
    public WALManager getWALManager() {
        return walManager;
    }

}

