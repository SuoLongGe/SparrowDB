import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 扩展的索引管理器 - 支持B+树索引和哈希索引
 */
public class ExtendedIndexManager {
    private StorageEngine storageEngine;
    private Map<String, Index> indexes; // 索引名称到索引实现的映射
    private Map<String, IndexType> indexTypes; // 索引名称到索引类型的映射
    private Map<String, Class<?>> keyTypes; // 索引名称到键类型的映射
    
    public ExtendedIndexManager(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
        this.indexes = new HashMap<>();
        this.indexTypes = new HashMap<>();
        this.keyTypes = new HashMap<>();
    }
    
    // ========== B+树索引方法 ==========
    
    /**
     * 创建整数B+树索引
     */
    public boolean createIntegerBPlusIndex(String indexName, int maxKeys) {
        if (indexes.containsKey(indexName)) {
            System.err.println("Index " + indexName + " already exists");
            return false;
        }
        
        BPlusTree index = new BPlusTree(storageEngine, indexName, maxKeys);
        indexes.put(indexName, index);
        indexTypes.put(indexName, IndexType.BPLUS_TREE);
        keyTypes.put(indexName, IntegerKey.class);
        
        System.out.println("Created integer B+ tree index: " + indexName);
        return true;
    }
    
    /**
     * 创建字符串B+树索引
     */
    public boolean createStringBPlusIndex(String indexName, int maxKeys) {
        if (indexes.containsKey(indexName)) {
            System.err.println("Index " + indexName + " already exists");
            return false;
        }
        
        BPlusTree index = new BPlusTree(storageEngine, indexName, maxKeys);
        indexes.put(indexName, index);
        indexTypes.put(indexName, IndexType.BPLUS_TREE);
        keyTypes.put(indexName, StringKey.class);
        
        System.out.println("Created string B+ tree index: " + indexName);
        return true;
    }
    
    // ========== 哈希索引方法 ==========
    
    /**
     * 创建整数哈希索引
     */
    public boolean createIntegerHashIndex(String indexName, int bucketCount) {
        if (indexes.containsKey(indexName)) {
            System.err.println("Index " + indexName + " already exists");
            return false;
        }
        
        HashIndex index = new HashIndex(storageEngine, indexName, bucketCount);
        indexes.put(indexName, index);
        indexTypes.put(indexName, IndexType.HASH_TABLE);
        keyTypes.put(indexName, Integer.class);
        
        System.out.println("Created integer hash index: " + indexName);
        return true;
    }
    
    /**
     * 创建字符串哈希索引
     */
    public boolean createStringHashIndex(String indexName, int bucketCount) {
        if (indexes.containsKey(indexName)) {
            System.err.println("Index " + indexName + " already exists");
            return false;
        }
        
        HashIndex index = new HashIndex(storageEngine, indexName, bucketCount);
        indexes.put(indexName, index);
        indexTypes.put(indexName, IndexType.HASH_TABLE);
        keyTypes.put(indexName, String.class);
        
        System.out.println("Created string hash index: " + indexName);
        return true;
    }
    
    // ========== 通用索引操作 ==========
    
    /**
     * 删除索引
     */
    public boolean dropIndex(String indexName) {
        if (!indexes.containsKey(indexName)) {
            System.err.println("Index " + indexName + " does not exist");
            return false;
        }
        
        indexes.remove(indexName);
        indexTypes.remove(indexName);
        keyTypes.remove(indexName);
        
        System.out.println("Dropped index: " + indexName);
        return true;
    }
    
    /**
     * 插入键值对到指定索引
     */
    public boolean insert(String indexName, Object key, int recordPageId) {
        Index index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return false;
        }
        
        return index.insert(key, recordPageId);
    }
    
    /**
     * 从指定索引删除键值
     */
    public boolean delete(String indexName, Object key) {
        Index index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return false;
        }
        
        return index.delete(key);
    }
    
    /**
     * 在指定索引中查找键值
     */
    public int search(String indexName, Object key) {
        Index index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return -1;
        }
        
        return index.search(key);
    }
    
    /**
     * 范围查询
     */
    public List<Integer> rangeSearch(String indexName, Object startKey, Object endKey) {
        Index index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return List.of();
        }
        
        return index.rangeSearch(startKey, endKey);
    }
    
    /**
     * 检查索引是否存在
     */
    public boolean hasIndex(String indexName) {
        return indexes.containsKey(indexName);
    }
    
    /**
     * 获取所有索引名称
     */
    public Set<String> getAllIndexNames() {
        return indexes.keySet();
    }
    
    /**
     * 获取索引类型
     */
    public IndexType getIndexType(String indexName) {
        return indexTypes.get(indexName);
    }
    
    /**
     * 获取键类型
     */
    public Class<?> getKeyType(String indexName) {
        return keyTypes.get(indexName);
    }
    
    /**
     * 获取索引
     */
    public Index getIndex(String indexName) {
        return indexes.get(indexName);
    }
    
    /**
     * 获取B+树索引
     */
    public BPlusTree getBPlusIndex(String indexName) {
        Index index = indexes.get(indexName);
        if (index instanceof BPlusTree) {
            return (BPlusTree) index;
        }
        return null;
    }
    
    /**
     * 获取哈希索引
     */
    public HashIndex getHashIndex(String indexName) {
        Index index = indexes.get(indexName);
        if (index instanceof HashIndex) {
            return (HashIndex) index;
        }
        return null;
    }
    
    /**
     * 获取索引信息
     */
    public void printIndexInfo(String indexName) {
        Index index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return;
        }
        
        index.printInfo();
    }
    
    /**
     * 打印索引结构
     */
    public void printIndexStructure(String indexName) {
        Index index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return;
        }
        
        index.printStructure();
    }
    
    /**
     * 打印所有索引信息
     */
    public void printAllIndexes() {
        System.out.println("\n=== All Indexes ===");
        if (indexes.isEmpty()) {
            System.out.println("No indexes created");
            return;
        }
        
        for (String indexName : indexes.keySet()) {
            printIndexInfo(indexName);
        }
    }
    
    /**
     * 按类型打印索引
     */
    public void printIndexesByType(IndexType type) {
        System.out.println("\n=== " + type.getDescription() + " ===");
        boolean found = false;
        
        for (Map.Entry<String, IndexType> entry : indexTypes.entrySet()) {
            if (entry.getValue() == type) {
                printIndexInfo(entry.getKey());
                found = true;
            }
        }
        
        if (!found) {
            System.out.println("No " + type.getDescription() + " found");
        }
    }
    
    /**
     * 获取存储引擎
     */
    public StorageEngine getStorageEngine() {
        return storageEngine;
    }
    
    /**
     * 获取索引统计信息
     */
    public void printStatistics() {
        System.out.println("\n=== Index Statistics ===");
        System.out.println("Total Indexes: " + indexes.size());
        
        int bplusCount = 0;
        int hashCount = 0;
        int totalSize = 0;
        
        for (IndexType type : indexTypes.values()) {
            if (type == IndexType.BPLUS_TREE) {
                bplusCount++;
            } else if (type == IndexType.HASH_TABLE) {
                hashCount++;
            }
        }
        
        for (Index index : indexes.values()) {
            totalSize += index.size();
        }
        
        System.out.println("B+ Tree Indexes: " + bplusCount);
        System.out.println("Hash Indexes: " + hashCount);
        System.out.println("Total Entries: " + totalSize);
    }
}
