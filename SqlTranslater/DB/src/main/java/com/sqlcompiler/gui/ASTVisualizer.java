package com.sqlcompiler.gui;

import com.sqlcompiler.ast.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * AST图形化可视化组件
 * 提供直观的树形结构展示
 */
public class ASTVisualizer extends JPanel {
    private ASTNode rootNode;
    private Map<ASTNode, NodeInfo> nodeInfoMap;
    private double scale = 1.0;
    private int offsetX = 0;
    private int offsetY = 0;
    private Point lastMousePos;
    private boolean isDragging = false;
    private ASTNode selectedNode = null;
    
    // 颜色配置
    private static final Color STATEMENT_COLOR = new Color(52, 152, 219);      // 蓝色
    private static final Color EXPRESSION_COLOR = new Color(46, 204, 113);     // 绿色
    private static final Color LITERAL_COLOR = new Color(241, 196, 15);        // 黄色
    private static final Color IDENTIFIER_COLOR = new Color(155, 89, 182);     // 紫色
    private static final Color CLAUSE_COLOR = new Color(230, 126, 34);         // 橙色
    private static final Color SELECTED_COLOR = new Color(231, 76, 60);        // 红色
    private static final Color BACKGROUND_COLOR = new Color(236, 240, 241);    // 浅灰色
    private static final Color LINE_COLOR = new Color(149, 165, 166);          // 深灰色
    
    // 节点尺寸
    private static final int NODE_WIDTH = 120;
    private static final int NODE_HEIGHT = 40;
    private static final int HORIZONTAL_SPACING = 200;
    private static final int VERTICAL_SPACING = 80;
    
    /**
     * 节点信息类
     */
    private static class NodeInfo {
        int x, y;
        String label;
        Color color;
        List<ASTNode> children;
        
        NodeInfo(String label, Color color) {
            this.label = label;
            this.color = color;
            this.children = new ArrayList<>();
        }
    }
    
    public ASTVisualizer() {
        setBackground(BACKGROUND_COLOR);
        setPreferredSize(new Dimension(600, 400));
        
        // 添加鼠标事件监听器
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
                isDragging = true;
                
                // 检查是否点击了节点
                ASTNode clickedNode = getNodeAt(e.getX(), e.getY());
                if (clickedNode != null) {
                    selectedNode = clickedNode;
                    repaint();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // 双击节点显示详细信息
                    ASTNode clickedNode = getNodeAt(e.getX(), e.getY());
                    if (clickedNode != null) {
                        showNodeDetails(clickedNode);
                    }
                }
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    int dx = e.getX() - lastMousePos.x;
                    int dy = e.getY() - lastMousePos.y;
                    offsetX += dx;
                    offsetY += dy;
                    lastMousePos = e.getPoint();
                    repaint();
                }
            }
        });
        
        addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            @Override
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                double scaleFactor = 1.1;
                if (e.getWheelRotation() < 0) {
                    scale *= scaleFactor;
                } else {
                    scale /= scaleFactor;
                }
                scale = Math.max(0.1, Math.min(5.0, scale)); // 限制缩放范围
                repaint();
            }
        });
    }
    
    /**
     * 设置要可视化的AST根节点
     */
    public void setAST(Statement ast) {
        this.rootNode = ast;
        this.nodeInfoMap = new HashMap<>();
        this.selectedNode = null;
        
        if (ast != null) {
            buildNodeInfo(ast);
            layoutNodes();
            // 自动适应窗口大小并居中
            SwingUtilities.invokeLater(() -> {
                fitToWindow();
            });
        }
        
        repaint();
    }
    
    /**
     * 构建节点信息
     */
    private void buildNodeInfo(ASTNode node) {
        if (node == null) return;
        
        String label = getNodeLabel(node);
        Color color = getNodeColor(node);
        NodeInfo info = new NodeInfo(label, color);
        nodeInfoMap.put(node, info);
        
        // 递归处理子节点
        List<ASTNode> children = getChildren(node);
        for (ASTNode child : children) {
            info.children.add(child);
            buildNodeInfo(child);
        }
    }
    
    /**
     * 获取节点标签
     */
    private String getNodeLabel(ASTNode node) {
        if (node instanceof CreateTableStatement) {
            return "CREATE TABLE\n" + ((CreateTableStatement) node).getTableName();
        } else if (node instanceof SelectStatement) {
            return "SELECT";
        } else if (node instanceof InsertStatement) {
            return "INSERT INTO\n" + ((InsertStatement) node).getTableName();
        } else if (node instanceof UpdateStatement) {
            return "UPDATE\n" + ((UpdateStatement) node).getTableName();
        } else if (node instanceof DeleteStatement) {
            return "DELETE FROM\n" + ((DeleteStatement) node).getTableName();
        } else if (node instanceof BinaryExpression) {
            return ((BinaryExpression) node).getOperator().getValue();
        } else if (node instanceof UnaryExpression) {
            return ((UnaryExpression) node).getOperator().getValue();
        } else if (node instanceof LiteralExpression) {
            String value = ((LiteralExpression) node).getValue();
            return value.length() > 10 ? value.substring(0, 10) + "..." : value;
        } else if (node instanceof IdentifierExpression) {
            return ((IdentifierExpression) node).getName();
        } else if (node instanceof DotExpression) {
            DotExpression dot = (DotExpression) node;
            return dot.getTableName() + "." + dot.getFieldName();
        } else if (node instanceof FunctionCallExpression) {
            return ((FunctionCallExpression) node).getFunctionName() + "()";
        } else if (node instanceof WhereClause) {
            return "WHERE";
        } else if (node instanceof OrderByClause) {
            return "ORDER BY";
        } else if (node instanceof GroupByClause) {
            return "GROUP BY";
        } else if (node instanceof HavingClause) {
            return "HAVING";
        } else if (node instanceof LimitClause) {
            return "LIMIT";
        } else if (node instanceof TableReference) {
            return "TABLE\n" + ((TableReference) node).getTableName();
        } else if (node instanceof JoinClause) {
            return "JOIN\n" + ((JoinClause) node).getTableName();
        } else if (node instanceof ColumnDefinition) {
            return "COLUMN\n" + ((ColumnDefinition) node).getColumnName();
        } else {
            return node.getClass().getSimpleName();
        }
    }
    
    /**
     * 获取节点颜色
     */
    private Color getNodeColor(ASTNode node) {
        if (node instanceof Statement) {
            return STATEMENT_COLOR;
        } else if (node instanceof Expression) {
            if (node instanceof LiteralExpression) {
                return LITERAL_COLOR;
            } else if (node instanceof IdentifierExpression || node instanceof DotExpression) {
                return IDENTIFIER_COLOR;
            } else {
                return EXPRESSION_COLOR;
            }
        } else if (node instanceof WhereClause || node instanceof OrderByClause || 
                   node instanceof GroupByClause || node instanceof HavingClause || 
                   node instanceof LimitClause) {
            return CLAUSE_COLOR;
        } else {
            return EXPRESSION_COLOR;
        }
    }
    
    /**
     * 获取子节点列表
     */
    private List<ASTNode> getChildren(ASTNode node) {
        List<ASTNode> children = new ArrayList<>();
        
        try {
            if (node instanceof CreateTableStatement) {
                CreateTableStatement stmt = (CreateTableStatement) node;
                children.addAll(stmt.getColumns());
                children.addAll(stmt.getConstraints());
            } else if (node instanceof SelectStatement) {
                SelectStatement stmt = (SelectStatement) node;
                children.addAll(stmt.getSelectList());
                if (stmt.getFromClause() != null) {
                    children.addAll(stmt.getFromClause());
                }
                if (stmt.getWhereClause() != null) {
                    children.add(stmt.getWhereClause());
                }
                if (stmt.getOrderByClause() != null) {
                    children.add(stmt.getOrderByClause());
                }
                if (stmt.getGroupByClause() != null) {
                    children.add(stmt.getGroupByClause());
                }
                if (stmt.getHavingClause() != null) {
                    children.add(stmt.getHavingClause());
                }
                if (stmt.getLimitClause() != null) {
                    children.add(stmt.getLimitClause());
                }
            } else if (node instanceof InsertStatement) {
                InsertStatement stmt = (InsertStatement) node;
                for (List<Expression> row : stmt.getValues()) {
                    children.addAll(row);
                }
            } else if (node instanceof UpdateStatement) {
                UpdateStatement stmt = (UpdateStatement) node;
                children.addAll(stmt.getSetClause().values());
                if (stmt.getWhereClause() != null) {
                    children.add(stmt.getWhereClause());
                }
            } else if (node instanceof DeleteStatement) {
                DeleteStatement stmt = (DeleteStatement) node;
                if (stmt.getWhereClause() != null) {
                    children.add(stmt.getWhereClause());
                }
            } else if (node instanceof BinaryExpression) {
                BinaryExpression expr = (BinaryExpression) node;
                children.add(expr.getLeft());
                children.add(expr.getRight());
            } else if (node instanceof UnaryExpression) {
                UnaryExpression expr = (UnaryExpression) node;
                children.add(expr.getOperand());
            } else if (node instanceof FunctionCallExpression) {
                FunctionCallExpression expr = (FunctionCallExpression) node;
                children.addAll(expr.getArguments());
            } else if (node instanceof InExpression) {
                InExpression expr = (InExpression) node;
                children.add(expr.getLeft());
                if (expr.isSubquery()) {
                    children.add(expr.getSubquery());
                } else {
                    children.addAll(expr.getValues());
                }
            } else if (node instanceof SubqueryExpression) {
                SubqueryExpression expr = (SubqueryExpression) node;
                children.add(expr.getSubquery());
            } else if (node instanceof WhereClause) {
                WhereClause clause = (WhereClause) node;
                children.add(clause.getCondition());
            } else if (node instanceof OrderByClause) {
                OrderByClause clause = (OrderByClause) node;
                for (OrderByClause.OrderByItem item : clause.getItems()) {
                    children.add(item.getExpression());
                }
            } else if (node instanceof GroupByClause) {
                GroupByClause clause = (GroupByClause) node;
                children.addAll(clause.getExpressions());
            } else if (node instanceof HavingClause) {
                HavingClause clause = (HavingClause) node;
                children.add(clause.getCondition());
            } else if (node instanceof LimitClause) {
                LimitClause clause = (LimitClause) node;
                children.add(clause.getLimit());
                if (clause.getOffset() != null) {
                    children.add(clause.getOffset());
                }
            } else if (node instanceof TableReference) {
                TableReference ref = (TableReference) node;
                if (ref.getJoins() != null) {
                    children.addAll(ref.getJoins());
                }
            } else if (node instanceof JoinClause) {
                JoinClause join = (JoinClause) node;
                children.add(join.getCondition());
            }
        } catch (Exception e) {
            // 忽略访问子节点时的异常
        }
        
        return children;
    }
    
    /**
     * 布局节点位置
     */
    private void layoutNodes() {
        if (rootNode == null) return;
        
        // 使用简单的层次布局算法
        Map<Integer, List<ASTNode>> levels = new HashMap<>();
        calculateLevels(rootNode, 0, levels);
        
        int startX = 50;
        int startY = 50;
        
        for (Map.Entry<Integer, List<ASTNode>> entry : levels.entrySet()) {
            int level = entry.getKey();
            List<ASTNode> nodes = entry.getValue();
            
            int y = startY + level * VERTICAL_SPACING;
            int totalWidth = (nodes.size() - 1) * HORIZONTAL_SPACING;
            int x = startX + (getWidth() - totalWidth) / 2;
            
            for (ASTNode node : nodes) {
                NodeInfo info = nodeInfoMap.get(node);
                if (info != null) {
                    info.x = x;
                    info.y = y;
                    x += HORIZONTAL_SPACING;
                }
            }
        }
    }
    
    /**
     * 计算节点层次
     */
    private void calculateLevels(ASTNode node, int level, Map<Integer, List<ASTNode>> levels) {
        if (node == null) return;
        
        levels.computeIfAbsent(level, k -> new ArrayList<>()).add(node);
        
        List<ASTNode> children = getChildren(node);
        for (ASTNode child : children) {
            calculateLevels(child, level + 1, levels);
        }
    }
    
    /**
     * 获取指定位置的节点
     */
    private ASTNode getNodeAt(int x, int y) {
        for (Map.Entry<ASTNode, NodeInfo> entry : nodeInfoMap.entrySet()) {
            NodeInfo info = entry.getValue();
            int nodeX = (int) (info.x * scale + offsetX);
            int nodeY = (int) (info.y * scale + offsetY);
            
            if (x >= nodeX && x <= nodeX + NODE_WIDTH * scale &&
                y >= nodeY && y <= nodeY + NODE_HEIGHT * scale) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * 显示节点详细信息
     */
    private void showNodeDetails(ASTNode node) {
        NodeInfo info = nodeInfoMap.get(node);
        if (info == null) return;
        
        String details = "节点类型: " + node.getClass().getSimpleName() + "\n" +
                        "标签: " + info.label + "\n" +
                        "位置: (" + info.x + ", " + info.y + ")\n" +
                        "子节点数量: " + info.children.size();
        
        JOptionPane.showMessageDialog(this, details, "节点详细信息", JOptionPane.INFORMATION_MESSAGE);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (rootNode == null || nodeInfoMap == null) {
            // 绘制空状态提示
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 16));
            String message = "暂无AST数据";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(message)) / 2;
            int y = getHeight() / 2;
            g2d.drawString(message, x, y);
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制连接线
        drawConnections(g2d);
        
        // 绘制节点
        drawNodes(g2d);
        
        // 绘制缩放信息
        drawScaleInfo(g2d);
    }
    
    /**
     * 绘制连接线
     */
    private void drawConnections(Graphics2D g2d) {
        g2d.setColor(LINE_COLOR);
        g2d.setStroke(new BasicStroke(2.0f));
        
        for (Map.Entry<ASTNode, NodeInfo> entry : nodeInfoMap.entrySet()) {
            NodeInfo parentInfo = entry.getValue();
            int parentX = (int) (parentInfo.x * scale + offsetX + NODE_WIDTH * scale / 2);
            int parentY = (int) (parentInfo.y * scale + offsetY + NODE_HEIGHT * scale);
            
            for (ASTNode child : parentInfo.children) {
                NodeInfo childInfo = nodeInfoMap.get(child);
                if (childInfo != null) {
                    int childX = (int) (childInfo.x * scale + offsetX + NODE_WIDTH * scale / 2);
                    int childY = (int) (childInfo.y * scale + offsetY);
                    
                    g2d.drawLine(parentX, parentY, childX, childY);
                }
            }
        }
    }
    
    /**
     * 绘制节点
     */
    private void drawNodes(Graphics2D g2d) {
        for (Map.Entry<ASTNode, NodeInfo> entry : nodeInfoMap.entrySet()) {
            ASTNode node = entry.getKey();
            NodeInfo info = entry.getValue();
            
            int x = (int) (info.x * scale + offsetX);
            int y = (int) (info.y * scale + offsetY);
            int width = (int) (NODE_WIDTH * scale);
            int height = (int) (NODE_HEIGHT * scale);
            
            // 选择颜色
            Color nodeColor = (node == selectedNode) ? SELECTED_COLOR : info.color;
            
            // 绘制节点背景
            RoundRectangle2D rect = new RoundRectangle2D.Float(x, y, width, height, 10, 10);
            g2d.setColor(nodeColor);
            g2d.fill(rect);
            
            // 绘制边框
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.draw(rect);
            
            // 绘制文本
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int)(12 * scale)));
            
            // 计算文本位置（居中）
            FontMetrics fm = g2d.getFontMetrics();
            String[] lines = info.label.split("\n");
            int lineHeight = fm.getHeight();
            int totalHeight = lines.length * lineHeight;
            int startY = y + (height - totalHeight) / 2 + fm.getAscent();
            
            for (int i = 0; i < lines.length; i++) {
                int lineWidth = fm.stringWidth(lines[i]);
                int lineX = x + (width - lineWidth) / 2;
                g2d.drawString(lines[i], lineX, startY + i * lineHeight);
            }
        }
    }
    
    /**
     * 绘制缩放信息
     */
    private void drawScaleInfo(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        String scaleText = String.format("缩放: %.1fx", scale);
        g2d.drawString(scaleText, 10, 20);
    }
    
    /**
     * 重置视图
     */
    public void resetView() {
        scale = 1.0;
        offsetX = 0;
        offsetY = 0;
        selectedNode = null;
        repaint();
    }
    
    /**
     * 放大视图
     */
    public void zoomIn() {
        scale *= 1.2;
        scale = Math.max(0.1, Math.min(5.0, scale)); // 限制缩放范围
        repaint();
    }
    
    /**
     * 缩小视图
     */
    public void zoomOut() {
        scale /= 1.2;
        scale = Math.max(0.1, Math.min(5.0, scale)); // 限制缩放范围
        repaint();
    }
    
    /**
     * 适应窗口大小
     */
    public void fitToWindow() {
        if (rootNode == null || nodeInfoMap == null) return;
        
        // 计算所有节点的边界
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        
        for (NodeInfo info : nodeInfoMap.values()) {
            minX = Math.min(minX, info.x);
            maxX = Math.max(maxX, info.x + NODE_WIDTH);
            minY = Math.min(minY, info.y);
            maxY = Math.max(maxY, info.y + NODE_HEIGHT);
        }
        
        // 计算合适的缩放比例
        double scaleX = (double) (getWidth() - 100) / (maxX - minX);
        double scaleY = (double) (getHeight() - 100) / (maxY - minY);
        scale = Math.min(scaleX, scaleY);
        scale = Math.max(0.1, Math.min(2.0, scale)); // 限制缩放范围
        
        // 居中显示
        int contentWidth = (int) ((maxX - minX) * scale);
        int contentHeight = (int) ((maxY - minY) * scale);
        offsetX = (getWidth() - contentWidth) / 2 - (int) (minX * scale);
        offsetY = (getHeight() - contentHeight) / 2 - (int) (minY * scale);
        
        repaint();
    }
}
