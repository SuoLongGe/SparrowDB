package com.sqlcompiler;

import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import com.sqlcompiler.catalog.TableInfo;

import com.sqlcompiler.gui.SQLAutoComplete;
import com.sqlcompiler.gui.SQLSyntaxHighlighter;
import com.sqlcompiler.gui.ASTVisualizer;
import com.sqlcompiler.gui.LineNumberScrollPane;
import com.sqlcompiler.gui.ResultTabbedPane;

import com.database.config.DatabaseConfig;


import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 数据库GUI界面
 * 提供SQL输入、结果显示和Token/AST显示功能
 */
public class DatabaseGUI extends JFrame {
    private EnhancedSQLCompiler compiler;
    private DatabaseEngine databaseEngine;
    
    // 界面组件
    private JTextPane sqlInputArea;
    private ResultTabbedPane resultTabbedPane;
    private JTextArea tokenArea;
    private JTextArea astArea;
    private JButton executeButton;
    private JButton clearButton;
    private JButton catalogButton;
    private JButton importButton;
    private JLabel statusLabel;
    
    // 菜单栏组件
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenuItem importSQLItem;
    private JMenuItem exportDBItem;
    private JMenuItem exportTableItem;
    private JMenuItem importDirItem;
    
    // 索引选择组件
    private JComboBox<String> indexTypeComboBox;
    private JLabel executionTimeLabel;
    
    // 存储格式选择组件
    private JComboBox<String> storageFormatComboBox;
    
    // 自动补全组件
    private SQLAutoComplete autoComplete;
    
    // 语法高亮组件
    private SQLSyntaxHighlighter syntaxHighlighter;
    
    // AST可视化组件
    private ASTVisualizer astVisualizer;
    
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
     * 初始化组件
     */
    private void initializeComponents() {
        // SQL输入区域 - 使用JTextPane支持语法高亮
        sqlInputArea = new JTextPane();
        sqlInputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        //sqlInputArea.setBorder(new TitledBorder("SQL输入区域"));
        // 设置固定大小，防止根据内容自动调整
        sqlInputArea.setSize(new Dimension(600, 200));
        sqlInputArea.setMinimumSize(new Dimension(600, 200));
        sqlInputArea.setMaximumSize(new Dimension(600, 200));
        sqlInputArea.setPreferredSize(new Dimension(600, 200));
        
        // 初始化语法高亮组件
        syntaxHighlighter = new SQLSyntaxHighlighter(sqlInputArea);
        
        // 初始化AST可视化组件
        astVisualizer = new ASTVisualizer();
        
        // 设置键盘快捷键
        setupKeyboardShortcuts();
        
        // 结果显示区域 - 使用标签栏组件
        resultTabbedPane = new ResultTabbedPane();
        
        // Token显示区域
        tokenArea = new JTextArea(15, 30);
        tokenArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
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
        executeButton.setToolTipText("执行SQL语句（有选中文本时执行选中部分，否则执行全部）");
        
        clearButton = new JButton("清空");
        clearButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        catalogButton = new JButton("查看目录");
        catalogButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        // 导入SQL文件按钮
        importButton = new JButton("导入SQL文件");
        importButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        importButton.setPreferredSize(new Dimension(120, 30));
        importButton.setToolTipText("从文件导入并执行SQL语句");

        // 索引选择组件
        String[] indexTypes = {"智能选择", "B+树索引", "哈希索引", "线性查找"};
        indexTypeComboBox = new JComboBox<>(indexTypes);
        indexTypeComboBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        indexTypeComboBox.setSelectedIndex(0); // 默认选择智能选择
        
        // 执行时间标签
        executionTimeLabel = new JLabel("执行时间: --");
        executionTimeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        executionTimeLabel.setForeground(new Color(100, 100, 100));
        
        // 存储格式选择组件
        String[] storageFormats = {"行式存储", "列式存储"};
        storageFormatComboBox = new JComboBox<>(storageFormats);
        storageFormatComboBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        storageFormatComboBox.setSelectedIndex(0); // 默认选择行式存储
        
        // 状态标签
        statusLabel = new JLabel("就绪");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        statusLabel.setForeground(Color.BLUE);
        
        // 初始化菜单栏
        initializeMenuBar();
    }
    
    /**
     * 初始化菜单栏
     */
    private void initializeMenuBar() {
        menuBar = new JMenuBar();
        // 设置菜单栏的背景色，确保可见
        menuBar.setBackground(Color.LIGHT_GRAY);
        menuBar.setBorder(BorderFactory.createRaisedBevelBorder());
        
        // 文件菜单
        fileMenu = new JMenu("文件");
        fileMenu.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        fileMenu.setForeground(Color.BLACK);
        
        // 导入SQL文件
        importSQLItem = new JMenuItem("导入SQL文件...");
        importSQLItem.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        importSQLItem.setToolTipText("从文件导入并执行SQL语句");
        
        // 导出数据库
        exportDBItem = new JMenuItem("导出数据库...");
        exportDBItem.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        exportDBItem.setToolTipText("将整个数据库导出为SQL文件");
        
        // 导出单个表
        exportTableItem = new JMenuItem("导出单个表...");
        exportTableItem.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        exportTableItem.setToolTipText("将指定表导出为SQL文件");
        
        // 批量导入目录
        importDirItem = new JMenuItem("批量导入目录...");
        importDirItem.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
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
    }
    
    /**
     * 设置布局
     */
    private void setupLayout() {
        setTitle("SparrowDB");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // 设置菜单栏
        setJMenuBar(menuBar);
        // 确保菜单栏可见
        menuBar.setVisible(true);
        System.out.println("菜单栏已设置，包含 " + menuBar.getMenuCount() + " 个菜单");
        
        // 顶部：SQL输入区域、自动补全建议和按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
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
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(executeButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(catalogButton);
        buttonPanel.add(importButton);
        
        // 添加索引选择组件
        JLabel indexLabel = new JLabel("索引方式:");
        indexLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        buttonPanel.add(indexLabel);
        buttonPanel.add(indexTypeComboBox);
        
        // 添加存储格式选择组件
        JLabel storageLabel = new JLabel("存储格式:");
        storageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        buttonPanel.add(storageLabel);
        buttonPanel.add(storageFormatComboBox);
        
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
        
        // 添加快捷键提示
        JLabel shortcutLabel = new JLabel("F5:执行选中 | F9:执行全部 | Ctrl+Enter:执行全部");
        shortcutLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        shortcutLabel.setForeground(new Color(100, 100, 100));
        buttonPanel.add(shortcutLabel);
        
        buttonPanel.add(statusLabel);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        
        // 底部：结果显示区域
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 左侧：执行结果（占满整个左侧）
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("执行结果"));
        
        // 直接添加标签栏组件
        resultPanel.add(resultTabbedPane, BorderLayout.CENTER);
        bottomPanel.add(resultPanel);
        
        // 右侧：Token列表和AST可视化（上下分布，高度比例2:3）
        JPanel rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Token列表（上半部分，占2/5高度）
        JPanel tokenPanel = new JPanel(new BorderLayout());
        tokenPanel.setBorder(BorderFactory.createTitledBorder("Token列表"));
        JScrollPane tokenScrollPane = new JScrollPane(tokenArea);
        tokenScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tokenScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tokenScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY)); // 内层单纯线框
        tokenPanel.add(tokenScrollPane, BorderLayout.CENTER);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.4; // 2/5 = 0.4
        gbc.fill = GridBagConstraints.BOTH;
        rightPanel.add(tokenPanel, gbc);
        
        // AST可视化（下半部分，占3/5高度）
        JPanel astPanel = new JPanel(new BorderLayout());
        astPanel.setBorder(BorderFactory.createTitledBorder("AST可视化"));
        
        // 添加AST可视化组件
        JScrollPane astVisualizerScrollPane = new JScrollPane(astVisualizer);
        astVisualizerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        astVisualizerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        astVisualizerScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY)); // 内层单纯线框
        astPanel.add(astVisualizerScrollPane, BorderLayout.CENTER);
        
        // 添加放大缩小按钮
        JPanel astButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        zoomInButton = new JButton("+");
        zoomInButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        zoomInButton.setPreferredSize(new Dimension(30, 25));
        zoomInButton.setToolTipText("放大");
        
        zoomOutButton = new JButton("-");
        zoomOutButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        zoomOutButton.setPreferredSize(new Dimension(30, 25));
        zoomOutButton.setToolTipText("缩小");
        
        fitButton = new JButton("适应");
        fitButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        fitButton.setPreferredSize(new Dimension(40, 25));
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
        rightPanel.add(astPanel, gbc);
        bottomPanel.add(rightPanel);
        
        add(bottomPanel, BorderLayout.CENTER);
        
        // 设置窗口大小和位置
        setSize(1200, 800);
        setLocationRelativeTo(null);
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
        
        catalogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showCatalog();
            }
        });
        
        // 导入SQL文件按钮事件
        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importSQLFile();
            }
        });
        
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
                resultTabbedPane.showMessage("数据库引擎初始化成功！");
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
                    
                    // 更新执行时间标签
                    executionTimeLabel.setText(String.format("执行时间: %.2f ms", executionTimeMs));
                    
                    // 判断是否为查询类指令
                    boolean isQuery = isQueryStatement(sql);
                    
                    if (execResult.isSuccess()) {
                        // 检查是否是批量执行结果
                        if (execResult.getBatchResults() != null && !execResult.getBatchResults().isEmpty()) {
                            // 批量执行结果
                            resultTabbedPane.showQueryMessage(sql, true, true, executionTimeMs, selectedIndexType);
                            resultTabbedPane.showQueryResult(execResult); // 这会调用showBatchResults
                        } else if (isQuery && execResult.getData() != null && !execResult.getData().isEmpty()) {
                            // 单个查询类指令且有数据，显示查询消息和结果
                            resultTabbedPane.showQueryMessage(sql, true, true, executionTimeMs, selectedIndexType);
                            resultTabbedPane.showQueryResult(execResult);
                        } else {
                            // 非查询类指令或查询无数据，显示详细消息
                            String message = isQuery ? "查询成功，但无数据返回" : execResult.getMessage();
                            resultTabbedPane.showMessage(message);
                        }
                    } else {
                        // 执行失败
                        resultTabbedPane.showQueryMessage(sql, true, false, executionTimeMs, selectedIndexType);
                        resultTabbedPane.showError(execResult.getMessage());
                    }
                } catch (Exception e) {
                    resultTabbedPane.showQueryMessage(sql, true, false, 0, "");
                    resultTabbedPane.showError("执行失败: " + e.getMessage() + "\n注意: 数据库引擎功能尚未完全实现");
                    executionTimeLabel.setText("执行时间: 错误");
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
        String trimmedSql = sql.trim().toLowerCase();
        return trimmedSql.startsWith("select") || 
               trimmedSql.startsWith("show") || 
               trimmedSql.startsWith("describe") || 
               trimmedSql.startsWith("desc") ||
               trimmedSql.startsWith("explain") ||
               trimmedSql.startsWith("call");
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

        executionTimeLabel.setText("执行时间: --");
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
