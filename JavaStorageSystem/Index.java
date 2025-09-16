import java.util.List;

/**
 * 索引接口 - 定义所有索引类型的通用操作
 */
public interface Index {
    
    /**
     * 插入键值对
     * @param key 键
     * @param recordPageId 记录页面ID
     * @return 是否插入成功
     */
    boolean insert(Object key, int recordPageId);
    
    /**
     * 删除键值对
     * @param key 键
     * @return 是否删除成功
     */
    boolean delete(Object key);
    
    /**
     * 查找键值
     * @param key 键
     * @return 记录页面ID，未找到返回-1
     */
    int search(Object key);
    
    /**
     * 范围查询
     * @param startKey 起始键
     * @param endKey 结束键
     * @return 记录页面ID列表
     */
    List<Integer> rangeSearch(Object startKey, Object endKey);
    
    /**
     * 获取索引名称
     * @return 索引名称
     */
    String getIndexName();
    
    /**
     * 获取索引类型
     * @return 索引类型
     */
    IndexType getIndexType();
    
    /**
     * 打印索引信息
     */
    void printInfo();
    
    /**
     * 打印索引结构
     */
    void printStructure();
    
    /**
     * 检查索引是否为空
     * @return 是否为空
     */
    boolean isEmpty();
    
    /**
     * 获取索引大小
     * @return 索引中的键值对数量
     */
    int size();
}
