import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 索引管理器 - 管理所有B+树索引
 */
public class IndexManager {
    private StorageEngine storageEngine;
    private Map<String, BPlusTree> indexes; // 索引名称到B+树的映射
    private Map<String, Class<? extends BPlusTreeKey>> keyTypes; // 索引名称到键类型的映射
    
    public IndexManager(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
        this.indexes = new HashMap<>();
        this.keyTypes = new HashMap<>();
    }
    
    /**
     * 创建整数索引
     */
    public boolean createIntegerIndex(String indexName, int maxKeys) {
        if (indexes.containsKey(indexName)) {
            System.err.println("Index " + indexName + " already exists");
            return false;
        }
        
        BPlusTree index = new BPlusTree(storageEngine, indexName, maxKeys);
        indexes.put(indexName, index);
        keyTypes.put(indexName, IntegerKey.class);
        
        System.out.println("Created integer index: " + indexName);
        return true;
    }
    
    /**
     * 创建字符串索引
     */
    public boolean createStringIndex(String indexName, int maxKeys) {
        if (indexes.containsKey(indexName)) {
            System.err.println("Index " + indexName + " already exists");
            return false;
        }
        
        BPlusTree index = new BPlusTree(storageEngine, indexName, maxKeys);
        indexes.put(indexName, index);
        keyTypes.put(indexName, StringKey.class);
        
        System.out.println("Created string index: " + indexName);
        return true;
    }
    
    /**
     * 删除索引
     */
    public boolean dropIndex(String indexName) {
        if (!indexes.containsKey(indexName)) {
            System.err.println("Index " + indexName + " does not exist");
            return false;
        }
        
        indexes.remove(indexName);
        keyTypes.remove(indexName);
        
        System.out.println("Dropped index: " + indexName);
        return true;
    }
    
    /**
     * 插入键值对到指定索引
     */
    public boolean insert(String indexName, Object key, int recordPageId) {
        BPlusTree index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return false;
        }
        
        BPlusTreeKey bPlusKey = createKey(indexName, key);
        if (bPlusKey == null) {
            return false;
        }
        
        return index.insert(bPlusKey, recordPageId);
    }
    
    /**
     * 从指定索引删除键值
     */
    public boolean delete(String indexName, Object key) {
        BPlusTree index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return false;
        }
        
        BPlusTreeKey bPlusKey = createKey(indexName, key);
        if (bPlusKey == null) {
            return false;
        }
        
        return index.delete(bPlusKey);
    }
    
    /**
     * 在指定索引中查找键值
     */
    public int search(String indexName, Object key) {
        BPlusTree index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return -1;
        }
        
        BPlusTreeKey bPlusKey = createKey(indexName, key);
        if (bPlusKey == null) {
            return -1;
        }
        
        return index.search(bPlusKey);
    }
    
    /**
     * 范围查询
     */
    public List<Integer> rangeSearch(String indexName, Object startKey, Object endKey) {
        BPlusTree index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return List.of();
        }
        
        BPlusTreeKey bPlusStartKey = createKey(indexName, startKey);
        BPlusTreeKey bPlusEndKey = createKey(indexName, endKey);
        if (bPlusStartKey == null || bPlusEndKey == null) {
            return List.of();
        }
        
        return index.rangeSearch(bPlusStartKey, bPlusEndKey);
    }
    
    /**
     * 获取索引中的所有键值
     */
    public List<BPlusTreeKey> getAllKeys(String indexName) {
        BPlusTree index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return List.of();
        }
        
        return index.getAllKeys();
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
     * 获取索引信息
     */
    public void printIndexInfo(String indexName) {
        BPlusTree index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return;
        }
        
        System.out.println("\n=== Index Information: " + indexName + " ===");
        System.out.println("Root Page ID: " + index.getRootPageId());
        System.out.println("Max Keys: " + index.getMaxKeys());
        System.out.println("Height: " + index.getHeight());
        System.out.println("Node Count: " + index.getNodeCount());
        System.out.println("Key Type: " + keyTypes.get(indexName).getSimpleName());
    }
    
    /**
     * 打印索引结构
     */
    public void printIndexStructure(String indexName) {
        BPlusTree index = indexes.get(indexName);
        if (index == null) {
            System.err.println("Index " + indexName + " does not exist");
            return;
        }
        
        System.out.println("\n=== Index Structure: " + indexName + " ===");
        index.printTree();
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
     * 创建键值对象
     */
    private BPlusTreeKey createKey(String indexName, Object key) {
        Class<? extends BPlusTreeKey> keyType = keyTypes.get(indexName);
        if (keyType == null) {
            System.err.println("Unknown key type for index " + indexName);
            return null;
        }
        
        try {
            if (keyType == IntegerKey.class) {
                if (key instanceof Integer) {
                    return new IntegerKey((Integer) key);
                } else if (key instanceof String) {
                    return new IntegerKey(Integer.parseInt((String) key));
                } else {
                    System.err.println("Invalid key type for integer index: " + key.getClass().getSimpleName());
                    return null;
                }
            } else if (keyType == StringKey.class) {
                if (key instanceof String) {
                    return new StringKey((String) key);
                } else {
                    return new StringKey(key.toString());
                }
            } else {
                System.err.println("Unsupported key type: " + keyType.getSimpleName());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error creating key: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取存储引擎
     */
    public StorageEngine getStorageEngine() {
        return storageEngine;
    }
    
    /**
     * 获取索引
     */
    public BPlusTree getIndex(String indexName) {
        return indexes.get(indexName);
    }
    
    /**
     * 获取键类型
     */
    public Class<? extends BPlusTreeKey> getKeyType(String indexName) {
        return keyTypes.get(indexName);
    }
}
