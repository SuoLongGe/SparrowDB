import java.util.*; // 导入Java集合类
import java.util.concurrent.locks.ReentrantReadWriteLock; // 导入读写锁类

/**
 * 缓冲池管理器类 - 管理页面缓存和替换策略
 */
public class BufferPoolManager {
    private final int poolSize; // 缓冲池大小
    private final Page[] pages; // 缓冲池中的页面数组
    private final DiskManager diskManager; // 磁盘管理器实例
    private final Map<Integer, Integer> pageTable; // 页ID到缓冲池索引的映射
    private final List<Integer> freeList; // 空闲页列表
    private final List<Integer> lruList;  // LRU替换策略列表
    private final List<Integer> fifoList; // FIFO替换策略列表
    private final ReplacementPolicy replacementPolicy; // 替换策略
    private final CacheStats stats; // 缓存统计信息
    private final ReentrantReadWriteLock bufferLock; // 缓冲池的读写锁

    /**
     * 构造函数
     */
    public BufferPoolManager(int poolSize, String dbFilename, ReplacementPolicy policy) {
        this.poolSize = poolSize; // 初始化缓冲池大小
        this.pages = new Page[poolSize]; // 初始化页面数组
        this.diskManager = new DiskManager(dbFilename); // 初始化磁盘管理器
        this.pageTable = new HashMap<>(); // 初始化页表
        this.freeList = new ArrayList<>(); // 初始化空闲页列表
        this.lruList = new ArrayList<>(); // 初始化LRU列表
        this.fifoList = new ArrayList<>(); // 初始化FIFO列表
        this.replacementPolicy = policy; // 设置替换策略
        this.stats = new CacheStats(); // 初始化缓存统计信息
        this.bufferLock = new ReentrantReadWriteLock(); // 初始化读写锁

        // 初始化缓冲池
        for (int i = 0; i < poolSize; i++) {
            pages[i] = new Page(); // 创建页面对象
            freeList.add(i); // 将页面索引添加到空闲列表
        }

        System.out.println("Buffer pool manager initialized with " + poolSize + " pages"); // 输出初始化信息
    }

    /**
     * 获取页面
     */
    public Page getPage(int pageId) {
        bufferLock.writeLock().lock(); // 获取写锁
        try {
            // 检查页面是否已在缓冲池中
            Integer frameIndex = pageTable.get(pageId);
            if (frameIndex != null) {
                pages[frameIndex].pin(); // 增加页面的pin计数
                updateReplacementData(frameIndex); // 更新替换策略数据
                stats.recordHit(); // 记录缓存命中
                logCacheHit(pageId); // 记录缓存命中日志
                return pages[frameIndex]; // 返回页面
            }

            // 页面不在缓冲池中，需要加载
            stats.recordMiss(); // 记录缓存未命中
            logCacheMiss(pageId); // 记录缓存未命中日志


            // 找到空闲帧或牺牲页面，优先使用 空闲帧。如果没有空闲帧，就根据替换策略（比如 FIFO / LRU）找到一个牺牲帧（victim page）。如果所有页面都被 Pin（不可替换），就返回失败。
            int targetFrame;
            if (!freeList.isEmpty()) {
                targetFrame = freeList.remove(0); // 从空闲列表中获取帧
            } else {
                targetFrame = findVictimPage(); // 找到牺牲页面
                if (targetFrame == -1) {
                    System.err.println("No available frame for page " + pageId); // 输出错误信息
                    return null; // 返回null
                }
            }

            // 从磁盘加载页面
            if (!diskManager.readPage(pageId, pages[targetFrame].getData())) {
                System.err.println("Failed to read page " + pageId + " from disk"); // 输出错误信息
                return null; // 返回null
            }

            // 更新页面元数据
            pages[targetFrame].setPageId(pageId); // 设置页面ID
            pages[targetFrame].setDirty(false); // 设置页面未被修改
            pages[targetFrame].setPinCount(1); // 设置页面的pin计数为1

            // 更新页表
            pageTable.put(pageId, targetFrame); // 将页面ID和帧索引添加到页表

            // 更新替换数据
            updateReplacementData(targetFrame); // 更新替换策略数据

            return pages[targetFrame]; // 返回页面
        } finally {
            bufferLock.writeLock().unlock(); // 释放写锁
        }
    }

    /**
     * 释放页面，允许页面被替换
     */
    public boolean unpinPage(int pageId, boolean isDirty) {
        bufferLock.writeLock().lock(); // 获取写锁
        try {
            Integer frameIndex = pageTable.get(pageId); // 获取页面的帧索引
            if (frameIndex == null) {
                return false; // 如果页面不在缓冲池中，返回false
            }

            if (pages[frameIndex].getPinCount() <= 0) {
                return false; // 如果页面未被pin，返回false
            }

            pages[frameIndex].unpin(); // 减少页面的pin计数
            if (isDirty) {
                pages[frameIndex].setDirty(true); // 如果页面被修改，设置为dirty
            }

            return true; // 返回成功
        } finally {
            bufferLock.writeLock().unlock(); // 释放写锁
        }
    }

    /**
     * 刷新页面到磁盘
     */
    public boolean flushPage(int pageId) {
        bufferLock.writeLock().lock(); // 获取写锁
        try {
            Integer frameIndex = pageTable.get(pageId); // 获取页面的帧索引
            if (frameIndex == null) {
                return false; // 如果页面不在缓冲池中，返回false
            }

            return flushPageInternal(frameIndex); // 刷新页面到磁盘
        } finally {
            bufferLock.writeLock().unlock(); // 释放写锁
        }
    }

    /**
     * 分配新页面
     */
    public Page newPage(int[] pageId) {
        bufferLock.writeLock().lock(); // 获取写锁
        try {
            // 在磁盘上分配新页面
            pageId[0] = diskManager.allocatePage(); // 分配新页面ID
            if (pageId[0] == Page.INVALID_PAGE_ID) {
                return null; // 如果分配失败，返回null
            }

            // 找到空闲帧
            int frameIndex;
            if (!freeList.isEmpty()) {
                frameIndex = freeList.remove(0); // 从空闲列表中获取帧
            } else {
                frameIndex = findVictimPage(); // 找到牺牲页面
                if (frameIndex == -1) {
                    return null; // 如果没有可用帧，返回null
                }
            }

            // 初始化新页面
            pages[frameIndex] = new Page(pageId[0]); // 创建新页面对象
            pages[frameIndex].setPinCount(1); // 设置页面的pin计数为1

            // 更新页表
            pageTable.put(pageId[0], frameIndex); // 将页面ID和帧索引添加到页表

            // 更新替换数据
            updateReplacementData(frameIndex); // 更新替换策略数据

            return pages[frameIndex]; // 返回新页面
        } finally {
            bufferLock.writeLock().unlock(); // 释放写锁
        }
    }

    /**
     * 删除页面
     */
    public boolean deletePage(int pageId) {
        bufferLock.writeLock().lock(); // 获取写锁
        try {
            Integer frameIndex = pageTable.get(pageId); // 获取页面的帧索引
            if (frameIndex == null) {
                return false; // 如果页面不在缓冲池中，返回false
            }

            // 检查页面是否被pin
            if (pages[frameIndex].isPinned()) {
                return false; // 如果页面被pin，返回false
            }

            // 从页表中移除
            pageTable.remove(pageId); // 移除页表中的页面条目

            // 将帧添加到空闲列表
            freeList.add(frameIndex); // 将帧索引添加到空闲列表

            // 从替换数据中移除
            removeFromReplacementData(frameIndex); // 从替换策略数据中移除

            // 在磁盘上释放页面
            diskManager.deallocatePage(pageId); // 释放磁盘上的页面

            return true; // 返回成功
        } finally {
            bufferLock.writeLock().unlock(); // 释放写锁
        }
    }

    /**
     * 获取统计信息
     */
    public CacheStats getStats() {
        return stats; // 返回缓存统计信息
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
        stats.reset(); // 重置缓存统计信息
    }

    /**
     * 刷新所有页面
     */
    public boolean flushAllPages() {
        bufferLock.writeLock().lock(); // 获取写锁
        try {
            boolean success = true;
            for (int i = 0; i < poolSize; i++) {
                if (pages[i].getPageId() != Page.INVALID_PAGE_ID && pages[i].isDirty()) {
                    if (!flushPageInternal(i)) {
                        success = false; // 如果刷新失败，设置成功标志为false
                    }
                }
            }
            return success; // 返回刷新结果
        } finally {
            bufferLock.writeLock().unlock(); // 释放写锁
        }
    }

    /**
     * 找到牺牲页面
     */
    private int findVictimPage() {
        for (int i = 0; i < poolSize; i++) {
            if (pages[i].getPageId() != Page.INVALID_PAGE_ID && !pages[i].isPinned()) {
                return i; // 返回第一个可牺牲的页面帧索引
            }
        }
        return -1; // 如果没有可牺牲的页面，返回-1
    }

    /**
     * 内部刷新页面
     */
    private boolean flushPageInternal(int frameIndex) {
        if (pages[frameIndex].getPageId() == Page.INVALID_PAGE_ID || !pages[frameIndex].isDirty()) {
            return true; // 如果页面无效或未被修改，返回true
        }

        boolean success = diskManager.writePage(pages[frameIndex].getPageId(), pages[frameIndex].getData()); // 将页面写入磁盘
        if (success) {
            pages[frameIndex].setDirty(false); // 设置页面为未修改
            logPageFlush(pages[frameIndex].getPageId()); // 记录页面刷新日志
        }

        return success; // 返回刷新结果
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
