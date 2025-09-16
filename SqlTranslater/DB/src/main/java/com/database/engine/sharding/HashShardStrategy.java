package com.database.engine.sharding;

import java.util.List;
import java.util.ArrayList;

/**
 * 哈希分片策略
 * 根据分片键的哈希值来选择分片
 */
public class HashShardStrategy implements ShardStrategy {
    
    @Override
    public ShardInfo selectShard(List<ShardInfo> shards, String shardKeyColumn, Object shardKeyValue) {
        if (shards == null || shards.isEmpty() || shardKeyValue == null) {
            return null;
        }
        
        // 过滤出活跃的哈希分片
        List<ShardInfo> activeShards = new ArrayList<>();
        for (ShardInfo shard : shards) {
            if (shard.isActive() && shard.getShardType() == ShardInfo.ShardType.HASH) {
                activeShards.add(shard);
            }
        }
        
        if (activeShards.isEmpty()) {
            return null;
        }
        
        // 计算哈希值
        int hashValue = Math.abs(shardKeyValue.hashCode());
        int shardIndex = hashValue % activeShards.size();
        
        return activeShards.get(shardIndex);
    }
    
    @Override
    public String getStrategyName() {
        return "HASH";
    }
    
    @Override
    public boolean isValidShardKey(Object shardKeyValue) {
        return shardKeyValue != null; // 任何非null值都可以作为哈希键
    }
    
    /**
     * 创建哈希分片
     * @param shardId 分片ID
     * @param tableName 表名
     * @param nodeId 节点ID
     * @param dataDirectory 数据目录
     * @param columnName 分片键列名
     * @param hashModulo 哈希模数
     * @return 创建的分片信息
     */
    public static ShardInfo createHashShard(String shardId, String tableName, String nodeId, 
                                          String dataDirectory, String columnName, int hashModulo) {
        ShardInfo shard = new ShardInfo(shardId, tableName, nodeId, dataDirectory, ShardInfo.ShardType.HASH);
        shard.setHashKey(columnName, hashModulo);
        return shard;
    }
    
    /**
     * 自动创建哈希分片
     * 根据分片数量自动创建哈希分片
     */
    public static List<ShardInfo> createAutoHashShards(String tableName, String nodeId, 
                                                      String dataDirectory, String columnName, int shardCount) {
        List<ShardInfo> shards = new ArrayList<>();
        
        if (shardCount <= 0) {
            return shards;
        }
        
        for (int i = 0; i < shardCount; i++) {
            String shardId = tableName + "_hash_shard_" + i;
            ShardInfo shard = createHashShard(shardId, tableName, nodeId, dataDirectory, columnName, shardCount);
            shards.add(shard);
        }
        
        return shards;
    }
    
    /**
     * 计算分片索引
     * 根据分片键值计算应该路由到哪个分片
     */
    public static int calculateShardIndex(Object shardKeyValue, int shardCount) {
        if (shardKeyValue == null || shardCount <= 0) {
            return 0;
        }
        
        int hashValue = Math.abs(shardKeyValue.hashCode());
        return hashValue % shardCount;
    }
    
    /**
     * 获取分片键的哈希值
     */
    public static int getHashValue(Object shardKeyValue) {
        if (shardKeyValue == null) {
            return 0;
        }
        return Math.abs(shardKeyValue.hashCode());
    }
}
