package com.database.engine;

import com.sqlcompiler.catalog.ViewInfo;
import com.sqlcompiler.ast.SelectStatement;
import com.sqlcompiler.ast.TableReference;
import com.sqlcompiler.ast.IdentifierExpression;
import com.sqlcompiler.ast.Expression;

import java.util.*;
import java.io.*;

/**
 * 视图管理器
 * 负责视图的创建、删除、查询重写等功能
 */
public class ViewManager {
    private final Map<String, ViewInfo> views;
    private final String systemViewsFile;
    private final StorageEngine storageEngine;
    
    public ViewManager(StorageEngine storageEngine) {
        this.views = new HashMap<>();
        this.storageEngine = storageEngine;
        // 使用StorageEngine的数据目录来构建正确的视图文件路径
        this.systemViewsFile = storageEngine.getDataDirectory() + "/__system_views__.tbl";
        loadViewsFromStorage();
    }
    
    /**
     * 创建视图
     */
    public void createView(String viewName, SelectStatement selectStatement, String originalQuery) {
        // 检查视图名是否已存在
        if (views.containsKey(viewName.toLowerCase())) {
            throw new RuntimeException("视图 " + viewName + " 已存在");
        }
        
        // 验证视图依赖的表是否存在
        validateViewDependencies(selectStatement);
        
        // 创建视图信息
        ViewInfo viewInfo = new ViewInfo(viewName, selectStatement, originalQuery);
        views.put(viewName.toLowerCase(), viewInfo);
        
        // 持久化视图信息
        persistViewInfo(viewInfo);
    }
    
    /**
     * 删除视图
     */
    public boolean dropView(String viewName, boolean ifExists) {
        String lowerName = viewName.toLowerCase();
        
        if (!views.containsKey(lowerName)) {
            if (ifExists) {
                return true; // IF EXISTS语义，不存在时不报错
            } else {
                throw new RuntimeException("视图 " + viewName + " 不存在");
            }
        }
        
        // 从内存中移除
        views.remove(lowerName);
        
        // 从存储中移除
        removeViewFromStorage(viewName);
        
        return true;
    }
    
    /**
     * 获取视图信息
     */
    public ViewInfo getView(String viewName) {
        return views.get(viewName.toLowerCase());
    }
    
    /**
     * 检查视图是否存在
     */
    public boolean viewExists(String viewName) {
        return views.containsKey(viewName.toLowerCase());
    }
    
    /**
     * 获取所有视图名
     */
    public Set<String> getAllViewNames() {
        return new HashSet<>(views.keySet());
    }
    
    /**
     * 查询重写：将查询中的视图引用替换为其定义的SELECT语句
     */
    public SelectStatement rewriteQuery(SelectStatement originalQuery) {
        // 这里简化实现，实际应该深度遍历AST并替换视图引用
        // 检查FROM子句中是否有视图引用
        if (originalQuery.getFromClause() != null) {
            List<TableReference> tables = originalQuery.getFromClause();
            for (TableReference table : tables) {
                String tableName = table.getTableName();
                if (viewExists(tableName)) {
                    // 找到视图引用，需要重写查询
                    ViewInfo viewInfo = getView(tableName);
                    // 这里简化处理，实际应该进行更复杂的查询重写
                    return viewInfo.getSelectStatement();
                }
            }
        }
        
        return originalQuery;
    }
    
    /**
     * 验证视图依赖的表是否存在
     */
    private void validateViewDependencies(SelectStatement selectStatement) {
        // 简化实现：检查FROM子句中的表
        if (selectStatement.getFromClause() != null) {
            List<TableReference> tables = selectStatement.getFromClause();
            for (TableReference table : tables) {
                String tableName = table.getTableName();
                // 这里应该检查表或视图是否存在
                // 暂时跳过验证，实际应该与CatalogManager集成
            }
        }
    }
    
    /**
     * 持久化视图信息到文件
     */
    private void persistViewInfo(ViewInfo viewInfo) {
        try {
            // 确保目录存在
            File file = new File(systemViewsFile);
            file.getParentFile().mkdirs();
            
            // 追加模式写入视图信息
            try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                writer.println("VIEW:" + viewInfo.getName() + 
                             "|QUERY:" + viewInfo.getOriginalQuery() + 
                             "|CREATE_TIME:" + viewInfo.getCreateTime());
            }
        } catch (IOException e) {
            throw new RuntimeException("无法保存视图信息: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从存储中移除视图信息
     */
    private void removeViewFromStorage(String viewName) {
        try {
            File file = new File(systemViewsFile);
            if (!file.exists()) {
                return;
            }
            
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("VIEW:" + viewName + "|")) {
                        lines.add(line);
                    }
                }
            }
            
            // 重写文件
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                for (String line : lines) {
                    writer.println(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("无法删除视图信息: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从存储中加载视图信息
     */
    private void loadViewsFromStorage() {
        try {
            File file = new File(systemViewsFile);
            if (!file.exists()) {
                return;
            }
            
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("VIEW:")) {
                        parseViewLine(line);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("加载视图信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析视图信息行
     */
    private void parseViewLine(String line) {
        try {
            String[] parts = line.split("\\|");
            if (parts.length >= 2) {
                String viewName = parts[0].substring("VIEW:".length());
                String query = parts[1].substring("QUERY:".length());
                
                // 这里简化处理，实际应该重新解析SQL语句生成AST
                // 暂时只存储查询字符串
                ViewInfo viewInfo = new ViewInfo(viewName, null, query);
                views.put(viewName.toLowerCase(), viewInfo);
            }
        } catch (Exception e) {
            System.err.println("解析视图信息失败: " + line + ", " + e.getMessage());
        }
    }
    
    /**
     * 清空所有视图
     */
    public void clear() {
        views.clear();
        try {
            File file = new File(systemViewsFile);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            System.err.println("清空视图文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取视图摘要信息
     */
    public String getViewsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("视图摘要:\n");
        sb.append("视图数量: ").append(views.size()).append("\n");
        
        for (ViewInfo view : views.values()) {
            sb.append("视图: ").append(view.getName())
              .append(" (查询: ").append(view.getOriginalQuery()).append(")\n");
        }
        
        return sb.toString();
    }
}
