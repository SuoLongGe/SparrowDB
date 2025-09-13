package com.sqlcompiler.gui;

import com.database.engine.ExecutionResult;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * 查询结果表格显示组件
 * 用于以表格形式展示SQL查询结果，类似Navicat的显示效果
 */
public class QueryResultTable extends JPanel {
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JScrollPane scrollPane;
    private JLabel statusLabel;
    
    public QueryResultTable() {
        initializeComponents();
        setupLayout();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // 创建表格模型
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格只读
            }
        };
        
        // 创建表格
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        resultTable.setRowHeight(25);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.setGridColor(new Color(200, 200, 200));
        resultTable.setShowGrid(true);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // 设置表格样式
        resultTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        resultTable.getTableHeader().setBackground(new Color(240, 240, 240));
        resultTable.getTableHeader().setForeground(Color.BLACK);
        
        // 创建滚动面板
        scrollPane = new JScrollPane(resultTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        // 状态标签
        statusLabel = new JLabel("暂无数据");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        statusLabel.setForeground(new Color(100, 100, 100));
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        
        // 底部状态栏
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 显示执行结果
     */
    public void displayResult(ExecutionResult result) {
        if (result == null || !result.isSuccess()) {
            showError(result != null ? result.getMessage() : "执行失败");
            return;
        }
        
        List<Map<String, Object>> data = result.getData();
        if (data == null || data.isEmpty()) {
            showMessage("查询成功，但无数据返回");
            return;
        }
        
        // 清空现有数据
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        
        // 获取列名
        Map<String, Object> firstRow = data.get(0);
        String[] columnNames = firstRow.keySet().toArray(new String[0]);
        
        // 设置列名
        for (String columnName : columnNames) {
            tableModel.addColumn(columnName);
        }
        
        // 添加数据行
        for (Map<String, Object> row : data) {
            Object[] rowData = new Object[columnNames.length];
            for (int i = 0; i < columnNames.length; i++) {
                Object value = row.get(columnNames[i]);
                rowData[i] = value != null ? value.toString() : "NULL";
            }
            tableModel.addRow(rowData);
        }
        
        // 自动调整列宽
        autoResizeColumns();
        
        // 更新状态
        statusLabel.setText(String.format("共 %d 行数据", data.size()));
        statusLabel.setForeground(new Color(0, 120, 0));
    }
    
    /**
     * 显示错误信息
     */
    public void showError(String errorMessage) {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(2);
        tableModel.setColumnIdentifiers(new String[]{"状态", "消息"});
        tableModel.addRow(new Object[]{"错误", errorMessage});
        
        statusLabel.setText("执行失败");
        statusLabel.setForeground(Color.RED);
    }
    
    /**
     * 显示消息
     */
    public void showMessage(String message) {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(1);
        tableModel.setColumnIdentifiers(new String[]{"消息"});
        tableModel.addRow(new Object[]{message});
        
        statusLabel.setText("执行完成");
        statusLabel.setForeground(new Color(0, 120, 0));
    }
    
    /**
     * 清空表格
     */
    public void clear() {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        statusLabel.setText("暂无数据");
        statusLabel.setForeground(new Color(100, 100, 100));
    }
    
    /**
     * 自动调整列宽
     */
    private void autoResizeColumns() {
        for (int i = 0; i < resultTable.getColumnCount(); i++) {
            TableColumn column = resultTable.getColumnModel().getColumn(i);
            int maxWidth = 0;
            
            // 计算列标题宽度
            FontMetrics fm = resultTable.getFontMetrics(resultTable.getFont());
            int headerWidth = fm.stringWidth(column.getHeaderValue().toString()) + 20;
            maxWidth = Math.max(maxWidth, headerWidth);
            
            // 计算数据宽度
            for (int row = 0; row < resultTable.getRowCount(); row++) {
                Object value = resultTable.getValueAt(row, i);
                if (value != null) {
                    int cellWidth = fm.stringWidth(value.toString()) + 10;
                    maxWidth = Math.max(maxWidth, cellWidth);
                }
            }
            
            // 设置最小和最大宽度
            column.setMinWidth(80);
            column.setMaxWidth(300);
            column.setPreferredWidth(Math.min(maxWidth, 300));
        }
    }
    
    /**
     * 获取表格组件（用于外部访问）
     */
    public JTable getTable() {
        return resultTable;
    }
    
    /**
     * 获取滚动面板（用于外部访问）
     */
    public JScrollPane getScrollPane() {
        return scrollPane;
    }
}
