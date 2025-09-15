package com.database.engine.sharding;

import com.database.engine.StorageAdapter;
import com.database.engine.CatalogManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 分片管理器
 * 负责管理所有表的分片信息，包括分片的创建、删除、查询等操作
 */
public class ShardManager {
    private final ShardRouter shardRouter;
    private final StorageAdapter storageAdapter;
    private final CatalogManager catalogManager;
    private final String dataDirectory;
    private final String currentNodeId;
    
    // 分片元数据存储
    private final Map<String, ShardMetadata> shardMetadataMap;
    private final String shardMetadataFile;
    
    public ShardManager(String dataDirectory, String currentNodeId, 
                       StorageAdapter storageAdapter, CatalogManager catalogManager) {
        this.dataDirectory = dataDirectory;
        this.currentNodeId = currentNodeId;
        this.storageAdapter = storageAdapter;
        this.catalogManager = catalogManager;
        this.shardRouter = new ShardRouter(currentNodeId);
        this.shardMetadataMap = new ConcurrentHashMap<>();
        this.shardMetadataFile = dataDirectory + File.separator + "__system_shards__.tbl";
        
        // 初始化分片元数据
        initializeShardMetadata();
    }
    
    /**
     * 初始化分片元数据
     */
    private void initializeShardMetadata() {
        try {
            loadShardMetadata();
        } catch (Exception e) {
            System.err.println("初始化分片元数据失败: " + e.getMessage());
            createDefaultShardMetadata();
        }
    }
    
    /**
     * 创建表的分片
     */
    public boolean createTableShards(String tableName, String shardKeyColumn, 
                                   ShardStrategy strategy, int shardCount) {
        try {
            System.out.println("开始创建分片: 表=" + tableName + ", 分片键=" + shardKeyColumn + ", 策略=" + strategy.getStrategyName() + ", 数量=" + shardCount);
            
            // 检查表是否存在
            if (!catalogManager.tableExists(tableName)) {
                System.err.println("表 " + tableName + " 不存在");
                return false;
            }
            System.out.println("表存在检查通过");
            
            // 检查是否已经存在分片
            if (shardMetadataMap.containsKey(tableName)) {
                System.err.println("表 " + tableName + " 已经存在分片");
                return false;
            }
            System.out.println("分片存在检查通过");
            
            // 创建分片
            System.out.println("开始创建分片实例...");
            List<ShardInfo> shards = createShards(tableName, shardKeyColumn, strategy, shardCount);
            System.out.println("分片实例创建完成，共 " + shards.size() + " 个分片");
            
            // 创建分片元数据
            System.out.println("创建分片元数据...");
            ShardMetadata metadata = new ShardMetadata(tableName, shardKeyColumn, strategy, shards);
            shardMetadataMap.put(tableName, metadata);
            System.out.println("分片元数据创建完成");
            
            // 注册到路由器
            System.out.println("注册到路由器...");
            shardRouter.registerTableShards(tableName, shards);
            shardRouter.setShardStrategy(tableName, strategy);
            System.out.println("路由器注册完成");
            
            // 保存元数据
            System.out.println("保存分片元数据...");
            saveShardMetadata();
            System.out.println("分片元数据保存完成");
            
            System.out.println("成功为表 " + tableName + " 创建了 " + shardCount + " 个分片");
            return true;
            
        } catch (Exception e) {
            System.err.println("创建表分片失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 创建分片
     */
    private List<ShardInfo> createShards(String tableName, String shardKeyColumn, 
                                        ShardStrategy strategy, int shardCount) {
        List<ShardInfo> shards = new ArrayList<>();
        
        if (strategy instanceof RangeShardStrategy) {
            // 范围分片 - 需要获取数据范围
            Object[] range = getDataRange(tableName, shardKeyColumn);
            if (range != null) {
                shards = RangeShardStrategy.createAutoRangeShards(
                    tableName, currentNodeId, dataDirectory, shardKeyColumn, 
                    range[0], range[1], shardCount);
            }
        } else if (strategy instanceof HashShardStrategy) {
            // 哈希分片
            shards = HashShardStrategy.createAutoHashShards(
                tableName, currentNodeId, dataDirectory, shardKeyColumn, shardCount);
        }
        
        return shards;
    }
    
    /**
     * 获取数据范围
     */
    private Object[] getDataRange(String tableName, String columnName) {
        try {
            // 为了避免阻塞，我们只扫描前1000行数据来估算范围
            // 或者直接返回默认范围，让用户手动指定
            System.out.println("正在获取表 " + tableName + " 的数据范围...");
            
            // 简化处理：直接返回默认范围，避免扫描整个表
            return new Object[]{0, 1000};
            
        } catch (Exception e) {
            System.err.println("获取数据范围失败: " + e.getMessage());
            return new Object[]{0, 1000};
        }
    }
    
    /**
     * 比较两个值的大小
     */
    @SuppressWarnings("unchecked")
    private int compareValues(Object value1, Object value2) {
        if (value1 instanceof Comparable && value2 instanceof Comparable) {
            return ((Comparable<Object>) value1).compareTo(value2);
        }
        return value1.toString().compareTo(value2.toString());
    }
    
    /**
     * 删除表的分片
     */
    public boolean dropTableShards(String tableName) {
        try {
            if (!shardMetadataMap.containsKey(tableName)) {
                System.err.println("表 " + tableName + " 没有分片");
                return false;
            }
            
            // 删除分片元数据
            shardMetadataMap.remove(tableName);
            
            // 从路由器中移除
            shardRouter.getTableShards(tableName).clear();
            
            // 保存元数据
            saveShardMetadata();
            
            System.out.println("成功删除表 " + tableName + " 的分片");
            return true;
            
        } catch (Exception e) {
            System.err.println("删除表分片失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 路由到分片
     */
    public ShardInfo routeToShard(String tableName, String shardKeyColumn, Object shardKeyValue) {
        return shardRouter.routeToShard(tableName, shardKeyColumn, shardKeyValue);
    }
    
    /**
     * 获取表的所有分片
     */
    public List<ShardInfo> getTableShards(String tableName) {
        return shardRouter.getTableShards(tableName);
    }
    
    /**
     * 获取活跃分片
     */
    public List<ShardInfo> getActiveShards(String tableName) {
        return shardRouter.getActiveShards(tableName);
    }
    
    /**
     * 获取本地分片
     */
    public List<ShardInfo> getLocalShards(String tableName) {
        return shardRouter.getLocalShards(tableName);
    }
    
    /**
     * 检查表是否已分片
     */
    public boolean isTableSharded(String tableName) {
        return shardMetadataMap.containsKey(tableName);
    }
    
    /**
     * 获取分片元数据
     */
    public ShardMetadata getShardMetadata(String tableName) {
        return shardMetadataMap.get(tableName);
    }
    
    /**
     * 更新分片记录数
     */
    public void updateShardRecordCount(String tableName, String shardId, long recordCount) {
        List<ShardInfo> shards = shardRouter.getTableShards(tableName);
        for (ShardInfo shard : shards) {
            if (shardId.equals(shard.getShardId())) {
                shard.setRecordCount(recordCount);
                shard.setLastUpdated(System.currentTimeMillis());
                break;
            }
        }
    }
    
    /**
     * 获取分片统计信息
     */
    public Map<String, Object> getShardStatistics(String tableName) {
        return shardRouter.getShardStatistics(tableName);
    }
    
    /**
     * 获取负载均衡信息
     */
    public Map<String, Object> getLoadBalanceInfo(String tableName) {
        return shardRouter.getLoadBalanceInfo(tableName);
    }
    
    /**
     * 保存分片元数据到文件
     */
    private void saveShardMetadata() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(shardMetadataFile, StandardCharsets.UTF_8))) {
            writer.println("# 分片元数据文件");
            writer.println("# 格式: table_name|shard_key_column|strategy|shard_count");
            
            for (ShardMetadata metadata : shardMetadataMap.values()) {
                writer.println(metadata.toFileFormat());
            }
            
        } catch (IOException e) {
            System.err.println("保存分片元数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 从文件加载分片元数据
     */
    private void loadShardMetadata() {
        File file = new File(shardMetadataFile);
        if (!file.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                ShardMetadata metadata = ShardMetadata.fromFileFormat(line);
                if (metadata != null) {
                    shardMetadataMap.put(metadata.getTableName(), metadata);
                    
                    // 注册到路由器
                    shardRouter.registerTableShards(metadata.getTableName(), metadata.getShards());
                    shardRouter.setShardStrategy(metadata.getTableName(), metadata.getStrategy());
                }
            }
            
        } catch (IOException e) {
            System.err.println("加载分片元数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建默认分片元数据
     */
    private void createDefaultShardMetadata() {
        // 创建空的元数据文件
        try (PrintWriter writer = new PrintWriter(new FileWriter(shardMetadataFile, StandardCharsets.UTF_8))) {
            writer.println("# 分片元数据文件");
            writer.println("# 格式: table_name|shard_key_column|strategy|shard_count");
        } catch (IOException e) {
            System.err.println("创建默认分片元数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取分片路由器
     */
    public ShardRouter getShardRouter() {
        return shardRouter;
    }
}
