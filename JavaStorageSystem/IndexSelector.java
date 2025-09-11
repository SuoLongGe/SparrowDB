import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能索引选择器
 * 根据查询模式、数据特征和性能统计自动选择最优索引类型
 */
public class IndexSelector {
    private ExtendedIndexManager indexManager;
    private Map<String, IndexStatistics> indexStats; // 索引统计信息
    private Map<String, QueryPattern> queryPatterns; // 查询模式分析
    private Map<String, DataCharacteristics> dataCharacteristics; // 数据特征分析
    
    // 配置参数
    private static final int MIN_DATA_SIZE_FOR_INDEX = 50; // 最小数据量才考虑索引
    private static final double HASH_INDEX_THRESHOLD = 0.8; // 哈希索引选择阈值
    private static final double BPLUS_INDEX_THRESHOLD = 0.3; // B+树索引选择阈值
    private static final int MAX_SAMPLE_SIZE = 1000; // 最大采样大小
    
    public IndexSelector(ExtendedIndexManager indexManager) {
        this.indexManager = indexManager;
        this.indexStats = new ConcurrentHashMap<>();
        this.queryPatterns = new ConcurrentHashMap<>();
        this.dataCharacteristics = new ConcurrentHashMap<>();
    }
    
    /**
     * 自动选择最优索引类型
     */
    public IndexType selectOptimalIndex(String indexName, List<Object> sampleData, 
                                       QueryPattern pattern, DataCharacteristics characteristics) {
        
        // 1. 数据量检查
        if (sampleData.size() < MIN_DATA_SIZE_FOR_INDEX) {
            System.out.println("数据量太小(" + sampleData.size() + ")，建议使用线性查找");
            return null; // 返回null表示不创建索引
        }
        
        // 2. 分析查询模式
        QueryPatternAnalysis analysis = analyzeQueryPattern(pattern);
        
        // 3. 分析数据特征
        DataAnalysis dataAnalysis = analyzeDataCharacteristics(sampleData, characteristics);
        
        // 4. 计算各索引类型的适用性分数
        IndexScores scores = calculateIndexScores(analysis, dataAnalysis, sampleData.size());
        
        // 5. 选择最优索引类型
        IndexType selectedType = selectBestIndex(scores);
        
        // 6. 记录选择决策
        recordSelectionDecision(indexName, selectedType, scores, analysis, dataAnalysis);
        
        return selectedType;
    }
    
    /**
     * 分析查询模式
     */
    private QueryPatternAnalysis analyzeQueryPattern(QueryPattern pattern) {
        QueryPatternAnalysis analysis = new QueryPatternAnalysis();
        
        // 计算等值查询比例
        double equalityRatio = (double) pattern.equalityQueries / 
                              (pattern.equalityQueries + pattern.rangeQueries + pattern.sortQueries);
        analysis.equalityQueryRatio = equalityRatio;
        
        // 计算范围查询比例
        double rangeRatio = (double) pattern.rangeQueries / 
                           (pattern.equalityQueries + pattern.rangeQueries + pattern.sortQueries);
        analysis.rangeQueryRatio = rangeRatio;
        
        // 计算排序查询比例
        double sortRatio = (double) pattern.sortQueries / 
                          (pattern.equalityQueries + pattern.rangeQueries + pattern.sortQueries);
        analysis.sortQueryRatio = sortRatio;
        
        // 计算查询频率
        analysis.queryFrequency = pattern.totalQueries / Math.max(1, pattern.timeWindowHours);
        
        // 计算并发度
        analysis.concurrencyLevel = pattern.maxConcurrentQueries;
        
        return analysis;
    }
    
    /**
     * 分析数据特征
     */
    private DataAnalysis analyzeDataCharacteristics(List<Object> sampleData, DataCharacteristics characteristics) {
        DataAnalysis analysis = new DataAnalysis();
        
        // 数据量
        analysis.dataSize = sampleData.size();
        
        // 数据分布分析
        if (characteristics.isNumeric) {
            analysis.distribution = analyzeNumericDistribution(sampleData);
        } else {
            analysis.distribution = analyzeStringDistribution(sampleData);
        }
        
        // 唯一性分析
        Set<Object> uniqueValues = new HashSet<>(sampleData);
        analysis.uniqueness = (double) uniqueValues.size() / sampleData.size();
        
        // 数据增长模式
        analysis.growthPattern = characteristics.growthPattern;
        
        // 内存限制
        analysis.memoryConstraint = characteristics.memoryLimit;
        
        return analysis;
    }
    
    /**
     * 分析数值分布
     */
    private DistributionType analyzeNumericDistribution(List<Object> sampleData) {
        if (sampleData.isEmpty()) return DistributionType.UNKNOWN;
        
        List<Double> values = new ArrayList<>();
        for (Object obj : sampleData) {
            if (obj instanceof Number) {
                values.add(((Number) obj).doubleValue());
            }
        }
        
        if (values.size() < 2) return DistributionType.UNIFORM;
        
        Collections.sort(values);
        
        // 计算方差
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0);
        
        // 简单的分布类型判断
        if (variance < mean * 0.1) {
            return DistributionType.UNIFORM;
        } else if (variance > mean * 2) {
            return DistributionType.SKEWED;
        } else {
            return DistributionType.NORMAL;
        }
    }
    
    /**
     * 分析字符串分布
     */
    private DistributionType analyzeStringDistribution(List<Object> sampleData) {
        if (sampleData.isEmpty()) return DistributionType.UNKNOWN;
        
        Map<String, Integer> frequency = new HashMap<>();
        for (Object obj : sampleData) {
            String str = obj.toString();
            frequency.put(str, frequency.getOrDefault(str, 0) + 1);
        }
        
        // 计算频率分布的方差
        List<Integer> frequencies = new ArrayList<>(frequency.values());
        double mean = frequencies.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = frequencies.stream()
            .mapToDouble(f -> Math.pow(f - mean, 2))
            .average().orElse(0);
        
        if (variance < mean * 0.1) {
            return DistributionType.UNIFORM;
        } else {
            return DistributionType.SKEWED;
        }
    }
    
    /**
     * 计算各索引类型的适用性分数
     */
    private IndexScores calculateIndexScores(QueryPatternAnalysis patternAnalysis, 
                                           DataAnalysis dataAnalysis, int dataSize) {
        IndexScores scores = new IndexScores();
        
        // 哈希索引分数计算
        scores.hashScore = calculateHashIndexScore(patternAnalysis, dataAnalysis, dataSize);
        
        // B+树索引分数计算
        scores.bplusScore = calculateBPlusIndexScore(patternAnalysis, dataAnalysis, dataSize);
        
        // 线性查找分数计算
        scores.linearScore = calculateLinearSearchScore(patternAnalysis, dataAnalysis, dataSize);
        
        return scores;
    }
    
    /**
     * 计算哈希索引分数
     */
    private double calculateHashIndexScore(QueryPatternAnalysis patternAnalysis, 
                                         DataAnalysis dataAnalysis, int dataSize) {
        double score = 0.0;
        
        // 等值查询比例权重最高
        score += patternAnalysis.equalityQueryRatio * 0.4;
        
        // 数据唯一性
        score += dataAnalysis.uniqueness * 0.2;
        
        // 查询频率
        if (patternAnalysis.queryFrequency > 100) {
            score += 0.2; // 高频率查询
        } else if (patternAnalysis.queryFrequency > 10) {
            score += 0.1;
        }
        
        // 并发度
        if (patternAnalysis.concurrencyLevel > 10) {
            score += 0.1; // 高并发
        }
        
        // 数据量惩罚（大数据集可能内存不足）
        if (dataSize > 10000) {
            score -= 0.1;
        } else if (dataSize > 1000) {
            score -= 0.05;
        }
        
        // 内存限制
        if (dataAnalysis.memoryConstraint < dataSize * 0.1) {
            score -= 0.2; // 内存不足
        }
        
        return Math.max(0, Math.min(1, score));
    }
    
    /**
     * 计算B+树索引分数
     */
    private double calculateBPlusIndexScore(QueryPatternAnalysis patternAnalysis, 
                                          DataAnalysis dataAnalysis, int dataSize) {
        double score = 0.0;
        
        // 范围查询支持
        score += patternAnalysis.rangeQueryRatio * 0.3;
        
        // 排序查询支持
        score += patternAnalysis.sortQueryRatio * 0.2;
        
        // 大数据集优势
        if (dataSize > 1000) {
            score += 0.2;
        } else if (dataSize > 500) {
            score += 0.1;
        }
        
        // 数据分布
        if (dataAnalysis.distribution == DistributionType.NORMAL) {
            score += 0.1;
        }
        
        // 等值查询也有一定支持
        score += patternAnalysis.equalityQueryRatio * 0.1;
        
        // 内存效率
        if (dataAnalysis.memoryConstraint < dataSize * 0.05) {
            score += 0.1; // 内存受限时B+树更优
        }
        
        return Math.max(0, Math.min(1, score));
    }
    
    /**
     * 计算线性查找分数
     */
    private double calculateLinearSearchScore(QueryPatternAnalysis patternAnalysis, 
                                            DataAnalysis dataAnalysis, int dataSize) {
        double score = 0.0;
        
        // 小数据集优势
        if (dataSize < 100) {
            score += 0.4;
        } else if (dataSize < 200) {
            score += 0.2;
        }
        
        // 低查询频率
        if (patternAnalysis.queryFrequency < 1) {
            score += 0.3;
        } else if (patternAnalysis.queryFrequency < 5) {
            score += 0.1;
        }
        
        // 内存极度受限
        if (dataAnalysis.memoryConstraint < dataSize * 0.01) {
            score += 0.3;
        }
        
        return Math.max(0, Math.min(1, score));
    }
    
    /**
     * 选择最优索引类型
     */
    private IndexType selectBestIndex(IndexScores scores) {
        // 如果线性查找分数最高且超过阈值
        if (scores.linearScore > 0.6 && scores.linearScore > scores.hashScore && 
            scores.linearScore > scores.bplusScore) {
            return null; // 不创建索引，使用线性查找
        }
        
        // 如果哈希索引分数最高且超过阈值
        if (scores.hashScore > HASH_INDEX_THRESHOLD && scores.hashScore > scores.bplusScore) {
            return IndexType.HASH_TABLE;
        }
        
        // 如果B+树索引分数最高且超过阈值
        if (scores.bplusScore > BPLUS_INDEX_THRESHOLD) {
            return IndexType.BPLUS_TREE;
        }
        
        // 默认选择哈希索引（等值查询场景较多）
        if (scores.hashScore > scores.bplusScore) {
            return IndexType.HASH_TABLE;
        } else {
            return IndexType.BPLUS_TREE;
        }
    }
    
    /**
     * 记录选择决策
     */
    private void recordSelectionDecision(String indexName, IndexType selectedType, 
                                       IndexScores scores, QueryPatternAnalysis patternAnalysis, 
                                       DataAnalysis dataAnalysis) {
        System.out.println("\n=== 索引选择决策: " + indexName + " ===");
        System.out.println("选择的索引类型: " + (selectedType != null ? selectedType.getDescription() : "线性查找"));
        System.out.println("各索引类型分数:");
        System.out.println("  哈希索引: " + String.format("%.3f", scores.hashScore));
        System.out.println("  B+树索引: " + String.format("%.3f", scores.bplusScore));
        System.out.println("  线性查找: " + String.format("%.3f", scores.linearScore));
        System.out.println("查询模式分析:");
        System.out.println("  等值查询比例: " + String.format("%.1f%%", patternAnalysis.equalityQueryRatio * 100));
        System.out.println("  范围查询比例: " + String.format("%.1f%%", patternAnalysis.rangeQueryRatio * 100));
        System.out.println("  排序查询比例: " + String.format("%.1f%%", patternAnalysis.sortQueryRatio * 100));
        System.out.println("  查询频率: " + String.format("%.1f", patternAnalysis.queryFrequency) + " 次/小时");
        System.out.println("数据特征分析:");
        System.out.println("  数据量: " + dataAnalysis.dataSize);
        System.out.println("  唯一性: " + String.format("%.1f%%", dataAnalysis.uniqueness * 100));
        System.out.println("  分布类型: " + dataAnalysis.distribution);
    }
    
    /**
     * 自动创建最优索引
     */
    public boolean createOptimalIndex(String indexName, List<Object> sampleData, 
                                    QueryPattern pattern, DataCharacteristics characteristics) {
        IndexType selectedType = selectOptimalIndex(indexName, sampleData, pattern, characteristics);
        
        if (selectedType == null) {
            System.out.println("建议使用线性查找，不创建索引");
            return false;
        }
        
        // 根据选择的类型创建索引
        if (selectedType == IndexType.HASH_TABLE) {
            int bucketCount = Math.max(10, sampleData.size() / 10);
            return indexManager.createIntegerHashIndex(indexName, bucketCount);
        } else if (selectedType == IndexType.BPLUS_TREE) {
            int maxKeys = Math.min(10, Math.max(3, sampleData.size() / 100));
            return indexManager.createIntegerBPlusIndex(indexName, maxKeys);
        }
        
        return false;
    }
    
    /**
     * 更新查询统计
     */
    public void updateQueryStats(String indexName, QueryType queryType, long executionTime) {
        IndexStatistics stats = indexStats.computeIfAbsent(indexName, k -> new IndexStatistics());
        stats.updateStats(queryType, executionTime);
    }
    
    /**
     * 获取索引统计信息
     */
    public IndexStatistics getIndexStatistics(String indexName) {
        return indexStats.get(indexName);
    }
    
    // ========== 内部类定义 ==========
    
    /**
     * 查询模式
     */
    public static class QueryPattern {
        public int equalityQueries = 0;    // 等值查询次数
        public int rangeQueries = 0;       // 范围查询次数
        public int sortQueries = 0;        // 排序查询次数
        public int totalQueries = 0;       // 总查询次数
        public int timeWindowHours = 1;    // 时间窗口（小时）
        public int maxConcurrentQueries = 1; // 最大并发查询数
        
        public QueryPattern() {}
        
        public QueryPattern(int equality, int range, int sort, int total, int timeWindow, int maxConcurrent) {
            this.equalityQueries = equality;
            this.rangeQueries = range;
            this.sortQueries = sort;
            this.totalQueries = total;
            this.timeWindowHours = timeWindow;
            this.maxConcurrentQueries = maxConcurrent;
        }
    }
    
    /**
     * 数据特征
     */
    public static class DataCharacteristics {
        public boolean isNumeric = true;           // 是否为数值类型
        public GrowthPattern growthPattern = GrowthPattern.STABLE; // 增长模式
        public long memoryLimit = Long.MAX_VALUE;  // 内存限制（字节）
        
        public DataCharacteristics() {}
        
        public DataCharacteristics(boolean isNumeric, GrowthPattern growthPattern, long memoryLimit) {
            this.isNumeric = isNumeric;
            this.growthPattern = growthPattern;
            this.memoryLimit = memoryLimit;
        }
    }
    
    /**
     * 增长模式枚举
     */
    public enum GrowthPattern {
        STABLE,     // 稳定
        LINEAR,     // 线性增长
        EXPONENTIAL // 指数增长
    }
    
    /**
     * 分布类型枚举
     */
    public enum DistributionType {
        UNIFORM,    // 均匀分布
        NORMAL,     // 正态分布
        SKEWED,     // 偏斜分布
        UNKNOWN     // 未知
    }
    
    /**
     * 查询类型枚举
     */
    public enum QueryType {
        EQUALITY,   // 等值查询
        RANGE,      // 范围查询
        SORT        // 排序查询
    }
    
    /**
     * 查询模式分析结果
     */
    private static class QueryPatternAnalysis {
        double equalityQueryRatio;
        double rangeQueryRatio;
        double sortQueryRatio;
        double queryFrequency;
        int concurrencyLevel;
    }
    
    /**
     * 数据分析结果
     */
    private static class DataAnalysis {
        int dataSize;
        DistributionType distribution;
        double uniqueness;
        GrowthPattern growthPattern;
        long memoryConstraint;
    }
    
    /**
     * 索引分数
     */
    private static class IndexScores {
        double hashScore;
        double bplusScore;
        double linearScore;
    }
    
    /**
     * 索引统计信息
     */
    public static class IndexStatistics {
        private Map<QueryType, Long> queryCounts = new HashMap<>();
        private Map<QueryType, Long> totalExecutionTime = new HashMap<>();
        private long lastUpdateTime = System.currentTimeMillis();
        
        public void updateStats(QueryType queryType, long executionTime) {
            queryCounts.put(queryType, queryCounts.getOrDefault(queryType, 0L) + 1);
            totalExecutionTime.put(queryType, totalExecutionTime.getOrDefault(queryType, 0L) + executionTime);
            lastUpdateTime = System.currentTimeMillis();
        }
        
        public double getAverageExecutionTime(QueryType queryType) {
            long count = queryCounts.getOrDefault(queryType, 0L);
            if (count == 0) return 0;
            return (double) totalExecutionTime.getOrDefault(queryType, 0L) / count;
        }
        
        public long getQueryCount(QueryType queryType) {
            return queryCounts.getOrDefault(queryType, 0L);
        }
    }
}
