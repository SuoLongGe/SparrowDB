package com.database.engine.sharding;

import java.util.Map;
import java.util.HashMap;

/**
 * 分片信息类
 * 存储单个分片的元数据信息
 */
public class ShardInfo {
    private String shardId;
    private String tableName;
    private String nodeId;
    private String dataDirectory;
    private ShardType shardType;
    private Map<String, Object> shardKey; // 分片键范围或哈希值
    private boolean isActive;
    private long recordCount;
    private long lastUpdated;
    
    public enum ShardType {
        RANGE,    // 范围分片
        HASH,     // 哈希分片
        LIST      // 列表分片
    }
    
    public ShardInfo(String shardId, String tableName, String nodeId, String dataDirectory, ShardType shardType) {
        this.shardId = shardId;
        this.tableName = tableName;
        this.nodeId = nodeId;
        this.dataDirectory = dataDirectory;
        this.shardType = shardType;
        this.shardKey = new HashMap<>();
        this.isActive = true;
        this.recordCount = 0;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getShardId() { return shardId; }
    public void setShardId(String shardId) { this.shardId = shardId; }
    
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    
    public String getDataDirectory() { return dataDirectory; }
    public void setDataDirectory(String dataDirectory) { this.dataDirectory = dataDirectory; }
    
    public ShardType getShardType() { return shardType; }
    public void setShardType(ShardType shardType) { this.shardType = shardType; }
    
    public Map<String, Object> getShardKey() { return shardKey; }
    public void setShardKey(Map<String, Object> shardKey) { this.shardKey = shardKey; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public long getRecordCount() { return recordCount; }
    public void setRecordCount(long recordCount) { this.recordCount = recordCount; }
    
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    
    /**
     * 设置范围分片的键值范围
     */
    public void setRangeKey(String columnName, Object minValue, Object maxValue) {
        shardKey.put("column", columnName);
        shardKey.put("min", minValue);
        shardKey.put("max", maxValue);
    }
    
    /**
     * 设置哈希分片的哈希值
     */
    public void setHashKey(String columnName, int hashValue) {
        shardKey.put("column", columnName);
        shardKey.put("hash", hashValue);
    }
    
    /**
     * 检查值是否属于此分片
     */
    public boolean containsValue(Object value) {
        if (!isActive || shardKey.isEmpty()) {
            return false;
        }
        
        switch (shardType) {
            case RANGE:
                return isInRange(value);
            case HASH:
                return isHashMatch(value);
            case LIST:
                return isInList(value);
            default:
                return false;
        }
    }
    
    private boolean isInRange(Object value) {
        Object min = shardKey.get("min");
        Object max = shardKey.get("max");
        
        if (min == null || max == null) {
            return false;
        }
        
        // 统一转换为字符串进行比较（适用于字符串分片键）
        if (value instanceof String && min instanceof String && max instanceof String) {
            String strValue = (String) value;
            String strMin = (String) min;
            String strMax = (String) max;
            return strValue.compareTo(strMin) >= 0 && strValue.compareTo(strMax) < 0;
        }
        
        // 数值类型比较
        if (value instanceof Number && min instanceof Number && max instanceof Number) {
            double numValue = ((Number) value).doubleValue();
            double numMin = ((Number) min).doubleValue();
            double numMax = ((Number) max).doubleValue();
            return numValue >= numMin && numValue < numMax;
        }
        
        // 通用Comparable比较
        if (value instanceof Comparable && min instanceof Comparable && max instanceof Comparable) {
            try {
                @SuppressWarnings("unchecked")
                Comparable<Object> compValue = (Comparable<Object>) value;
                @SuppressWarnings("unchecked")
                Comparable<Object> compMin = (Comparable<Object>) min;
                @SuppressWarnings("unchecked")
                Comparable<Object> compMax = (Comparable<Object>) max;
                
                return compValue.compareTo(compMin) >= 0 && compValue.compareTo(compMax) < 0;
            } catch (ClassCastException e) {
                // 类型不兼容，尝试字符串比较
                String strValue = value.toString();
                String strMin = min.toString();
                String strMax = max.toString();
                return strValue.compareTo(strMin) >= 0 && strValue.compareTo(strMax) < 0;
            }
        }
        
        return false;
    }
    
    private boolean isHashMatch(Object value) {
        Object hash = shardKey.get("hash");
        if (hash == null) {
            return false;
        }
        
        int valueHash = Math.abs(value.hashCode());
        int targetHash = (Integer) hash;
        
        return valueHash % targetHash == 0;
    }
    
    private boolean isInList(Object value) {
        @SuppressWarnings("unchecked")
        java.util.List<Object> list = (java.util.List<Object>) shardKey.get("values");
        return list != null && list.contains(value);
    }
    
    @Override
    public String toString() {
        return String.format("ShardInfo{id='%s', table='%s', node='%s', type=%s, active=%s, records=%d}", 
                           shardId, tableName, nodeId, shardType, isActive, recordCount);
    }
}
