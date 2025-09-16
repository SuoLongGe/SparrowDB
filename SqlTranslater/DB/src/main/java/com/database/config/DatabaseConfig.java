package com.database.config;

/**
 * 数据库配置类
 * 统一管理所有路径和配置参数
 */
public class DatabaseConfig {
    
    // 默认配置
    public static final String DEFAULT_DATABASE_NAME = "sparrow_db";
    public static final String DEFAULT_DATA_DIRECTORY = "./data";
    public static final String TEST_DATA_DIRECTORY = "./test_data";
    
    // 系统表文件名
    public static final String SYSTEM_TABLES_FILE = "__system_tables__.tbl";
    public static final String SYSTEM_COLUMNS_FILE = "__system_columns__.tbl";
    public static final String SYSTEM_CONSTRAINTS_FILE = "__system_constraints__.tbl";
    
    // 存储系统配置
    public static final int BUFFER_POOL_SIZE = 50;
    public static final String REPLACEMENT_POLICY = "LRU";
    
    /**
     * 获取相对于指定基准目录的数据目录路径
     * @param baseDirectory 基准目录（通常是运行时的工作目录）
     * @param relativePath 相对路径
     * @return 完整的数据目录路径
     */
    public static String getDataDirectory(String baseDirectory, String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            relativePath = DEFAULT_DATA_DIRECTORY;
        }
        
        // 如果是绝对路径，直接返回
        if (java.nio.file.Paths.get(relativePath).isAbsolute()) {
            return relativePath;
        }
        
        // 处理相对路径
        try {
            java.nio.file.Path basePath = java.nio.file.Paths.get(baseDirectory);
            java.nio.file.Path dataPath = basePath.resolve(relativePath).normalize();
            return dataPath.toString();
        } catch (Exception e) {
            // 如果路径解析失败，返回默认路径
            return DEFAULT_DATA_DIRECTORY;
        }
    }
    
    /**
     * 获取GUI程序的数据目录路径
     * GUI程序运行在SqlTranslater/DB目录下，需要向上两级到达项目根目录
     */
    public static String getGUIDataDirectory() {
        return "../../data";
    }
    
    /**
     * 获取命令行程序的数据目录路径
     * 统一使用项目根目录的data文件夹
     */
    public static String getCLIDataDirectory() {
        return "../../data";
    }
    
    /**
     * 根据当前工作目录自动检测合适的数据目录路径
     * 统一使用项目根目录的data文件夹
     */
    public static String getAutoDetectedDataDirectory() {
        // 统一使用项目根目录的data文件夹
        return "../../data";
    }
    
    /**
     * 测试方法 - 显示当前配置信息
     */
    public static void main(String[] args) {
        System.out.println("=== DatabaseConfig 配置信息 ===");
        System.out.println("当前工作目录: " + System.getProperty("user.dir"));
        System.out.println("默认数据库名: " + DEFAULT_DATABASE_NAME);
        System.out.println("默认数据目录: " + DEFAULT_DATA_DIRECTORY);
        System.out.println("GUI数据目录: " + getGUIDataDirectory());
        System.out.println("CLI数据目录: " + getCLIDataDirectory());
        System.out.println("自动检测的数据目录: " + getAutoDetectedDataDirectory());
        
        // 检查数据目录是否存在
        String autoDataDir = getAutoDetectedDataDirectory();
        java.io.File dataDir = new java.io.File(autoDataDir);
        System.out.println("数据目录是否存在: " + dataDir.exists());
        
        if (dataDir.exists()) {
            java.io.File[] tblFiles = dataDir.listFiles((dir, name) -> name.endsWith(".tbl"));
            System.out.println("找到的表文件数量: " + (tblFiles != null ? tblFiles.length : 0));
        }
    }
}
