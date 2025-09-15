package com.database;

import com.database.engine.DatabaseEngine;
import com.database.config.DatabaseConfig;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多数据库管理器
 * 负责管理多个数据库实例，类似于MySQL的数据库管理功能
 */
public class MultiDatabaseManager {
    private final String baseDataDirectory;
    private final Map<String, DatabaseEngine> databaseEngines;
    private String currentDatabase;
    
    public MultiDatabaseManager(String baseDataDirectory) {
        this.baseDataDirectory = baseDataDirectory;
        this.databaseEngines = new ConcurrentHashMap<>();
        this.currentDatabase = "main"; // 默认数据库
        
        // 确保基础数据目录存在
        File baseDir = new File(baseDataDirectory);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        
        // 自动发现现有数据库
        discoverExistingDatabases();
    }
    
    /**
     * 发现现有数据库
     */
    private void discoverExistingDatabases() {
        File baseDir = new File(baseDataDirectory);
        if (baseDir.exists() && baseDir.isDirectory()) {
            File[] subdirs = baseDir.listFiles(File::isDirectory);
            if (subdirs != null) {
                for (File subdir : subdirs) {
                    String dbName = subdir.getName();
                    // 跳过日志目录和其他非数据库目录
                    if (!dbName.equals("log") && !dbName.equals("tmp") && !dbName.equals("backup")) {
                        // 检查是否包含系统表文件，确认这是一个数据库目录
                        File systemTablesFile = new File(subdir, "__system_tables__.tbl");
                        if (systemTablesFile.exists() || hasTableFiles(subdir)) {
                            System.out.println("发现数据库: " + dbName);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 检查目录是否包含表文件
     */
    private boolean hasTableFiles(File dir) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".tbl"));
        return files != null && files.length > 0;
    }
    
    /**
     * 获取所有数据库名称
     */
    public Set<String> getAllDatabaseNames() {
        Set<String> dbNames = new HashSet<>();
        File baseDir = new File(baseDataDirectory);
        if (baseDir.exists() && baseDir.isDirectory()) {
            File[] subdirs = baseDir.listFiles(File::isDirectory);
            if (subdirs != null) {
                for (File subdir : subdirs) {
                    String dbName = subdir.getName();
                    // 跳过特殊目录
                    if (!dbName.equals("log") && !dbName.equals("tmp") && !dbName.equals("backup")) {
                        dbNames.add(dbName);
                    }
                }
            }
        }
        return dbNames;
    }
    
    /**
     * 创建数据库
     */
    public boolean createDatabase(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("数据库名不能为空");
        }
        
        // 检查数据库名是否合法
        if (!isValidDatabaseName(databaseName)) {
            throw new IllegalArgumentException("数据库名包含非法字符");
        }
        
        String dbPath = baseDataDirectory + File.separator + databaseName;
        File dbDir = new File(dbPath);
        
        if (dbDir.exists()) {
            return false; // 数据库已存在
        }
        
        // 创建数据库目录
        if (!dbDir.mkdirs()) {
            throw new RuntimeException("无法创建数据库目录: " + dbPath);
        }
        
        try {
            // 创建数据库引擎实例
            DatabaseEngine engine = new DatabaseEngine(databaseName, dbPath);
            if (engine.initialize()) {
                System.out.println("数据库 '" + databaseName + "' 创建成功");
                return true;
            } else {
                // 如果初始化失败，删除创建的目录
                deleteDirectory(dbDir);
                return false;
            }
        } catch (Exception e) {
            // 如果创建失败，删除创建的目录
            deleteDirectory(dbDir);
            throw new RuntimeException("创建数据库失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除数据库
     */
    public boolean dropDatabase(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("数据库名不能为空");
        }
        
        if (databaseName.equals("main")) {
            throw new IllegalArgumentException("不能删除main数据库");
        }
        
        // 如果当前连接的是要删除的数据库，先切换到main
        if (databaseName.equals(currentDatabase)) {
            useDatabase("main");
        }
        
        // 关闭数据库引擎
        DatabaseEngine engine = databaseEngines.remove(databaseName);
        if (engine != null) {
            try {
                engine.shutdown();
            } catch (Exception e) {
                System.err.println("关闭数据库引擎时出错: " + e.getMessage());
            }
        }
        
        // 删除数据库目录
        String dbPath = baseDataDirectory + File.separator + databaseName;
        File dbDir = new File(dbPath);
        
        if (!dbDir.exists()) {
            return false; // 数据库不存在
        }
        
        return deleteDirectory(dbDir);
    }
    
    /**
     * 使用指定数据库
     */
    public boolean useDatabase(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("数据库名不能为空");
        }
        
        String dbPath = baseDataDirectory + File.separator + databaseName;
        File dbDir = new File(dbPath);
        
        if (!dbDir.exists()) {
            return false; // 数据库不存在
        }
        
        this.currentDatabase = databaseName;
        return true;
    }
    
    /**
     * 获取当前数据库引擎
     */
    public DatabaseEngine getCurrentDatabaseEngine() {
        return getDatabaseEngine(currentDatabase);
    }
    
    /**
     * 获取指定数据库的引擎
     */
    public DatabaseEngine getDatabaseEngine(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            databaseName = currentDatabase;
        }
        
        DatabaseEngine engine = databaseEngines.get(databaseName);
        if (engine == null) {
            // 懒加载数据库引擎
            String dbPath = baseDataDirectory + File.separator + databaseName;
            File dbDir = new File(dbPath);
            
            if (dbDir.exists()) {
                try {
                    engine = new DatabaseEngine(databaseName, dbPath);
                    if (engine.initialize()) {
                        databaseEngines.put(databaseName, engine);
                    } else {
                        throw new RuntimeException("数据库引擎初始化失败");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("无法加载数据库 '" + databaseName + "': " + e.getMessage(), e);
                }
            } else {
                throw new RuntimeException("数据库 '" + databaseName + "' 不存在");
            }
        }
        
        return engine;
    }
    
    /**
     * 获取当前数据库名
     */
    public String getCurrentDatabase() {
        return currentDatabase;
    }
    
    /**
     * 检查数据库是否存在
     */
    public boolean databaseExists(String databaseName) {
        String dbPath = baseDataDirectory + File.separator + databaseName;
        return new File(dbPath).exists();
    }
    
    /**
     * 关闭所有数据库连接
     */
    public void shutdown() {
        for (Map.Entry<String, DatabaseEngine> entry : databaseEngines.entrySet()) {
            try {
                entry.getValue().shutdown();
            } catch (Exception e) {
                System.err.println("关闭数据库 '" + entry.getKey() + "' 时出错: " + e.getMessage());
            }
        }
        databaseEngines.clear();
    }
    
    /**
     * 验证数据库名是否合法
     */
    private boolean isValidDatabaseName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // 数据库名只能包含字母、数字、下划线，且不能以数字开头
        return name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$") && name.length() <= 64;
    }
    
    /**
     * 递归删除目录
     */
    private boolean deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return dir.delete();
    }
    
    /**
     * 获取数据库统计信息
     */
    public Map<String, Object> getDatabaseStats(String databaseName) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            DatabaseEngine engine = getDatabaseEngine(databaseName);
            stats.put("name", databaseName);
            stats.put("path", baseDataDirectory + File.separator + databaseName);
            stats.put("tableCount", engine.getCatalogManager().getAllTableNames().size());
            stats.put("viewCount", engine.getViewManager().getAllViewNames().size());
            stats.put("functionCount", engine.getFunctionManager().getAllFunctionNames().size());
            stats.put("isConnected", databaseEngines.containsKey(databaseName));
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
}
