package com.sqlcompiler.gui;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * 带行号的滚动面板
 * 将文本组件和行号面板组合在一起，实现同步滚动
 */
public class LineNumberScrollPane extends JPanel {
    private JTextComponent textComponent;
    private LineNumberPanel lineNumberPanel;
    private JScrollPane scrollPane;
    
    public LineNumberScrollPane(JTextComponent textComponent) {
        this.textComponent = textComponent;
        this.lineNumberPanel = new LineNumberPanel(textComponent);
        this.scrollPane = new JScrollPane(textComponent);
        
        setupLayout();
        setupSynchronizedScrolling();
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 将行号面板放在左侧
        add(lineNumberPanel, BorderLayout.WEST);
        
        // 将滚动面板放在中央
        add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * 设置同步滚动
     */
    private void setupSynchronizedScrolling() {
        // 监听文本组件的滚动事件
        textComponent.addPropertyChangeListener("font", e -> {
            // 字体变化时重新计算行号面板
            SwingUtilities.invokeLater(() -> {
                lineNumberPanel.repaint();
            });
        });
        
        // 监听滚动条变化
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
        
        // 垂直滚动同步 - 实时更新
        verticalScrollBar.addAdjustmentListener(e -> {
            lineNumberPanel.repaint();
        });
        
        // 水平滚动同步（行号面板不需要水平滚动）
        horizontalScrollBar.addAdjustmentListener(e -> {
            lineNumberPanel.repaint();
        });
        
        // 监听文本组件的可见区域变化
        textComponent.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                lineNumberPanel.repaint();
            }
        });
        
        // 监听文本组件的滚动事件
        textComponent.addPropertyChangeListener("visibleRect", e -> {
            lineNumberPanel.repaint();
        });
    }
    
    /**
     * 获取内部的滚动面板
     */
    public JScrollPane getScrollPane() {
        return scrollPane;
    }
    
    /**
     * 获取行号面板
     */
    public LineNumberPanel getLineNumberPanel() {
        return lineNumberPanel;
    }
    
    /**
     * 获取文本组件
     */
    public JTextComponent getTextComponent() {
        return textComponent;
    }
    
    /**
     * 设置滚动条策略
     */
    public void setVerticalScrollBarPolicy(int policy) {
        if (scrollPane != null) {
            scrollPane.setVerticalScrollBarPolicy(policy);
        }
    }
    
    public void setHorizontalScrollBarPolicy(int policy) {
        if (scrollPane != null) {
            scrollPane.setHorizontalScrollBarPolicy(policy);
        }
    }
    
    /**
     * 设置边框
     */
    public void setBorder(javax.swing.border.Border border) {
        if (scrollPane != null) {
            scrollPane.setBorder(border);
        }
    }
    
    /**
     * 设置大小
     */
    @Override
    public void setSize(Dimension d) {
        super.setSize(d);
        scrollPane.setSize(d);
    }
    
    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        scrollPane.setSize(width, height);
    }
    
    @Override
    public void setPreferredSize(Dimension preferredSize) {
        super.setPreferredSize(preferredSize);
        scrollPane.setPreferredSize(preferredSize);
    }
    
    @Override
    public void setMinimumSize(Dimension minimumSize) {
        super.setMinimumSize(minimumSize);
        scrollPane.setMinimumSize(minimumSize);
    }
    
    @Override
    public void setMaximumSize(Dimension maximumSize) {
        super.setMaximumSize(maximumSize);
        scrollPane.setMaximumSize(maximumSize);
    }
}
