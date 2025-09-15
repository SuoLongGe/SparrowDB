package com.sqlcompiler;

import com.database.engine.DatabaseEngine;
import com.database.engine.sharding.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 分片管理器对话框
 * 提供分片创建、查看、删除等功能
 */
public class ShardManagerDialog extends JDialog {
    private final DatabaseEngine databaseEngine;
    private JTable shardTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> tableComboBox;
    private JComboBox<String> strategyComboBox;
    private JTextField shardKeyField;
    private JSpinner shardCountSpinner;
    private JButton createShardButton;
    private JButton deleteShardButton;
    private JButton refreshButton;
    private JButton showStatsButton;
    
    public ShardManagerDialog(Frame parent, DatabaseEngine databaseEngine) {
        super(parent, "分片管理器", true);
        this.databaseEngine = databaseEngine;
        
        // 初始化组件
        initializeComponents();
        
        // 设置布局
        setupLayout();
        
        // 设置事件处理器
        setupEventHandlers();
        
        // 设置对话框属性
        setSize(800, 600);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // 加载数据
        refreshTableList();
        refreshShardTable();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // 分片表格
        String[] columnNames = {"表名", "分片ID", "节点ID", "分片类型", "分片键列", "策略", "状态", "记录数", "数据目录"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格只读
            }
        };
        shardTable = new JTable(tableModel);
        shardTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shardTable.getTableHeader().setReorderingAllowed(false);
        
        // 表选择下拉框
        tableComboBox = new JComboBox<>();
        tableComboBox.setPreferredSize(new Dimension(150, 25));
        
        // 策略选择下拉框
        strategyComboBox = new JComboBox<>(new String[]{"HASH", "RANGE"});
        strategyComboBox.setPreferredSize(new Dimension(100, 25));
        
        // 分片键输入框
        shardKeyField = new JTextField(15);
        shardKeyField.setToolTipText("分片键列名，如：id, user_id, order_date");
        
        // 分片数量选择器
        shardCountSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        shardCountSpinner.setPreferredSize(new Dimension(80, 25));
        
        // 按钮
        createShardButton = new JButton("创建分片");
        createShardButton.setPreferredSize(new Dimension(100, 30));
        
        deleteShardButton = new JButton("删除分片");
        deleteShardButton.setPreferredSize(new Dimension(100, 30));
        deleteShardButton.setEnabled(false);
        
        refreshButton = new JButton("刷新");
        refreshButton.setPreferredSize(new Dimension(80, 30));
        
        showStatsButton = new JButton("统计信息");
        showStatsButton.setPreferredSize(new Dimension(100, 30));
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 顶部控制面板
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createTitledBorder("分片操作"));
        
        topPanel.add(new JLabel("表名:"));
        topPanel.add(tableComboBox);
        
        topPanel.add(new JLabel("分片键:"));
        topPanel.add(shardKeyField);
        
        topPanel.add(new JLabel("策略:"));
        topPanel.add(strategyComboBox);
        
        topPanel.add(new JLabel("分片数:"));
        topPanel.add(shardCountSpinner);
        
        topPanel.add(createShardButton);
        topPanel.add(deleteShardButton);
        topPanel.add(refreshButton);
        topPanel.add(showStatsButton);
        
        add(topPanel, BorderLayout.NORTH);
        
        // 中间分片表格
        JScrollPane scrollPane = new JScrollPane(shardTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("分片信息"));
        add(scrollPane, BorderLayout.CENTER);
        
        // 底部状态面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel statusLabel = new JLabel("选择表后可以创建分片，选择分片行后可以删除分片");
        statusLabel.setForeground(Color.BLUE);
        bottomPanel.add(statusLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 创建分片按钮
        createShardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createShard();
            }
        });
        
        // 删除分片按钮
        deleteShardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteShard();
            }
        });
        
        // 刷新按钮
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshTableList();
                refreshShardTable();
            }
        });
        
        // 统计信息按钮
        showStatsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showShardStats();
            }
        });
        
        // 表格选择事件
        shardTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = shardTable.getSelectedRow();
                deleteShardButton.setEnabled(selectedRow >= 0);
            }
        });
        
        // 表选择事件
        tableComboBox.addActionListener(e -> {
            String selectedTable = (String) tableComboBox.getSelectedItem();
            if (selectedTable != null && !selectedTable.isEmpty()) {
                // 自动填充分片键（使用第一个列名）
                try {
                    autoFillShardKey(selectedTable);
                } catch (Exception ex) {
                    // 忽略错误
                }
            }
        });
    }
    
    /**
     * 刷新表列表
     */
    private void refreshTableList() {
        try {
            tableComboBox.removeAllItems();
            tableComboBox.addItem(""); // 空选项
            
            for (String tableName : databaseEngine.getCatalogManager().getAllTableNames()) {
                if (!tableName.startsWith("__system_")) { // 过滤系统表
                    tableComboBox.addItem(tableName);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "刷新表列表失败: " + e.getMessage(), 
                                        "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 刷新分片表格
     */
    private void refreshShardTable() {
        try {
            tableModel.setRowCount(0); // 清空表格
            
            for (String tableName : databaseEngine.getCatalogManager().getAllTableNames()) {
                if (databaseEngine.getShardManager().isTableSharded(tableName)) {
                    List<ShardInfo> shards = databaseEngine.getShardManager().getTableShards(tableName);
                    ShardMetadata metadata = databaseEngine.getShardManager().getShardMetadata(tableName);
                    
                    for (ShardInfo shard : shards) {
                        Object[] row = {
                            shard.getTableName(),
                            shard.getShardId(),
                            shard.getNodeId(),
                            shard.getShardType().toString(),
                            metadata.getShardKeyColumn(),
                            metadata.getStrategy().getStrategyName(),
                            shard.isActive() ? "活跃" : "非活跃",
                            shard.getRecordCount(),
                            shard.getDataDirectory()
                        };
                        tableModel.addRow(row);
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "刷新分片表格失败: " + e.getMessage(), 
                                        "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 自动填充分片键
     */
    private void autoFillShardKey(String tableName) {
        try {
            // 获取表的第一列作为默认分片键
            var tableInfo = databaseEngine.getCatalogManager().getTable(tableName);
            if (tableInfo != null && !tableInfo.getColumnNames().isEmpty()) {
                String firstColumn = tableInfo.getColumnNames().get(0);
                shardKeyField.setText(firstColumn);
            }
        } catch (Exception e) {
            // 忽略错误
        }
    }
    
    /**
     * 创建分片
     */
    private void createShard() {
        String tableName = (String) tableComboBox.getSelectedItem();
        String shardKeyColumn = shardKeyField.getText().trim();
        String strategyName = (String) strategyComboBox.getSelectedItem();
        int shardCount = (Integer) shardCountSpinner.getValue();
        
        // 验证输入
        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择要分片的表", "输入错误", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (shardKeyColumn.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入分片键列名", "输入错误", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 检查表是否已经分片
        if (databaseEngine.getShardManager().isTableSharded(tableName)) {
            int result = JOptionPane.showConfirmDialog(this, 
                "表 " + tableName + " 已经存在分片，是否要重新创建？\n这将删除现有的分片配置。",
                "确认重新创建分片", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        try {
            // 构建CREATE SHARD SQL命令
            String sql = String.format("CREATE SHARD %s BY %s USING %s (%d)", 
                                     tableName, shardKeyColumn, strategyName, shardCount);
            
            // 执行SQL命令
            var result = databaseEngine.executeSQL(sql);
            
            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(this, result.getMessage(), 
                                            "创建分片成功", JOptionPane.INFORMATION_MESSAGE);
                refreshShardTable();
            } else {
                JOptionPane.showMessageDialog(this, "创建分片失败: " + result.getMessage(), 
                                            "错误", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "创建分片时发生错误: " + e.getMessage(), 
                                        "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 删除分片
     */
    private void deleteShard() {
        int selectedRow = shardTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的分片", "选择错误", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String tableName = (String) tableModel.getValueAt(selectedRow, 0);
        
        int result = JOptionPane.showConfirmDialog(this, 
            "确定要删除表 " + tableName + " 的所有分片吗？\n此操作不可撤销！",
            "确认删除分片", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            try {
                String sql = "DROP SHARD " + tableName;
                var execResult = databaseEngine.executeSQL(sql);
                
                if (execResult.isSuccess()) {
                    JOptionPane.showMessageDialog(this, execResult.getMessage(), 
                                                "删除分片成功", JOptionPane.INFORMATION_MESSAGE);
                    refreshShardTable();
                } else {
                    JOptionPane.showMessageDialog(this, "删除分片失败: " + execResult.getMessage(), 
                                                "错误", JOptionPane.ERROR_MESSAGE);
                }
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "删除分片时发生错误: " + e.getMessage(), 
                                            "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * 显示分片统计信息
     */
    private void showShardStats() {
        String tableName = (String) tableComboBox.getSelectedItem();
        if (tableName == null || tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择要查看统计信息的表", "选择错误", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!databaseEngine.getShardManager().isTableSharded(tableName)) {
            JOptionPane.showMessageDialog(this, "表 " + tableName + " 没有分片", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        try {
            String sql = "SHARD STATS " + tableName;
            var result = databaseEngine.executeSQL(sql);
            
            if (result.isSuccess()) {
                // 显示统计信息
                showStatsDialog(tableName, result.getData());
            } else {
                JOptionPane.showMessageDialog(this, "获取统计信息失败: " + result.getMessage(), 
                                            "错误", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "获取统计信息时发生错误: " + e.getMessage(), 
                                        "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 显示统计信息对话框
     */
    private void showStatsDialog(String tableName, List<Map<String, Object>> statsData) {
        if (statsData == null || statsData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有统计信息", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        Map<String, Object> stats = statsData.get(0);
        
        StringBuilder message = new StringBuilder();
        message.append("表 ").append(tableName).append(" 的分片统计信息:\n\n");
        message.append("分片键列: ").append(stats.get("shard_key_column")).append("\n");
        message.append("分片策略: ").append(stats.get("strategy")).append("\n");
        message.append("总分片数: ").append(stats.get("total_shards")).append("\n");
        message.append("活跃分片数: ").append(stats.get("active_shards")).append("\n");
        message.append("总记录数: ").append(stats.get("total_records")).append("\n");
        message.append("平均记录数/分片: ").append(stats.get("average_records_per_shard")).append("\n");
        message.append("负载均衡: ").append((Boolean) stats.get("is_balanced") ? "是" : "否").append("\n");
        message.append("变异系数: ").append(String.format("%.4f", stats.get("coefficient_of_variation"))).append("\n");
        message.append("最大记录数: ").append(stats.get("max_records")).append("\n");
        message.append("最小记录数: ").append(stats.get("min_records")).append("\n");
        
        JOptionPane.showMessageDialog(this, message.toString(), 
                                    "分片统计信息 - " + tableName, JOptionPane.INFORMATION_MESSAGE);
    }
}
