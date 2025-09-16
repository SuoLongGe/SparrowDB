package com.database.engine.sharding;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分片路由器
 * 负责根据分片键将数据路由到正确的分片
 */
public class ShardRouter {
    private final Map<String, List<ShardInfo>> tableShards;
    private final Map<String, ShardStrategy> shardStrategies;
    private final String currentNodeId;
    
    public ShardRouter(String currentNodeId) {
        this.currentNodeId = currentNodeId;
        this.tableShards = new ConcurrentHashMap<>();
        this.shardStrategies = new ConcurrentHashMap<>();
    }
    
    /**
     * 注册表的分片信息
     */
    public void registerTableShards(String tableName, List<ShardInfo> shards) {
        tableShards.put(tableName, new ArrayList<>(shards));
    }
    
    /**
     * 设置表的分片策略
     */
    public void setShardStrategy(String tableName, ShardStrategy strategy) {
        shardStrategies.put(tableName, strategy);
    }
    
    /**
     * 根据分片键路由到目标分片
     */
    public ShardInfo routeToShard(String tableName, String shardKeyColumn, Object shardKeyValue) {
        List<ShardInfo> shards = tableShards.get(tableName);
        if (shards == null || shards.isEmpty()) {
            return null;
        }
        
        ShardStrategy strategy = shardStrategies.get(tableName);
        if (strategy == null) {
            // 默认使用范围分片策略
            strategy = new RangeShardStrategy();
        }
        
        return strategy.selectShard(shards, shardKeyColumn, shardKeyValue);
    }
    
    /**
     * 获取表的所有分片
     */
    public List<ShardInfo> getTableShards(String tableName) {
        return tableShards.getOrDefault(tableName, new ArrayList<>());
    }
    
    /**
     * 获取活跃的分片
     */
    public List<ShardInfo> getActiveShards(String tableName) {
        List<ShardInfo> allShards = getTableShards(tableName);
        List<ShardInfo> activeShards = new ArrayList<>();
        
        for (ShardInfo shard : allShards) {
            if (shard.isActive()) {
                activeShards.add(shard);
            }
        }
        
        return activeShards;
    }
    
    /**
     * 获取本地分片（当前节点上的分片）
     */
    public List<ShardInfo> getLocalShards(String tableName) {
        List<ShardInfo> allShards = getTableShards(tableName);
        List<ShardInfo> localShards = new ArrayList<>();
        
        for (ShardInfo shard : allShards) {
            if (shard.isActive() && currentNodeId.equals(shard.getNodeId())) {
                localShards.add(shard);
            }
        }
        
        return localShards;
    }
    
    /**
     * 添加新分片
     */
    public void addShard(String tableName, ShardInfo shard) {
        List<ShardInfo> shards = tableShards.computeIfAbsent(tableName, k -> new ArrayList<>());
        shards.add(shard);
    }
    
    /**
     * 移除分片
     */
    public boolean removeShard(String tableName, String shardId) {
        List<ShardInfo> shards = tableShards.get(tableName);
        if (shards == null) {
            return false;
        }
        
        return shards.removeIf(shard -> shardId.equals(shard.getShardId()));
    }
    
    /**
     * 更新分片状态
     */
    public void updateShardStatus(String tableName, String shardId, boolean isActive) {
        List<ShardInfo> shards = tableShards.get(tableName);
        if (shards == null) {
            return;
        }
        
        for (ShardInfo shard : shards) {
            if (shardId.equals(shard.getShardId())) {
                shard.setActive(isActive);
                shard.setLastUpdated(System.currentTimeMillis());
                break;
            }
        }
    }
    
    /**
     * 获取分片统计信息
     */
    public Map<String, Object> getShardStatistics(String tableName) {
        List<ShardInfo> shards = getTableShards(tableName);
        Map<String, Object> stats = new HashMap<>();
        
        int totalShards = shards.size();
        int activeShards = 0;
        long totalRecords = 0;
        
        for (ShardInfo shard : shards) {
            if (shard.isActive()) {
                activeShards++;
                totalRecords += shard.getRecordCount();
            }
        }
        
        stats.put("totalShards", totalShards);
        stats.put("activeShards", activeShards);
        stats.put("totalRecords", totalRecords);
        stats.put("averageRecordsPerShard", activeShards > 0 ? totalRecords / activeShards : 0);
        
        return stats;
    }
    
    /**
     * 检查是否需要重新分片
     */
    public boolean needsResharding(String tableName, long threshold) {
        List<ShardInfo> activeShards = getActiveShards(tableName);
        if (activeShards.isEmpty()) {
            return false;
        }
        
        long totalRecords = 0;
        for (ShardInfo shard : activeShards) {
            totalRecords += shard.getRecordCount();
        }
        
        long averageRecords = totalRecords / activeShards.size();
        return averageRecords > threshold;
    }
    
    /**
     * 获取分片负载均衡信息
     */
    public Map<String, Object> getLoadBalanceInfo(String tableName) {
        List<ShardInfo> activeShards = getActiveShards(tableName);
        Map<String, Object> balanceInfo = new HashMap<>();
        
        if (activeShards.isEmpty()) {
            balanceInfo.put("balanced", true);
            balanceInfo.put("variance", 0.0);
            return balanceInfo;
        }
        
        long totalRecords = 0;
        long maxRecords = 0;
        long minRecords = Long.MAX_VALUE;
        
        for (ShardInfo shard : activeShards) {
            long records = shard.getRecordCount();
            totalRecords += records;
            maxRecords = Math.max(maxRecords, records);
            minRecords = Math.min(minRecords, records);
        }
        
        double averageRecords = (double) totalRecords / activeShards.size();
        double variance = 0.0;
        
        for (ShardInfo shard : activeShards) {
            double diff = shard.getRecordCount() - averageRecords;
            variance += diff * diff;
        }
        
        variance /= activeShards.size();
        double standardDeviation = Math.sqrt(variance);
        double coefficientOfVariation = averageRecords > 0 ? standardDeviation / averageRecords : 0.0;
        
        balanceInfo.put("balanced", coefficientOfVariation < 0.2); // 变异系数小于0.2认为平衡
        balanceInfo.put("variance", variance);
        balanceInfo.put("standardDeviation", standardDeviation);
        balanceInfo.put("coefficientOfVariation", coefficientOfVariation);
        balanceInfo.put("maxRecords", maxRecords);
        balanceInfo.put("minRecords", minRecords);
        balanceInfo.put("averageRecords", averageRecords);
        
        return balanceInfo;
    }
}
