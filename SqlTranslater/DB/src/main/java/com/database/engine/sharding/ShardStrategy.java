package com.database.engine.sharding;

import java.util.List;

/**
 * 分片策略接口
 * 定义如何选择分片的策略
 */
public interface ShardStrategy {
    
    /**
     * 根据分片键选择目标分片
     * @param shards 可用分片列表
     * @param shardKeyColumn 分片键列名
     * @param shardKeyValue 分片键值
     * @return 选中的分片，如果没有找到则返回null
     */
    ShardInfo selectShard(List<ShardInfo> shards, String shardKeyColumn, Object shardKeyValue);
    
    /**
     * 获取策略名称
     */
    String getStrategyName();
    
    /**
     * 检查分片键是否有效
     */
    boolean isValidShardKey(Object shardKeyValue);
}
