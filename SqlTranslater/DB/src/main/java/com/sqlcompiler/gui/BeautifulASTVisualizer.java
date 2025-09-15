package com.sqlcompiler.gui;

import com.sqlcompiler.ast.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Cursor;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * 美观的AST图形化可视化组件
 * 真正的树形结构：从根节点一层一层向下展开
 */
public class BeautifulASTVisualizer extends JPanel {
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
    private static final Color INTERMEDIATE_COLOR = new Color(26, 188, 156);   // 青色 - 中间层节点
    
    // 节点尺寸和间距
    private static final int NODE_WIDTH = 140;
    private static final int NODE_HEIGHT = 50;
    private static final int LEVEL_HEIGHT = 120;  // 层与层之间的垂直距离
    private static final int MIN_SIBLING_DISTANCE = 80;  // 兄弟节点之间的最小距离（增加避免重叠）
    
    /**
     * 节点信息类
     */
    private static class NodeInfo {
        int x, y;
        String label;
        Color color;
        List<ASTNode> children;
        int subtreeWidth;  // 子树宽度
        int level;         // 节点层次
        
        NodeInfo(String label, Color color) {
            this.label = label;
            this.color = color;
            this.children = new ArrayList<>();
            this.subtreeWidth = 0;
            this.level = 0;
        }
    }
    
    public BeautifulASTVisualizer() {
        setBackground(BACKGROUND_COLOR);
        setPreferredSize(new Dimension(1000, 800));
        
        // 添加鼠标事件监听器
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    lastMousePos = e.getPoint();
                    isDragging = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    
                    // 检查是否点击了节点
                    ASTNode clickedNode = getNodeAt(e.getPoint());
                    if (clickedNode != null) {
                        selectedNode = clickedNode;
                        repaint();
                    }
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    isDragging = false;
                    setCursor(Cursor.getDefaultCursor());
                }
                
                if (SwingUtilities.isRightMouseButton(e)) {
                    // 右键显示详细信息
                    ASTNode clickedNode = getNodeAt(e.getPoint());
                    if (clickedNode != null) {
                        showNodeDetails(clickedNode);
                    }
                }
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging && lastMousePos != null) {
                    int deltaX = e.getX() - lastMousePos.x;
                    int deltaY = e.getY() - lastMousePos.y;
                    
                    offsetX += deltaX;
                    offsetY += deltaY;
                    
                    lastMousePos = e.getPoint();
                    repaint();
                }
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        
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
        
        // 添加组件大小改变监听器，确保在窗口大小改变时重新居中
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (rootNode != null && nodeInfoMap != null && !nodeInfoMap.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        fitToWindow();
                    });
                }
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
            buildEnhancedNodeInfo(ast);
            layoutTreeNodes();
            // 自动适应窗口大小并居中
            SwingUtilities.invokeLater(() -> {
                fitToWindow();
            });
        } else {
            // 如果没有AST，重置视图
            resetView();
        }
        
        repaint();
    }
    
    /**
     * 构建增强的节点信息，包含中间层节点
     */
    private void buildEnhancedNodeInfo(ASTNode node) {
        if (node == null) return;
        
        String label = getNodeLabel(node);
        Color color = getNodeColor(node);
        NodeInfo info = new NodeInfo(label, color);
        nodeInfoMap.put(node, info);
        
        // 递归处理子节点，为SELECT语句创建中间层节点
        List<ASTNode> children = getEnhancedChildren(node);
        for (ASTNode child : children) {
            info.children.add(child);
            buildEnhancedNodeInfo(child);
        }
    }
    
    /**
     * 获取增强的子节点列表，包含中间层节点
     */
    private List<ASTNode> getEnhancedChildren(ASTNode node) {
        List<ASTNode> children = new ArrayList<>();
        
        try {
            if (node instanceof SelectStatement) {
                SelectStatement stmt = (SelectStatement) node;
                
                // 创建SELECT_LIST中间层节点
                if (stmt.getSelectList() != null && !stmt.getSelectList().isEmpty()) {
                    SelectListClause selectListClause = new SelectListClause(
                        stmt.getSelectList(), 
                        stmt.getPosition()
                    );
                    children.add(selectListClause);
                }
                
                // 创建FROM_CLAUSE中间层节点
                if (stmt.getFromClause() != null && !stmt.getFromClause().isEmpty()) {
                    FromClause fromClause = new FromClause(
                        stmt.getFromClause(), 
                        stmt.getPosition()
                    );
                    children.add(fromClause);
                }
                
                // 添加其他子句
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
            } else if (node instanceof SelectListClause) {
                // SELECT_LIST的子节点是具体的表达式
                SelectListClause clause = (SelectListClause) node;
                children.addAll(clause.getExpressions());
            } else if (node instanceof FromClause) {
                // FROM_CLAUSE的子节点是表引用
                FromClause clause = (FromClause) node;
                children.addAll(clause.getTableReferences());
            } else {
                // 其他节点使用原有的逻辑
                children.addAll(getOriginalChildren(node));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return children;
    }
    
    /**
     * 获取原有的子节点列表（用于非SELECT语句）
     */
    private List<ASTNode> getOriginalChildren(ASTNode node) {
        List<ASTNode> children = new ArrayList<>();
        
        try {
            if (node instanceof BatchStatement) {
                BatchStatement batch = (BatchStatement) node;
                children.addAll(batch.getStatements());
            } else if (node instanceof CreateTableStatement) {
                CreateTableStatement stmt = (CreateTableStatement) node;
                children.addAll(stmt.getColumns());
                children.addAll(stmt.getConstraints());
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
                // 表引用通常没有子节点
            } else if (node instanceof JoinClause) {
                JoinClause join = (JoinClause) node;
                if (join.getCondition() != null) {
                    children.add(join.getCondition());
                }
            } else if (node instanceof ColumnDefinition) {
                // 列定义通常没有子节点
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return children;
    }
    
    /**
     * 获取节点标签
     */
    private String getNodeLabel(ASTNode node) {
        if (node instanceof BatchStatement) {
            BatchStatement batch = (BatchStatement) node;
            return "BATCH\n" + batch.getStatementCount() + " statements";
        } else if (node instanceof CreateTableStatement) {
            return "CREATE TABLE\n" + ((CreateTableStatement) node).getTableName();
        } else if (node instanceof SelectStatement) {
            return "SELECT";
        } else if (node instanceof SelectListClause) {
            return "SELECT_LIST";
        } else if (node instanceof FromClause) {
            return "FROM_CLAUSE";
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
            return value.length() > 12 ? value.substring(0, 12) + "..." : value;
        } else if (node instanceof IdentifierExpression) {
            String name = ((IdentifierExpression) node).getName();
            return name.length() > 12 ? name.substring(0, 12) + "..." : name;
        } else if (node instanceof DotExpression) {
            DotExpression dot = (DotExpression) node;
            String text = dot.getTableName() + "." + dot.getFieldName();
            return text.length() > 12 ? text.substring(0, 12) + "..." : text;
        } else if (node instanceof FunctionCallExpression) {
            String name = ((FunctionCallExpression) node).getFunctionName();
            return name.length() > 10 ? name.substring(0, 10) + "..." : name + "()";
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
            String name = ((TableReference) node).getTableName();
            return "TABLE\n" + (name.length() > 10 ? name.substring(0, 10) + "..." : name);
        } else if (node instanceof JoinClause) {
            String name = ((JoinClause) node).getTableName();
            return "JOIN\n" + (name.length() > 10 ? name.substring(0, 10) + "..." : name);
        } else if (node instanceof ColumnDefinition) {
            String name = ((ColumnDefinition) node).getColumnName();
            return "COLUMN\n" + (name.length() > 10 ? name.substring(0, 10) + "..." : name);
        } else {
            String name = node.getClass().getSimpleName();
            return name.length() > 12 ? name.substring(0, 12) + "..." : name;
        }
    }
    
    /**
     * 获取节点颜色
     */
    private Color getNodeColor(ASTNode node) {
        if (node instanceof Statement) {
            return STATEMENT_COLOR;
        } else if (node instanceof SelectListClause || node instanceof FromClause) {
            return INTERMEDIATE_COLOR; // 中间层节点使用青色
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
     * 获取指定位置的节点
     */
    private ASTNode getNodeAt(Point point) {
        for (Map.Entry<ASTNode, NodeInfo> entry : nodeInfoMap.entrySet()) {
            NodeInfo info = entry.getValue();
            int scaledX = (int) (info.x * scale + offsetX);
            int scaledY = (int) (info.y * scale + offsetY);
            int scaledWidth = (int) (NODE_WIDTH * scale);
            int scaledHeight = (int) (NODE_HEIGHT * scale);
            
            if (point.x >= scaledX && point.x <= scaledX + scaledWidth &&
                point.y >= scaledY && point.y <= scaledY + scaledHeight) {
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
    
    /**
     * 树形布局算法 - 真正的树形结构
     */
    private void layoutTreeNodes() {
        if (rootNode == null) return;
        
        // 1. 计算每个节点的子树宽度
        calculateSubtreeWidths(rootNode);
        
        // 2. 从根节点开始，递归布局每个节点
        NodeInfo rootInfo = nodeInfoMap.get(rootNode);
        if (rootInfo != null) {
            rootInfo.x = getWidth() / 2 - NODE_WIDTH / 2;  // 根节点居中
            rootInfo.y = 50;
            rootInfo.level = 0;
            
            layoutSubtree(rootNode, rootInfo.x + NODE_WIDTH / 2, rootInfo.y + NODE_HEIGHT);
        }
        
        // 3. 后处理：检测和修复重叠的节点
        fixOverlappingNodes();
    }
    
    /**
     * 计算子树宽度
     */
    private int calculateSubtreeWidths(ASTNode node) {
        NodeInfo info = nodeInfoMap.get(node);
        if (info == null) return 0;
        
        if (info.children.isEmpty()) {
            // 叶子节点
            info.subtreeWidth = NODE_WIDTH;
        } else {
            // 内部节点：子树宽度 = 所有子节点子树宽度之和 + 间距
            int totalChildWidth = 0;
            for (ASTNode child : info.children) {
                totalChildWidth += calculateSubtreeWidths(child);
            }
            
            // 添加子节点之间的间距，确保不会重叠
            if (info.children.size() > 1) {
                totalChildWidth += (info.children.size() - 1) * MIN_SIBLING_DISTANCE;
            }
            
            // 确保子树宽度至少是节点宽度
            info.subtreeWidth = Math.max(NODE_WIDTH, totalChildWidth);
        }
        
        return info.subtreeWidth;
    }
    
    /**
     * 布局子树
     */
    private void layoutSubtree(ASTNode node, int parentCenterX, int parentY) {
        NodeInfo info = nodeInfoMap.get(node);
        if (info == null) return;
        
        // 设置当前节点的层次
        info.level = getNodeLevel(node);
        
        if (info.children.isEmpty()) {
            // 叶子节点：位置已经由父节点确定
            return;
        }
        
        // 计算子节点的起始X位置（使子节点在父节点下方居中）
        int childStartX = parentCenterX - info.subtreeWidth / 2;
        
        // 布局每个子节点
        int currentX = childStartX;
        for (ASTNode child : info.children) {
            NodeInfo childInfo = nodeInfoMap.get(child);
            if (childInfo != null) {
                // 设置子节点位置，确保不重叠
                childInfo.x = currentX;
                childInfo.y = parentY + LEVEL_HEIGHT;
                childInfo.level = info.level + 1;
                
                // 递归布局子节点的子树
                layoutSubtree(child, childInfo.x + NODE_WIDTH / 2, childInfo.y + NODE_HEIGHT);
                
                // 移动到下一个子节点位置，确保有足够间距
                currentX += childInfo.subtreeWidth + MIN_SIBLING_DISTANCE;
            }
        }
    }
    
    /**
     * 获取节点层次
     */
    private int getNodeLevel(ASTNode node) {
        int level = 0;
        ASTNode current = node;
        
        // 向上遍历到根节点
        while (current != null && current != rootNode) {
            // 查找父节点
            for (Map.Entry<ASTNode, NodeInfo> entry : nodeInfoMap.entrySet()) {
                if (entry.getValue().children.contains(current)) {
                    current = entry.getKey();
                    level++;
                    break;
                }
            }
            if (current == node) break; // 防止无限循环
        }
        
        return level;
    }
    
    /**
     * 检测和修复重叠的节点
     */
    private void fixOverlappingNodes() {
        // 按层次分组节点
        Map<Integer, List<ASTNode>> nodesByLevel = new HashMap<>();
        for (Map.Entry<ASTNode, NodeInfo> entry : nodeInfoMap.entrySet()) {
            NodeInfo info = entry.getValue();
            nodesByLevel.computeIfAbsent(info.level, k -> new ArrayList<>()).add(entry.getKey());
        }
        
        // 对每一层检查重叠
        for (Map.Entry<Integer, List<ASTNode>> entry : nodesByLevel.entrySet()) {
            List<ASTNode> nodes = entry.getValue();
            if (nodes.size() <= 1) continue;
            
            // 按X坐标排序
            nodes.sort((a, b) -> {
                NodeInfo infoA = nodeInfoMap.get(a);
                NodeInfo infoB = nodeInfoMap.get(b);
                return Integer.compare(infoA.x, infoB.x);
            });
            
            // 检查并修复重叠
            for (int i = 1; i < nodes.size(); i++) {
                NodeInfo prevInfo = nodeInfoMap.get(nodes.get(i - 1));
                NodeInfo currInfo = nodeInfoMap.get(nodes.get(i));
                
                // 检查是否重叠
                int prevRight = prevInfo.x + NODE_WIDTH;
                int currLeft = currInfo.x;
                
                if (prevRight > currLeft) {
                    // 有重叠，移动当前节点
                    int overlap = prevRight - currLeft;
                    int newX = prevRight + MIN_SIBLING_DISTANCE;
                    currInfo.x = newX;
                    
                    // 递归调整后续节点
                    for (int j = i + 1; j < nodes.size(); j++) {
                        NodeInfo nextInfo = nodeInfoMap.get(nodes.get(j));
                        nextInfo.x += overlap + MIN_SIBLING_DISTANCE;
                    }
                }
            }
        }
    }
    
    /**
     * 适应窗口大小
     */
    public void fitToWindow() {
        if (nodeInfoMap == null || nodeInfoMap.isEmpty()) {
            // 如果没有节点，重置到默认状态
            scale = 1.0;
            offsetX = 0;
            offsetY = 0;
            return;
        }
        
        // 计算所有节点的边界
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        
        for (NodeInfo info : nodeInfoMap.values()) {
            minX = Math.min(minX, info.x);
            minY = Math.min(minY, info.y);
            maxX = Math.max(maxX, info.x + NODE_WIDTH);
            maxY = Math.max(maxY, info.y + NODE_HEIGHT);
        }
        
        // 计算缩放比例，留出边距
        int margin = 80;
        int availableWidth = getWidth() - margin * 2;
        int availableHeight = getHeight() - margin * 2;
        
        if (availableWidth > 0 && availableHeight > 0) {
            double scaleX = (double) availableWidth / (maxX - minX);
            double scaleY = (double) availableHeight / (maxY - minY);
            scale = Math.min(scaleX, scaleY);
            scale = Math.max(0.1, Math.min(1.0, scale)); // 限制缩放范围
        } else {
            scale = 1.0;
        }
        
        // 计算偏移量以居中显示
        int contentWidth = (int) ((maxX - minX) * scale);
        int contentHeight = (int) ((maxY - minY) * scale);
        offsetX = (getWidth() - contentWidth) / 2 - (int) (minX * scale);
        offsetY = (getHeight() - contentHeight) / 2 - (int) (minY * scale);
        
        repaint();
    }
    
    /**
     * 重置视图
     */
    public void resetView() {
        scale = 1.0;
        offsetX = 0;
        offsetY = 0;
        selectedNode = null;
        // 如果有AST，自动适应窗口大小
        if (rootNode != null && nodeInfoMap != null && !nodeInfoMap.isEmpty()) {
            fitToWindow();
        } else {
            repaint();
        }
    }
    
    /**
     * 手动居中视图
     */
    public void centerView() {
        if (rootNode != null && nodeInfoMap != null && !nodeInfoMap.isEmpty()) {
            fitToWindow();
        }
    }
    
    /**
     * 放大视图
     */
    public void zoomIn() {
        scale *= 1.2;
        scale = Math.min(5.0, scale);
        repaint();
    }
    
    /**
     * 缩小视图
     */
    public void zoomOut() {
        scale /= 1.2;
        scale = Math.max(0.1, scale);
        repaint();
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
            
            // 绘制节点边框
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.draw(rect);
            
            // 绘制节点文本 - 确保文本不超出节点边界
            // 只有当节点足够大时才显示文字
            if (width > 20 && height > 15) {
                g2d.setColor(Color.WHITE);
                int fontSize = Math.max(6, (int) (12 * scale));
                g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
                FontMetrics fm = g2d.getFontMetrics();
                
                String[] lines = info.label.split("\n");
                int lineHeight = fm.getHeight();
                int totalTextHeight = lines.length * lineHeight;
                
                // 检查是否有足够空间显示文字
                if (totalTextHeight <= height - 8 && lineHeight > 0) {
                    int startY = y + (height - totalTextHeight) / 2 + fm.getAscent();
                    
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i];
                        int lineWidth = fm.stringWidth(line);
                        
                        // 如果文本太宽，截断并添加省略号
                        if (lineWidth > width - 10) {
                            while (lineWidth > width - 20 && line.length() > 3) {
                                line = line.substring(0, line.length() - 1);
                                lineWidth = fm.stringWidth(line + "...");
                            }
                            line = line + "...";
                        }
                        
                        // 确保文字在节点范围内
                        int lineX = x + (width - fm.stringWidth(line)) / 2;
                        int lineY = startY + i * lineHeight;
                        
                        // 检查文字是否在节点范围内
                        if (lineY >= y + 5 && lineY <= y + height - 5) {
                            g2d.drawString(line, lineX, lineY);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 绘制缩放信息
     */
    private void drawScaleInfo(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        String scaleText = String.format("缩放: %.1f%%", scale * 100);
        g2d.drawString(scaleText, 10, 20);
        
        // 添加操作提示
        String hintText = "拖拽移动视图 | 滚轮缩放 | 右键查看详情";
        g2d.setColor(Color.GRAY);
        g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        g2d.drawString(hintText, 10, getHeight() - 10);
    }
}
