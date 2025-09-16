package com.sqlcompiler.gui;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * 行号显示面板
 * 为文本组件提供行号显示功能
 */
public class LineNumberPanel extends JPanel {
    private JTextComponent textComponent;
    private Font font;
    private int lineHeight;
    private int fontAscent;
    private int fontHeight;
    private int fontWidth;
    private int maxDigits;
    
    // 颜色配置
    private static final Color BACKGROUND_COLOR = new Color(248, 248, 248);
    private static final Color FOREGROUND_COLOR = new Color(128, 128, 128);
    private static final Color BORDER_COLOR = new Color(200, 200, 200);
    
    public LineNumberPanel(JTextComponent textComponent) {
        this.textComponent = textComponent;
        this.font = textComponent.getFont();
        
        // 设置面板属性
        setBackground(BACKGROUND_COLOR);
        setForeground(FOREGROUND_COLOR);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));
        
        // 计算字体相关属性
        FontMetrics fm = getFontMetrics(font);
        fontAscent = fm.getAscent();
        fontHeight = fm.getHeight();
        fontWidth = fm.charWidth('0');
        lineHeight = fontHeight;
        
        // 设置初始宽度
        updateWidth();
        
        // 监听文本变化
        textComponent.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateWidth();
                repaint();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateWidth();
                repaint();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateWidth();
                repaint();
            }
        });
        
        // 监听字体变化
        textComponent.addPropertyChangeListener("font", e -> {
            font = textComponent.getFont();
            FontMetrics fm2 = getFontMetrics(font);
            fontAscent = fm2.getAscent();
            fontHeight = fm2.getHeight();
            fontWidth = fm2.charWidth('0');
            lineHeight = fontHeight;
            updateWidth();
            repaint();
        });
    }
    
    /**
     * 更新面板宽度
     */
    private void updateWidth() {
        int lineCount = getLineCount();
        maxDigits = String.valueOf(lineCount).length();
        
        // 设置宽度，至少显示3位数字，并留出一些边距
        int width = Math.max(3, maxDigits) * fontWidth + 10;
        setPreferredSize(new Dimension(width, 0));
        setMinimumSize(new Dimension(width, 0));
        setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
    }
    
    /**
     * 获取文本行数
     */
    private int getLineCount() {
        Document doc = textComponent.getDocument();
        if (doc == null) return 1;
        
        Element root = doc.getDefaultRootElement();
        return root.getElementCount();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(font);
        g2d.setColor(getForeground());
        
        // 获取文档信息
        Document doc = textComponent.getDocument();
        if (doc == null) return;
        
        Element root = doc.getDefaultRootElement();
        int lineCount = root.getElementCount();
        
        // 获取文本组件的可见区域和滚动位置
        Rectangle visibleRect = textComponent.getVisibleRect();
        int scrollY = visibleRect.y;
        
        // 计算可见区域内的行范围
        int startLine = scrollY / lineHeight;
        int endLine = Math.min(lineCount, (scrollY + visibleRect.height + lineHeight - 1) / lineHeight);
        
        // 绘制行号 - 行号应该与文本行完全对应
        for (int line = startLine; line < endLine; line++) {
            // 计算行号在行号面板中的Y坐标（与文本组件同步）
            int y = line * lineHeight + fontAscent - scrollY;
            String lineNumber = String.valueOf(line + 1);
            
            // 右对齐显示行号
            int x = getWidth() - fontWidth * lineNumber.length() - 5;
            g2d.drawString(lineNumber, x, y);
        }
    }
    
    /**
     * 获取行号面板的宽度
     */
    public int getLineNumberWidth() {
        return getPreferredSize().width;
    }
}
