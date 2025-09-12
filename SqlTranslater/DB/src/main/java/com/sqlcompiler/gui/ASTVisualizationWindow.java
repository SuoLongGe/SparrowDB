package com.sqlcompiler.gui;

import com.sqlcompiler.ast.Statement;

import javax.swing.*;
import java.awt.*;

/**
 * AST可视化窗口
 * 提供独立的AST图形化显示窗口
 */
public class ASTVisualizationWindow extends JFrame {
    private ASTVisualizer visualizer;
    private JToolBar toolBar;
    private JLabel statusLabel;
    
    public ASTVisualizationWindow() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        setTitle("AST可视化 - SparrowDB");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // 创建AST可视化器
        visualizer = new ASTVisualizer();
        
        // 创建工具栏
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        // 添加工具栏按钮
        JButton resetButton = new JButton("重置视图");
        resetButton.setToolTipText("重置缩放和位置");
        resetButton.addActionListener(e -> visualizer.resetView());
        
        JButton fitButton = new JButton("适应窗口");
        fitButton.setToolTipText("自动调整视图以适应窗口大小");
        fitButton.addActionListener(e -> visualizer.fitToWindow());
        
        JButton zoomInButton = new JButton("放大");
        zoomInButton.setToolTipText("放大视图");
        zoomInButton.addActionListener(e -> zoomIn());
        
        JButton zoomOutButton = new JButton("缩小");
        zoomOutButton.setToolTipText("缩小视图");
        zoomOutButton.addActionListener(e -> zoomOut());
        
        toolBar.add(resetButton);
        toolBar.add(fitButton);
        toolBar.addSeparator();
        toolBar.add(zoomInButton);
        toolBar.add(zoomOutButton);
        
        // 状态标签
        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        
        // 添加使用说明
        JLabel helpLabel = new JLabel("使用说明: 拖拽移动视图 | 滚轮缩放 | 单击选择节点 | 双击查看详情");
        helpLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        helpLabel.setForeground(Color.GRAY);
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 顶部工具栏
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toolBar, BorderLayout.WEST);
        topPanel.add(statusLabel, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
        
        // 中央可视化区域
        JScrollPane scrollPane = new JScrollPane(visualizer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createTitledBorder("AST树形结构"));
        
        add(scrollPane, BorderLayout.CENTER);
        
        // 底部帮助信息
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel helpLabel = new JLabel("使用说明: 拖拽移动视图 | 滚轮缩放 | 单击选择节点 | 双击查看详情");
        helpLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        helpLabel.setForeground(Color.GRAY);
        bottomPanel.add(helpLabel);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // 设置窗口属性
        setSize(1000, 700);
        setLocationRelativeTo(null);
    }
    
    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 窗口关闭事件
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                dispose();
            }
        });
    }
    
    /**
     * 设置要显示的AST
     */
    public void setAST(Statement ast) {
        visualizer.setAST(ast);
        if (ast != null) {
            statusLabel.setText("AST已加载");
            statusLabel.setForeground(Color.GREEN);
        } else {
            statusLabel.setText("无AST数据");
            statusLabel.setForeground(Color.GRAY);
        }
    }
    
    /**
     * 放大视图
     */
    private void zoomIn() {
        // 通过模拟鼠标滚轮事件来放大
        java.awt.event.MouseWheelEvent event = new java.awt.event.MouseWheelEvent(
            visualizer, 
            java.awt.event.MouseWheelEvent.MOUSE_WHEEL, 
            System.currentTimeMillis(), 
            0, 
            visualizer.getWidth() / 2, 
            visualizer.getHeight() / 2, 
            0, 
            false, 
            java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 
            1, 
            -1
        );
        visualizer.dispatchEvent(event);
    }
    
    /**
     * 缩小视图
     */
    private void zoomOut() {
        // 通过模拟鼠标滚轮事件来缩小
        java.awt.event.MouseWheelEvent event = new java.awt.event.MouseWheelEvent(
            visualizer, 
            java.awt.event.MouseWheelEvent.MOUSE_WHEEL, 
            System.currentTimeMillis(), 
            0, 
            visualizer.getWidth() / 2, 
            visualizer.getHeight() / 2, 
            0, 
            false, 
            java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 
            1, 
            1
        );
        visualizer.dispatchEvent(event);
    }
    
    /**
     * 显示窗口
     */
    public void showWindow() {
        setVisible(true);
        toFront();
    }
    
    /**
     * 隐藏窗口
     */
    public void hideWindow() {
        setVisible(false);
    }
}
