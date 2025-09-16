package com.sqlcompiler;

import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import com.database.MultiDatabaseManager;
import com.sqlcompiler.catalog.TableInfo;

import com.sqlcompiler.gui.SQLAutoComplete;
import com.sqlcompiler.gui.SQLSyntaxHighlighter;
import com.sqlcompiler.gui.BeautifulASTVisualizer;
import com.sqlcompiler.gui.LineNumberScrollPane;
import com.sqlcompiler.gui.ResultTabbedPane;

import com.database.config.DatabaseConfig;


import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

/**
 * 数据库GUI界面
 * 提供SQL输入、结果显示和Token/AST显示功能
 */
public class DatabaseGUI extends JFrame {
    private EnhancedSQLCompiler compiler;
    private DatabaseEngine databaseEngine;
    private MultiDatabaseManager databaseManager;
    
    // 主题系统
    private static Theme currentTheme = new LightTheme();
    private JMenu themeMenu;
    
    // 字体管理系统
    private static class FontManager {
        private static Font chinesePrimaryFont;
        private static Font chineseCodeFont;
        
        /**
         * 获取支持中文的主要字体
         */
        public static Font getChineseFont(int style, int size) {
            if (chinesePrimaryFont == null) {
                chinesePrimaryFont = findBestChineseFont(false);
            }
            return chinesePrimaryFont.deriveFont(style, (float)size);
        }
        
        /**
         * 获取支持中文的代码字体
         */
        public static Font getChineseCodeFont(int style, int size) {
            if (chineseCodeFont == null) {
                chineseCodeFont = findBestChineseFont(true);
            }
            return chineseCodeFont.deriveFont(style, (float)size);
        }
        
        /**
         * 查找系统中最佳的中文字体
         */
        private static Font findBestChineseFont(boolean isCodeFont) {
            // 定义字体候选列表
            String[] primaryFonts = {
                "Microsoft YaHei UI Light",  // 微软雅黑UI Light
                "Microsoft YaHei UI",        // 微软雅黑UI
                "Microsoft YaHei",           // 微软雅黑
                "PingFang SC",               // 苹方 (macOS)
                "Noto Sans CJK SC",          // Google Noto
                "Source Han Sans SC",        // 思源黑体
                "WenQuanYi Micro Hei",       // 文泉驿微米黑 (Linux)
                "SimHei",                    // 黑体
                "SimSun",                    // 宋体
                "Dialog"                     // 系统默认
            };
            
            String[] codeFonts = {
                "JetBrains Mono",            // JetBrains Mono
                "Fira Code",                 // Fira Code
                "Source Code Pro",           // Source Code Pro
                "Consolas",                  // Consolas
                "Monaco",                    // Monaco (macOS)
                "Menlo",                     // Menlo (macOS)
                "DejaVu Sans Mono",          // DejaVu Sans Mono (Linux)
                "Liberation Mono",           // Liberation Mono (Linux)
                "Courier New",               // Courier New
                "Microsoft YaHei",           // 微软雅黑（备选）
                "Monospaced"                 // 系统等宽字体
            };
            
            String[] candidates = isCodeFont ? codeFonts : primaryFonts;
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] availableFonts = ge.getAvailableFontFamilyNames();
            
            // 测试中文字符
            String testChars = "中文测试ABCabc123";
            
            for (String fontName : candidates) {
                // 检查字体是否可用
                boolean fontAvailable = false;
                for (String available : availableFonts) {
                    if (available.equals(fontName)) {
                        fontAvailable = true;
                        break;
                    }
                }
                
                if (fontAvailable) {
                    try {
                        Font testFont = new Font(fontName, Font.PLAIN, 12);
                        
                        // 测试字体是否能显示中文
                        boolean canDisplayChinese = true;
                        for (char c : testChars.toCharArray()) {
                            if (!testFont.canDisplay(c)) {
                                canDisplayChinese = false;
                                break;
                            }
                        }
                        
                        if (canDisplayChinese) {
                            System.out.println("字体选择成功: " + fontName + " (类型: " + (isCodeFont ? "代码" : "界面") + ")");
                            return testFont;
                        }
                    } catch (Exception e) {
                        System.err.println("字体测试失败: " + fontName + " - " + e.getMessage());
                    }
                }
            }
            
            // 如果所有字体都不可用，创建一个复合字体
            System.out.println("使用复合字体方案");
            return createFallbackFont(isCodeFont);
        }
        
        /**
         * 创建备用复合字体
         */
        private static Font createFallbackFont(boolean isCodeFont) {
            try {
                // 尝试创建逻辑字体，这些字体会自动映射到系统可用的物理字体
                String logicalFontName = isCodeFont ? Font.MONOSPACED : Font.DIALOG;
                Font logicalFont = new Font(logicalFontName, Font.PLAIN, 12);
                
                // 测试逻辑字体
                if (logicalFont.canDisplay('中') && logicalFont.canDisplay('文')) {
                    System.out.println("使用逻辑字体: " + logicalFontName);
                    return logicalFont;
                }
                
                // 最后的备选方案：使用SansSerif
                Font sansSerifFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
                System.out.println("使用SansSerif字体");
                return sansSerifFont;
                
            } catch (Exception e) {
                System.err.println("创建备用字体失败: " + e.getMessage());
                // 返回默认字体
                return new Font(Font.DIALOG, Font.PLAIN, 12);
            }
        }
        
        /**
         * 重置字体缓存
         */
        public static void resetFontCache() {
            chinesePrimaryFont = null;
            chineseCodeFont = null;
        }
        
        /**
         * 初始化字体系统
         */
        public static void initializeFontSystem() {
            try {
                // 设置系统属性以改善字体渲染
                System.setProperty("awt.useSystemAAFontSettings", "on");
                System.setProperty("swing.aatext", "true");
                // 设置默认外观
                // UIManager相关方法在某些JDK版本中可能不可用，暂时注释
                /*
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
                } catch (Exception e) {
                    System.err.println("设置外观失败: " + e.getMessage());
                }
                */
                
                // 预加载字体
                getChineseFont(Font.PLAIN, 12);
                getChineseCodeFont(Font.PLAIN, 14);
                
                System.out.println("字体系统初始化完成");
            } catch (Exception e) {
                System.err.println("字体系统初始化失败: " + e.getMessage());
            }
        }
    }
    
    // 主题接口
    interface Theme {
        Color getPrimaryColor();
        Color getSecondaryColor();
        Color getAccentColor();
        Color getSuccessColor();
        Color getWarningColor();
        Color getDangerColor();
        Color getBackgroundColor();
        Color getCardBackgroundColor();
        Color getTextPrimaryColor();
        Color getTextSecondaryColor();
        Color getBorderColor();
        String getName();
        Font getPrimaryFont();
        Font getCodeFont();
    }
    
    // 现代浅色主题
    static class LightTheme implements Theme {
        public Color getPrimaryColor() { return new Color(33, 37, 41); }
        public Color getSecondaryColor() { return new Color(108, 117, 125); }
        public Color getAccentColor() { return new Color(99, 102, 241); }  // 现代紫蓝色
        public Color getSuccessColor() { return new Color(34, 197, 94); }  // 鲜艳绿色
        public Color getWarningColor() { return new Color(251, 191, 36); } // 温暖橙色
        public Color getDangerColor() { return new Color(239, 68, 68); }   // 现代红色
        public Color getBackgroundColor() { return new Color(250, 250, 250); } // 纯净白背景
        public Color getCardBackgroundColor() { return new Color(255, 255, 255); } // 卡片白色
        public Color getTextPrimaryColor() { return new Color(17, 24, 39); }   // 深色文字
        public Color getTextSecondaryColor() { return new Color(107, 114, 128); } // 中性灰文字
        public Color getBorderColor() { return new Color(229, 231, 235); }      // 淡边框
        public String getName() { return "\u73b0\u4ee3\u6d45\u8272"; }
        public Font getPrimaryFont() { return FontManager.getChineseFont(Font.PLAIN, 12); }
        public Font getCodeFont() { return FontManager.getChineseCodeFont(Font.PLAIN, 14); }
    }
    
    // 科技深色主题
    static class DarkTheme implements Theme {
        public Color getPrimaryColor() { return new Color(248, 249, 250); }
        public Color getSecondaryColor() { return new Color(156, 163, 175); }
        public Color getAccentColor() { return new Color(99, 102, 241); }  // 科技紫
        public Color getSuccessColor() { return new Color(34, 197, 94); }  // 霓虹绿
        public Color getWarningColor() { return new Color(251, 191, 36); } // 电子橙
        public Color getDangerColor() { return new Color(239, 68, 68); }   // 警告红
        public Color getBackgroundColor() { return new Color(17, 24, 39); } // 深空背景
        public Color getCardBackgroundColor() { return new Color(31, 41, 55); } // 卡片背景
        public Color getTextPrimaryColor() { return new Color(243, 244, 246); } // 亮文字
        public Color getTextSecondaryColor() { return new Color(156, 163, 175); } // 灰文字
        public Color getBorderColor() { return new Color(55, 65, 81); }          // 边框色
        public String getName() { return "\u79d1\u6280\u6df1\u8272"; }
        public Font getPrimaryFont() { return FontManager.getChineseFont(Font.PLAIN, 12); }
        public Font getCodeFont() { return FontManager.getChineseCodeFont(Font.PLAIN, 14); }
    }
    
    // 极光蓝主题
    static class BlueTheme implements Theme {
        public Color getPrimaryColor() { return new Color(255, 255, 255); }
        public Color getSecondaryColor() { return new Color(100, 116, 139); }
        public Color getAccentColor() { return new Color(59, 130, 246); }   // 极光蓝
        public Color getSuccessColor() { return new Color(16, 185, 129); }  // 青绿色
        public Color getWarningColor() { return new Color(245, 158, 11); }  // 琥珀色
        public Color getDangerColor() { return new Color(239, 68, 68); }    // 珊瑚红
        public Color getBackgroundColor() { return new Color(240, 249, 255); } // 冰蓝背景
        public Color getCardBackgroundColor() { return new Color(255, 255, 255); } // 纯白卡片
        public Color getTextPrimaryColor() { return new Color(15, 23, 42); }    // 深蓝文字
        public Color getTextSecondaryColor() { return new Color(100, 116, 139); } // 石板灰
        public Color getBorderColor() { return new Color(186, 230, 253); }       // 天蓝边框
        public String getName() { return "\u6781\u5149\u84dd"; }
        public Font getPrimaryFont() { return FontManager.getChineseFont(Font.PLAIN, 12); }
        public Font getCodeFont() { return FontManager.getChineseCodeFont(Font.PLAIN, 14); }
    }
    
    // 界面组件
    private JTextPane sqlInputArea;
    private ResultTabbedPane resultTabbedPane;
    private JTextArea tokenArea;
    private JTextArea astArea;
    private JButton executeButton;
    private JButton clearButton;
    private JButton importButton;
    private JButton shardManagerButton;
    private JLabel statusLabel;
    


    // 菜单栏组件
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenuItem importSQLItem;
    private JMenuItem exportDBItem;
    private JMenuItem exportTableItem;
    private JMenuItem importDirItem;
    // 数据库对象管理组件
    private JTree databaseTree;
    private JScrollPane treeScrollPane;
    private JButton refreshButton;
    
    // 数据库选择组件
    private JComboBox<String> databaseComboBox;
    private JButton createDatabaseButton;
    private JButton dropDatabaseButton;

    // 右键上下文菜单
    private JPopupMenu treeContextMenu;
    private JMenuItem exportTableContextItem;
    private JMenuItem showTableDataItem;
    private JMenuItem deleteTableItem;


    // 索引选择组件
    private JComboBox<String> indexTypeComboBox;
    
    // 存储格式选择组件
    private JComboBox<String> storageFormatComboBox;
    
    // 谓词下推优化开关
    private JCheckBox predicatePushdownCheckBox;
    
    // 子查询优化开关 - 已移除，现在自动启用
    
    // 性能指标显示
    private JLabel performanceLabel;
    
    // 子查询改写信息显示
    private JLabel subqueryRewriteLabel;
    
    // 自动补全组件
    private SQLAutoComplete autoComplete;
    
    // 语法高亮组件
    private SQLSyntaxHighlighter syntaxHighlighter;
    
    // AST可视化组件
    private BeautifulASTVisualizer astVisualizer;
    
    // AST可视化控制按钮
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JButton fitButton;
    
    public DatabaseGUI() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        initializeDatabase();
    }
    
    /**
     * 设置现代化外观
     */
    private void setupModernLookAndFeel() {
        try {
            // 设置系统外观
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            
            // 自定义UI属性
            UIManager.put("Button.font", currentTheme.getPrimaryFont());
            UIManager.put("Label.font", currentTheme.getPrimaryFont());
            UIManager.put("TextField.font", currentTheme.getPrimaryFont());
            UIManager.put("ComboBox.font", currentTheme.getPrimaryFont());
            UIManager.put("Table.font", new Font(currentTheme.getPrimaryFont().getName(), Font.PLAIN, 11));
            UIManager.put("Tree.font", currentTheme.getPrimaryFont());
            
            // 设置背景色
            UIManager.put("Panel.background", currentTheme.getBackgroundColor());
            UIManager.put("Button.background", currentTheme.getCardBackgroundColor());
            UIManager.put("TextField.background", currentTheme.getCardBackgroundColor());
            UIManager.put("ComboBox.background", currentTheme.getCardBackgroundColor());
            
        } catch (Exception e) {
            System.err.println("无法设置外观: " + e.getMessage());
        }
    }
    
    /**
     * 创建现代科技风格按钮
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
        
        button.setFont(FontManager.getChineseFont(isBold ? Font.BOLD : Font.PLAIN, fontSize));
        button.setForeground(textColor);
        button.setBackground(bgColor);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 35));
        
        // 添加悬停效果
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
     * 样式化下拉框
     */
    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(FontManager.getChineseFont(Font.PLAIN, 12));
        comboBox.setBackground(currentTheme.getCardBackgroundColor());
        comboBox.setForeground(currentTheme.getTextPrimaryColor());
        comboBox.setBorder(new CompoundBorder(
            new LineBorder(currentTheme.getBorderColor(), 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        comboBox.setPreferredSize(new Dimension(130, 35));
    }
    
    /**
     * 样式化复选框
     */
    private void styleCheckBox(JCheckBox checkBox) {
        checkBox.setFont(FontManager.getChineseFont(Font.PLAIN, 12));
        checkBox.setBackground(currentTheme.getBackgroundColor());
        checkBox.setForeground(currentTheme.getTextPrimaryColor());
        checkBox.setBorder(new CompoundBorder(
            new LineBorder(currentTheme.getBorderColor(), 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        checkBox.setFocusPainted(false);
    }
    
    /**
     * 样式化菜单项
     */
    private void styleMenuItem(JMenuItem menuItem) {
        menuItem.setFont(FontManager.getChineseFont(Font.PLAIN, 12));
        menuItem.setBackground(currentTheme.getCardBackgroundColor());
        menuItem.setForeground(currentTheme.getTextPrimaryColor());
        menuItem.setBorder(new EmptyBorder(8, 15, 8, 15));
    }
    
    /**
     * 样式化树形控件
     */
    private void styleTree(JTree tree) {
        tree.setFont(FontManager.getChineseFont(Font.PLAIN, 12));
        tree.setBackground(currentTheme.getCardBackgroundColor());
        tree.setForeground(currentTheme.getTextPrimaryColor());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(28);
        tree.setBorder(new EmptyBorder(8, 8, 8, 8));
    }
    
    /**
     * 样式化标签
     */
    private void styleLabel(JLabel label) {
        label.setFont(FontManager.getChineseFont(Font.PLAIN, 12));
        label.setForeground(currentTheme.getTextPrimaryColor());
    }
    
    /**
     * 创建现代科技风格标题边框
     */
    private TitledBorder createStyledTitledBorder(String title) {
        // 创建自定义边框类
        Border customBorder = new Border() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 绘制渐变边框
                int borderWidth = 2;
                Color accentColor = currentTheme.getAccentColor();
                
                // 左侧渐变线
                GradientPaint leftGradient = new GradientPaint(x, y, accentColor, x, y + height, 
                    new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 50));
                g2d.setPaint(leftGradient);
                g2d.fillRect(x, y, borderWidth, height);
                
                // 顶部渐变线
                GradientPaint topGradient = new GradientPaint(x, y, accentColor, x + width, y, 
                    new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 30));
                g2d.setPaint(topGradient);
                g2d.fillRect(x, y, width, borderWidth);
                
                // 右侧和底部淡化边框
                g2d.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 80));
                g2d.fillRect(x + width - borderWidth, y, borderWidth, height);
                g2d.fillRect(x, y + height - borderWidth, width, borderWidth);
                
                g2d.dispose();
            }
            
            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(15, 15, 15, 15);
            }
            
            @Override
            public boolean isBorderOpaque() {
                return false;
            }
        };
        
        TitledBorder border = BorderFactory.createTitledBorder(customBorder, title);
        border.setTitleFont(FontManager.getChineseFont(Font.BOLD, 13));
        border.setTitleColor(currentTheme.getAccentColor());
        border.setTitleJustification(TitledBorder.LEFT);
        border.setTitlePosition(TitledBorder.TOP);
        return border;
    }
    
    /**
     * 创建科技风格窗口图标
     */
    private Image createWindowIcon() {
        int size = 32;
        java.awt.image.BufferedImage icon = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // 创建渐变背景
        Color accentColor = currentTheme.getAccentColor();
        GradientPaint gradient = new GradientPaint(0, 0, brightenColor(accentColor, 0.2f), 
                                                   size, size, darkenColor(accentColor, 0.1f));
        g2d.setPaint(gradient);
        g2d.fillRoundRect(2, 2, size-4, size-4, 10, 10);
        
        // 添加高光效果
        g2d.setColor(new Color(255, 255, 255, 60));
        g2d.fillRoundRect(3, 3, size-6, (size-4)/2, 8, 8);
        
        // 绘制数据库层状结构
        g2d.setColor(currentTheme.getCardBackgroundColor());
        g2d.setStroke(new BasicStroke(1.5f));
        
        // 数据库圆柱体效果
        int centerX = size / 2;
        int baseY = size - 8;
        int layerHeight = 3;
        
        // 绘制三层数据
        for (int i = 0; i < 3; i++) {
            int y = baseY - i * layerHeight - 2;
            g2d.drawOval(centerX - 8, y - 1, 16, 4);
            if (i < 2) {
                g2d.drawLine(centerX - 8, y + 1, centerX - 8, y + layerHeight + 1);
                g2d.drawLine(centerX + 8, y + 1, centerX + 8, y + layerHeight + 1);
            }
        }
        
        // 添加科技光点
        g2d.setColor(brightenColor(accentColor, 0.5f));
        g2d.fillOval(size - 8, 4, 3, 3);
        g2d.fillOval(6, size - 8, 2, 2);
        
        g2d.dispose();
        return icon;
    }
    
    /**
     * 切换主题
     */
    private void switchTheme(Theme newTheme) {
        currentTheme = newTheme;
        
        // 更新所有组件的样式
        updateComponentThemes();
        
        // 刷新界面
        SwingUtilities.updateComponentTreeUI(this);
        repaint();
        
        statusLabel.setText("已切换到" + currentTheme.getName());
        statusLabel.setForeground(currentTheme.getSuccessColor());
    }
    
    /**
     * 更新组件主题
     */
    private void updateComponentThemes() {
        // 更新背景
        getContentPane().setBackground(currentTheme.getBackgroundColor());
        
        // 更新SQL输入区域
        sqlInputArea.setFont(currentTheme.getCodeFont());
        sqlInputArea.setBackground(currentTheme.getCardBackgroundColor());
        sqlInputArea.setForeground(currentTheme.getTextPrimaryColor());
        
        // 更新Token和AST显示区域
        tokenArea.setFont(currentTheme.getCodeFont());
        tokenArea.setBackground(currentTheme.getBackgroundColor());
        tokenArea.setForeground(currentTheme.getTextPrimaryColor());
        
        astArea.setFont(currentTheme.getCodeFont());
        astArea.setBackground(currentTheme.getBackgroundColor());
        astArea.setForeground(currentTheme.getTextPrimaryColor());
        
        // 更新按钮 - 统一使用指定颜色
        Color buttonColor = new Color(127, 198, 255);
        updateButtonTheme(executeButton, buttonColor);
        updateButtonTheme(clearButton, buttonColor);
        updateButtonTheme(importButton, buttonColor);
        updateButtonTheme(shardManagerButton, buttonColor);
        updateButtonTheme(refreshButton, buttonColor);
        updateButtonTheme(createDatabaseButton, buttonColor);
        updateButtonTheme(dropDatabaseButton, buttonColor);
        
        // 更新缩放按钮
        if (zoomInButton != null) updateButtonTheme(zoomInButton, buttonColor);
        if (zoomOutButton != null) updateButtonTheme(zoomOutButton, buttonColor);
        if (fitButton != null) updateButtonTheme(fitButton, buttonColor);
        
        // 更新状态标签
        statusLabel.setFont(FontManager.getChineseFont(Font.PLAIN, 12));
        statusLabel.setBackground(currentTheme.getBackgroundColor());
        performanceLabel.setFont(FontManager.getChineseFont(Font.PLAIN, 11));
        performanceLabel.setBackground(currentTheme.getBackgroundColor());
        subqueryRewriteLabel.setFont(FontManager.getChineseFont(Font.PLAIN, 11));
        subqueryRewriteLabel.setBackground(currentTheme.getBackgroundColor());
        
        // 更新菜单栏
        menuBar.setBackground(currentTheme.getCardBackgroundColor());
        fileMenu.setFont(FontManager.getChineseFont(Font.BOLD, 14));
        fileMenu.setForeground(currentTheme.getTextPrimaryColor());
        themeMenu.setFont(FontManager.getChineseFont(Font.BOLD, 14));
        themeMenu.setForeground(currentTheme.getTextPrimaryColor());
        
        // 更新树形控件
        databaseTree.setBackground(currentTheme.getCardBackgroundColor());
        databaseTree.setForeground(currentTheme.getTextPrimaryColor());
        
        // 更新面板
        updatePanelThemes();
    }
    
    /**
     * 更新按钮主题
     */
    private void updateButtonTheme(JButton button, Color bgColor) {
        if (button != null) {
            button.setBackground(bgColor);
            button.setForeground(currentTheme.getCardBackgroundColor());
            button.repaint();
        }
    }
    
    /**
     * 更新面板主题
     */
    private void updatePanelThemes() {
        // 递归更新所有面板的背景色
        updatePanelBackground(this);
    }
    
    /**
     * 递归更新面板背景
     */
    private void updatePanelBackground(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                if (panel.getBorder() instanceof TitledBorder || 
                    panel.getComponentCount() > 0) {
                    panel.setBackground(currentTheme.getCardBackgroundColor());
                } else {
                    panel.setBackground(currentTheme.getBackgroundColor());
                }
                updatePanelBackground(panel);
            } else if (component instanceof Container) {
                updatePanelBackground((Container) component);
            }
        }
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // 初始化字体系统
        FontManager.initializeFontSystem();
        
        // 设置现代化外观和编码
        setupModernLookAndFeel();
        
        // 设置系统编码为UTF-8
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("user.language", "zh");
        System.setProperty("user.country", "CN");
        
        // SQL输入区域 - 使用JTextPane支持语法高亮
        sqlInputArea = new JTextPane();
        sqlInputArea.setFont(currentTheme.getCodeFont());
        sqlInputArea.setBackground(currentTheme.getCardBackgroundColor());
        sqlInputArea.setForeground(currentTheme.getTextPrimaryColor());
        sqlInputArea.setBorder(new CompoundBorder(
            new LineBorder(currentTheme.getBorderColor(), 1, true),
            new EmptyBorder(10, 10, 10, 10)
        ));
        // 设置固定大小，防止根据内容自动调整
        sqlInputArea.setSize(new Dimension(600, 200));
        sqlInputArea.setMinimumSize(new Dimension(600, 200));
        sqlInputArea.setMaximumSize(new Dimension(600, 200));
        sqlInputArea.setPreferredSize(new Dimension(600, 200));
        
        // 初始化语法高亮组件
        syntaxHighlighter = new SQLSyntaxHighlighter(sqlInputArea);
        
        // 初始化AST可视化组件
        astVisualizer = new BeautifulASTVisualizer();
        
        // 设置键盘快捷键
        setupKeyboardShortcuts();
        
        // 结果显示区域 - 使用标签栏组件
        resultTabbedPane = new ResultTabbedPane();
        
        // Token显示区域
        tokenArea = new JTextArea(15, 30);
        tokenArea.setFont(currentTheme.getCodeFont());
        tokenArea.setEditable(false);
        tokenArea.setBackground(currentTheme.getBackgroundColor());
        tokenArea.setForeground(currentTheme.getTextPrimaryColor());
        tokenArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // AST显示区域
        astArea = new JTextArea(15, 30);
        astArea.setFont(currentTheme.getCodeFont());
        astArea.setEditable(false);
        astArea.setBackground(currentTheme.getBackgroundColor());
        astArea.setForeground(currentTheme.getTextPrimaryColor());
        astArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // 按钮 - 使用统一颜色
        Color buttonColor = new Color(127, 198, 255);
        executeButton = createStyledButton("执行SQL", buttonColor, currentTheme.getCardBackgroundColor(), 14, true);
        executeButton.setToolTipText("执行SQL语句（有选中文本时执行选中部分，否则执行全部）");
        
        clearButton = createStyledButton("清空", buttonColor, currentTheme.getCardBackgroundColor(), 12, true);
        
        // 导入SQL文件按钮
        importButton = createStyledButton("导入SQL文件", buttonColor, currentTheme.getCardBackgroundColor(), 11, true);
        importButton.setPreferredSize(new Dimension(120, 35));
        importButton.setToolTipText("从文件导入并执行SQL语句");

        // 分片管理按钮
        shardManagerButton = createStyledButton("分片管理", buttonColor, currentTheme.getCardBackgroundColor(), 11, true);
        shardManagerButton.setPreferredSize(new Dimension(100, 35));
        shardManagerButton.setToolTipText("管理数据库分片");

        // 索引选择组件
        String[] indexTypes = {"智能选择", "B+树索引", "哈希索引", "线性查找"};
        indexTypeComboBox = new JComboBox<>(indexTypes);
        styleComboBox(indexTypeComboBox);
        // 单独调整索引选择框的尺寸以确保文字完全显示
        indexTypeComboBox.setPreferredSize(new Dimension(140, 38));
        indexTypeComboBox.setSelectedIndex(0); // 默认选择智能选择
        
        // 存储格式选择组件
        String[] storageFormats = {"行式存储", "列式存储"};
        storageFormatComboBox = new JComboBox<>(storageFormats);
        styleComboBox(storageFormatComboBox);
        // 单独调整存储格式选择框的尺寸以确保文字完全显示
        storageFormatComboBox.setPreferredSize(new Dimension(120, 38));
        storageFormatComboBox.setSelectedIndex(0); // 默认选择行式存储
        
        // 谓词下推优化开关
        predicatePushdownCheckBox = new JCheckBox("谓词下推优化");
        styleCheckBox(predicatePushdownCheckBox);
        predicatePushdownCheckBox.setSelected(true); // 默认启用
        predicatePushdownCheckBox.setToolTipText("启用谓词下推优化，将WHERE条件下推到数据源以减少数据传输");
        
        // 子查询优化开关 - 已移除，现在自动启用
        
        // 状态标签
        statusLabel = new JLabel("就绪");
        statusLabel.setFont(currentTheme.getPrimaryFont());
        statusLabel.setForeground(currentTheme.getAccentColor());
        statusLabel.setOpaque(true);
        statusLabel.setBackground(currentTheme.getBackgroundColor());
        statusLabel.setBorder(new CompoundBorder(
            new LineBorder(currentTheme.getBorderColor(), 1, true),
            new EmptyBorder(8, 15, 8, 15)
        ));
        
        // 性能指标标签
        performanceLabel = new JLabel("性能: 未测量");
        performanceLabel.setFont(new Font(currentTheme.getPrimaryFont().getName(), Font.PLAIN, 11));
        performanceLabel.setForeground(currentTheme.getTextSecondaryColor());
        performanceLabel.setOpaque(true);
        performanceLabel.setBackground(currentTheme.getBackgroundColor());
        performanceLabel.setBorder(new CompoundBorder(
            new LineBorder(currentTheme.getBorderColor(), 1, true),
            new EmptyBorder(5, 12, 5, 12)
        ));
        
        // 子查询改写信息标签
        subqueryRewriteLabel = new JLabel("子查询优化: 无");
        subqueryRewriteLabel.setFont(new Font(currentTheme.getPrimaryFont().getName(), Font.PLAIN, 11));
        subqueryRewriteLabel.setForeground(currentTheme.getTextSecondaryColor());
        subqueryRewriteLabel.setOpaque(true);
        subqueryRewriteLabel.setBackground(currentTheme.getBackgroundColor());
        subqueryRewriteLabel.setBorder(new CompoundBorder(
            new LineBorder(currentTheme.getBorderColor(), 1, true),
            new EmptyBorder(5, 12, 5, 12)
        ));
        

        // 初始化菜单栏
        initializeMenuBar();
    }
    
    /**
     * 初始化菜单栏
     */
    private void initializeMenuBar() {
        menuBar = new JMenuBar();
        // 设置主题化菜单栏样式
        menuBar.setBackground(currentTheme.getCardBackgroundColor());
        menuBar.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, currentTheme.getAccentColor()),
            new EmptyBorder(8, 15, 8, 15)
        ));
        
        // 文件菜单
        fileMenu = new JMenu("文件");
        fileMenu.setFont(FontManager.getChineseFont(Font.BOLD, 14));
        fileMenu.setForeground(currentTheme.getTextPrimaryColor());
        
        // 主题菜单
        themeMenu = new JMenu("主题");
        themeMenu.setFont(FontManager.getChineseFont(Font.BOLD, 14));
        themeMenu.setForeground(currentTheme.getTextPrimaryColor());
        
        // 添加主题选项
        JMenuItem lightThemeItem = new JMenuItem("现代浅色");
        lightThemeItem.setFont(FontManager.getChineseFont(Font.PLAIN, 12));
        lightThemeItem.addActionListener(e -> switchTheme(new LightTheme()));
        
        JMenuItem darkThemeItem = new JMenuItem("科技深色");
        darkThemeItem.setFont(FontManager.getChineseFont(Font.PLAIN, 12));
        darkThemeItem.addActionListener(e -> switchTheme(new DarkTheme()));
        
        JMenuItem blueThemeItem = new JMenuItem("极光蓝");
        blueThemeItem.setFont(FontManager.getChineseFont(Font.PLAIN, 12));
        blueThemeItem.addActionListener(e -> switchTheme(new BlueTheme()));
        
        themeMenu.add(lightThemeItem);
        themeMenu.add(darkThemeItem);
        themeMenu.add(blueThemeItem);
        
        // 导入SQL文件
        importSQLItem = new JMenuItem("导入SQL文件...");
        styleMenuItem(importSQLItem);
        importSQLItem.setToolTipText("从文件导入并执行SQL语句");
        
        // 导出数据库
        exportDBItem = new JMenuItem("导出数据库...");
        styleMenuItem(exportDBItem);
        exportDBItem.setToolTipText("将整个数据库导出为SQL文件");
        
        // 导出单个表
        exportTableItem = new JMenuItem("导出单个表...");
        styleMenuItem(exportTableItem);
        exportTableItem.setToolTipText("将指定表导出为SQL文件");
        
        // 批量导入目录
        importDirItem = new JMenuItem("批量导入目录...");
        styleMenuItem(importDirItem);
        importDirItem.setToolTipText("从目录批量导入SQL文件");
        
        // 添加菜单项到文件菜单
        fileMenu.add(importSQLItem);
        fileMenu.addSeparator();
        fileMenu.add(exportDBItem);
        fileMenu.add(exportTableItem);
        fileMenu.addSeparator();
        fileMenu.add(importDirItem);
        
        // 添加菜单到菜单栏
        menuBar.add(fileMenu);
        menuBar.add(themeMenu);

        // 数据库对象管理组件
        databaseTree = new JTree();
        styleTree(databaseTree);
        treeScrollPane = new JScrollPane(databaseTree);

        // 初始化右键上下文菜单
        initializeContextMenu();
        treeScrollPane.setPreferredSize(new Dimension(250, 400));
        treeScrollPane.setBorder(BorderFactory.createTitledBorder("数据库对象"));
        
        Color buttonColor = new Color(127, 198, 255);
        refreshButton = createStyledButton("刷新", buttonColor, currentTheme.getCardBackgroundColor(), 11, true);
        refreshButton.setPreferredSize(new Dimension(80, 32));
        
        // 数据库选择组件
        databaseComboBox = new JComboBox<>();
        styleComboBox(databaseComboBox);
        databaseComboBox.setToolTipText("选择当前数据库");
        
        createDatabaseButton = createStyledButton("创建数据库", buttonColor, currentTheme.getCardBackgroundColor(), 11, false);
        createDatabaseButton.setPreferredSize(new Dimension(110, 35));
        createDatabaseButton.setToolTipText("创建新数据库");
        
        dropDatabaseButton = createStyledButton("删除数据库", buttonColor, currentTheme.getCardBackgroundColor(), 11, false);
        dropDatabaseButton.setPreferredSize(new Dimension(110, 35));
        dropDatabaseButton.setToolTipText("删除当前数据库");

    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        updateWindowTitle();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // 设置整体背景
        getContentPane().setBackground(currentTheme.getBackgroundColor());
        


        // 设置菜单栏
        setJMenuBar(menuBar);
        // 确保菜单栏可见
        menuBar.setVisible(true);
        System.out.println("菜单栏已设置，包含 " + menuBar.getMenuCount() + " 个菜单");

        // 左侧：数据库对象树
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(300, 0));
        leftPanel.setBackground(currentTheme.getCardBackgroundColor());
        leftPanel.setBorder(new CompoundBorder(
            new LineBorder(currentTheme.getBorderColor(), 1, true),
            new EmptyBorder(20, 20, 20, 15)
        ));
        
        // 顶部：数据库选择面板
        JPanel databaseSelectionPanel = new JPanel(new GridBagLayout());
        databaseSelectionPanel.setBackground(currentTheme.getCardBackgroundColor());
        databaseSelectionPanel.setBorder(createStyledTitledBorder("数据库"));
        
        GridBagConstraints dbGbc = new GridBagConstraints();
        dbGbc.insets = new Insets(2, 2, 2, 2);
        
        // 数据库下拉框
        dbGbc.gridx = 0; dbGbc.gridy = 0; dbGbc.gridwidth = 2; dbGbc.fill = GridBagConstraints.HORIZONTAL; dbGbc.weightx = 1.0;
        databaseSelectionPanel.add(databaseComboBox, dbGbc);
        
        // 创建数据库按钮
        dbGbc.gridx = 0; dbGbc.gridy = 1; dbGbc.gridwidth = 1; dbGbc.fill = GridBagConstraints.HORIZONTAL; dbGbc.weightx = 0.5;
        databaseSelectionPanel.add(createDatabaseButton, dbGbc);
        
        // 删除数据库按钮
        dbGbc.gridx = 1; dbGbc.gridy = 1; dbGbc.gridwidth = 1; dbGbc.fill = GridBagConstraints.HORIZONTAL; dbGbc.weightx = 0.5;
        databaseSelectionPanel.add(dropDatabaseButton, dbGbc);
        
        leftPanel.add(databaseSelectionPanel, BorderLayout.NORTH);
        
        // 数据库对象树
        treeScrollPane.setBackground(currentTheme.getCardBackgroundColor());
        treeScrollPane.setBorder(createStyledTitledBorder("数据库对象"));
        leftPanel.add(treeScrollPane, BorderLayout.CENTER);
        
        // 左侧按钮面板
        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        leftButtonPanel.setBackground(currentTheme.getCardBackgroundColor());
        leftButtonPanel.add(refreshButton);
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);
        
        add(leftPanel, BorderLayout.WEST);
        
        // 右侧：主要内容区域
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(currentTheme.getBackgroundColor());
        
        // 顶部：SQL输入区域、自动补全建议和按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(currentTheme.getCardBackgroundColor());
        topPanel.setBorder(new CompoundBorder(
            new LineBorder(currentTheme.getBorderColor(), 2, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        // SQL输入区域 - 使用带行号的滚动面板
        LineNumberScrollPane sqlScrollPane = new LineNumberScrollPane(sqlInputArea);
        sqlScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sqlScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // 设置滚动面板的固定大小，防止自动调整
        sqlScrollPane.setSize(new Dimension(600, 200));
        sqlScrollPane.setMinimumSize(new Dimension(600, 200));
        sqlScrollPane.setMaximumSize(new Dimension(600, 200));
        sqlScrollPane.setPreferredSize(new Dimension(600, 200));
        topPanel.add(sqlScrollPane, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(currentTheme.getCardBackgroundColor());
        buttonPanel.add(executeButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(importButton);
        buttonPanel.add(shardManagerButton);

        // 添加索引选择组件
        JLabel indexLabel = new JLabel("索引方式:");
        styleLabel(indexLabel);
        buttonPanel.add(indexLabel);
        buttonPanel.add(indexTypeComboBox);
        
        // 添加存储格式选择组件
        JLabel storageLabel = new JLabel("存储格式:");
        styleLabel(storageLabel);
        buttonPanel.add(storageLabel);
        buttonPanel.add(storageFormatComboBox);
        
        // 添加谓词下推优化开关
        buttonPanel.add(predicatePushdownCheckBox);
        
        // 子查询优化开关 - 已移除，现在自动启用
        
        buttonPanel.add(statusLabel);
        buttonPanel.add(performanceLabel);
        buttonPanel.add(subqueryRewriteLabel);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        rightPanel.add(topPanel, BorderLayout.NORTH);
        
        // 底部：结果显示区域
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        bottomPanel.setBackground(currentTheme.getBackgroundColor());
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // 左侧：执行结果（占满整个左侧）
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBackground(currentTheme.getCardBackgroundColor());
        resultPanel.setBorder(createStyledTitledBorder("执行结果"));
        
        // 直接添加标签栏组件
        resultPanel.add(resultTabbedPane, BorderLayout.CENTER);
        bottomPanel.add(resultPanel);
        
        // 右侧：Token列表和AST可视化（上下分布，高度比例2:3）
        JPanel rightDetailPanel = new JPanel(new GridBagLayout());
        rightDetailPanel.setBackground(currentTheme.getBackgroundColor());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Token列表（上半部分，占2/5高度）
        JPanel tokenPanel = new JPanel(new BorderLayout());
        tokenPanel.setBackground(currentTheme.getCardBackgroundColor());
        tokenPanel.setBorder(createStyledTitledBorder("Token列表"));
        JScrollPane tokenScrollPane = new JScrollPane(tokenArea);
        tokenScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tokenScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tokenScrollPane.setBorder(new LineBorder(currentTheme.getBorderColor(), 1, true));
        tokenScrollPane.setBackground(currentTheme.getCardBackgroundColor());
        tokenPanel.add(tokenScrollPane, BorderLayout.CENTER);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.4; // 2/5 = 0.4
        gbc.fill = GridBagConstraints.BOTH;
        rightDetailPanel.add(tokenPanel, gbc);
        
        // AST可视化（下半部分，占3/5高度）
        JPanel astPanel = new JPanel(new BorderLayout());
        astPanel.setBackground(currentTheme.getCardBackgroundColor());
        astPanel.setBorder(createStyledTitledBorder("AST可视化"));
        
        // 添加AST可视化组件
        JScrollPane astVisualizerScrollPane = new JScrollPane(astVisualizer);
        astVisualizerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        astVisualizerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        astVisualizerScrollPane.setBorder(new LineBorder(currentTheme.getBorderColor(), 1, true));
        astVisualizerScrollPane.setBackground(currentTheme.getCardBackgroundColor());
        astPanel.add(astVisualizerScrollPane, BorderLayout.CENTER);
        
        // 添加放大缩小按钮
        JPanel astButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        astButtonPanel.setBackground(currentTheme.getCardBackgroundColor());
        
        Color zoomButtonColor = new Color(127, 198, 255);
        zoomInButton = createStyledButton("+", zoomButtonColor, currentTheme.getCardBackgroundColor(), 12, true);
        zoomInButton.setPreferredSize(new Dimension(40, 35));
        zoomInButton.setToolTipText("放大");
        
        zoomOutButton = createStyledButton("-", zoomButtonColor, currentTheme.getCardBackgroundColor(), 12, true);
        zoomOutButton.setPreferredSize(new Dimension(40, 35));
        zoomOutButton.setToolTipText("缩小");
        
        fitButton = createStyledButton("适应", zoomButtonColor, currentTheme.getCardBackgroundColor(), 10, true);
        fitButton.setPreferredSize(new Dimension(70, 35));
        fitButton.setToolTipText("适应窗口大小");
        
        astButtonPanel.add(zoomInButton);
        astButtonPanel.add(zoomOutButton);
        astButtonPanel.add(fitButton);
        astPanel.add(astButtonPanel, BorderLayout.SOUTH);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.6; // 3/5 = 0.6
        gbc.fill = GridBagConstraints.BOTH;
        rightDetailPanel.add(astPanel, gbc);
        bottomPanel.add(rightDetailPanel);
        
        rightPanel.add(bottomPanel, BorderLayout.CENTER);
        
        add(rightPanel, BorderLayout.CENTER);
        
        // 设置窗口大小和位置
        setSize(1600, 900);
        setLocationRelativeTo(null);
        
        // 添加窗口图标（可选）
        setIconImage(createWindowIcon());
    }
    
    /**
     * 设置键盘快捷键
     */
    private void setupKeyboardShortcuts() {
        // F5 - 执行选中
        sqlInputArea.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke("F5"), "executeSelected");
        sqlInputArea.getActionMap().put("executeSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeSelectedSQL();
            }
        });
        
        // F9 - 执行全部
        sqlInputArea.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke("F9"), "executeAll");
        sqlInputArea.getActionMap().put("executeAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeSQL();
            }
        });
        
        // Ctrl+Enter - 执行全部
        sqlInputArea.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke("ctrl ENTER"), "executeAllCtrl");
        sqlInputArea.getActionMap().put("executeAllCtrl", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeSQL();
            }
        });
    }
    
    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 智能执行：有选中文本时执行选中部分，否则执行全部
                String selectedText = sqlInputArea.getSelectedText();
                if (selectedText != null && !selectedText.trim().isEmpty()) {
                    executeSelectedSQL();
                } else {
                    executeSQL();
                }
            }
        });
        
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearAll();
            }
        });
        
        
        // 导入SQL文件按钮事件
        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importSQLFile();
            }
        });
        
        // 分片管理按钮事件
        shardManagerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showShardManager();
            }
        });
        
        // 谓词下推优化开关事件
        predicatePushdownCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean enabled = predicatePushdownCheckBox.isSelected();
                databaseEngine.setPredicatePushdownEnabled(enabled);
                statusLabel.setText("谓词下推优化已" + (enabled ? "启用" : "禁用"));
                statusLabel.setForeground(enabled ? Color.GREEN : Color.ORANGE);
            }
        });
        
        // 子查询优化开关事件 - 已移除，现在自动启用

        // AST可视化按钮事件处理器
        zoomInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                astVisualizer.zoomIn();
            }
        });
        
        zoomOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                astVisualizer.zoomOut();
            }
        });
        
        fitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                astVisualizer.fitToWindow();
            }
        });
        
        // 数据库对象管理按钮事件处理器
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshDatabaseTree();
            }
        });
        
        // 数据库选择事件处理器
        databaseComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switchDatabase();
            }
        });
        
        // 创建数据库按钮事件处理器
        createDatabaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createDatabase();
            }
        });
        
        // 删除数据库按钮事件处理器
        dropDatabaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dropDatabase();
            }
        });
        
        // 数据库树点击事件处理器
        databaseTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            @Override
            public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
                handleTreeSelection(e);
            }
        });
        
        // 添加键盘快捷键
        sqlInputArea.getInputMap().put(KeyStroke.getKeyStroke("ctrl ENTER"), "execute");
        sqlInputArea.getActionMap().put("execute", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeSQL();
            }
        });
        
        // 添加自动补全快捷键
        sqlInputArea.getInputMap().put(KeyStroke.getKeyStroke("ctrl SPACE"), "autocomplete");
        sqlInputArea.getActionMap().put("autocomplete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (autoComplete != null) {
                    autoComplete.showAutoComplete();
                }
            }
        });
        
        // 菜单事件处理器
        setupMenuEventHandlers();
    }
    
    /**
     * 初始化右键上下文菜单
     */
    private void initializeContextMenu() {
        treeContextMenu = new JPopupMenu();

        // 导出表选项
        exportTableContextItem = new JMenuItem("导出表");
        exportTableContextItem.setIcon(new ImageIcon()); // 可以添加图标
        exportTableContextItem.addActionListener(e -> exportSelectedTable());

        // 显示表数据选项
        showTableDataItem = new JMenuItem("显示表数据");
        showTableDataItem.addActionListener(e -> showSelectedTableData());

        // 删除表选项
        deleteTableItem = new JMenuItem("删除表");
        deleteTableItem.setForeground(Color.RED);
        deleteTableItem.addActionListener(e -> deleteSelectedTable());

        // 添加菜单项到上下文菜单
        treeContextMenu.add(showTableDataItem);
        treeContextMenu.addSeparator();
        treeContextMenu.add(exportTableContextItem);
        treeContextMenu.addSeparator();
        treeContextMenu.add(deleteTableItem);

        // 为数据库树添加鼠标右键监听器
        databaseTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
        });
    }

    /**
     * 显示右键上下文菜单
     */
    private void showContextMenu(java.awt.event.MouseEvent e) {
        // 获取点击位置的节点
        javax.swing.tree.TreePath path = databaseTree.getPathForLocation(e.getX(), e.getY());
        if (path == null) return;

        // 选中该节点
        databaseTree.setSelectionPath(path);

        // 获取选中的节点
        javax.swing.tree.DefaultMutableTreeNode selectedNode =
            (javax.swing.tree.DefaultMutableTreeNode) path.getLastPathComponent();

        if (selectedNode == null) return;

        // 获取父节点
        javax.swing.tree.DefaultMutableTreeNode parentNode =
            (javax.swing.tree.DefaultMutableTreeNode) selectedNode.getParent();

        if (parentNode != null && parentNode.toString().equals("表")) {
            // 只有在表节点上才显示上下文菜单
            treeContextMenu.show(databaseTree, e.getX(), e.getY());
        }
    }

    /**
     * 设置菜单事件处理器
     */
    private void setupMenuEventHandlers() {
        // 导入SQL文件
        importSQLItem.addActionListener(e -> importSQLFile());
        
        // 导出数据库
        exportDBItem.addActionListener(e -> exportDatabase());
        
        // 导出单个表
        exportTableItem.addActionListener(e -> exportTable());
        
        // 批量导入目录
        importDirItem.addActionListener(e -> importDirectory());
    }
    
    /**
     * 初始化数据库
     */
    private void initializeDatabase() {
        try {
            // 初始化多数据库管理器
            String baseDataDirectory = DatabaseConfig.getAutoDetectedDataDirectory();
            databaseManager = new MultiDatabaseManager(baseDataDirectory);
            
            // 获取默认数据库引擎（main数据库）
            databaseEngine = databaseManager.getCurrentDatabaseEngine();
            
            if (databaseEngine != null) {
                // 使用数据库引擎的目录管理器创建SQL编译器，确保目录同步
                compiler = new EnhancedSQLCompiler(databaseEngine.getCatalogManager().getCatalog());
                
                // 初始化自动补全组件
                autoComplete = new SQLAutoComplete(sqlInputArea, compiler.getCatalog());
                
                statusLabel.setText("数据库已连接 - " + databaseManager.getCurrentDatabase());
                statusLabel.setForeground(Color.GREEN);
                resultTabbedPane.showMessage("多数据库系统初始化成功！当前数据库: " + databaseManager.getCurrentDatabase());
                
                // 刷新数据库列表
                refreshDatabaseList();
                
                // 初始化完成后自动刷新数据库树
                refreshDatabaseTree();
            } else {
                statusLabel.setText("数据库连接失败");
                statusLabel.setForeground(Color.RED);
                resultTabbedPane.showError("数据库引擎初始化失败！");
            }
        } catch (Exception e) {
            statusLabel.setText("初始化错误");
            statusLabel.setForeground(Color.RED);
            resultTabbedPane.showError("初始化错误: " + e.getMessage());
        }
    }
    
    /**
     * 执行选中的SQL语句
     */
    private void executeSelectedSQL() {
        String selectedText = sqlInputArea.getSelectedText();
        if (selectedText == null || selectedText.trim().isEmpty()) {
            statusLabel.setText("请先选中要执行的SQL语句");
            statusLabel.setForeground(Color.ORANGE);
            return;
        }
        
        // 临时保存原始文本
        String originalText = sqlInputArea.getText();
        int selectionStart = sqlInputArea.getSelectionStart();
        int selectionEnd = sqlInputArea.getSelectionEnd();
        
        // 设置选中的文本为当前文本
        sqlInputArea.setText(selectedText.trim());
        
        // 执行SQL
        executeSQL();
        
        // 恢复原始文本
        sqlInputArea.setText(originalText);
        
        // 恢复选中状态
        sqlInputArea.setSelectionStart(selectionStart);
        sqlInputArea.setSelectionEnd(selectionEnd);
        
        // 更新状态
        statusLabel.setText("已执行选中语句");
        statusLabel.setForeground(Color.GREEN);
    }
    
    /**
     * 执行SQL语句
     */
    private void executeSQL() {
        String sql = sqlInputArea.getText().trim();
        if (sql.isEmpty()) {
            statusLabel.setText("请输入SQL语句");
            statusLabel.setForeground(Color.ORANGE);
            return;
        }
        
        // 处理特殊命令
        if ("exit".equalsIgnoreCase(sql)) {
            System.exit(0);
        }
        
        if ("catalog".equalsIgnoreCase(sql)) {
            showCatalog();
            return;
        }
        
        if ("clear".equalsIgnoreCase(sql)) {
            compiler.clearCatalog();
            resultTabbedPane.showMessage("目录已清空");
            return;
        }
        
        // 处理数据库管理命令
        if (handleDatabaseCommands(sql)) {
            return;
        }
        
        statusLabel.setText("正在执行...");
        statusLabel.setForeground(Color.BLUE);
        
        // 清空显示区域
        tokenArea.setText("");
        astArea.setText("");
        astVisualizer.setAST(null);
        
        try {
            // 检查是否是批量SQL语句
            boolean isMultiStatement = sql.contains(";") && sql.split(";").length > 1;
            
            // 使用增强版SQL编译器进行编译
            EnhancedSQLCompiler.CompilationResult result;
            if (isMultiStatement) {
                result = compiler.compileBatch(sql);
            } else {
                result = compiler.compile(sql);
            }
            
            // 显示Token信息
            displayTokens(result);
            
            // 显示AST信息（文本形式）
            displayAST(result);
            
            // 显示AST可视化
            displayASTVisualization(result);
            
            // 显示执行结果
            if (result.isSuccess()) {
                // 尝试执行SQL（如果数据库引擎支持）
                try {
                    // 获取选择的索引类型
                    String selectedIndexType = (String) indexTypeComboBox.getSelectedItem();
                    
                    // 获取选择的存储格式
                    String selectedStorageFormat = (String) storageFormatComboBox.getSelectedItem();
                    
                    // 获取表实际的存储格式
                    String actualStorageFormat = getActualStorageFormat(sql);
                    if (actualStorageFormat != null) {
                        resultTabbedPane.showMessage("表存储格式: " + actualStorageFormat);
                    }
                    
                    // 设置数据库引擎的索引类型
                    databaseEngine.setIndexType(selectedIndexType);
                    
                    // 设置数据库引擎的存储格式
                    databaseEngine.setStorageFormat(selectedStorageFormat);
                    
                    // 测量执行时间
                    long startTime = System.nanoTime();
                    ExecutionResult execResult = databaseEngine.executeSQL(sql);
                    long endTime = System.nanoTime();
                    
                    // 计算执行时间（毫秒）
                    double executionTimeMs = (endTime - startTime) / 1_000_000.0;
                    
        // 更新性能指标显示
        updatePerformanceMetrics(executionTimeMs, execResult, predicatePushdownCheckBox.isSelected());
        
        // 更新子查询改写信息显示
        updateSubqueryRewriteInfo(execResult);
                    
                    
                    // 判断是否为查询类指令
                    boolean isQuery = isQueryStatement(sql);
                    
                    if (execResult.isSuccess()) {
                        // 检查是否是批量执行结果
                        if (execResult.getBatchResults() != null && !execResult.getBatchResults().isEmpty()) {
                            // 批量执行结果
                            resultTabbedPane.showQueryMessage(sql, true, true, executionTimeMs, selectedIndexType, execResult);
                            resultTabbedPane.showQueryResult(execResult); // 这会调用showBatchResults
                        } else if (isQuery && execResult.getData() != null && !execResult.getData().isEmpty()) {
                            // 单个查询类指令且有数据，显示查询消息和结果
                            resultTabbedPane.showQueryMessage(sql, true, true, executionTimeMs, selectedIndexType, execResult);
                            resultTabbedPane.showQueryResult(execResult);
                        } else {
                            // 非查询类指令或查询无数据，显示详细消息
                            String message = isQuery ? "查询成功，但无数据返回" : execResult.getMessage();
                            resultTabbedPane.showMessage(message);
                        }
                    } else {
                        // 执行失败
                        resultTabbedPane.showQueryMessage(sql, true, false, executionTimeMs, selectedIndexType, execResult);
                        resultTabbedPane.showError(execResult.getMessage());
                    }
                } catch (Exception e) {
                    resultTabbedPane.showQueryMessage(sql, true, false, 0, "");
                    resultTabbedPane.showError("执行失败: " + e.getMessage() + "\n注意: 数据库引擎功能尚未完全实现");
                }
                
                statusLabel.setText("执行成功");
                statusLabel.setForeground(Color.GREEN);
            } else {
                // 编译失败，显示错误信息
                resultTabbedPane.showCompileError(sql, result.getErrors());
                statusLabel.setText("编译失败");
                statusLabel.setForeground(Color.RED);
            }
            
        } catch (Exception e) {
            resultTabbedPane.showError("程序错误: " + e.getMessage());
            statusLabel.setText("程序错误");
            statusLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * 内部执行SQL查询（不修改输入区域，不重置AST可视化）
     */
    private void executeSQLInternal(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            statusLabel.setText("SQL语句为空");
            statusLabel.setForeground(Color.ORANGE);
            return;
        }
        
        statusLabel.setText("正在执行...");
        statusLabel.setForeground(Color.BLUE);
        
        // 不清空显示区域，保持用户当前的AST可视化
        // tokenArea.setText("");
        // astArea.setText("");
        // astVisualizer.setAST(null);
        
        try {
            // 检查是否是批量SQL语句
            boolean isMultiStatement = sql.contains(";") && sql.split(";").length > 1;
            
            // 使用增强版SQL编译器进行编译
            EnhancedSQLCompiler.CompilationResult result;
            if (isMultiStatement) {
                result = compiler.compileBatch(sql);
            } else {
                result = compiler.compile(sql);
            }
            
            // 不显示Token信息，保持用户当前的显示
            // displayTokens(result);
            
            // 不显示AST信息，保持用户当前的显示
            // displayAST(result);
            
            // 不显示AST可视化，保持用户当前的显示
            // displayASTVisualization(result);
            
            // 显示执行结果
            if (result.isSuccess()) {
                // 尝试执行SQL（如果数据库引擎支持）
                try {
                    // 获取选择的索引类型
                    String selectedIndexType = (String) indexTypeComboBox.getSelectedItem();
                    
                    // 获取选择的存储格式
                    String selectedStorageFormat = (String) storageFormatComboBox.getSelectedItem();
                    
                    // 获取表实际的存储格式
                    String actualStorageFormat = getActualStorageFormat(sql);
                    if (actualStorageFormat != null) {
                        resultTabbedPane.showMessage("表存储格式: " + actualStorageFormat);
                    }
                    
                    // 设置数据库引擎的索引类型
                    databaseEngine.setIndexType(selectedIndexType);
                    
                    // 设置数据库引擎的存储格式
                    databaseEngine.setStorageFormat(selectedStorageFormat);
                    
                    // 测量执行时间
                    long startTime = System.nanoTime();
                    ExecutionResult execResult = databaseEngine.executeSQL(sql);
                    long endTime = System.nanoTime();
                    
                    // 计算执行时间（毫秒）
                    double executionTimeMs = (endTime - startTime) / 1_000_000.0;
                    
                    // 更新性能指标显示
                    updatePerformanceMetrics(executionTimeMs, execResult, predicatePushdownCheckBox.isSelected());
                    
                    // 更新子查询改写信息显示
                    updateSubqueryRewriteInfo(execResult);
                    
                    // 判断是否为查询类指令
                    boolean isQuery = isQueryStatement(sql);
                    
                    if (execResult.isSuccess()) {
                        // 检查是否是批量执行结果
                        if (execResult.getBatchResults() != null && !execResult.getBatchResults().isEmpty()) {
                            // 批量执行结果
                            resultTabbedPane.showQueryMessage(sql, true, true, executionTimeMs, selectedIndexType, execResult);
                            resultTabbedPane.showQueryResult(execResult); // 这会调用showBatchResults
                        } else if (isQuery && execResult.getData() != null && !execResult.getData().isEmpty()) {
                            // 单个查询类指令且有数据，显示查询消息和结果
                            resultTabbedPane.showQueryMessage(sql, true, true, executionTimeMs, selectedIndexType, execResult);
                            resultTabbedPane.showQueryResult(execResult);
                        } else {
                            // 非查询类指令或查询无数据，显示详细消息
                            String message = isQuery ? "查询成功，但无数据返回" : execResult.getMessage();
                            resultTabbedPane.showMessage(message);
                        }
                    } else {
                        // 执行失败
                        resultTabbedPane.showQueryMessage(sql, true, false, executionTimeMs, selectedIndexType, execResult);
                        resultTabbedPane.showError(execResult.getMessage());
                    }
                } catch (Exception e) {
                    resultTabbedPane.showQueryMessage(sql, true, false, 0, "");
                    resultTabbedPane.showError("执行失败: " + e.getMessage() + "\n注意: 数据库引擎功能尚未完全实现");
                }
                
                statusLabel.setText("执行成功");
                statusLabel.setForeground(Color.GREEN);
            } else {
                // 编译失败，显示错误信息
                resultTabbedPane.showCompileError(sql, result.getErrors());
                statusLabel.setText("编译失败");
                statusLabel.setForeground(Color.RED);
            }
            
        } catch (Exception e) {
            resultTabbedPane.showError("程序错误: " + e.getMessage());
            statusLabel.setText("程序错误");
            statusLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * 判断是否为查询类指令
     */
    private boolean isQueryStatement(String sql) {
        String[] lines = sql.split("\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            // 跳过注释行
            if (trimmedLine.startsWith("--")) {
                continue;
            }
            // 跳过空行
            if (trimmedLine.isEmpty()) {
                continue;
            }
            // 检查是否为查询语句
            String lowerLine = trimmedLine.toLowerCase();
            if (lowerLine.startsWith("select") || 
                lowerLine.startsWith("show") || 
                lowerLine.startsWith("describe") || 
                lowerLine.startsWith("desc") ||
                lowerLine.startsWith("explain") ||
                lowerLine.startsWith("call")) {
                return true;
            }
            // 如果遇到非注释非空行但不是查询语句，则不是查询
            break;
        }
        return false;
    }
    
    /**
     * 更新性能指标显示
     */
    private void updatePerformanceMetrics(double executionTimeMs, ExecutionResult result, boolean predicatePushdownEnabled) {
        StringBuilder metrics = new StringBuilder();
        metrics.append("性能: ");
        metrics.append(String.format("%.2f", executionTimeMs)).append("ms");

        if (result.isSuccess() && result.getData() != null) {
            int rowCount = result.getData().size();
            metrics.append(" | 行数: ").append(rowCount);

            if (rowCount > 0) {
                double rowsPerMs = rowCount / executionTimeMs;
                metrics.append(" | 速率: ").append(String.format("%.1f", rowsPerMs)).append("行/ms");
            }
        }

        if (predicatePushdownEnabled) {
            metrics.append(" | 优化: 启用");
        } else {
            metrics.append(" | 优化: 禁用");
        }

        performanceLabel.setText(metrics.toString());

        // 根据执行时间设置颜色
        if (executionTimeMs < 10) {
            performanceLabel.setForeground(Color.GREEN);
        } else if (executionTimeMs < 100) {
            performanceLabel.setForeground(Color.ORANGE);
        } else {
            performanceLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * 更新子查询改写信息显示
     */
    private void updateSubqueryRewriteInfo(ExecutionResult result) {
        if (result.getSubqueryRewriteInfo() != null && !result.getSubqueryRewriteInfo().isEmpty()) {
            subqueryRewriteLabel.setText("子查询优化: " + result.getSubqueryRewriteInfo());
            subqueryRewriteLabel.setForeground(Color.BLUE);
        } else {
            subqueryRewriteLabel.setText("子查询优化: 自动检测中");
            subqueryRewriteLabel.setForeground(Color.GREEN);
        }
    }
    
    /**
     * 显示Token信息
     */
    private void displayTokens(EnhancedSQLCompiler.CompilationResult result) {
        tokenArea.setText(result.getFormattedTokens());
    }
    
    /**
     * 显示AST信息
     */
    private void displayAST(EnhancedSQLCompiler.CompilationResult result) {
        astArea.setText(result.getFormattedAST());
    }
    
    /**
     * 显示AST可视化
     */
    private void displayASTVisualization(EnhancedSQLCompiler.CompilationResult result) {
        if (result.isSuccess() && result.getStatement() != null) {
            astVisualizer.setAST(result.getStatement());
        } else {
            astVisualizer.setAST(null);
        }
    }
    
    /**
     * 处理数据库管理命令
     */
    private boolean handleDatabaseCommands(String sql) {
        String trimmedSql = sql.trim().toLowerCase();
        
        // CREATE DATABASE命令
        if (trimmedSql.startsWith("create database")) {
            String[] parts = sql.trim().split("\\s+");
            if (parts.length >= 3) {
                String dbName = parts[2].replaceAll("[;]", "").trim();
                try {
                    if (databaseManager.createDatabase(dbName)) {
                        statusLabel.setText("数据库 '" + dbName + "' 创建成功");
                        statusLabel.setForeground(Color.GREEN);
                        resultTabbedPane.showMessage("数据库 '" + dbName + "' 创建成功！");
                        refreshDatabaseList();
                        return true;
                    } else {
                        statusLabel.setText("数据库创建失败");
                        statusLabel.setForeground(Color.RED);
                        resultTabbedPane.showError("数据库 '" + dbName + "' 已存在或创建失败");
                        return true;
                    }
                } catch (Exception e) {
                    statusLabel.setText("创建数据库错误");
                    statusLabel.setForeground(Color.RED);
                    resultTabbedPane.showError("创建数据库错误: " + e.getMessage());
                    return true;
                }
            } else {
                resultTabbedPane.showError("CREATE DATABASE语法错误，正确语法: CREATE DATABASE database_name");
                return true;
            }
        }
        
        // DROP DATABASE命令
        if (trimmedSql.startsWith("drop database")) {
            String[] parts = sql.trim().split("\\s+");
            if (parts.length >= 3) {
                String dbName = parts[2].replaceAll("[;]", "").trim();
                if (dbName.equals("main")) {
                    resultTabbedPane.showError("不能删除main数据库！");
                    return true;
                }
                
                int confirm = JOptionPane.showConfirmDialog(this,
                    "确定要删除数据库 '" + dbName + "' 吗？\n此操作不可撤销！",
                    "确认删除数据库",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        if (databaseManager.dropDatabase(dbName)) {
                            statusLabel.setText("数据库 '" + dbName + "' 已删除");
                            statusLabel.setForeground(Color.GREEN);
                            resultTabbedPane.showMessage("数据库 '" + dbName + "' 删除成功！");
                            refreshDatabaseList();
                            
                            // 如果删除的是当前数据库，需要更新界面
                            if (dbName.equals(databaseManager.getCurrentDatabase())) {
                                databaseEngine = databaseManager.getCurrentDatabaseEngine();
                                compiler = new EnhancedSQLCompiler(databaseEngine.getCatalogManager().getCatalog());
                                autoComplete = new SQLAutoComplete(sqlInputArea, compiler.getCatalog());
                                refreshDatabaseTree();
                            }
                            return true;
                        } else {
                            statusLabel.setText("数据库删除失败");
                            statusLabel.setForeground(Color.RED);
                            resultTabbedPane.showError("数据库 '" + dbName + "' 不存在或删除失败");
                            return true;
                        }
                    } catch (Exception e) {
                        statusLabel.setText("删除数据库错误");
                        statusLabel.setForeground(Color.RED);
                        resultTabbedPane.showError("删除数据库错误: " + e.getMessage());
                        return true;
                    }
                }
                return true; // 用户取消了删除操作
            } else {
                resultTabbedPane.showError("DROP DATABASE语法错误，正确语法: DROP DATABASE database_name");
                return true;
            }
        }
        
        // USE DATABASE命令
        if (trimmedSql.startsWith("use")) {
            String[] parts = sql.trim().split("\\s+");
            if (parts.length >= 2) {
                String dbName = parts[1].replaceAll("[;]", "").trim();
                try {
                    if (databaseManager.useDatabase(dbName)) {
                        // 更新界面
                        databaseEngine = databaseManager.getCurrentDatabaseEngine();
                        compiler = new EnhancedSQLCompiler(databaseEngine.getCatalogManager().getCatalog());
                        autoComplete = new SQLAutoComplete(sqlInputArea, compiler.getCatalog());
                        
                        statusLabel.setText("已切换到数据库: " + dbName);
                        statusLabel.setForeground(Color.GREEN);
                        resultTabbedPane.showMessage("已切换到数据库: " + dbName);
                        
                        // 更新UI组件
                        databaseComboBox.setSelectedItem(dbName);
                        dropDatabaseButton.setEnabled(!dbName.equals("main"));
                        updateWindowTitle();
                        refreshDatabaseTree();
                        return true;
                    } else {
                        statusLabel.setText("切换数据库失败");
                        statusLabel.setForeground(Color.RED);
                        resultTabbedPane.showError("数据库 '" + dbName + "' 不存在");
                        return true;
                    }
                } catch (Exception e) {
                    statusLabel.setText("切换数据库错误");
                    statusLabel.setForeground(Color.RED);
                    resultTabbedPane.showError("切换数据库错误: " + e.getMessage());
                    return true;
                }
            } else {
                resultTabbedPane.showError("USE语法错误，正确语法: USE database_name");
                return true;
            }
        }
        
        // SHOW DATABASES命令
        if (trimmedSql.startsWith("show databases")) {
            try {
                Set<String> databaseNames = databaseManager.getAllDatabaseNames();
                StringBuilder result = new StringBuilder("数据库列表:\n");
                String currentDb = databaseManager.getCurrentDatabase();
                
                for (String dbName : databaseNames) {
                    result.append("- ").append(dbName);
                    if (dbName.equals(currentDb)) {
                        result.append(" (当前)");
                    }
                    result.append("\n");
                }
                
                resultTabbedPane.showMessage(result.toString());
                statusLabel.setText("显示数据库列表成功");
                statusLabel.setForeground(Color.GREEN);
                return true;
            } catch (Exception e) {
                statusLabel.setText("显示数据库列表失败");
                statusLabel.setForeground(Color.RED);
                resultTabbedPane.showError("显示数据库列表失败: " + e.getMessage());
                return true;
            }
        }
        
        return false; // 不是数据库管理命令
    }
    
    /**
     * 显示目录信息
     */
    private void showCatalog() {
        try {
            String catalogInfo = compiler.getCatalogInfo();
            resultTabbedPane.showMessage("目录信息:\n" + catalogInfo);
        } catch (Exception e) {
            resultTabbedPane.showError("获取目录信息失败: " + e.getMessage());
        }
    }
    
    
    /**
     * 获取表实际的存储格式
     */
    private String getActualStorageFormat(String sql) {
        try {
            // 从SQL中提取表名
            String tableName = extractTableNameFromSQL(sql);
            if (tableName == null) {
                return null;
            }
            
            // 通过数据库引擎获取表的实际存储格式
            if (databaseEngine != null) {
                // 检查表是否存在
                if (databaseEngine.getCatalogManager().tableExists(tableName)) {
                    TableInfo tableInfo = databaseEngine.getCatalogManager().getTable(tableName);
                    if (tableInfo != null) {
                        String storageFormat = tableInfo.getStorageFormat();
                        if ("COLUMN".equals(storageFormat)) {
                            return "列式存储";
                        } else if ("ROW".equals(storageFormat)) {
                            return "行式存储";
                        } else {
                            return "行式存储"; // 默认
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果获取失败，不显示存储格式信息
            System.err.println("获取表存储格式失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 从SQL语句中提取表名
     */
    private String extractTableNameFromSQL(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }
        
        String upperSql = sql.toUpperCase().trim();
        
        // 处理SELECT语句
        if (upperSql.startsWith("SELECT")) {
            // 查找FROM关键字
            int fromIndex = upperSql.indexOf(" FROM ");
            if (fromIndex != -1) {
                String afterFrom = upperSql.substring(fromIndex + 6).trim();
                // 取第一个词作为表名（忽略别名）
                String[] parts = afterFrom.split("\\s+");
                if (parts.length > 0) {
                    return parts[0].toLowerCase();
                }
            }
        }
        // 处理INSERT语句
        else if (upperSql.startsWith("INSERT")) {
            int intoIndex = upperSql.indexOf(" INTO ");
            if (intoIndex != -1) {
                String afterInto = upperSql.substring(intoIndex + 6).trim();
                String[] parts = afterInto.split("\\s+");
                if (parts.length > 0) {
                    return parts[0].toLowerCase();
                }
            }
        }
        // 处理UPDATE语句
        else if (upperSql.startsWith("UPDATE")) {
            String afterUpdate = upperSql.substring(6).trim();
            String[] parts = afterUpdate.split("\\s+");
            if (parts.length > 0) {
                return parts[0].toLowerCase();
            }
        }
        // 处理DELETE语句
        else if (upperSql.startsWith("DELETE")) {
            int fromIndex = upperSql.indexOf(" FROM ");
            if (fromIndex != -1) {
                String afterFrom = upperSql.substring(fromIndex + 6).trim();
                String[] parts = afterFrom.split("\\s+");
                if (parts.length > 0) {
                    return parts[0].toLowerCase();
                }
            }
        }
        // 处理CREATE TABLE语句
        else if (upperSql.startsWith("CREATE TABLE")) {
            String afterCreate = upperSql.substring(12).trim();
            String[] parts = afterCreate.split("\\s+");
            if (parts.length > 0) {
                return parts[0].toLowerCase();
            }
        }
        // 处理DROP TABLE语句
        else if (upperSql.startsWith("DROP TABLE")) {
            String afterDrop = upperSql.substring(10).trim();
            String[] parts = afterDrop.split("\\s+");
            if (parts.length > 0) {
                return parts[0].toLowerCase();
            }
        }
        
        return null;
    }
    
    /**
     * 清空所有区域
     */
    private void clearAll() {
        sqlInputArea.setText("");
        resultTabbedPane.clear();
        tokenArea.setText("");
        astArea.setText("");
        astVisualizer.setAST(null);

        statusLabel.setText("已清空");
        statusLabel.setForeground(Color.BLUE);
    }
    
    /**
     * 导入SQL文件
     */
    private void importSQLFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择SQL文件");
        fileChooser.setFileFilter(new FileNameExtensionFilter("SQL文件 (*.sql)", "sql"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // 询问是否容错模式
            int option = JOptionPane.showConfirmDialog(this,
                "是否在遇到错误时继续执行后续语句？\n" +
                "是：容错模式（跳过错误语句继续执行）\n" +
                "否：快速失败模式（遇到错误时停止）",
                "执行模式选择",
                JOptionPane.YES_NO_OPTION);
            
            boolean continueOnError = (option == JOptionPane.YES_OPTION);
            
            statusLabel.setText("正在导入SQL文件...");
            statusLabel.setForeground(Color.BLUE);
            
            try {
                ExecutionResult execResult = databaseEngine.importSQLFile(selectedFile.getAbsolutePath(), continueOnError);
                
                if (execResult.isSuccess()) {
                    statusLabel.setText("导入成功");
                    statusLabel.setForeground(Color.GREEN);
                    resultTabbedPane.showMessage("SQL文件导入成功!\n" + execResult.getMessage());
                } else {
                    statusLabel.setText("导入失败");
                    statusLabel.setForeground(Color.RED);
                    resultTabbedPane.showError("SQL文件导入失败:\n" + execResult.getMessage());
                }
            } catch (Exception e) {
                statusLabel.setText("导入错误");
                statusLabel.setForeground(Color.RED);
                resultTabbedPane.showError("导入错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 导出数据库
     */
    private void exportDatabase() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存数据库导出文件");
        fileChooser.setFileFilter(new FileNameExtensionFilter("SQL文件 (*.sql)", "sql"));
        fileChooser.setSelectedFile(new File("database_backup.sql"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // 确保文件扩展名为.sql
            String filePath = selectedFile.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".sql")) {
                filePath += ".sql";
                selectedFile = new File(filePath);
            }
            
            // 如果文件已存在，询问是否覆盖
            if (selectedFile.exists()) {
                int option = JOptionPane.showConfirmDialog(this,
                    "文件已存在，是否覆盖？",
                    "确认覆盖",
                    JOptionPane.YES_NO_OPTION);
                if (option != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            // 导出选项对话框
            ExportOptionsDialog optionsDialog = new ExportOptionsDialog(this);
            optionsDialog.setVisible(true);
            
            if (optionsDialog.isConfirmed()) {
                statusLabel.setText("正在导出数据库...");
                statusLabel.setForeground(Color.BLUE);
                
                try {
                    // 处理表名列表
                    List<String> tableNames = null;
                    String selectedTablesStr = optionsDialog.getSelectedTables();
                    if (selectedTablesStr != null && !selectedTablesStr.trim().isEmpty()) {
                        tableNames = Arrays.asList(selectedTablesStr.split(","));
                        for (int i = 0; i < tableNames.size(); i++) {
                            tableNames.set(i, tableNames.get(i).trim());
                        }
                    }
                    
                    ExecutionResult execResult = databaseEngine.exportDatabaseToSQL(
                        selectedFile.getAbsolutePath(),
                        tableNames,
                        optionsDialog.isIncludeStructure(),
                        optionsDialog.isIncludeData()
                    );
                    
                    if (execResult.isSuccess()) {
                        statusLabel.setText("导出成功");
                        statusLabel.setForeground(Color.GREEN);
                        resultTabbedPane.showMessage("数据库导出成功!\n文件: " + selectedFile.getAbsolutePath() + "\n" + execResult.getMessage());
                    } else {
                        statusLabel.setText("导出失败");
                        statusLabel.setForeground(Color.RED);
                        resultTabbedPane.showError("数据库导出失败:\n" + execResult.getMessage());
                    }
                } catch (Exception e) {
                    statusLabel.setText("导出错误");
                    statusLabel.setForeground(Color.RED);
                    resultTabbedPane.showError("导出错误: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 导出单个表
     */
    private void exportTable() {
        // 获取所有表名
        try {
            ExecutionResult tablesResult = databaseEngine.executeSQL("SHOW TABLES");
            if (!tablesResult.isSuccess() || tablesResult.getData() == null || tablesResult.getData().isEmpty()) {
                JOptionPane.showMessageDialog(this, "数据库中没有表可以导出", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // 创建表选择对话框
            String[] tableNames = tablesResult.getData().stream()
                .map(row -> row.values().iterator().next().toString())
                .toArray(String[]::new);
            
            String selectedTable = (String) JOptionPane.showInputDialog(this,
                "选择要导出的表:",
                "选择表",
                JOptionPane.QUESTION_MESSAGE,
                null,
                tableNames,
                tableNames[0]);
            
            if (selectedTable != null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("保存表导出文件");
                fileChooser.setFileFilter(new FileNameExtensionFilter("SQL文件 (*.sql)", "sql"));
                fileChooser.setSelectedFile(new File(selectedTable + "_backup.sql"));
                
                int result = fileChooser.showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    
                    // 确保文件扩展名为.sql
                    String filePath = selectedFile.getAbsolutePath();
                    if (!filePath.toLowerCase().endsWith(".sql")) {
                        filePath += ".sql";
                        selectedFile = new File(filePath);
                    }
                    
                    statusLabel.setText("正在导出表 " + selectedTable + "...");
                    statusLabel.setForeground(Color.BLUE);
                    
                    try {
                        ExecutionResult execResult = databaseEngine.exportTableToSQL(selectedTable, selectedFile.getAbsolutePath());
                        
                        if (execResult.isSuccess()) {
                            statusLabel.setText("导出成功");
                            statusLabel.setForeground(Color.GREEN);
                            resultTabbedPane.showMessage("表 " + selectedTable + " 导出成功!\n文件: " + selectedFile.getAbsolutePath() + "\n" + execResult.getMessage());
                        } else {
                            statusLabel.setText("导出失败");
                            statusLabel.setForeground(Color.RED);
                            resultTabbedPane.showError("表导出失败:\n" + execResult.getMessage());
                        }
                    } catch (Exception e) {
                        statusLabel.setText("导出错误");
                        statusLabel.setForeground(Color.RED);
                        resultTabbedPane.showError("导出错误: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            resultTabbedPane.showError("获取表列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出表到指定文件和格式
     */
    private void exportTableToFile(String tableName, File outputFile, String format) {
        try {
            statusLabel.setText("正在导出表 " + tableName + " 为 " + format + " 格式...");
            statusLabel.setForeground(Color.BLUE);

            switch (format.toUpperCase()) {
                case "SQL":
                    exportTableToSQL(tableName, outputFile);
                    break;
                case "CSV":
                    exportTableToCSV(tableName, outputFile);
                    break;
                case "JSON":
                    exportTableToJSON(tableName, outputFile);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的导出格式: " + format);
            }

            statusLabel.setText("导出成功");
            statusLabel.setForeground(Color.GREEN);
            resultTabbedPane.showMessage("表 " + tableName + " 导出成功!\n文件: " + outputFile.getAbsolutePath() + "\n格式: " + format);

        } catch (Exception e) {
            statusLabel.setText("导出失败");
            statusLabel.setForeground(Color.RED);
            resultTabbedPane.showError("导出失败:\n" + e.getMessage());
        }
    }

    /**
     * 导出表为SQL格式
     */
    private void exportTableToSQL(String tableName, File outputFile) throws Exception {
        ExecutionResult result = databaseEngine.exportTableToSQL(tableName, outputFile.getAbsolutePath());
        if (!result.isSuccess()) {
            throw new Exception(result.getMessage());
        }
    }

    /**
     * 导出表为CSV格式
     */
    private void exportTableToCSV(String tableName, File outputFile) throws Exception {
        // 获取表数据
        ExecutionResult result = databaseEngine.executeSQL("SELECT * FROM " + tableName);
        if (!result.isSuccess()) {
            throw new Exception("查询表数据失败: " + result.getMessage());
        }

        // 写入CSV文件
        java.io.FileWriter writer = new java.io.FileWriter(outputFile, java.nio.charset.StandardCharsets.UTF_8);
        java.io.BufferedWriter bufferedWriter = new java.io.BufferedWriter(writer);

        try {
            List<Map<String, Object>> data = result.getData();
            if (data != null && !data.isEmpty()) {
                // 获取表结构信息作为CSV标题行
                TableInfo tableInfo = databaseEngine.getCatalogManager().getTable(tableName);
                if (tableInfo != null) {
                    // 写入列名作为标题行
                    List<String> columnNames = tableInfo.getColumnNames();
                    for (int i = 0; i < columnNames.size(); i++) {
                        if (i > 0) bufferedWriter.write(",");
                        bufferedWriter.write("\"" + columnNames.get(i) + "\"");
                    }
                    bufferedWriter.newLine();
                }

                // 写入数据行
                for (Map<String, Object> row : data) {
                    List<String> columnNames = tableInfo.getColumnNames();
                    for (int i = 0; i < columnNames.size(); i++) {
                        if (i > 0) bufferedWriter.write(",");
                        Object value = row.get(columnNames.get(i));
                        if (value != null) {
                            String valueStr = value.toString();
                            // 处理包含逗号或引号的值
                            if (valueStr.contains(",") || valueStr.contains("\"") || valueStr.contains("\n")) {
                                valueStr = "\"" + valueStr.replace("\"", "\"\"") + "\"";
                            }
                            bufferedWriter.write(valueStr);
                        }
                    }
                    bufferedWriter.newLine();
                }
            }
        } finally {
            bufferedWriter.close();
        }
    }

    /**
     * 导出表为JSON格式
     */
    private void exportTableToJSON(String tableName, File outputFile) throws Exception {
        // 获取表数据
        ExecutionResult result = databaseEngine.executeSQL("SELECT * FROM " + tableName);
        if (!result.isSuccess()) {
            throw new Exception("查询表数据失败: " + result.getMessage());
        }

        // 写入JSON文件
        java.io.FileWriter writer = new java.io.FileWriter(outputFile, java.nio.charset.StandardCharsets.UTF_8);
        java.io.BufferedWriter bufferedWriter = new java.io.BufferedWriter(writer);

        try {
            List<Map<String, Object>> data = result.getData();
            TableInfo tableInfo = databaseEngine.getCatalogManager().getTable(tableName);

            bufferedWriter.write("{\n");
            bufferedWriter.write("  \"tableName\": \"" + tableName + "\",\n");
            bufferedWriter.write("  \"exportTime\": \"" + java.time.LocalDateTime.now().toString() + "\",\n");

            if (tableInfo != null) {
                List<String> columnNames = tableInfo.getColumnNames();
                bufferedWriter.write("  \"columns\": [");
                for (int i = 0; i < columnNames.size(); i++) {
                    if (i > 0) bufferedWriter.write(", ");
                    bufferedWriter.write("\"" + columnNames.get(i) + "\"");
                }
                bufferedWriter.write("],\n");
            }

            bufferedWriter.write("  \"data\": [\n");

            if (data != null && !data.isEmpty()) {
                List<String> columnNames = tableInfo != null ? tableInfo.getColumnNames() : null;

                for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                    if (rowIndex > 0) bufferedWriter.write(",\n");

                    Map<String, Object> row = data.get(rowIndex);
                    bufferedWriter.write("    {\n");

                    List<String> rowColumnNames = columnNames != null ? columnNames : new ArrayList<>(row.keySet());
                    for (int colIndex = 0; colIndex < rowColumnNames.size(); colIndex++) {
                        if (colIndex > 0) bufferedWriter.write(",\n");

                        String columnName = rowColumnNames.get(colIndex);
                        Object value = row.get(columnName);
                        bufferedWriter.write("      \"" + columnName + "\": ");

                        if (value == null) {
                            bufferedWriter.write("null");
                        } else if (value instanceof Number) {
                            bufferedWriter.write(value.toString());
                        } else if (value instanceof Boolean) {
                            bufferedWriter.write(value.toString().toLowerCase());
                        } else {
                            // 字符串值需要转义
                            String valueStr = value.toString()
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n")
                                .replace("\r", "\\r")
                                .replace("\t", "\\t");
                            bufferedWriter.write("\"" + valueStr + "\"");
                        }
                    }
                    bufferedWriter.write("\n    }");
                }
            }

            bufferedWriter.write("\n  ]\n");
            bufferedWriter.write("}\n");

        } finally {
            bufferedWriter.close();
        }
    }

    /**
     * 批量导入目录
     */
    private void importDirectory() {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setDialogTitle("选择包含SQL文件的目录");
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int result = dirChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = dirChooser.getSelectedFile();
            
            // 询问文件模式和容错模式
            String pattern = JOptionPane.showInputDialog(this,
                "输入文件名模式（如 *.sql）：",
                "文件模式",
                JOptionPane.QUESTION_MESSAGE);
            
            if (pattern == null || pattern.trim().isEmpty()) {
                pattern = "*.sql";
            }
            
            int option = JOptionPane.showConfirmDialog(this,
                "是否在遇到错误时继续执行？\n" +
                "是：容错模式（跳过错误文件继续执行）\n" +
                "否：快速失败模式（遇到错误时停止）",
                "执行模式选择",
                JOptionPane.YES_NO_OPTION);
            
            boolean continueOnError = (option == JOptionPane.YES_OPTION);
            
            statusLabel.setText("正在批量导入目录...");
            statusLabel.setForeground(Color.BLUE);
            
            try {
                ExecutionResult execResult = databaseEngine.importSQLDirectory(
                    selectedDir.getAbsolutePath(), 
                    pattern, 
                    continueOnError
                );
                
                if (execResult.isSuccess()) {
                    statusLabel.setText("批量导入成功");
                    statusLabel.setForeground(Color.GREEN);
                    resultTabbedPane.showMessage("目录批量导入成功!\n" + execResult.getMessage());
                } else {
                    statusLabel.setText("批量导入失败");
                    statusLabel.setForeground(Color.RED);
                    resultTabbedPane.showError("目录批量导入失败:\n" + execResult.getMessage());
                }
            } catch (Exception e) {
                statusLabel.setText("批量导入错误");
                statusLabel.setForeground(Color.RED);
                resultTabbedPane.showError("批量导入错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 导出选项对话框
     */
    private static class ExportOptionsDialog extends JDialog {
        private boolean confirmed = false;
        private boolean includeStructure = true;
        private boolean includeData = true;
        private String selectedTables = "";
        
        private JCheckBox structureCheckBox;
        private JCheckBox dataCheckBox;
        private JTextField tablesField;
        
        public ExportOptionsDialog(Frame parent) {
            super(parent, "导出选项", true);
            initComponents();
            setupLayout();
            setupEventHandlers();
            setLocationRelativeTo(parent);
        }
        
        private void initComponents() {
            structureCheckBox = new JCheckBox("包含表结构", true);
            dataCheckBox = new JCheckBox("包含数据", true);
            tablesField = new JTextField(20);
            tablesField.setToolTipText("留空导出所有表，或用逗号分隔指定表名");
        }
        
        private void setupLayout() {
            setLayout(new BorderLayout());
            
            JPanel mainPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            
            gbc.gridx = 0; gbc.gridy = 0;
            mainPanel.add(structureCheckBox, gbc);
            
            gbc.gridy = 1;
            mainPanel.add(dataCheckBox, gbc);
            
            gbc.gridy = 2;
            mainPanel.add(new JLabel("指定表（可选）:"), gbc);
            
            gbc.gridy = 3;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            mainPanel.add(tablesField, gbc);
            
            add(mainPanel, BorderLayout.CENTER);
            
            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton okButton = new JButton("确定");
            JButton cancelButton = new JButton("取消");
            
            okButton.addActionListener(e -> {
                confirmed = true;
                includeStructure = structureCheckBox.isSelected();
                includeData = dataCheckBox.isSelected();
                selectedTables = tablesField.getText().trim();
                dispose();
            });
            
            cancelButton.addActionListener(e -> dispose());
            
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
            add(buttonPanel, BorderLayout.SOUTH);
            
            pack();
        }
        
        private void setupEventHandlers() {
            // 确保至少选择一个选项
            ActionListener checkboxListener = e -> {
                if (!structureCheckBox.isSelected() && !dataCheckBox.isSelected()) {
                    if (e.getSource() == structureCheckBox) {
                        dataCheckBox.setSelected(true);
                    } else {
                        structureCheckBox.setSelected(true);
                    }
                }
            };
            
            structureCheckBox.addActionListener(checkboxListener);
            dataCheckBox.addActionListener(checkboxListener);
        }
        
        public boolean isConfirmed() { return confirmed; }
        public boolean isIncludeStructure() { return includeStructure; }
        public boolean isIncludeData() { return includeData; }
        public String getSelectedTables() { return selectedTables; }
    }
    
    /**
     * 更新窗口标题显示当前数据库
     */
    private void updateWindowTitle() {
        try {
            String currentDb = databaseManager.getCurrentDatabase();
            setTitle("SparrowDB - 当前数据库: " + (currentDb != null ? currentDb : "未选择"));
        } catch (Exception e) {
            setTitle("SparrowDB");
        }
    }
    
    /**
     * 刷新数据库列表
     */
    private void refreshDatabaseList() {
        try {
            Set<String> databaseNames = databaseManager.getAllDatabaseNames();
            
            // 清空并重新填充数据库下拉框
            databaseComboBox.removeAllItems();
            for (String dbName : databaseNames) {
                databaseComboBox.addItem(dbName);
            }
            
            // 选择当前数据库
            String currentDb = databaseManager.getCurrentDatabase();
            if (currentDb != null && databaseNames.contains(currentDb)) {
                databaseComboBox.setSelectedItem(currentDb);
            }
            
            // 更新删除按钮状态（main数据库不能删除）
            dropDatabaseButton.setEnabled(!currentDb.equals("main"));
            
        } catch (Exception e) {
            statusLabel.setText("刷新数据库列表失败: " + e.getMessage());
            statusLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * 切换数据库
     */
    private void switchDatabase() {
        String selectedDb = (String) databaseComboBox.getSelectedItem();
        if (selectedDb != null && !selectedDb.equals(databaseManager.getCurrentDatabase())) {
            try {
                if (databaseManager.useDatabase(selectedDb)) {
                    // 更新当前数据库引擎
                    databaseEngine = databaseManager.getCurrentDatabaseEngine();
                    
                    // 重新初始化编译器和自动补全
                    compiler = new EnhancedSQLCompiler(databaseEngine.getCatalogManager().getCatalog());
                    autoComplete = new SQLAutoComplete(sqlInputArea, compiler.getCatalog());
                    
                    // 更新状态
                    statusLabel.setText("已切换到数据库: " + selectedDb);
                    statusLabel.setForeground(Color.GREEN);
                    
                    // 更新删除按钮状态
                    dropDatabaseButton.setEnabled(!selectedDb.equals("main"));
                    
                    // 更新窗口标题
                    updateWindowTitle();
                    
                    // 刷新数据库树
                    refreshDatabaseTree();
                    
                    resultTabbedPane.showMessage("已切换到数据库: " + selectedDb);
                } else {
                    statusLabel.setText("切换数据库失败");
                    statusLabel.setForeground(Color.RED);
                    resultTabbedPane.showError("数据库 '" + selectedDb + "' 不存在");
                }
            } catch (Exception e) {
                statusLabel.setText("切换数据库错误: " + e.getMessage());
                statusLabel.setForeground(Color.RED);
                resultTabbedPane.showError("切换数据库错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 创建数据库
     */
    private void createDatabase() {
        String dbName = JOptionPane.showInputDialog(this,
            "请输入新数据库名称:\n(只能包含字母、数字、下划线，且不能以数字开头)",
            "创建数据库",
            JOptionPane.QUESTION_MESSAGE);
        
        if (dbName != null && !dbName.trim().isEmpty()) {
            dbName = dbName.trim();
            try {
                if (databaseManager.createDatabase(dbName)) {
                    statusLabel.setText("数据库 '" + dbName + "' 创建成功");
                    statusLabel.setForeground(Color.GREEN);
                    resultTabbedPane.showMessage("数据库 '" + dbName + "' 创建成功！");
                    
                    // 刷新数据库列表
                    refreshDatabaseList();
                } else {
                    statusLabel.setText("数据库创建失败");
                    statusLabel.setForeground(Color.RED);
                    resultTabbedPane.showError("数据库 '" + dbName + "' 已存在或创建失败");
                }
            } catch (Exception e) {
                statusLabel.setText("创建数据库错误: " + e.getMessage());
                statusLabel.setForeground(Color.RED);
                resultTabbedPane.showError("创建数据库错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 删除数据库
     */
    private void dropDatabase() {
        String currentDb = databaseManager.getCurrentDatabase();
        
        if (currentDb.equals("main")) {
            JOptionPane.showMessageDialog(this,
                "不能删除main数据库！",
                "警告",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "确定要删除数据库 '" + currentDb + "' 吗？\n" +
            "此操作将删除该数据库中的所有表、视图、函数等数据，且不可撤销！",
            "确认删除数据库",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (databaseManager.dropDatabase(currentDb)) {
                    statusLabel.setText("数据库 '" + currentDb + "' 已删除");
                    statusLabel.setForeground(Color.GREEN);
                    resultTabbedPane.showMessage("数据库 '" + currentDb + "' 删除成功！已切换到main数据库。");
                    
                    // 刷新数据库列表和树
                    refreshDatabaseList();
                    
                    // 切换到main数据库
                    databaseEngine = databaseManager.getCurrentDatabaseEngine();
                    compiler = new EnhancedSQLCompiler(databaseEngine.getCatalogManager().getCatalog());
                    autoComplete = new SQLAutoComplete(sqlInputArea, compiler.getCatalog());
                    refreshDatabaseTree();
                } else {
                    statusLabel.setText("数据库删除失败");
                    statusLabel.setForeground(Color.RED);
                    resultTabbedPane.showError("数据库 '" + currentDb + "' 不存在或删除失败");
                }
            } catch (Exception e) {
                statusLabel.setText("删除数据库错误: " + e.getMessage());
                statusLabel.setForeground(Color.RED);
                resultTabbedPane.showError("删除数据库错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 刷新数据库树
     */
    private void refreshDatabaseTree() {
        try {
            // 获取数据库中的所有表
            java.util.Set<String> tableNames = databaseEngine.getCatalogManager().getAllTableNames();
            
            // 创建树模型
            String currentDbName = databaseManager.getCurrentDatabase();
            javax.swing.tree.DefaultMutableTreeNode root = new javax.swing.tree.DefaultMutableTreeNode("数据库: " + currentDbName);
            
            // 添加表节点
            javax.swing.tree.DefaultMutableTreeNode tablesNode = new javax.swing.tree.DefaultMutableTreeNode("表");
            for (String tableName : tableNames) {
                if (!tableName.startsWith("__system_")) { // 过滤系统表
                    javax.swing.tree.DefaultMutableTreeNode tableNode = new javax.swing.tree.DefaultMutableTreeNode(tableName);
                    tablesNode.add(tableNode);
                }
            }
            root.add(tablesNode);
            
            // 添加函数节点
            javax.swing.tree.DefaultMutableTreeNode functionsNode = new javax.swing.tree.DefaultMutableTreeNode("函数");
            java.util.Set<String> functionNames = databaseEngine.getFunctionManager().getAllFunctionNames();
            for (String functionName : functionNames) {
                javax.swing.tree.DefaultMutableTreeNode functionNode = new javax.swing.tree.DefaultMutableTreeNode(functionName);
                functionsNode.add(functionNode);
            }
            root.add(functionsNode);
            
            // 添加视图节点
            javax.swing.tree.DefaultMutableTreeNode viewsNode = new javax.swing.tree.DefaultMutableTreeNode("视图");
            java.util.Set<String> viewNames = databaseEngine.getViewManager().getAllViewNames();
            for (String viewName : viewNames) {
                javax.swing.tree.DefaultMutableTreeNode viewNode = new javax.swing.tree.DefaultMutableTreeNode(viewName);
                viewsNode.add(viewNode);
            }
            root.add(viewsNode);
            
            // 设置树模型
            javax.swing.tree.DefaultTreeModel treeModel = new javax.swing.tree.DefaultTreeModel(root);
            databaseTree.setModel(treeModel);
            
            // 展开根节点
            for (int i = 0; i < databaseTree.getRowCount(); i++) {
                databaseTree.expandRow(i);
            }
            
            statusLabel.setText("数据库树已刷新");
            statusLabel.setForeground(Color.GREEN);
        } catch (Exception e) {
            statusLabel.setText("刷新数据库树失败: " + e.getMessage());
            statusLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * 处理树选择事件
     */
    private void handleTreeSelection(javax.swing.event.TreeSelectionEvent e) {
        javax.swing.tree.DefaultMutableTreeNode selectedNode = 
            (javax.swing.tree.DefaultMutableTreeNode) databaseTree.getLastSelectedPathComponent();
        
        if (selectedNode == null) return;
        
        String nodeName = selectedNode.toString();
        javax.swing.tree.DefaultMutableTreeNode parentNode = 
            (javax.swing.tree.DefaultMutableTreeNode) selectedNode.getParent();
        
        if (parentNode == null) return;
        
        String parentName = parentNode.toString();
        
        // 如果选择的是表节点，显示表数据
        if (parentName.equals("表")) {
            showTableData(nodeName);
        }
        // 如果选择的是函数节点，显示函数信息
        else if (parentName.equals("函数")) {
            showFunctionInfo(nodeName);
        }
        // 如果选择的是视图节点，显示视图数据
        else if (parentName.equals("视图")) {
            showViewData(nodeName);
        }
    }
    
    /**
     * 显示表数据
     */
    private void showTableData(String tableName) {
        try {
            // 执行SELECT * FROM tableName查询，但不修改用户输入区域
            String sql = "SELECT * FROM " + tableName;
            
            // 直接执行查询而不修改输入区域
            executeSQLInternal(sql);
            
            statusLabel.setText("正在显示表 " + tableName + " 的数据");
            statusLabel.setForeground(Color.BLUE);
        } catch (Exception e) {
            statusLabel.setText("显示表数据失败: " + e.getMessage());
            statusLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * 导出选中的表
     */
    private void exportSelectedTable() {
        javax.swing.tree.DefaultMutableTreeNode selectedNode =
            (javax.swing.tree.DefaultMutableTreeNode) databaseTree.getLastSelectedPathComponent();

        if (selectedNode == null) return;

        String tableName = selectedNode.toString();
        javax.swing.tree.DefaultMutableTreeNode parentNode =
            (javax.swing.tree.DefaultMutableTreeNode) selectedNode.getParent();

        if (parentNode == null || !parentNode.toString().equals("表")) {
            JOptionPane.showMessageDialog(this, "请选择一个表进行导出", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 显示导出格式选择对话框
        String[] exportFormats = {"SQL", "CSV", "JSON"};
        String selectedFormat = (String) JOptionPane.showInputDialog(
            this,
            "请选择导出格式:",
            "导出表 - " + tableName,
            JOptionPane.QUESTION_MESSAGE,
            null,
            exportFormats,
            exportFormats[0]
        );

        if (selectedFormat == null) return; // 用户取消了选择

        // 显示文件选择对话框
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出表 - " + tableName);

        // 根据选择的格式设置文件过滤器和默认文件名
        String extension;
        String description;
        switch (selectedFormat) {
            case "CSV":
                extension = "csv";
                description = "CSV文件 (*.csv)";
                break;
            case "JSON":
                extension = "json";
                description = "JSON文件 (*.json)";
                break;
            default: // SQL
                extension = "sql";
                description = "SQL文件 (*.sql)";
                break;
        }

        fileChooser.setFileFilter(new FileNameExtensionFilter(description, extension));
        fileChooser.setSelectedFile(new File(tableName + "." + extension));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            // 确保文件有正确的扩展名
            if (!selectedFile.getName().toLowerCase().endsWith("." + extension)) {
                selectedFile = new File(selectedFile.getAbsolutePath() + "." + extension);
            }

            // 执行导出
            exportTableToFile(tableName, selectedFile, selectedFormat);
        }
    }

    /**
     * 显示选中表的数据
     */
    private void showSelectedTableData() {
        javax.swing.tree.DefaultMutableTreeNode selectedNode =
            (javax.swing.tree.DefaultMutableTreeNode) databaseTree.getLastSelectedPathComponent();

        if (selectedNode == null) return;

        String tableName = selectedNode.toString();
        javax.swing.tree.DefaultMutableTreeNode parentNode =
            (javax.swing.tree.DefaultMutableTreeNode) selectedNode.getParent();

        if (parentNode != null && parentNode.toString().equals("表")) {
            showTableData(tableName);
        }
    }

    /**
     * 删除选中的表
     */
    private void deleteSelectedTable() {
        javax.swing.tree.DefaultMutableTreeNode selectedNode =
            (javax.swing.tree.DefaultMutableTreeNode) databaseTree.getLastSelectedPathComponent();

        if (selectedNode == null) return;

        String tableName = selectedNode.toString();
        javax.swing.tree.DefaultMutableTreeNode parentNode =
            (javax.swing.tree.DefaultMutableTreeNode) selectedNode.getParent();

        if (parentNode == null || !parentNode.toString().equals("表")) {
            JOptionPane.showMessageDialog(this, "请选择一个表进行删除", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 确认对话框
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "确定要删除表 '" + tableName + "' 吗？\n此操作不可撤销！",
            "确认删除",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                ExecutionResult result = databaseEngine.executeSQL("DROP TABLE " + tableName);
                if (result.isSuccess()) {
                    statusLabel.setText("表 " + tableName + " 已删除");
                    statusLabel.setForeground(Color.GREEN);
                    refreshDatabaseTree(); // 刷新树显示
                    resultTabbedPane.showMessage("表 " + tableName + " 删除成功！");
                } else {
                    statusLabel.setText("删除表失败");
                    statusLabel.setForeground(Color.RED);
                    resultTabbedPane.showError("删除表失败:\n" + result.getMessage());
                }
            } catch (Exception e) {
                statusLabel.setText("删除表失败: " + e.getMessage());
                statusLabel.setForeground(Color.RED);
                resultTabbedPane.showError("删除表失败:\n" + e.getMessage());
            }
        }
    }

    /**
     * 显示函数信息
     */
    private void showFunctionInfo(String functionName) {
        try {
            // 获取函数定义
            com.database.engine.FunctionManager.UserDefinedFunction function = 
                databaseEngine.getFunctionManager().getFunction(functionName);
            
            if (function == null) {
                statusLabel.setText("函数 " + functionName + " 不存在");
                statusLabel.setForeground(Color.RED);
                return;
            }
            
            // 构建函数信息
            StringBuilder functionInfo = new StringBuilder();
            functionInfo.append("函数名: ").append(functionName).append("\n");
            functionInfo.append("返回类型: ").append(function.getReturnType()).append("\n");
            functionInfo.append("参数: ");
            
            java.util.List<com.sqlcompiler.ast.CreateFunctionStatement.FunctionParameter> params = function.getParameters();
            if (params.isEmpty()) {
                functionInfo.append("无参数\n");
            } else {
                for (int i = 0; i < params.size(); i++) {
                    if (i > 0) functionInfo.append(", ");
                    functionInfo.append(params.get(i).getName()).append(" ").append(params.get(i).getType());
                }
                functionInfo.append("\n");
            }
            
            functionInfo.append("函数体:\n").append(function.getBody());
            
            // 在结果区域显示函数信息
            resultTabbedPane.showMessage("函数信息:\n" + functionInfo.toString());
            
            statusLabel.setText("显示函数 " + functionName + " 的定义");
            statusLabel.setForeground(Color.BLUE);
        } catch (Exception e) {
            statusLabel.setText("显示函数信息失败: " + e.getMessage());
            statusLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * 显示视图数据
     */
    private void showViewData(String viewName) {
        try {
            // 执行SELECT * FROM viewName查询，但不修改用户输入区域
            String sql = "SELECT * FROM " + viewName;
            
            // 直接执行查询而不修改输入区域
            executeSQLInternal(sql);
            
            statusLabel.setText("正在显示视图 " + viewName + " 的数据");
            statusLabel.setForeground(Color.BLUE);
        } catch (Exception e) {
            statusLabel.setText("显示视图数据失败: " + e.getMessage());
            statusLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * 显示分片管理器
     */
    private void showShardManager() {
        ShardManagerDialog dialog = new ShardManagerDialog(this, databaseEngine);
        dialog.setVisible(true);
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new DatabaseGUI().setVisible(true);
            }
        });
    }
}
