package com.sqlcompiler;

import com.database.engine.DatabaseEngine;
import com.database.engine.sharding.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 现代化分片管理器对话框
 * 提供分片创建、查看、删除等功能，采用科技感设计
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
    private JLabel statusLabel;
    
    // 现代化配色方案
    private static final Color BUTTON_COLOR = new Color(127, 198, 255);
    private static final Color PRIMARY_BACKGROUND = new Color(250, 250, 250);
    private static final Color CARD_BACKGROUND = new Color(255, 255, 255);
    private static final Color TEXT_PRIMARY = new Color(17, 24, 39);
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    private static final Color BORDER_COLOR = new Color(229, 231, 235);
    private static final Color SUCCESS_COLOR = new Color(127, 198, 255);
    private static final Color WARNING_COLOR = new Color(127, 198, 255);
    private static final Color DANGER_COLOR = new Color(127, 198, 255);
    
    public ShardManagerDialog(Frame parent, DatabaseEngine databaseEngine) {
        super(parent, "分片管理器", true);
        this.databaseEngine = databaseEngine;
        
        // 设置现代化外观
        setupModernLookAndFeel();
        
        // 初始化组件
        initializeComponents();
        
        // 设置布局
        setupLayout();
        
        // 设置事件处理器
        setupEventHandlers();
        
        // 设置对话框属性
        setSize(900, 700);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(PRIMARY_BACKGROUND);
        
        // 加载数据
        refreshTableList();
        refreshShardTable();
        
        // 设置窗口图标
        setIconImage(createWindowIcon());
    }
    
    /**
     * 设置现代化外观
     */
    private void setupModernLookAndFeel() {
        try {
            // 自定义UI属性
            UIManager.put("Panel.background", PRIMARY_BACKGROUND);
            UIManager.put("Button.background", CARD_BACKGROUND);
            UIManager.put("TextField.background", CARD_BACKGROUND);
            UIManager.put("ComboBox.background", CARD_BACKGROUND);
            UIManager.put("Table.background", CARD_BACKGROUND);
            UIManager.put("Table.gridColor", BORDER_COLOR);
            UIManager.put("Table.selectionBackground", new Color(BUTTON_COLOR.getRed(), BUTTON_COLOR.getGreen(), BUTTON_COLOR.getBlue(), 100));
        } catch (Exception e) {
            System.err.println("无法设置外观: " + e.getMessage());
        }
    }
    
    /**
     * 创建现代科技风格按钮 - 与主界面完全一致
     */
    private JButton createStyledButton(String text, Color bgColor, Color textColor, int fontSize, boolean isBold) {
        JButton button = new JButton(text) {
            private boolean isAnimating = false;
            private float animationProgress = 0f;
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int width = getWidth();
                int height = getHeight();
                int cornerRadius = 12; // 更大的圆角
                
                // 创建按钮阴影
                if (!getModel().isPressed()) {
                    g2d.setColor(new Color(0, 0, 0, 25));
                    g2d.fillRoundRect(2, 4, width - 4, height - 4, cornerRadius, cornerRadius);
                }
                
                // 创建渐变背景
                Color startColor, endColor;
                if (getModel().isPressed()) {
                    startColor = darkenColor(bgColor, 0.2f);
                    endColor = darkenColor(bgColor, 0.1f);
                } else if (getModel().isRollover()) {
                    startColor = brightenColor(bgColor, 0.1f);
                    endColor = bgColor;
                } else {
                    startColor = brightenColor(bgColor, 0.05f);
                    endColor = darkenColor(bgColor, 0.05f);
                }
                
                // 绘制渐变背景
                GradientPaint gradient = new GradientPaint(0, 0, startColor, 0, height, endColor);
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);
                
                // 添加高光效果
                if (!getModel().isPressed()) {
                    g2d.setColor(new Color(255, 255, 255, 40));
                    g2d.fillRoundRect(1, 1, width - 2, height / 2, cornerRadius, cornerRadius);
                }
                
                // 绘制边框
                g2d.setStroke(new BasicStroke(1.5f));
                if (getModel().isRollover()) {
                    g2d.setColor(brightenColor(bgColor, 0.3f));
                } else {
                    g2d.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 100));
                }
                g2d.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
                
                // 绘制文本
                g2d.setColor(textColor);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (width - fm.stringWidth(getText())) / 2;
                int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), textX, textY);
                
                g2d.dispose();
            }
        };
        
        button.setFont(new Font("Microsoft YaHei UI", isBold ? Font.BOLD : Font.PLAIN, fontSize));
        button.setForeground(textColor);
        button.setBackground(bgColor);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 35));
        
        // 添加悬停效果 - 与主界面完全一致
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });
        
        return button;
    }
    
    /**
     * 样式化下拉框
     */
    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        comboBox.setBackground(CARD_BACKGROUND);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
    }
    
    /**
     * 样式化文本框
     */
    private void styleTextField(JTextField textField) {
        textField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        textField.setBackground(CARD_BACKGROUND);
        textField.setForeground(TEXT_PRIMARY);
        textField.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
    }
    
    /**
     * 样式化微调器
     */
    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        spinner.setBackground(CARD_BACKGROUND);
        spinner.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(4, 8, 4, 8)
        ));
    }
    
    /**
     * 样式化表格
     */
    private void styleTable(JTable table) {
        table.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        table.setRowHeight(32);
        table.setBackground(CARD_BACKGROUND);
        table.setForeground(TEXT_PRIMARY);
        table.setSelectionBackground(new Color(BUTTON_COLOR.getRed(), BUTTON_COLOR.getGreen(), BUTTON_COLOR.getBlue(), 100));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER_COLOR);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        
        // 样式化表头
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        header.setBackground(new Color(248, 249, 250));
        header.setForeground(TEXT_PRIMARY);
        header.setBorder(new LineBorder(BORDER_COLOR, 1));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        
        // 自定义单元格渲染器
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
                setBorder(new EmptyBorder(8, 12, 8, 12));
                
                if (isSelected) {
                    setBackground(new Color(BUTTON_COLOR.getRed(), BUTTON_COLOR.getGreen(), BUTTON_COLOR.getBlue(), 100));
                    setForeground(TEXT_PRIMARY);
                } else {
                    setBackground(row % 2 == 0 ? CARD_BACKGROUND : new Color(248, 249, 250));
                    setForeground(TEXT_PRIMARY);
                }
                
                return this;
            }
        });
    }
    
    /**
     * 创建现代化标题边框
     */
    private TitledBorder createStyledTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
            new LineBorder(BUTTON_COLOR, 2, true), 
            title
        );
        border.setTitleFont(new Font("Microsoft YaHei UI", Font.BOLD, 13));
        border.setTitleColor(BUTTON_COLOR);
        border.setTitleJustification(TitledBorder.LEFT);
        border.setTitlePosition(TitledBorder.TOP);
        return border;
    }
    
    /**
     * 创建窗口图标
     */
    private Image createWindowIcon() {
        int size = 32;
        java.awt.image.BufferedImage icon = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 创建渐变背景
        GradientPaint gradient = new GradientPaint(0, 0, brightenColor(BUTTON_COLOR, 0.2f), 
                                                   size, size, darkenColor(BUTTON_COLOR, 0.1f));
        g2d.setPaint(gradient);
        g2d.fillRoundRect(2, 2, size-4, size-4, 10, 10);
        
        // 绘制分片图标
        g2d.setColor(CARD_BACKGROUND);
        g2d.setStroke(new BasicStroke(2.0f));
        
        // 绘制分片符号
        int centerX = size / 2;
        int centerY = size / 2;
        g2d.drawRect(centerX - 8, centerY - 8, 6, 6);
        g2d.drawRect(centerX - 2, centerY - 8, 6, 6);
        g2d.drawRect(centerX + 4, centerY - 8, 6, 6);
        g2d.drawRect(centerX - 8, centerY - 2, 6, 6);
        g2d.drawRect(centerX - 2, centerY - 2, 6, 6);
        g2d.drawRect(centerX + 4, centerY - 2, 6, 6);
        
        g2d.dispose();
        return icon;
    }
    
    /**
     * 加亮颜色
     */
    private Color brightenColor(Color color, float factor) {
        int red = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor));
        int green = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor));
        int blue = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(red, green, blue);
    }
    
    /**
     * 变暗颜色
     */
    private Color darkenColor(Color color, float factor) {
        int red = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int green = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int blue = Math.max(0, (int) (color.getBlue() * (1 - factor)));
        return new Color(red, green, blue);
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
        styleTable(shardTable);
        
        // 表选择下拉框
        tableComboBox = new JComboBox<>();
        tableComboBox.setPreferredSize(new Dimension(180, 40));
        styleComboBox(tableComboBox);
        
        // 策略选择下拉框
        strategyComboBox = new JComboBox<>(new String[]{"HASH", "RANGE"});
        strategyComboBox.setPreferredSize(new Dimension(120, 40));
        styleComboBox(strategyComboBox);
        
        // 分片键输入框
        shardKeyField = new JTextField(15);
        shardKeyField.setPreferredSize(new Dimension(150, 40));
        shardKeyField.setToolTipText("分片键列名，如：id, user_id, order_date");
        styleTextField(shardKeyField);
        
        // 分片数量选择器
        shardCountSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        shardCountSpinner.setPreferredSize(new Dimension(100, 40));
        styleSpinner(shardCountSpinner);
        
        // 按钮 - 使用与主界面完全一致的风格
        createShardButton = createStyledButton("创建分片", SUCCESS_COLOR, CARD_BACKGROUND, 12, true);
        createShardButton.setPreferredSize(new Dimension(110, 35));
        
        deleteShardButton = createStyledButton("删除分片", DANGER_COLOR, CARD_BACKGROUND, 12, true);
        deleteShardButton.setPreferredSize(new Dimension(110, 35));
        deleteShardButton.setEnabled(false);
        
        refreshButton = createStyledButton("刷新", BUTTON_COLOR, CARD_BACKGROUND, 12, true);
        refreshButton.setPreferredSize(new Dimension(90, 35));
        
        showStatsButton = createStyledButton("统计信息", WARNING_COLOR, CARD_BACKGROUND, 12, true);
        showStatsButton.setPreferredSize(new Dimension(110, 35));
        
        // 状态标签
        statusLabel = new JLabel("选择表后可以创建分片，选择分片行后可以删除分片");
        statusLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_SECONDARY);
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(PRIMARY_BACKGROUND);
        
        // 顶部控制面板
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());
        topPanel.setBackground(CARD_BACKGROUND);
        topPanel.setBorder(new CompoundBorder(
            createStyledTitledBorder("分片操作"),
            new EmptyBorder(15, 20, 15, 20)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 第一行
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel tableLabel = new JLabel("表名:");
        tableLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        tableLabel.setForeground(TEXT_PRIMARY);
        topPanel.add(tableLabel, gbc);
        
        gbc.gridx = 1;
        topPanel.add(tableComboBox, gbc);
        
        gbc.gridx = 2;
        JLabel keyLabel = new JLabel("分片键:");
        keyLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        keyLabel.setForeground(TEXT_PRIMARY);
        topPanel.add(keyLabel, gbc);
        
        gbc.gridx = 3;
        topPanel.add(shardKeyField, gbc);
        
        // 第二行
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel strategyLabel = new JLabel("策略:");
        strategyLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        strategyLabel.setForeground(TEXT_PRIMARY);
        topPanel.add(strategyLabel, gbc);
        
        gbc.gridx = 1;
        topPanel.add(strategyComboBox, gbc);
        
        gbc.gridx = 2;
        JLabel countLabel = new JLabel("分片数:");
        countLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        countLabel.setForeground(TEXT_PRIMARY);
        topPanel.add(countLabel, gbc);
        
        gbc.gridx = 3;
        topPanel.add(shardCountSpinner, gbc);
        
        // 第三行 - 按钮
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(CARD_BACKGROUND);
        buttonPanel.add(createShardButton);
        buttonPanel.add(deleteShardButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(showStatsButton);
        
        topPanel.add(buttonPanel, gbc);
        
        add(topPanel, BorderLayout.NORTH);
        
        // 中间分片表格
        JScrollPane scrollPane = new JScrollPane(shardTable);
        scrollPane.setBorder(createStyledTitledBorder("分片信息"));
        scrollPane.setBackground(CARD_BACKGROUND);
        scrollPane.getViewport().setBackground(CARD_BACKGROUND);
        scrollPane.setPreferredSize(new Dimension(850, 400));
        add(scrollPane, BorderLayout.CENTER);
        
        // 底部状态面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        bottomPanel.setBackground(PRIMARY_BACKGROUND);
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
                statusLabel.setText("已刷新分片信息");
                statusLabel.setForeground(SUCCESS_COLOR);
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
                if (selectedRow >= 0) {
                    statusLabel.setText("已选择分片，可以删除");
                    statusLabel.setForeground(TEXT_SECONDARY);
                } else {
                    statusLabel.setText("选择表后可以创建分片，选择分片行后可以删除分片");
                    statusLabel.setForeground(TEXT_SECONDARY);
                }
            }
        });
        
        // 表选择事件
        tableComboBox.addActionListener(e -> {
            String selectedTable = (String) tableComboBox.getSelectedItem();
            if (selectedTable != null && !selectedTable.isEmpty()) {
                // 自动填充分片键（使用第一个列名）
                try {
                    autoFillShardKey(selectedTable);
                    statusLabel.setText("已选择表: " + selectedTable);
                    statusLabel.setForeground(SUCCESS_COLOR);
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
                statusLabel.setText("分片创建成功: " + tableName);
                statusLabel.setForeground(SUCCESS_COLOR);
            } else {
                JOptionPane.showMessageDialog(this, "创建分片失败: " + result.getMessage(), 
                                            "错误", JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("分片创建失败");
                statusLabel.setForeground(DANGER_COLOR);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "创建分片时发生错误: " + e.getMessage(), 
                                        "错误", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("分片创建错误");
            statusLabel.setForeground(DANGER_COLOR);
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
                    statusLabel.setText("分片删除成功: " + tableName);
                    statusLabel.setForeground(SUCCESS_COLOR);
                } else {
                    JOptionPane.showMessageDialog(this, "删除分片失败: " + execResult.getMessage(), 
                                                "错误", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("分片删除失败");
                    statusLabel.setForeground(DANGER_COLOR);
                }
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "删除分片时发生错误: " + e.getMessage(), 
                                            "错误", JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("分片删除错误");
                statusLabel.setForeground(DANGER_COLOR);
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
                statusLabel.setText("已显示统计信息: " + tableName);
                statusLabel.setForeground(SUCCESS_COLOR);
            } else {
                JOptionPane.showMessageDialog(this, "获取统计信息失败: " + result.getMessage(), 
                                            "错误", JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("获取统计信息失败");
                statusLabel.setForeground(DANGER_COLOR);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "获取统计信息时发生错误: " + e.getMessage(), 
                                        "错误", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("统计信息错误");
            statusLabel.setForeground(DANGER_COLOR);
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