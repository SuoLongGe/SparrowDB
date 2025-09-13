import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 测试选中执行功能
 */
public class TestSelectedExecution extends JFrame {
    private JTextPane textPane;
    private JButton executeSelectedButton;
    private JTextArea resultArea;
    
    public TestSelectedExecution() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
    private void initializeComponents() {
        setTitle("选中执行功能测试");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // 创建文本输入区域
        textPane = new JTextPane();
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textPane.setText("SELECT * FROM users;\n\nSELECT * FROM products;\n\nSELECT COUNT(*) FROM users;");
        
        // 创建执行选中按钮
        executeSelectedButton = new JButton("执行选中");
        executeSelectedButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        executeSelectedButton.setBackground(new Color(52, 152, 219));
        executeSelectedButton.setForeground(Color.WHITE);
        
        // 创建结果显示区域
        resultArea = new JTextArea(10, 50);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setEditable(false);
        resultArea.setBackground(Color.WHITE);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 顶部：输入区域
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("SQL输入区域"));
        
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        topPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(executeSelectedButton);
        
        JLabel instructionLabel = new JLabel("请选中要执行的SQL语句，然后点击'执行选中'按钮");
        instructionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        instructionLabel.setForeground(Color.BLUE);
        buttonPanel.add(instructionLabel);
        
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);
        
        // 底部：结果显示区域
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("执行结果"));
        
        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        bottomPanel.add(resultScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.CENTER);
        
        // 设置窗口大小
        setSize(800, 600);
        setLocationRelativeTo(null);
    }
    
    private void setupEventHandlers() {
        executeSelectedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeSelectedSQL();
            }
        });
    }
    
    private void executeSelectedSQL() {
        String selectedText = textPane.getSelectedText();
        if (selectedText == null || selectedText.trim().isEmpty()) {
            resultArea.append("请先选中要执行的SQL语句\n");
            return;
        }
        
        // 临时保存原始文本
        String originalText = textPane.getText();
        int selectionStart = textPane.getSelectionStart();
        int selectionEnd = textPane.getSelectionEnd();
        
        // 设置选中的文本为当前文本
        textPane.setText(selectedText.trim());
        
        // 模拟执行SQL
        resultArea.append("=== 执行选中的SQL ===\n");
        resultArea.append("选中的SQL: " + selectedText.trim() + "\n");
        resultArea.append("执行时间: " + System.currentTimeMillis() % 1000 + " ms\n");
        resultArea.append("执行结果: 模拟执行成功\n\n");
        
        // 恢复原始文本
        textPane.setText(originalText);
        
        // 恢复选中状态
        textPane.setSelectionStart(selectionStart);
        textPane.setSelectionEnd(selectionEnd);
        
        // 滚动到底部
        resultArea.setCaretPosition(resultArea.getDocument().getLength());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new TestSelectedExecution().setVisible(true);
            }
        });
    }
}
