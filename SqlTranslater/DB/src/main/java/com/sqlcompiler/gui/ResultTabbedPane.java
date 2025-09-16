package com.sqlcompiler.gui;

import com.database.engine.ExecutionResult;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * 结果标签栏组件
 * 包含消息标签页和结果标签页，类似Navicat的显示方式
 */
public class ResultTabbedPane extends JPanel {
    private JTabbedPane tabbedPane;
    private JTextArea messageArea;
    private JPanel resultPanel;
    private int resultTabCount = 0;
    
    public ResultTabbedPane() {
        initializeComponents();
        setupLayout();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // 创建标签栏
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        // 创建消息区域
        messageArea = new JTextArea();
        messageArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        messageArea.setEditable(false);
        messageArea.setBackground(Color.WHITE);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        
        // 创建消息标签页
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        messageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        messageScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        tabbedPane.addTab("消息", messageScrollPane);
        
        // 创建结果面板容器
        resultPanel = new JPanel(new BorderLayout());
        tabbedPane.addTab("结果", resultPanel);
        
        // 默认显示消息标签页
        tabbedPane.setSelectedIndex(0);
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    /**
     * 显示非查询类指令的消息
     */
    public void showMessage(String message) {
        messageArea.append(message + "\n");
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
        
        // 切换到消息标签页
        tabbedPane.setSelectedIndex(0);
    }
    
    /**
     * 显示查询类指令的消息信息
     */
    public void showQueryMessage(String sql, boolean compileSuccess, boolean executeSuccess, 
                                double executionTime, String indexType) {
        showQueryMessage(sql, compileSuccess, executeSuccess, executionTime, indexType, null);
    }
    
    /**
     * 显示查询类指令的消息信息（包含执行结果）
     */
    public void showQueryMessage(String sql, boolean compileSuccess, boolean executeSuccess, 
                                double executionTime, String indexType, ExecutionResult result) {
        StringBuilder message = new StringBuilder();
        message.append("=== 执行信息 ===\n");
        message.append("SQL: ").append(sql).append("\n");
        message.append("编译状态: ").append(compileSuccess ? "成功" : "失败").append("\n");
        
        if (compileSuccess) {
            message.append("索引方式: ").append(indexType).append("\n");
            message.append("执行状态: ").append(executeSuccess ? "成功" : "失败").append("\n");
            if (executeSuccess) {
                message.append(String.format("执行时间: %.2f ms\n", executionTime));
                
                // 显示子查询优化信息
                if (result != null && result.getSubqueryRewriteInfo() != null && 
                    !result.getSubqueryRewriteInfo().isEmpty()) {
                    message.append("子查询优化: ").append(result.getSubqueryRewriteInfo()).append("\n");
                    // 如果DatabaseEngine打印了改写后的SQL到ExecutionResult.message中，尝试解析并展示
                    String originalMsg = result.getMessage();
                    if (originalMsg != null && originalMsg.contains("改写后SQL:")) {
                        String[] parts = originalMsg.split("改写后SQL:");
                        String rewritten = parts[parts.length - 1].trim();
                        if (!rewritten.isEmpty()) {
                            message.append("改写后SQL: ").append(rewritten).append("\n");
                        }
                    }
                }
            }
        }
        
        message.append("\n").append("=".repeat(50)).append("\n\n");
        
        messageArea.append(message.toString());
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
        
        // 切换到消息标签页
        tabbedPane.setSelectedIndex(0);
    }
    
    /**
     * 显示查询结果
     */
    public void showQueryResult(ExecutionResult result) {
        if (result == null || !result.isSuccess()) {
            showError(result != null ? result.getMessage() : "执行失败");
            return;
        }
        
        // 检查是否是批量结果
        if (result.getBatchResults() != null && !result.getBatchResults().isEmpty()) {
            showBatchResults(result);
            return;
        }
        
        List<Map<String, Object>> data = result.getData();
        if (data == null || data.isEmpty()) {
            showMessage("查询成功，但无数据返回");
            return;
        }
        
        // 清空现有的结果标签页，为单个查询结果做准备
        clearResultTabs();
        
        // 创建表格组件
        QueryResultTable resultTable = new QueryResultTable();
        resultTable.displayResult(result);
        
        // 创建新的结果标签页
        tabbedPane.addTab("结果", resultTable);
        
        // 切换到结果标签页
        tabbedPane.setSelectedIndex(1);
    }
    
    /**
     * 显示批量执行结果
     */
    public void showBatchResults(ExecutionResult batchResult) {
        if (batchResult == null) {
            showError("批量执行失败：结果为空");
            return;
        }
        
        List<ExecutionResult> results = batchResult.getBatchResults();
        if (results == null || results.isEmpty()) {
            if (batchResult.isSuccess()) {
                showMessage("批量执行成功，但无结果返回");
            } else {
                showError(batchResult.getMessage());
            }
            return;
        }
        
        // 清空现有的结果标签页
        clearResultTabs();
        
        // 构建详细的批处理执行报告
        StringBuilder detailReport = new StringBuilder();
        detailReport.append("=== 批量执行详细报告 ===\n");
        detailReport.append(batchResult.getMessage()).append("\n\n");
        
        int successCount = 0;
        int failureCount = 0;
        
        // 为每个有查询结果的结果创建标签页，同时构建详细报告
        int queryResultCount = 0;
        for (int i = 0; i < results.size(); i++) {
            ExecutionResult result = results.get(i);
            
            // 添加到详细报告
            detailReport.append("语句 ").append(i + 1).append(": ");
            if (result.isSuccess()) {
                successCount++;
                detailReport.append("✓ 执行成功");
                if (result.getMessage() != null && !result.getMessage().trim().isEmpty()) {
                    detailReport.append(" - ").append(result.getMessage());
                }
                
                // 如果有查询结果，创建标签页
                if (result.getData() != null && !result.getData().isEmpty()) {
                    queryResultCount++;
                    
                    // 创建结果表格
                    QueryResultTable resultTable = new QueryResultTable();
                    resultTable.displayResult(result);
                    
                    // 创建新的标签页
                    String tabTitle = "结果" + queryResultCount + " (语句" + (i + 1) + ")";
                    tabbedPane.addTab(tabTitle, resultTable);
                    
                    detailReport.append(" [查看结果标签页: ").append(tabTitle).append("]");
                }
            } else {
                failureCount++;
                detailReport.append("✗ 执行失败");
                if (result.getMessage() != null && !result.getMessage().trim().isEmpty()) {
                    detailReport.append(" - ").append(result.getMessage());
                }
            }
            detailReport.append("\n");
        }
        
        // 添加统计信息
        detailReport.append("\n=== 执行统计 ===\n");
        detailReport.append("总计语句: ").append(results.size()).append("\n");
        detailReport.append("成功: ").append(successCount).append("\n");
        detailReport.append("失败: ").append(failureCount).append("\n");
        detailReport.append("成功率: ").append(String.format("%.1f%%", (double)successCount / results.size() * 100)).append("\n");
        
        // 显示详细报告
        showMessage(detailReport.toString());
        
        // 如果有查询结果，切换到第一个结果标签页
        if (queryResultCount > 0) {
            tabbedPane.setSelectedIndex(1); // 跳过消息标签页
        }
    }
    
    /**
     * 清空结果标签页
     */
    private void clearResultTabs() {
        // 保留消息标签页，删除其他标签页
        while (tabbedPane.getTabCount() > 1) {
            tabbedPane.removeTabAt(1);
        }
        resultTabCount = 0;
        
        // 清空结果面板
        resultPanel.removeAll();
        resultPanel.revalidate();
        resultPanel.repaint();
    }
    
    /**
     * 显示错误信息
     */
    public void showError(String errorMessage) {
        messageArea.append("=== 错误信息 ===\n");
        messageArea.append("错误: " + errorMessage + "\n");
        messageArea.append("\n" + "=".repeat(50) + "\n\n");
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
        
        // 切换到消息标签页
        tabbedPane.setSelectedIndex(0);
    }
    
    /**
     * 显示编译错误
     */
    public void showCompileError(String sql, List<String> errors) {
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append("=== 编译失败 ===\n");
        errorMsg.append("SQL: ").append(sql).append("\n");
        if (errors != null) {
            for (String error : errors) {
                errorMsg.append("错误: ").append(error).append("\n");
            }
        }
        errorMsg.append("\n").append("=".repeat(50)).append("\n\n");
        
        messageArea.append(errorMsg.toString());
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
        
        // 切换到消息标签页
        tabbedPane.setSelectedIndex(0);
    }
    
    /**
     * 清空所有内容
     */
    public void clear() {
        messageArea.setText("");
        resultPanel.removeAll();
        resultTabCount = 0;
        
        // 清空所有结果标签页
        clearResultTabs();
        
        // 切换到消息标签页
        tabbedPane.setSelectedIndex(0);
    }
    
    /**
     * 获取标签栏组件
     */
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }
    
    /**
     * 获取消息区域
     */
    public JTextArea getMessageArea() {
        return messageArea;
    }
    
    /**
     * 获取结果面板
     */
    public JPanel getResultPanel() {
        return resultPanel;
    }
}
