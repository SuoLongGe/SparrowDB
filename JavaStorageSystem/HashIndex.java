import java.util.*;

/**
 * 哈希索引实现
 * 使用链式冲突解决法，支持整数和字符串键
 */
public class HashIndex implements Index {
    private StorageEngine storageEngine;
    private String indexName;
    private int bucketCount;
    private Map<Integer, List<HashEntry>> buckets;
    private int size;
    
    /**
     * 哈希表条目
     */
    private static class HashEntry {
        Object key;
        int recordPageId;
        
        public HashEntry(Object key, int recordPageId) {
            this.key = key;
            this.recordPageId = recordPageId;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            HashEntry hashEntry = (HashEntry) obj;
            return Objects.equals(key, hashEntry.key);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
        
        @Override
        public String toString() {
            return "HashEntry{key=" + key + ", recordPageId=" + recordPageId + "}";
        }
    }
    
    public HashIndex(StorageEngine storageEngine, String indexName, int bucketCount) {
        this.storageEngine = storageEngine;
        this.indexName = indexName;
        this.bucketCount = bucketCount;
        this.buckets = new HashMap<>();
        this.size = 0;
        
        // 初始化桶
        for (int i = 0; i < bucketCount; i++) {
            buckets.put(i, new ArrayList<>());
        }
        
        System.out.println("Created hash index: " + indexName + " with " + bucketCount + " buckets");
    }
    
    @Override
    public boolean insert(Object key, int recordPageId) {
        if (key == null) {
            System.err.println("Key cannot be null");
            return false;
        }
        
        int bucketIndex = hash(key);
        List<HashEntry> bucket = buckets.get(bucketIndex);
        
        // 检查是否已存在
        for (HashEntry entry : bucket) {
            if (Objects.equals(entry.key, key)) {
                // 更新现有条目
                entry.recordPageId = recordPageId;
                return true;
            }
        }
        
        // 添加新条目
        bucket.add(new HashEntry(key, recordPageId));
        size++;
        return true;
    }
    
    @Override
    public boolean delete(Object key) {
        if (key == null) {
            return false;
        }
        
        int bucketIndex = hash(key);
        List<HashEntry> bucket = buckets.get(bucketIndex);
        
        Iterator<HashEntry> iterator = bucket.iterator();
        while (iterator.hasNext()) {
            HashEntry entry = iterator.next();
            if (Objects.equals(entry.key, key)) {
                iterator.remove();
                size--;
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public int search(Object key) {
        if (key == null) {
            return -1;
        }
        
        int bucketIndex = hash(key);
        List<HashEntry> bucket = buckets.get(bucketIndex);
        
        for (HashEntry entry : bucket) {
            if (Objects.equals(entry.key, key)) {
                return entry.recordPageId;
            }
        }
        
        return -1;
    }
    
    @Override
    public List<Integer> rangeSearch(Object startKey, Object endKey) {
        // 哈希索引不支持范围查询
        System.out.println("Hash index does not support range search");
        return new ArrayList<>();
    }
    
    @Override
    public String getIndexName() {
        return indexName;
    }
    
    @Override
    public IndexType getIndexType() {
        return IndexType.HASH_TABLE;
    }
    
    @Override
    public void printInfo() {
        System.out.println("\n=== Hash Index Information: " + indexName + " ===");
        System.out.println("Index Type: " + getIndexType().getDescription());
        System.out.println("Bucket Count: " + bucketCount);
        System.out.println("Size: " + size);
        System.out.println("Load Factor: " + String.format("%.2f", (double) size / bucketCount));
        
        // 统计桶的使用情况
        int usedBuckets = 0;
        int maxBucketSize = 0;
        for (List<HashEntry> bucket : buckets.values()) {
            if (!bucket.isEmpty()) {
                usedBuckets++;
                maxBucketSize = Math.max(maxBucketSize, bucket.size());
            }
        }
        
        System.out.println("Used Buckets: " + usedBuckets + "/" + bucketCount);
        System.out.println("Max Bucket Size: " + maxBucketSize);
    }
    
    @Override
    public void printStructure() {
        System.out.println("\n=== Hash Index Structure: " + indexName + " ===");
        
        for (int i = 0; i < bucketCount; i++) {
            List<HashEntry> bucket = buckets.get(i);
            if (!bucket.isEmpty()) {
                System.out.println("Bucket " + i + ": " + bucket);
            }
        }
    }
    
    @Override
    public boolean isEmpty() {
        return size == 0;
    }
    
    @Override
    public int size() {
        return size;
    }
    
    /**
     * 计算键的哈希值
     */
    private int hash(Object key) {
        if (key instanceof Integer) {
            return Math.abs(((Integer) key).hashCode()) % bucketCount;
        } else if (key instanceof String) {
            return Math.abs(key.hashCode()) % bucketCount;
        } else {
            return Math.abs(key.hashCode()) % bucketCount;
        }
    }
    
    /**
     * 获取所有键值
     */
    public List<Object> getAllKeys() {
        List<Object> keys = new ArrayList<>();
        for (List<HashEntry> bucket : buckets.values()) {
            for (HashEntry entry : bucket) {
                keys.add(entry.key);
            }
        }
        return keys;
    }
    
    /**
     * 获取所有键值对
     */
    public List<HashEntry> getAllEntries() {
        List<HashEntry> entries = new ArrayList<>();
        for (List<HashEntry> bucket : buckets.values()) {
            entries.addAll(bucket);
        }
        return entries;
    }
    
    /**
     * 清空索引
     */
    public void clear() {
        for (List<HashEntry> bucket : buckets.values()) {
            bucket.clear();
        }
        size = 0;
    }
    
    /**
     * 获取桶数量
     */
    public int getBucketCount() {
        return bucketCount;
    }
    
    /**
     * 获取负载因子
     */
    public double getLoadFactor() {
        return (double) size / bucketCount;
    }
}
