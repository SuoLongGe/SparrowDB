package com.sqlcompiler;

import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

import com.sqlcompiler.gui.SQLAutoComplete;
import com.sqlcompiler.gui.SQLSyntaxHighlighter;

import com.database.config.DatabaseConfig;
import com.sqlcompiler.ast.ASTPrinter;
import com.sqlcompiler.ast.Statement;
import com.sqlcompiler.execution.ExecutionPlan;
import com.sqlcompiler.lexer.Token;
import com.sqlcompiler.lexer.TokenType;


import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 数据库GUI界面
 * 提供SQL输入、结果显示和Token/AST显示功能
 */
public class DatabaseGUI extends JFrame {
    private EnhancedSQLCompiler compiler;
    private DatabaseEngine databaseEngine;
    
    // 界面组件
    private JTextPane sqlInputArea;
    private JTextArea resultArea;
    private JTextArea tokenArea;
    private JTextArea astArea;
    private JButton executeButton;
    private JButton clearButton;
    private JButton catalogButton;
    private JLabel statusLabel;
    
    // 索引选择组件
    private JComboBox<String> indexTypeComboBox;
    private JLabel executionTimeLabel;
    
    // 自动补全组件
    private SQLAutoComplete autoComplete;
    
    // 语法高亮组件
    private SQLSyntaxHighlighter syntaxHighlighter;
    
    public DatabaseGUI() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        initializeDatabase();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // SQL输入区域 - 使用JTextPane支持语法高亮
        sqlInputArea = new JTextPane();
        sqlInputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        sqlInputArea.setBorder(new TitledBorder("SQL输入区域 (支持语法高亮)"));
        sqlInputArea.setPreferredSize(new Dimension(600, 200));
        
        // 初始化语法高亮组件
        syntaxHighlighter = new SQLSyntaxHighlighter(sqlInputArea);
        
        // 结果显示区域
        resultArea = new JTextArea(15, 30);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setBorder(new TitledBorder("执行结果"));
        resultArea.setEditable(false);
        resultArea.setBackground(Color.WHITE);
        
        // Token显示区域
        tokenArea = new JTextArea(15, 30);
        tokenArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        tokenArea.setBorder(new TitledBorder("Token列表"));
        tokenArea.setEditable(false);
        tokenArea.setBackground(Color.WHITE);
        
        // AST显示区域
        astArea = new JTextArea(15, 30);
        astArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        astArea.setBorder(new TitledBorder("AST结构"));
        astArea.setEditable(false);
        astArea.setBackground(Color.WHITE);
        
        // 按钮
        executeButton = new JButton("执行SQL");
        executeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        executeButton.setBackground(new Color(76, 175, 80));
        executeButton.setForeground(Color.BLACK);
        
        clearButton = new JButton("清空");
        clearButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        catalogButton = new JButton("查看目录");
        catalogButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        // 索引选择组件
        String[] indexTypes = {"智能选择", "B+树索引", "哈希索引", "线性查找"};
        indexTypeComboBox = new JComboBox<>(indexTypes);
        indexTypeComboBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        indexTypeComboBox.setSelectedIndex(0); // 默认选择智能选择
        
        // 执行时间标签
        executionTimeLabel = new JLabel("执行时间: --");
        executionTimeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        executionTimeLabel.setForeground(new Color(100, 100, 100));
        
        // 状态标签
        statusLabel = new JLabel("就绪");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        statusLabel.setForeground(Color.BLUE);
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setTitle("SparrowDB - 迷你数据库管理界面");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // 顶部：SQL输入区域、自动补全建议和按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // SQL输入区域
        JScrollPane sqlScrollPane = new JScrollPane(sqlInputArea);
        sqlScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        topPanel.add(sqlScrollPane, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(executeButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(catalogButton);
        
        // 添加索引选择组件
        JLabel indexLabel = new JLabel("索引方式:");
        indexLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        buttonPanel.add(indexLabel);
        buttonPanel.add(indexTypeComboBox);
        
        // 添加执行时间标签
        buttonPanel.add(executionTimeLabel);
        
        // 添加语法高亮开关按钮
        JCheckBox highlightCheckBox = new JCheckBox("语法高亮", true);
        highlightCheckBox.addActionListener(e -> {
            if (syntaxHighlighter != null) {
                syntaxHighlighter.setSyntaxHighlightingEnabled(highlightCheckBox.isSelected());
            }
        });
        buttonPanel.add(highlightCheckBox);
        
        buttonPanel.add(statusLabel);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        
        // 底部：结果显示区域
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 左侧：执行结果
        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        resultScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        resultScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        bottomPanel.add(resultScrollPane);
        
        // 右侧：Token和AST显示
        JPanel rightPanel = new JPanel(new GridLayout(2, 1));
        
        JScrollPane tokenScrollPane = new JScrollPane(tokenArea);
        tokenScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tokenScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rightPanel.add(tokenScrollPane);
        
        JScrollPane astScrollPane = new JScrollPane(astArea);
        astScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        astScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rightPanel.add(astScrollPane);
        
        bottomPanel.add(rightPanel);
        add(bottomPanel, BorderLayout.CENTER);
        
        // 设置窗口大小和位置
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }
    
    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeSQL();
            }
        });
        
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearAll();
            }
        });
        
        catalogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCatalog();
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
    }
    
    /**
     * 初始化数据库
     */
    private void initializeDatabase() {
        try {
            // 先初始化数据库引擎 - 使用自动检测的数据目录路径
            String dataDirectory = DatabaseConfig.getAutoDetectedDataDirectory();
            databaseEngine = new DatabaseEngine("SparrowDB", dataDirectory);
            
            if (databaseEngine.initialize()) {

                // 使用数据库引擎的目录管理器创建SQL编译器，确保目录同步
                compiler = new EnhancedSQLCompiler(databaseEngine.getCatalogManager().getCatalog());
                
                // 初始化自动补全组件
                autoComplete = new SQLAutoComplete(sqlInputArea, compiler.getCatalog());
                
                statusLabel.setText("数据库已连接");
                statusLabel.setForeground(Color.GREEN);
                appendToResult("数据库引擎初始化成功！\n");
                appendToResult("使用简单文件存储系统\n");
                appendToResult("支持的命令：CREATE TABLE、INSERT、SELECT、DELETE\n");
                appendToResult("输入'exit'退出程序，输入'catalog'查看目录信息\n");
                appendToResult("自动补全功能已启用：按Tab键或Ctrl+Space触发\n\n");
            } else {
                statusLabel.setText("数据库连接失败");
                statusLabel.setForeground(Color.RED);
                appendToResult("数据库引擎初始化失败！\n");
            }
        } catch (Exception e) {
            statusLabel.setText("初始化错误");
            statusLabel.setForeground(Color.RED);
            appendToResult("初始化错误: " + e.getMessage() + "\n");
        }
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
            appendToResult("目录已清空\n");
            return;
        }
        
        statusLabel.setText("正在执行...");
        statusLabel.setForeground(Color.BLUE);
        
        // 清空显示区域
        tokenArea.setText("");
        astArea.setText("");
        
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
            
            // 显示AST信息
            displayAST(result);
            
            // 显示执行结果
            if (result.isSuccess()) {
                appendToResult("=== 编译成功 ===\n");
                appendToResult("SQL: " + sql + "\n\n");
                
                // 尝试执行SQL（如果数据库引擎支持）
                try {
                    // 获取选择的索引类型
                    String selectedIndexType = (String) indexTypeComboBox.getSelectedItem();
                    appendToResult("使用索引方式: " + selectedIndexType + "\n");
                    
                    // 设置数据库引擎的索引类型
                    databaseEngine.setIndexType(selectedIndexType);
                    
                    // 测量执行时间
                    long startTime = System.nanoTime();
                    ExecutionResult execResult = databaseEngine.executeSQL(sql);
                    long endTime = System.nanoTime();
                    
                    // 计算执行时间（毫秒）
                    double executionTimeMs = (endTime - startTime) / 1_000_000.0;
                    
                    // 更新执行时间标签
                    executionTimeLabel.setText(String.format("执行时间: %.2f ms", executionTimeMs));
                    
                    if (execResult.isSuccess()) {
                        appendToResult("=== 执行成功 ===\n");
                        appendToResult(String.format("执行时间: %.2f ms\n", executionTimeMs));
                        if (execResult.getData() != null) {
                            appendToResult(execResult.getData().toString() + "\n");
                        }
                    } else {
                        appendToResult("=== 执行失败 ===\n");
                        appendToResult("错误: " + execResult.getMessage() + "\n");
                    }
                } catch (Exception e) {
                    appendToResult("=== 执行失败 ===\n");
                    appendToResult("错误: " + e.getMessage() + "\n");
                    appendToResult("注意: 数据库引擎功能尚未完全实现\n");
                    executionTimeLabel.setText("执行时间: 错误");
                }
                
                statusLabel.setText("执行成功");
                statusLabel.setForeground(Color.GREEN);
            } else {
                appendToResult("=== 编译失败 ===\n");
                appendToResult("SQL: " + sql + "\n");
                if (result.getErrors() != null) {
                    for (String error : result.getErrors()) {
                        appendToResult("错误: " + error + "\n");
                    }
                }
                statusLabel.setText("编译失败");
                statusLabel.setForeground(Color.RED);
            }
            
        } catch (Exception e) {
            appendToResult("=== 程序错误 ===\n");
            appendToResult("错误: " + e.getMessage() + "\n");
            statusLabel.setText("程序错误");
            statusLabel.setForeground(Color.RED);
        }
        
        appendToResult("\n" + "=".repeat(50) + "\n\n");
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
     * 显示目录信息
     */
    private void showCatalog() {
        try {
            String catalogInfo = compiler.getCatalogInfo();
            appendToResult("=== 目录信息 ===\n");
            appendToResult(catalogInfo + "\n");
            appendToResult("\n" + "=".repeat(50) + "\n\n");
        } catch (Exception e) {
            appendToResult("获取目录信息失败: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * 清空所有区域
     */
    private void clearAll() {
        sqlInputArea.setText("");
        resultArea.setText("");
        tokenArea.setText("");
        astArea.setText("");
        executionTimeLabel.setText("执行时间: --");
        statusLabel.setText("已清空");
        statusLabel.setForeground(Color.BLUE);
    }
    
    
    /**
     * 向结果区域添加文本
     */
    private void appendToResult(String text) {
        resultArea.append(text);
        resultArea.setCaretPosition(resultArea.getDocument().getLength());
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
