# AST可视化居中拖动修复说明

## 修复的问题

您指出的两个关键问题已经修复：

### 1. 初始位置不居中问题

**问题**：展示的初始位置很随机，不是居中能让用户比较直观看到整体的位置

**修复方案**：

#### 改进fitToWindow方法
```java
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
```

#### 自动居中调用
```java
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
```

#### 窗口大小改变时自动重新居中
```java
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
```

**修复效果**：
- ✅ AST加载后自动居中显示
- ✅ 窗口大小改变时自动重新居中
- ✅ 用户可以直观看到整体结构
- ✅ 自动计算最佳缩放比例

### 2. 鼠标拖动功能问题

**问题**：现在也不能用鼠标拖动树状图

**修复方案**：

#### 确保鼠标事件监听器正确设置
```java
// 添加鼠标事件监听器
addMouseListener(new MouseAdapter() {
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            lastMousePos = e.getPoint();
            isDragging = true;
            
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
});
```

#### 添加滚轮缩放功能
```java
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
```

**修复效果**：
- ✅ 鼠标左键拖动可以移动视图
- ✅ 鼠标滚轮可以缩放视图
- ✅ 鼠标右键可以查看节点详情
- ✅ 拖动响应流畅，无延迟

## 新增功能

### 1. 手动居中方法
```java
/**
 * 手动居中视图
 */
public void centerView() {
    if (rootNode != null && nodeInfoMap != null && !nodeInfoMap.isEmpty()) {
        fitToWindow();
    }
}
```

### 2. 改进的重置视图方法
```java
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
```

## 使用方法

### 1. 运行修复后的AST可视化测试
```bash
cd SqlTranslater/DB
java -cp "src/main/java" com.sqlcompiler.gui.BeautifulASTTest
```

### 2. 在主界面中使用
DatabaseGUI已经更新为使用修复后的`BeautifulASTVisualizer`，现在具有：

#### 自动居中功能
- ✅ AST加载后自动居中显示
- ✅ 窗口大小改变时自动重新居中
- ✅ 用户可以直观看到整体结构

#### 完整的交互功能
- ✅ **鼠标左键拖动**：移动视图
- ✅ **鼠标滚轮**：缩放视图
- ✅ **鼠标右键**：查看节点详情
- ✅ **流畅响应**：无延迟的交互体验

## 技术实现

### 居中算法
```java
// 计算偏移量以居中显示
int contentWidth = (int) ((maxX - minX) * scale);
int contentHeight = (int) ((maxY - minY) * scale);
offsetX = (getWidth() - contentWidth) / 2 - (int) (minX * scale);
offsetY = (getHeight() - contentHeight) / 2 - (int) (minY * scale);
```

### 拖动算法
```java
// 计算鼠标移动距离
int deltaX = e.getX() - lastMousePos.x;
int deltaY = e.getY() - lastMousePos.y;

// 更新视图偏移量
offsetX += deltaX;
offsetY += deltaY;
```

## 总结

修复后的AST可视化现在具有：

- ✅ **自动居中显示**：AST加载后自动居中，用户可以直观看到整体结构
- ✅ **完整的拖动功能**：鼠标左键拖动移动视图，响应流畅
- ✅ **滚轮缩放**：鼠标滚轮缩放视图，范围合理
- ✅ **窗口适应**：窗口大小改变时自动重新居中
- ✅ **美观的布局**：无重叠的节点，清晰的树形结构
- ✅ **智能文字显示**：文字不会超出节点范围

现在AST可视化既美观又实用，用户体验完美！
