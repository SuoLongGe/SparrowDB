/**
 * 缓存统计信息类
 */
public class CacheStats {
    private int cacheHits;
    private int cacheMisses;
    private int totalAccesses;
    
    /**
     * 构造函数
     */
    public CacheStats() {
        this.cacheHits = 0;
        this.cacheMisses = 0;
        this.totalAccesses = 0;
    }
    
    /**
     * 记录缓存命中
     */
    public void recordHit() {
        this.cacheHits++;
        this.totalAccesses++;
    }
    
    /**
     * 记录缓存未命中
     */
    public void recordMiss() {
        this.cacheMisses++;
        this.totalAccesses++;
    }
    
    /**
     * 获取缓存命中率
     */
    public double getHitRate() {
        return totalAccesses > 0 ? (double) cacheHits / totalAccesses : 0.0;
    }
    
    /**
     * 重置统计信息
     */
    public void reset() {
        this.cacheHits = 0;
        this.cacheMisses = 0;
        this.totalAccesses = 0;
    }
    
    // Getter方法
    public int getCacheHits() {
        return cacheHits;
    }
    
    public int getCacheMisses() {
        return cacheMisses;
    }
    
    public int getTotalAccesses() {
        return totalAccesses;
    }
    
    @Override
    public String toString() {
        return String.format("CacheStats{hits=%d, misses=%d, total=%d, hitRate=%.2f%%}", 
                           cacheHits, cacheMisses, totalAccesses, getHitRate() * 100);
    }
}
