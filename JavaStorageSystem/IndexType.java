/**
 * 索引类型枚举
 */
public enum IndexType {
    BPLUS_TREE("B+树索引"),
    HASH_TABLE("哈希索引");
    
    private final String description;
    
    IndexType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}
