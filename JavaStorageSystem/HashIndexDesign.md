# 哈希索引设计文档

## 实现方式选择

### 推荐方案：在IndexManager基础上扩展

**优势**：
1. **统一接口**：保持与B+树索引相同的API接口
2. **代码复用**：复用现有的存储引擎、页面管理、缓存等基础设施
3. **灵活选择**：可以根据查询类型选择最适合的索引类型
4. **维护简单**：统一的索引管理，便于维护和扩展

### 不推荐：单独实现哈希索引

**劣势**：
1. **代码重复**：需要重新实现存储、缓存、序列化等功能
2. **接口不统一**：需要维护两套不同的API
3. **资源浪费**：无法共享存储引擎的资源

## 设计架构

### 1. 索引类型枚举

```java
public enum IndexType {
    BPLUS_TREE,    // B+树索引
    HASH_TABLE     // 哈希索引
}
```

### 2. 索引接口抽象

```java
public interface Index {
    boolean insert(Object key, int recordPageId);
    boolean delete(Object key);
    int search(Object key);
    List<Integer> rangeSearch(Object startKey, Object endKey);
    void printInfo();
    void printStructure();
    // ... 其他通用方法
}
```

### 3. 哈希索引实现

```java
public class HashIndex implements Index {
    private StorageEngine storageEngine;
    private String indexName;
    private int bucketCount;
    private Map<Integer, List<HashEntry>> buckets;
    
    // 实现Index接口的所有方法
    // 哈希索引不支持范围查询，rangeSearch返回空列表
}
```

### 4. 扩展IndexManager

```java
public class IndexManager {
    private Map<String, Index> indexes;  // 改为通用Index接口
    private Map<String, IndexType> indexTypes;  // 记录索引类型
    private Map<String, Class<?>> keyTypes;
    
    // 新增哈希索引创建方法
    public boolean createHashIndex(String indexName, int bucketCount);
    
    // 现有方法保持不变，通过多态调用具体实现
}
```

## 哈希索引特点

### 优势
1. **O(1)查找**：平均情况下查找时间复杂度为O(1)
2. **插入快速**：插入操作通常很快
3. **内存友好**：可以完全在内存中实现

### 劣势
1. **不支持范围查询**：只能进行等值查询
2. **哈希冲突**：需要处理哈希冲突
3. **内存限制**：大数据集可能超出内存限制

## 使用场景对比

| 查询类型 | B+树索引 | 哈希索引 |
|---------|---------|---------|
| 等值查询 | O(log n) | O(1) |
| 范围查询 | O(log n + k) | 不支持 |
| 排序查询 | 支持 | 不支持 |
| 插入性能 | O(log n) | O(1) |
| 删除性能 | O(log n) | O(1) |
| 内存使用 | 较少 | 较多 |

## 实现建议

### 1. 渐进式实现
- 先实现HashIndex类
- 再扩展IndexManager
- 最后更新StorageEngine接口

### 2. 配置选择
- 小数据集、频繁等值查询 → 哈希索引
- 大数据集、需要范围查询 → B+树索引
- 混合场景 → 同时创建两种索引

### 3. 性能优化
- 哈希索引使用链式冲突解决
- 支持动态扩容
- 内存和磁盘混合存储

## 代码结构

```
IndexManager.java          # 扩展支持多种索引类型
├── Index.java            # 索引接口
├── BPlusTree.java        # B+树索引实现
├── HashIndex.java        # 哈希索引实现
└── IndexType.java        # 索引类型枚举
```

## 总结

推荐在现有IndexManager基础上扩展哈希索引，这样可以：
1. 保持API的一致性
2. 复用现有的存储基础设施
3. 提供灵活的索引选择
4. 便于后续扩展其他索引类型（如位图索引、倒排索引等）
