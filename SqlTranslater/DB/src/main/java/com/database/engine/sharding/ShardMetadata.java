package com.database.engine.sharding;

import java.util.List;
import java.util.ArrayList;

/**
 * 分片元数据类
 * 存储表的分片配置信息
 */
public class ShardMetadata {
    private String tableName;
    private String shardKeyColumn;
    private ShardStrategy strategy;
    private List<ShardInfo> shards;
    private long createdAt;
    private long lastModified;
    
    public ShardMetadata(String tableName, String shardKeyColumn, ShardStrategy strategy, List<ShardInfo> shards) {
        this.tableName = tableName;
        this.shardKeyColumn = shardKeyColumn;
        this.strategy = strategy;
        this.shards = new ArrayList<>(shards);
        this.createdAt = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    
    public String getShardKeyColumn() { return shardKeyColumn; }
    public void setShardKeyColumn(String shardKeyColumn) { this.shardKeyColumn = shardKeyColumn; }
    
    public ShardStrategy getStrategy() { return strategy; }
    public void setStrategy(ShardStrategy strategy) { this.strategy = strategy; }
    
    public List<ShardInfo> getShards() { return shards; }
    public void setShards(List<ShardInfo> shards) { this.shards = shards; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }
    
    /**
     * 添加分片
     */
    public void addShard(ShardInfo shard) {
        shards.add(shard);
        lastModified = System.currentTimeMillis();
    }
    
    /**
     * 移除分片
     */
    public boolean removeShard(String shardId) {
        boolean removed = shards.removeIf(shard -> shardId.equals(shard.getShardId()));
        if (removed) {
            lastModified = System.currentTimeMillis();
        }
        return removed;
    }
    
    /**
     * 获取分片数量
     */
    public int getShardCount() {
        return shards.size();
    }
    
    /**
     * 获取活跃分片数量
     */
    public int getActiveShardCount() {
        return (int) shards.stream().filter(ShardInfo::isActive).count();
    }
    
    /**
     * 获取总记录数
     */
    public long getTotalRecordCount() {
        return shards.stream().mapToLong(ShardInfo::getRecordCount).sum();
    }
    
    /**
     * 转换为文件格式
     */
    public String toFileFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append(tableName).append("|");
        sb.append(shardKeyColumn).append("|");
        sb.append(strategy.getStrategyName()).append("|");
        sb.append(shards.size()).append("|");
        sb.append(createdAt).append("|");
        sb.append(lastModified);
        
        // 添加分片详细信息
        for (ShardInfo shard : shards) {
            sb.append("|").append(shard.getShardId()).append(":");
            sb.append(shard.getNodeId()).append(":");
            sb.append(shard.getDataDirectory()).append(":");
            sb.append(shard.getShardType()).append(":");
            sb.append(shard.isActive()).append(":");
            sb.append(shard.getRecordCount());
        }
        
        return sb.toString();
    }
    
    /**
     * 从文件格式解析
     */
    public static ShardMetadata fromFileFormat(String line) {
        try {
            String[] parts = line.split("\\|");
            if (parts.length < 6) {
                return null;
            }
            
            String tableName = parts[0];
            String shardKeyColumn = parts[1];
            String strategyName = parts[2];
            int shardCount = Integer.parseInt(parts[3]);
            long createdAt = Long.parseLong(parts[4]);
            long lastModified = Long.parseLong(parts[5]);
            
            // 创建策略
            ShardStrategy strategy;
            if ("RANGE".equals(strategyName)) {
                strategy = new RangeShardStrategy();
            } else if ("HASH".equals(strategyName)) {
                strategy = new HashShardStrategy();
            } else {
                return null;
            }
            
            // 解析分片信息
            List<ShardInfo> shards = new ArrayList<>();
            for (int i = 6; i < parts.length; i++) {
                String shardInfo = parts[i];
                String[] shardParts = shardInfo.split(":");
                if (shardParts.length >= 6) {
                    String shardId = shardParts[0];
                    String nodeId = shardParts[1];
                    String dataDirectory = shardParts[2];
                    ShardInfo.ShardType shardType = ShardInfo.ShardType.valueOf(shardParts[3]);
                    boolean isActive = Boolean.parseBoolean(shardParts[4]);
                    long recordCount = Long.parseLong(shardParts[5]);
                    
                    ShardInfo shard = new ShardInfo(shardId, tableName, nodeId, dataDirectory, shardType);
                    shard.setActive(isActive);
                    shard.setRecordCount(recordCount);
                    shards.add(shard);
                }
            }
            
            ShardMetadata metadata = new ShardMetadata(tableName, shardKeyColumn, strategy, shards);
            metadata.setCreatedAt(createdAt);
            metadata.setLastModified(lastModified);
            
            return metadata;
            
        } catch (Exception e) {
            System.err.println("解析分片元数据失败: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public String toString() {
        return String.format("ShardMetadata{table='%s', keyColumn='%s', strategy=%s, shards=%d, active=%d, records=%d}", 
                           tableName, shardKeyColumn, strategy.getStrategyName(), 
                           getShardCount(), getActiveShardCount(), getTotalRecordCount());
    }
}
