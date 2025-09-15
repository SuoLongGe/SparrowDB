package com.database.engine.sharding;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 范围分片策略
 * 根据分片键的值范围来选择分片
 */
public class RangeShardStrategy implements ShardStrategy {
    
    @Override
    public ShardInfo selectShard(List<ShardInfo> shards, String shardKeyColumn, Object shardKeyValue) {
        if (shards == null || shards.isEmpty() || shardKeyValue == null) {
            return null;
        }
        
        // 过滤出活跃的分片
        List<ShardInfo> activeShards = new ArrayList<>();
        for (ShardInfo shard : shards) {
            if (shard.isActive() && shard.getShardType() == ShardInfo.ShardType.RANGE) {
                activeShards.add(shard);
            }
        }
        
        if (activeShards.isEmpty()) {
            return null;
        }
        
        // 按范围查找匹配的分片
        for (ShardInfo shard : activeShards) {
            if (shard.containsValue(shardKeyValue)) {
                return shard;
            }
        }
        
        // 如果没有找到匹配的分片，返回第一个分片作为默认
        return activeShards.get(0);
    }
    
    @Override
    public String getStrategyName() {
        return "RANGE";
    }
    
    @Override
    public boolean isValidShardKey(Object shardKeyValue) {
        return shardKeyValue instanceof Comparable;
    }
    
    /**
     * 创建范围分片
     * @param shardId 分片ID
     * @param tableName 表名
     * @param nodeId 节点ID
     * @param dataDirectory 数据目录
     * @param columnName 分片键列名
     * @param minValue 最小值
     * @param maxValue 最大值
     * @return 创建的分片信息
     */
    public static ShardInfo createRangeShard(String shardId, String tableName, String nodeId, 
                                           String dataDirectory, String columnName, 
                                           Object minValue, Object maxValue) {
        ShardInfo shard = new ShardInfo(shardId, tableName, nodeId, dataDirectory, ShardInfo.ShardType.RANGE);
        shard.setRangeKey(columnName, minValue, maxValue);
        return shard;
    }
    
    /**
     * 自动创建范围分片
     * 根据数据分布自动计算分片范围
     */
    public static List<ShardInfo> createAutoRangeShards(String tableName, String nodeId, 
                                                       String dataDirectory, String columnName,
                                                       Object minValue, Object maxValue, int shardCount) {
        List<ShardInfo> shards = new ArrayList<>();
        
        if (shardCount <= 0 || minValue == null || maxValue == null) {
            return shards;
        }
        
        // 计算每个分片的范围大小
        if (minValue instanceof Number && maxValue instanceof Number) {
            double min = ((Number) minValue).doubleValue();
            double max = ((Number) maxValue).doubleValue();
            double range = max - min;
            double shardSize = range / shardCount;
            
            for (int i = 0; i < shardCount; i++) {
                String shardId = tableName + "_shard_" + i;
                double shardMin = min + i * shardSize;
                double shardMax = (i == shardCount - 1) ? max : min + (i + 1) * shardSize;
                
                ShardInfo shard = createRangeShard(shardId, tableName, nodeId, dataDirectory, 
                                                 columnName, shardMin, shardMax);
                shards.add(shard);
            }
        } else if (minValue instanceof String && maxValue instanceof String) {
            // 字符串范围分片（按字典序）
            String minStr = (String) minValue;
            String maxStr = (String) maxValue;
            
            // 简单的字符串分片策略：按首字符分片
            char minChar = minStr.charAt(0);
            char maxChar = maxStr.charAt(0);
            int charRange = maxChar - minChar + 1;
            int charsPerShard = Math.max(1, charRange / shardCount);
            
            for (int i = 0; i < shardCount; i++) {
                String shardId = tableName + "_shard_" + i;
                char shardMinChar = (char) (minChar + i * charsPerShard);
                char shardMaxChar = (i == shardCount - 1) ? maxChar : (char) (minChar + (i + 1) * charsPerShard - 1);
                
                ShardInfo shard = createRangeShard(shardId, tableName, nodeId, dataDirectory, 
                                                 columnName, String.valueOf(shardMinChar), String.valueOf(shardMaxChar));
                shards.add(shard);
            }
        }
        
        return shards;
    }
}
