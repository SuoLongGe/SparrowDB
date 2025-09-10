import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 缓冲池管理器类 - 管理页面缓存和替换策略
 */
public class BufferPoolManager {
    private final int poolSize;
    private final Page[] pages;
    private final DiskManager diskManager;
    private final Map<Integer, Integer> pageTable; // 页ID到缓冲池索引的映射
    private final List<Integer> freeList; // 空闲页列表
    private final List<Integer> lruList;  // LRU列表
    private final List<Integer> fifoList; // FIFO列表
    private final ReplacementPolicy replacementPolicy;
    private final CacheStats stats;
    private final ReentrantReadWriteLock bufferLock;
    
    /**
     * 构造函数
     */
    public BufferPoolManager(int poolSize, String dbFilename, ReplacementPolicy policy) {
        this.poolSize = poolSize;
        this.pages = new Page[poolSize];
        this.diskManager = new DiskManager(dbFilename);
        this.pageTable = new HashMap<>();
        this.freeList = new ArrayList<>();
        this.lruList = new ArrayList<>();
        this.fifoList = new ArrayList<>();
        this.replacementPolicy = policy;
        this.stats = new CacheStats();
        this.bufferLock = new ReentrantReadWriteLock();
        
        // 初始化缓冲池
        for (int i = 0; i < poolSize; i++) {
            pages[i] = new Page();
            freeList.add(i);
        }
        
        System.out.println("Buffer pool manager initialized with " + poolSize + " pages");
    }
    
    /**
     * 获取页面
     */
    public Page getPage(int pageId) {
        bufferLock.writeLock().lock();
        try {
            // 检查页面是否已在缓冲池中
            Integer frameIndex = pageTable.get(pageId);
            if (frameIndex != null) {
                pages[frameIndex].pin();
                updateReplacementData(frameIndex);
                stats.recordHit();
                logCacheHit(pageId);
                return pages[frameIndex];
            }
            
            // 页面不在缓冲池中，需要加载
            stats.recordMiss();
            logCacheMiss(pageId);
            
            // 找到空闲帧或牺牲页面
            int targetFrame;
            if (!freeList.isEmpty()) {
                targetFrame = freeList.remove(0);
            } else {
                targetFrame = findVictimPage();
                if (targetFrame == -1) {
                    System.err.println("No available frame for page " + pageId);
                    return null;
                }
            }
            
            // 从磁盘加载页面
            if (!diskManager.readPage(pageId, pages[targetFrame].getData())) {
                System.err.println("Failed to read page " + pageId + " from disk");
                return null;
            }
            
            // 更新页面元数据
            pages[targetFrame].setPageId(pageId);
            pages[targetFrame].setDirty(false);
            pages[targetFrame].setPinCount(1);
            
            // 更新页表
            pageTable.put(pageId, targetFrame);
            
            // 更新替换数据
            updateReplacementData(targetFrame);
            
            return pages[targetFrame];
        } finally {
            bufferLock.writeLock().unlock();
        }
    }
    
    /**
     * 释放页面
     */
    public boolean unpinPage(int pageId, boolean isDirty) {
        bufferLock.writeLock().lock();
        try {
            Integer frameIndex = pageTable.get(pageId);
            if (frameIndex == null) {
                return false;
            }
            
            if (pages[frameIndex].getPinCount() <= 0) {
                return false;
            }
            
            pages[frameIndex].unpin();
            if (isDirty) {
                pages[frameIndex].setDirty(true);
            }
            
            return true;
        } finally {
            bufferLock.writeLock().unlock();
        }
    }
    
    /**
     * 刷新页面到磁盘
     */
    public boolean flushPage(int pageId) {
        bufferLock.writeLock().lock();
        try {
            Integer frameIndex = pageTable.get(pageId);
            if (frameIndex == null) {
                return false;
            }
            
            return flushPageInternal(frameIndex);
        } finally {
            bufferLock.writeLock().unlock();
        }
    }
    
    /**
     * 分配新页面
     */
    public Page newPage(int[] pageId) {
        bufferLock.writeLock().lock();
        try {
            // 在磁盘上分配新页面
            pageId[0] = diskManager.allocatePage();
            if (pageId[0] == Page.INVALID_PAGE_ID) {
                return null;
            }
            
            // 找到空闲帧
            int frameIndex;
            if (!freeList.isEmpty()) {
                frameIndex = freeList.remove(0);
            } else {
                frameIndex = findVictimPage();
                if (frameIndex == -1) {
                    return null;
                }
            }
            
            // 初始化新页面
            pages[frameIndex] = new Page(pageId[0]);
            pages[frameIndex].setPinCount(1);
            
            // 更新页表
            pageTable.put(pageId[0], frameIndex);
            
            // 更新替换数据
            updateReplacementData(frameIndex);
            
            return pages[frameIndex];
        } finally {
            bufferLock.writeLock().unlock();
        }
    }
    
    /**
     * 删除页面
     */
    public boolean deletePage(int pageId) {
        bufferLock.writeLock().lock();
        try {
            Integer frameIndex = pageTable.get(pageId);
            if (frameIndex == null) {
                return false;
            }
            
            // 检查页面是否被pin
            if (pages[frameIndex].isPinned()) {
                return false;
            }
            
            // 从页表中移除
            pageTable.remove(pageId);
            
            // 将帧添加到空闲列表
            freeList.add(frameIndex);
            
            // 从替换数据中移除
            removeFromReplacementData(frameIndex);
            
            // 在磁盘上释放页面
            diskManager.deallocatePage(pageId);
            
            return true;
        } finally {
            bufferLock.writeLock().unlock();
        }
    }
    
    /**
     * 获取统计信息
     */
    public CacheStats getStats() {
        return stats;
    }
    
    /**
     * 打印统计信息
     */
    public void printStats() {
        System.out.println("\n=== Buffer Pool Statistics ===");
        System.out.println("Cache hits: " + stats.getCacheHits());
        System.out.println("Cache misses: " + stats.getCacheMisses());
        System.out.println("Total accesses: " + stats.getTotalAccesses());
        System.out.printf("Hit rate: %.2f%%\n", stats.getHitRate() * 100);
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        stats.reset();
    }
    
    /**
     * 刷新所有页面
     */
    public boolean flushAllPages() {
        bufferLock.writeLock().lock();
        try {
            boolean success = true;
            for (int i = 0; i < poolSize; i++) {
                if (pages[i].getPageId() != Page.INVALID_PAGE_ID && pages[i].isDirty()) {
                    if (!flushPageInternal(i)) {
                        success = false;
                    }
                }
            }
            return success;
        } finally {
            bufferLock.writeLock().unlock();
        }
    }
    
    /**
     * 找到牺牲页面
     */
    private int findVictimPage() {
        for (int i = 0; i < poolSize; i++) {
            if (pages[i].getPageId() != Page.INVALID_PAGE_ID && !pages[i].isPinned()) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 内部刷新页面
     */
    private boolean flushPageInternal(int frameIndex) {
        if (pages[frameIndex].getPageId() == Page.INVALID_PAGE_ID || !pages[frameIndex].isDirty()) {
            return true;
        }
        
        boolean success = diskManager.writePage(pages[frameIndex].getPageId(), pages[frameIndex].getData());
        if (success) {
            pages[frameIndex].setDirty(false);
            logPageFlush(pages[frameIndex].getPageId());
        }
        
        return success;
    }
    
    /**
     * 更新替换数据
     */
    private void updateReplacementData(int frameIndex) {
        if (replacementPolicy == ReplacementPolicy.LRU) {
            // 从LRU列表中移除（如果存在）
            lruList.remove(Integer.valueOf(frameIndex));
            // 添加到末尾（最近使用）
            lruList.add(frameIndex);
        } else if (replacementPolicy == ReplacementPolicy.FIFO) {
            // 如果不在FIFO列表中，则添加
            if (!fifoList.contains(frameIndex)) {
                fifoList.add(frameIndex);
            }
        }
    }
    
    /**
     * 从替换数据中移除
     */
    private void removeFromReplacementData(int frameIndex) {
        if (replacementPolicy == ReplacementPolicy.LRU) {
            lruList.remove(Integer.valueOf(frameIndex));
        } else if (replacementPolicy == ReplacementPolicy.FIFO) {
            fifoList.remove(Integer.valueOf(frameIndex));
        }
    }
    
    /**
     * 记录缓存命中
     */
    private void logCacheHit(int pageId) {
        System.out.println("[CACHE HIT] Page " + pageId);
    }
    
    /**
     * 记录缓存未命中
     */
    private void logCacheMiss(int pageId) {
        System.out.println("[CACHE MISS] Page " + pageId);
    }
    
    /**
     * 记录页面刷新
     */
    private void logPageFlush(int pageId) {
        System.out.println("[PAGE FLUSH] Page " + pageId);
    }
}
