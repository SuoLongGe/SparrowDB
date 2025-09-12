package com.sqlcompiler.gui;

import com.sqlcompiler.EnhancedSQLCompiler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 集成AST可视化测试程序
 * 测试AST可视化直接集成到主界面的功能
 */
public class IntegratedASTTest extends JFrame {
    private JTextArea sqlInputArea;
    private JTextArea resultArea;
    private JTextArea tokenArea;
    private JTextArea astArea;
    private ASTVisualizer astVisualizer;
    private JButton executeButton;
    private JButton clearButton;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JButton fitButton;
    private EnhancedSQLCompiler compiler;
    
    public IntegratedASTTest() {
        // 初始化SQL编译器
        compiler = new EnhancedSQLCompiler();
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initializeComponents() {
        setTitle("集成AST可视化测试 - SparrowDB");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // SQL输入区域
        sqlInputArea = new JTextArea(5, 50);
        sqlInputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        sqlInputArea.setBorder(BorderFactory.createTitledBorder("SQL语句"));
        sqlInputArea.setText("SELECT name, age FROM students WHERE age > 18 ORDER BY name");
        
        // 结果显示区域
        resultArea = new JTextArea(10, 30);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setBorder(BorderFactory.createTitledBorder("执行结果"));
        resultArea.setEditable(false);
        resultArea.setBackground(Color.WHITE);
        
        // Token显示区域
        tokenArea = new JTextArea(10, 30);
        tokenArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        tokenArea.setBorder(BorderFactory.createTitledBorder("Token列表"));
        tokenArea.setEditable(false);
        tokenArea.setBackground(Color.WHITE);
        
        // AST文本显示区域
        astArea = new JTextArea(10, 30);
        astArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        astArea.setBorder(BorderFactory.createTitledBorder("AST结构（文本）"));
        astArea.setEditable(false);
        astArea.setBackground(Color.WHITE);
        
        // AST可视化组件
        astVisualizer = new ASTVisualizer();
        
        // 按钮
        executeButton = new JButton("执行SQL");
        executeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        executeButton.setBackground(new Color(76, 175, 80));
        executeButton.setForeground(Color.WHITE);
        
        clearButton = new JButton("清空");
        clearButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 顶部：SQL输入和按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane sqlScrollPane = new JScrollPane(sqlInputArea);
        topPanel.add(sqlScrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(executeButton);
        buttonPanel.add(clearButton);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        
        // 底部：结果显示区域
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 左侧：执行结果（占满整个左侧）
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("执行结果"));
        
        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        resultScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        resultScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        resultScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY)); // 内层单纯线框
        resultPanel.add(resultScrollPane, BorderLayout.CENTER);
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
        
        // 设置窗口属性
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }
    
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
    }
    
    private void executeSQL() {
        String sql = sqlInputArea.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入SQL语句", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 清空显示区域
        resultArea.setText("");
        tokenArea.setText("");
        astArea.setText("");
        astVisualizer.setAST(null);
        
        try {
            // 使用SQL编译器来解析SQL
            EnhancedSQLCompiler.CompilationResult result = compiler.compile(sql);
            
            // 显示Token信息
            if (result.getTokens() != null) {
                StringBuilder tokenText = new StringBuilder();
                for (var token : result.getTokens()) {
                    if (token.getType() != com.sqlcompiler.lexer.TokenType.EOF) {
                        tokenText.append(token.toString()).append("\n");
                    }
                }
                tokenArea.setText(tokenText.toString());
            }
            
            // 显示AST信息（文本形式）
            if (result.getAstStructure() != null) {
                astArea.setText(result.getAstStructure());
            }
            
            // 显示AST可视化
            if (result.isSuccess() && result.getStatement() != null) {
                astVisualizer.setAST(result.getStatement());
                resultArea.setText("SQL编译成功！\n" + sql);
            } else {
                String errorMsg = "SQL编译失败";
                if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                    errorMsg += ":\n" + String.join("\n", result.getErrors());
                }
                resultArea.setText(errorMsg);
            }
            
        } catch (Exception ex) {
            resultArea.setText("执行失败: " + ex.getMessage());
        }
    }
    
    private void clearAll() {
        sqlInputArea.setText("");
        resultArea.setText("");
        tokenArea.setText("");
        astArea.setText("");
        astVisualizer.setAST(null);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new IntegratedASTTest().setVisible(true);
            }
        });
    }
}
