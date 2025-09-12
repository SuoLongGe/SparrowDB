package com.sqlcompiler.gui;

import com.sqlcompiler.catalog.Catalog;
import com.sqlcompiler.catalog.TableInfo;
import com.sqlcompiler.catalog.ColumnInfo;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * SQL自动补全组件
 * 提供SQL关键字、表名、列名的自动补全功能
 * 使用下拉选择框，类似Navicat的自动补全体验
 */
public class SQLAutoComplete {
    private final JTextComponent textArea;
    private final Catalog catalog;
    private JWindow popupWindow;
    private JList<String> suggestionList;
    private DefaultListModel<String> listModel;
    private List<String> currentSuggestions;
    private int currentCaretPosition;
    private String currentWord;
    private boolean isPopupVisible = false;
    
    // SQL关键字列表
    private static final Set<String> SQL_KEYWORDS = new HashSet<>(Arrays.asList(
        "CREATE", "TABLE", "INSERT", "INTO", "VALUES", "SELECT", "FROM", "WHERE",
        "DELETE", "DROP", "ALTER", "UPDATE", "SET", "AND", "OR", "NOT", "AS",
        "DISTINCT", "ORDER", "BY", "GROUP", "HAVING", "LIMIT", "OFFSET",
        "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON", "IS", "NULL",
        "TRUE", "FALSE", "LIKE", "IN", "BETWEEN", "ASC", "DESC",
        "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "DEFAULT",
        "AUTO_INCREMENT", "CHECK", "COUNT", "SUM", "AVG", "MAX", "MIN",
        "INT", "INTEGER", "VARCHAR", "CHAR", "TEXT", "DECIMAL", "FLOAT",
        "DOUBLE", "BOOLEAN", "DATE", "TIME", "TIMESTAMP"
    ));
    
    public SQLAutoComplete(JTextComponent textArea, Catalog catalog) {
        this.textArea = textArea;
        this.catalog = catalog;
        this.currentSuggestions = new ArrayList<>();
        this.currentCaretPosition = 0;
        this.currentWord = "";
        
        setupAutoComplete();
    }
    
    
    /**
     * 设置自动补全功能
     */
    private void setupAutoComplete() {
        // 创建弹出窗口
        popupWindow = new JWindow();
        popupWindow.setFocusable(false);
        popupWindow.setAlwaysOnTop(true);
        
        // 创建建议列表
        listModel = new DefaultListModel<>();
        suggestionList = new JList<>(listModel);
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        suggestionList.setBackground(Color.WHITE);
        suggestionList.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(128, 128, 128), 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        suggestionList.setVisibleRowCount(8); // 显示8行
        
        // 设置选择颜色
        suggestionList.setSelectionBackground(new Color(51, 153, 255));
        suggestionList.setSelectionForeground(Color.WHITE);
        
        // 添加鼠标点击事件
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    acceptSuggestion();
                }
            }
        });
        
        // 设置弹出窗口内容
        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setPreferredSize(new Dimension(250, 200));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        popupWindow.add(scrollPane);
        
        // 添加键盘事件监听器
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e);
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyReleased(e);
            }
        });
        
        // 添加鼠标事件监听器
        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                hidePopup();
            }
        });
        
        // 添加焦点事件监听器
        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // 延迟隐藏，给用户时间点击建议
                javax.swing.Timer timer = new javax.swing.Timer(200, evt -> hidePopup());
                timer.setRepeats(false);
                timer.start();
            }
        });
        
        // 添加文档监听器，当文本变化时更新弹出窗口位置
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updatePopupPosition();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updatePopupPosition();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updatePopupPosition();
            }
        });
    }
    
    /**
     * 处理按键按下事件
     */
    private void handleKeyPressed(KeyEvent e) {
        if (isPopupVisible) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    e.consume();
                    selectPreviousSuggestion();
                    break;
                case KeyEvent.VK_DOWN:
                    e.consume();
                    selectNextSuggestion();
                    break;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_TAB:
                    e.consume();
                    acceptSuggestion();
                    break;
                case KeyEvent.VK_ESCAPE:
                    e.consume();
                    hidePopup();
                    break;
            }
        }
    }
    
    /**
     * 处理按键释放事件
     */
    private void handleKeyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB && !isPopupVisible) {
            // Tab键触发自动补全
            triggerAutoComplete();
        } else if (Character.isLetterOrDigit(e.getKeyChar()) || e.getKeyChar() == '_') {
            // 字母、数字或下划线触发自动补全
            triggerAutoComplete();
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            // 空格键也可能触发补全
            triggerAutoComplete();
        }
    }
    
    /**
     * 触发自动补全
     */
    private void triggerAutoComplete() {
        currentCaretPosition = textArea.getCaretPosition();
        currentWord = getCurrentWord();
        
        if (currentWord.length() >= 1) {
            generateSuggestions();
            if (!currentSuggestions.isEmpty()) {
                showPopup();
            }
        } else {
            hidePopup();
        }
    }
    
    /**
     * 获取当前光标位置的单词
     */
    private String getCurrentWord() {
        String text = textArea.getText();
        int pos = currentCaretPosition;
        
        // 向前查找单词开始位置
        int start = pos;
        while (start > 0 && (Character.isLetterOrDigit(text.charAt(start - 1)) || 
                            text.charAt(start - 1) == '_')) {
            start--;
        }
        
        // 向后查找单词结束位置
        int end = pos;
        while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || 
                                      text.charAt(end) == '_')) {
            end++;
        }
        
        return text.substring(start, end);
    }
    
    /**
     * 生成建议列表
     */
    private void generateSuggestions() {
        currentSuggestions.clear();
        String word = currentWord.toUpperCase();
        
        // 添加SQL关键字建议
        for (String keyword : SQL_KEYWORDS) {
            if (keyword.startsWith(word)) {
                currentSuggestions.add(keyword);
            }
        }
        
        // 添加表名建议
        for (String tableName : catalog.getAllTableNames()) {
            if (tableName.toUpperCase().startsWith(word)) {
                currentSuggestions.add(tableName);
            }
        }
        
        // 添加列名建议（基于当前上下文）
        addColumnSuggestions(word);
        
        // 按字母顺序排序
        Collections.sort(currentSuggestions);
        
        // 限制建议数量
        if (currentSuggestions.size() > 20) {
            currentSuggestions = currentSuggestions.subList(0, 20);
        }
    }
    
    /**
     * 添加列名建议
     */
    private void addColumnSuggestions(String word) {
        String text = textArea.getText();
        int pos = currentCaretPosition;
        
        // 查找最近的FROM子句来确定表名
        String recentTableName = findRecentTableName(text, pos);
        
        if (recentTableName != null) {
            TableInfo table = catalog.getTable(recentTableName);
            if (table != null) {
                for (ColumnInfo column : table.getColumns()) {
                    if (column.getName().toUpperCase().startsWith(word)) {
                        currentSuggestions.add(column.getName());
                    }
                }
            }
        } else {
            // 如果没有找到特定表，添加所有表的列名
            for (String tableName : catalog.getAllTableNames()) {
                TableInfo table = catalog.getTable(tableName);
                if (table != null) {
                    for (ColumnInfo column : table.getColumns()) {
                        if (column.getName().toUpperCase().startsWith(word)) {
                            currentSuggestions.add(tableName + "." + column.getName());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 查找最近的表名（在FROM子句中）
     */
    private String findRecentTableName(String text, int pos) {
        // 从当前位置向前查找最近的FROM关键字
        String beforeCursor = text.substring(0, pos);
        String[] words = beforeCursor.split("\\s+");
        
        for (int i = words.length - 1; i >= 0; i--) {
            if ("FROM".equalsIgnoreCase(words[i]) && i + 1 < words.length) {
                String tableName = words[i + 1].replaceAll("[^a-zA-Z0-9_]", "");
                if (catalog.tableExists(tableName)) {
                    return tableName;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 显示弹出窗口
     */
    private void showPopup() {
        if (currentSuggestions.isEmpty()) {
            hidePopup();
            return;
        }
        
        // 更新列表模型
        listModel.clear();
        for (String suggestion : currentSuggestions) {
            listModel.addElement(suggestion);
        }
        
        // 设置选择
        suggestionList.setSelectedIndex(0);
        
        // 计算弹出窗口位置 - 始终显示在SQL输入区域下方
        Point popupLocation = calculatePopupPosition();
        
        // 显示弹出窗口
        popupWindow.setLocation(popupLocation);
        popupWindow.pack();
        popupWindow.setVisible(true);
        isPopupVisible = true;
    }
    
    /**
     * 计算弹出窗口位置，确保显示在SQL输入区域下方
     */
    private Point calculatePopupPosition() {
        // 获取SQL输入区域的位置和大小
        Rectangle textAreaBounds = textArea.getBounds();
        Point textAreaLocation = textArea.getLocationOnScreen();
        
        // 计算SQL输入区域在屏幕上的位置
        Point textAreaScreenLocation = SwingUtilities.convertPoint(
            textArea.getParent(), textArea.getLocation(), null);
        
        // 获取主窗口的位置
        Window mainWindow = SwingUtilities.getWindowAncestor(textArea);
        Point mainWindowLocation = mainWindow.getLocationOnScreen();
        
        // 计算弹出窗口应该显示的位置
        // 水平位置：与SQL输入区域左对齐
        int popupX = mainWindowLocation.x + textAreaScreenLocation.x;
        
        // 垂直位置：显示在SQL输入区域下方
        int popupY = mainWindowLocation.y + textAreaScreenLocation.y + textAreaBounds.height;
        
        // 确保弹出窗口不超出主窗口边界
        Rectangle mainWindowBounds = mainWindow.getBounds();
        int maxX = mainWindowLocation.x + mainWindowBounds.width - 250; // 250是弹出窗口宽度
        int maxY = mainWindowLocation.y + mainWindowBounds.height - 200; // 200是弹出窗口高度
        
        if (popupX > maxX) {
            popupX = maxX;
        }
        if (popupY > maxY) {
            // 如果下方空间不够，显示在SQL输入区域上方
            popupY = mainWindowLocation.y + textAreaScreenLocation.y - 200;
        }
        
        // 确保不超出屏幕边界
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (popupX < 0) popupX = 0;
        if (popupY < 0) popupY = 0;
        if (popupX + 250 > screenSize.width) popupX = screenSize.width - 250;
        if (popupY + 200 > screenSize.height) popupY = screenSize.height - 200;
        
        return new Point(popupX, popupY);
    }
    
    /**
     * 更新弹出窗口位置
     */
    private void updatePopupPosition() {
        if (isPopupVisible) {
            Point newLocation = calculatePopupPosition();
            popupWindow.setLocation(newLocation);
        }
    }
    
    /**
     * 隐藏弹出窗口
     */
    private void hidePopup() {
        popupWindow.setVisible(false);
        isPopupVisible = false;
    }
    
    /**
     * 选择上一个建议
     */
    private void selectPreviousSuggestion() {
        int selectedIndex = suggestionList.getSelectedIndex();
        if (selectedIndex > 0) {
            suggestionList.setSelectedIndex(selectedIndex - 1);
        }
    }
    
    /**
     * 选择下一个建议
     */
    private void selectNextSuggestion() {
        int selectedIndex = suggestionList.getSelectedIndex();
        if (selectedIndex < suggestionList.getModel().getSize() - 1) {
            suggestionList.setSelectedIndex(selectedIndex + 1);
        }
    }
    
    /**
     * 接受当前建议
     */
    private void acceptSuggestion() {
        String selectedSuggestion = suggestionList.getSelectedValue();
        if (selectedSuggestion != null) {
            replaceCurrentWord(selectedSuggestion);
            hidePopup();
        }
    }
    
    /**
     * 替换当前单词
     */
    private void replaceCurrentWord(String replacement) {
        String text = textArea.getText();
        int pos = currentCaretPosition;
        
        // 计算单词的开始和结束位置
        int start = pos;
        while (start > 0 && (Character.isLetterOrDigit(text.charAt(start - 1)) || 
                            text.charAt(start - 1) == '_')) {
            start--;
        }
        
        int end = pos;
        while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || 
                                      text.charAt(end) == '_')) {
            end++;
        }
        
        // 替换单词
        String newText = text.substring(0, start) + replacement + text.substring(end);
        textArea.setText(newText);
        
        // 设置光标位置
        textArea.setCaretPosition(start + replacement.length());
    }
    
    /**
     * 手动触发自动补全（供外部调用）
     */
    public void showAutoComplete() {
        triggerAutoComplete();
    }
    
    /**
     * 更新目录信息（当数据库结构发生变化时调用）
     */
    public void updateCatalog(Catalog newCatalog) {
        // 这里可以添加目录更新逻辑
        // 由于Catalog对象是引用传递，通常不需要特殊处理
    }
}
