import java.util.List;
import java.util.ArrayList;

/**
 * 存储引擎类 - 对外统一接口
 */
public class StorageEngine {
    private final BufferPoolManager bufferPoolManager;
    
    /**
     * 构造函数
     */
    public StorageEngine(int bufferPoolSize, String dbFilename, ReplacementPolicy policy) {
        this.bufferPoolManager = new BufferPoolManager(bufferPoolSize, dbFilename, policy);
        System.out.println("Storage engine initialized");
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
}
