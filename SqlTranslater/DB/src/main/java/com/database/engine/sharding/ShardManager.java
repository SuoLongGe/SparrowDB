package com.database.engine.sharding;

import com.database.engine.StorageAdapter;
import com.database.engine.CatalogManager;
import com.sqlcompiler.catalog.TableInfo;
import com.sqlcompiler.catalog.ColumnInfo;
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
            
            // 迁移现有数据到分片
            System.out.println("开始迁移现有数据到分片...");
            migrateExistingData(tableName, shardKeyColumn, shards);
            System.out.println("数据迁移完成");
            
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
        
        // 为每个分片创建独立的存储文件
        createShardFiles(tableName, shards);
        
        return shards;
    }
    
    /**
     * 为每个分片创建独立的存储文件
     */
    private void createShardFiles(String tableName, List<ShardInfo> shards) {
        try {
            // 获取原表的结构信息
            TableInfo tableInfo = catalogManager.getTable(tableName);
            if (tableInfo == null) {
                System.err.println("无法获取表 " + tableName + " 的结构信息");
                return;
            }
            
            System.out.println("开始创建分片文件，共 " + shards.size() + " 个分片");
            
            for (ShardInfo shard : shards) {
                String shardFileName = shard.getShardId() + ".tbl";
                String shardFilePath = dataDirectory + File.separator + shardFileName;
                
                System.out.println("创建分片文件: " + shardFilePath);
                
                // 创建分片文件，复制原表的结构
                createShardFileWithSchema(shardFilePath, tableInfo);
                
                // 更新分片信息，记录实际的文件路径
                shard.setDataDirectory(dataDirectory);
                shard.setShardId(shard.getShardId()); // 确保分片ID正确
                
                System.out.println("分片文件创建完成: " + shardFileName);
            }
            
            System.out.println("所有分片文件创建完成");
            
        } catch (Exception e) {
            System.err.println("创建分片文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建分片文件并写入表结构
     */
    private void createShardFileWithSchema(String filePath, TableInfo tableInfo) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
            // 写入表元数据头
            writer.println("# Table Metadata");
            writer.println("TABLE_NAME=" + tableInfo.getName());
            writer.println("COLUMN_COUNT=" + tableInfo.getColumns().size());
            
            // 写入列定义
            for (ColumnInfo column : tableInfo.getColumns()) {
                writer.println("COLUMN=" + column.getName() + ":" + column.getDataType() + ":" + column.getLength());
            }
            
            writer.println("# End Metadata");
            writer.println("PAGE:1");
            
            System.out.println("分片文件结构写入完成: " + filePath);
            
        } catch (IOException e) {
            System.err.println("创建分片文件失败: " + filePath + " - " + e.getMessage());
            throw new RuntimeException("创建分片文件失败", e);
        }
    }
    
    /**
     * 迁移现有数据到分片
     */
    private void migrateExistingData(String tableName, String shardKeyColumn, List<ShardInfo> shards) {
        try {
            // 读取原表的所有数据
            List<Map<String, Object>> allRecords = storageAdapter.scanTable(tableName);
            System.out.println("读取到原表数据 " + allRecords.size() + " 条记录");
            
            if (allRecords.isEmpty()) {
                System.out.println("原表无数据，跳过迁移");
                return;
            }
            
            // 为每个分片准备数据
            Map<String, List<Map<String, Object>>> shardDataMap = new HashMap<>();
            for (ShardInfo shard : shards) {
                shardDataMap.put(shard.getShardId(), new ArrayList<>());
            }
            
            // 根据分片键将数据分配到对应分片
            ShardStrategy strategy = shardMetadataMap.get(tableName).getStrategy();
            int migratedCount = 0;
            
            for (Map<String, Object> record : allRecords) {
                Object shardKeyValue = record.get(shardKeyColumn);
                if (shardKeyValue == null) {
                    System.err.println("警告: 记录缺少分片键 " + shardKeyColumn + "，跳过: " + record);
                    continue;
                }
                
                // 根据分片策略选择目标分片
                ShardInfo targetShard = strategy.selectShard(shards, shardKeyColumn, shardKeyValue);
                if (targetShard != null) {
                    shardDataMap.get(targetShard.getShardId()).add(record);
                    migratedCount++;
                } else {
                    System.err.println("警告: 无法为记录找到合适的分片，跳过: " + record);
                }
            }
            
            System.out.println("数据分配完成，共分配 " + migratedCount + " 条记录");
            
            // 将数据写入对应的分片文件
            for (ShardInfo shard : shards) {
                List<Map<String, Object>> shardRecords = shardDataMap.get(shard.getShardId());
                if (!shardRecords.isEmpty()) {
                    writeRecordsToShardFile(shard, shardRecords);
                    System.out.println("分片 " + shard.getShardId() + " 写入 " + shardRecords.size() + " 条记录");
                }
            }
            
        } catch (Exception e) {
            System.err.println("数据迁移失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 将记录写入分片文件
     */
    private void writeRecordsToShardFile(ShardInfo shard, List<Map<String, Object>> records) {
        try {
            String shardFilePath = dataDirectory + File.separator + shard.getShardId() + ".tbl";
            File shardFile = new File(shardFilePath);
            
            // 读取现有文件内容
            List<String> existingLines = new ArrayList<>();
            if (shardFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(shardFile, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        existingLines.add(line);
                    }
                }
            }
            
            // 写入数据到文件
            try (PrintWriter writer = new PrintWriter(new FileWriter(shardFile, StandardCharsets.UTF_8))) {
                // 写入元数据部分
                for (String line : existingLines) {
                    if (line.startsWith("PAGE:1")) {
                        writer.println(line);
                        break;
                    }
                    writer.println(line);
                }
                
                // 写入数据记录
                for (Map<String, Object> record : records) {
                    String serializedRecord = serializeRecord(record);
                    writer.println(serializedRecord);
                }
            }
            
        } catch (IOException e) {
            System.err.println("写入分片文件失败: " + shard.getShardId() + " - " + e.getMessage());
            throw new RuntimeException("写入分片文件失败", e);
        }
    }
    
    /**
     * 序列化记录为字符串
     */
    private String serializeRecord(Map<String, Object> record) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            if (!first) {
                sb.append("|");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        
        return sb.toString();
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
     * 检查表是否有分片
     */
    public boolean hasShards(String tableName) {
        return shardMetadataMap.containsKey(tableName);
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
