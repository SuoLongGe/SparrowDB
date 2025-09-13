package com.sqlcompiler.gui;

import com.sqlcompiler.EnhancedSQLCompiler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * AST可视化测试程序
 * 用于测试AST图形化显示功能
 */
public class ASTVisualizationTest extends JFrame {
    private ASTVisualizer visualizer;
    private JTextArea sqlInputArea;
    private JButton visualizeButton;
    private JButton testButton;
    private EnhancedSQLCompiler compiler;
    
    public ASTVisualizationTest() {
        // 初始化SQL编译器
        compiler = new EnhancedSQLCompiler();
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initializeComponents() {
        setTitle("AST可视化测试 - SparrowDB");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // SQL输入区域
        sqlInputArea = new JTextArea(5, 50);
        sqlInputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        sqlInputArea.setBorder(BorderFactory.createTitledBorder("SQL语句"));
        sqlInputArea.setText("SELECT name, age FROM students WHERE age > 18 ORDER BY name");
        
        // AST可视化器
        visualizer = new ASTVisualizer();
        
        // 按钮
        visualizeButton = new JButton("可视化AST");
        visualizeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        visualizeButton.setBackground(new Color(76, 175, 80));
        visualizeButton.setForeground(Color.WHITE);
        
        testButton = new JButton("测试示例");
        testButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        testButton.setBackground(new Color(33, 150, 243));
        testButton.setForeground(Color.WHITE);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 顶部：SQL输入和按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane sqlScrollPane = new JScrollPane(sqlInputArea);
        topPanel.add(sqlScrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(visualizeButton);
        buttonPanel.add(testButton);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        
        // 中央：AST可视化区域
        JScrollPane visualizerScrollPane = new JScrollPane(visualizer);
        visualizerScrollPane.setBorder(BorderFactory.createTitledBorder("AST树形结构"));
        add(visualizerScrollPane, BorderLayout.CENTER);
        
        // 设置窗口属性
        setSize(1000, 700);
        setLocationRelativeTo(null);
    }
    
    private void setupEventHandlers() {
        visualizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                visualizeCurrentSQL();
            }
        });
        
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showTestExamples();
            }
        });
    }
    
    private void visualizeCurrentSQL() {
        String sql = sqlInputArea.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入SQL语句", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // 使用SQL编译器来解析SQL并生成AST
            EnhancedSQLCompiler.CompilationResult result = compiler.compile(sql);
            
            if (result.isSuccess() && result.getStatement() != null) {
                visualizer.setAST(result.getStatement());
                JOptionPane.showMessageDialog(this, "AST可视化已更新", "成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                String errorMsg = "SQL编译失败";
                if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                    errorMsg += ":\n" + String.join("\n", result.getErrors());
                }
                JOptionPane.showMessageDialog(this, errorMsg, "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "可视化失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showTestExamples() {
        String[] examples = {
            "SELECT name, age FROM students WHERE age > 18",
            "CREATE TABLE users (id INT, name VARCHAR(50), email VARCHAR(100))",
            "INSERT INTO students (name, age) VALUES ('张三', 20), ('李四', 22)",
            "UPDATE students SET age = age + 1 WHERE grade = 'A'",
            "DELETE FROM students WHERE age < 18"
        };
        
        String selected = (String) JOptionPane.showInputDialog(
            this,
            "选择一个测试示例:",
            "测试示例",
            JOptionPane.QUESTION_MESSAGE,
            null,
            examples,
            examples[0]
        );
        
        if (selected != null) {
            sqlInputArea.setText(selected);
        }
    }
    
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ASTVisualizationTest().setVisible(true);
            }
        });
    }
}
