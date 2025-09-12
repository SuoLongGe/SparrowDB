import java.util.List;
import java.util.ArrayList;

/**
 * 存储引擎类 - 对外统一接口
 */
public class StorageEngine {
    private final BufferPoolManager bufferPoolManager;
    private final ExtendedIndexManager indexManager;
    
    /**
     * 构造函数
     */
    public StorageEngine(int bufferPoolSize, String dbFilename, ReplacementPolicy policy) {
        this.bufferPoolManager = new BufferPoolManager(bufferPoolSize, dbFilename, policy);
        // 使用扩展的索引管理器，支持B+树和哈希索引
        this.indexManager = new ExtendedIndexManager(this);
        System.out.println("Storage engine initialized with dual index support (B+ Tree and Hash)");
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
    public ExtendedIndexManager getIndexManager() {
        return indexManager;
    }
    
    /**
     * 创建整数B+树索引
     */
    public boolean createIntegerIndex(String indexName, int maxKeys) {
        return indexManager.createIntegerBPlusIndex(indexName, maxKeys);
    }
    
    /**
     * 创建字符串B+树索引
     */
    public boolean createStringIndex(String indexName, int maxKeys) {
        return indexManager.createStringBPlusIndex(indexName, maxKeys);
    }
    
    /**
     * 创建整数B+树索引（显式方法）
     */
    public boolean createIntegerBPlusIndex(String indexName, int maxKeys) {
        return indexManager.createIntegerBPlusIndex(indexName, maxKeys);
    }
    
    /**
     * 创建字符串B+树索引（显式方法）
     */
    public boolean createStringBPlusIndex(String indexName, int maxKeys) {
        return indexManager.createStringBPlusIndex(indexName, maxKeys);
    }
    
    /**
     * 创建整数哈希索引
     */
    public boolean createIntegerHashIndex(String indexName, int bucketCount) {
        return indexManager.createIntegerHashIndex(indexName, bucketCount);
    }
    
    /**
     * 创建字符串哈希索引
     */
    public boolean createStringHashIndex(String indexName, int bucketCount) {
        return indexManager.createStringHashIndex(indexName, bucketCount);
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
     * 获取索引类型
     */
    public IndexType getIndexType(String indexName) {
        return indexManager.getIndexType(indexName);
    }
    
    /**
     * 按类型打印索引
     */
    public void printIndexesByType(IndexType type) {
        indexManager.printIndexesByType(type);
    }
    
    /**
     * 打印索引统计信息
     */
    public void printIndexStatistics() {
        indexManager.printStatistics();
    }
}
