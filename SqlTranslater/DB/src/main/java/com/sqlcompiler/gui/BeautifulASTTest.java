package com.sqlcompiler.gui;

import com.sqlcompiler.SQLCompiler;
import com.sqlcompiler.ast.Statement;
import com.sqlcompiler.exception.CompilationException;
import com.sqlcompiler.lexer.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 美观的树形AST可视化测试程序
 * 真正的树形结构：从根节点一层一层向下展开
 */
public class BeautifulASTTest extends JFrame {
    private JTextArea sqlInput;
    private BeautifulASTVisualizer astVisualizer;
    private JTextArea astOutput;
    
    public BeautifulASTTest() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initializeComponents() {
        setTitle("美观的树形AST可视化测试 - SparrowDB");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        
        // SQL输入区域
        sqlInput = new JTextArea(5, 50);
        sqlInput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        sqlInput.setText("SELECT * FROM user");
        sqlInput.setBorder(BorderFactory.createTitledBorder("SQL语句输入"));
        
        // AST可视化器
        astVisualizer = new BeautifulASTVisualizer();
        astVisualizer.setBorder(BorderFactory.createTitledBorder("美观的树形AST可视化 (真正的树形结构)"));
        
        // AST文本输出
        astOutput = new JTextArea(10, 50);
        astOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        astOutput.setEditable(false);
        astOutput.setBorder(BorderFactory.createTitledBorder("AST文本输出"));
        
        // 控制按钮
        JButton parseButton = new JButton("解析SQL并显示AST");
        parseButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        parseButton.setBackground(new Color(46, 204, 113));
        parseButton.setForeground(Color.WHITE);
        
        JButton clearButton = new JButton("清空");
        clearButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        clearButton.setBackground(new Color(231, 76, 60));
        clearButton.setForeground(Color.WHITE);
        
        // 示例按钮
        JButton example1Button = new JButton("示例1: SELECT * FROM user");
        JButton example2Button = new JButton("示例2: SELECT name, age FROM students WHERE age > 18");
        JButton example3Button = new JButton("示例3: SELECT * FROM users ORDER BY name");
        
        // 添加事件监听器
        parseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parseAndDisplayAST();
            }
        });
        
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sqlInput.setText("");
                astOutput.setText("");
                astVisualizer.setAST(null);
            }
        });
        
        example1Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sqlInput.setText("SELECT * FROM user");
                parseAndDisplayAST();
            }
        });
        
        example2Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sqlInput.setText("SELECT name, age FROM students WHERE age > 18");
                parseAndDisplayAST();
            }
        });
        
        example3Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sqlInput.setText("SELECT * FROM users ORDER BY name");
                parseAndDisplayAST();
            }
        });
        
        // 创建面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 顶部面板 - SQL输入和控制按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JScrollPane(sqlInput), BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(parseButton);
        buttonPanel.add(clearButton);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 示例按钮面板
        JPanel examplePanel = new JPanel(new FlowLayout());
        examplePanel.add(new JLabel("示例:"));
        examplePanel.add(example1Button);
        examplePanel.add(example2Button);
        examplePanel.add(example3Button);
        topPanel.add(examplePanel, BorderLayout.NORTH);
        
        // 中间面板 - AST可视化
        JPanel middlePanel = new JPanel(new BorderLayout());
        middlePanel.add(astVisualizer, BorderLayout.CENTER);
        
        // 底部面板 - AST文本输出
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JScrollPane(astOutput), BorderLayout.CENTER);
        
        // 组装主面板
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(middlePanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // 设置布局比例
        JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, middlePanel);
        splitPane1.setDividerLocation(200);
        splitPane1.setResizeWeight(0.2);
        
        JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane1, bottomPanel);
        splitPane2.setDividerLocation(600);
        splitPane2.setResizeWeight(0.8);
        
        add(splitPane2);
    }
    
    private void setupLayout() {
        // 布局已在initializeComponents中设置
    }
    
    private void setupEventHandlers() {
        // 事件处理已在initializeComponents中设置
    }
    
    private void parseAndDisplayAST() {
        String sql = sqlInput.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入SQL语句", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // 解析SQL语句
            SQLCompiler compiler = new SQLCompiler();
            SQLCompiler.CompilationResult result = compiler.compile(sql);
            
            if (!result.isSuccess()) {
                throw new CompilationException("SQL编译失败: " + result.getErrors(), new Position(1, 1), "编译错误");
            }
            
            Statement ast = result.getStatement();
            
            // 显示AST可视化
            astVisualizer.setAST(ast);
            
            // 显示AST文本输出
            astOutput.setText(ast.toString());
            
            // 显示成功消息
            JOptionPane.showMessageDialog(this, 
                "SQL解析成功！\n\n" +
                "美观的树形AST可视化特性：\n" +
                "🌳 真正的树形结构：从根节点一层一层向下展开\n" +
                "📐 美观的布局：每个父节点的子节点在其下方居中排列\n" +
                "🎨 完整的3层层次结构：SELECT → SELECT_LIST/FROM_CLAUSE → 具体内容\n" +
                "🔗 准确的连接线：连接线准确连接到节点中心\n" +
                "📝 智能文本处理：文本不会超出节点边界\n" +
                "🎯 清晰的层次：每层节点整齐排列，无交叉\n" +
                "🖱️ 流畅的交互：拖拽和缩放功能完善\n\n" +
                "这才是真正的树形AST可视化！", 
                "解析成功", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (CompilationException e) {
            JOptionPane.showMessageDialog(this, 
                "SQL解析失败：\n" + e.getMessage(), 
                "解析错误", 
                JOptionPane.ERROR_MESSAGE);
            astOutput.setText("解析失败: " + e.getMessage());
            astVisualizer.setAST(null);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "发生未知错误：\n" + e.getMessage(), 
                "错误", 
                JOptionPane.ERROR_MESSAGE);
            astOutput.setText("发生错误: " + e.getMessage());
            astVisualizer.setAST(null);
        }
    }
    
    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            new BeautifulASTTest().setVisible(true);
        });
    }
}
