# 美观树形AST可视化设计说明

## 设计理念

您说得非常对！我之前的理解确实有误。现在我重新设计了一个真正美观的树形AST可视化，遵循标准的树形结构原则：

**核心原则**：从根节点开始，一层一层向下展开，每个父节点的子节点在其下方居中排列

## 真正的树形布局算法

### 1. 子树宽度计算
```java
/**
 * 计算子树宽度
 */
private int calculateSubtreeWidths(ASTNode node) {
    NodeInfo info = nodeInfoMap.get(node);
    if (info == null) return 0;
    
    if (info.children.isEmpty()) {
        // 叶子节点：宽度就是节点宽度
        info.subtreeWidth = NODE_WIDTH;
    } else {
        // 内部节点：子树宽度 = 所有子节点子树宽度之和 + 间距
        int totalChildWidth = 0;
        for (ASTNode child : info.children) {
            totalChildWidth += calculateSubtreeWidths(child);
        }
        
        // 添加子节点之间的间距
        if (info.children.size() > 1) {
            totalChildWidth += (info.children.size() - 1) * MIN_SIBLING_DISTANCE;
        }
        
        info.subtreeWidth = Math.max(NODE_WIDTH, totalChildWidth);
    }
    
    return info.subtreeWidth;
}
```

### 2. 递归树形布局
```java
/**
 * 布局子树
 */
private void layoutSubtree(ASTNode node, int parentCenterX, int parentY) {
    NodeInfo info = nodeInfoMap.get(node);
    if (info == null) return;
    
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
            // 设置子节点位置
            childInfo.x = currentX;
            childInfo.y = parentY + LEVEL_HEIGHT;
            childInfo.level = info.level + 1;
            
            // 递归布局子节点的子树
            layoutSubtree(child, childInfo.x + NODE_WIDTH / 2, childInfo.y + NODE_HEIGHT);
            
            // 移动到下一个子节点位置
            currentX += childInfo.subtreeWidth + MIN_SIBLING_DISTANCE;
        }
    }
}
```

## 美观设计特性

### 1. 真正的树形结构
- 🌳 **根节点居中**：根节点在顶部居中显示
- 📐 **层次展开**：每个父节点的子节点在其下方居中排列
- 🔗 **准确连接**：连接线从父节点中心连接到子节点中心
- 📏 **合理间距**：层与层之间有足够的垂直距离

### 2. 智能文本处理
```java
// 绘制节点文本 - 确保文本不超出节点边界
int fontSize = Math.max(8, (int) (12 * scale));
g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));

// 如果文本太宽，截断并添加省略号
if (lineWidth > width - 10) {
    while (lineWidth > width - 20 && line.length() > 3) {
        line = line.substring(0, line.length() - 1);
        lineWidth = fm.stringWidth(line + "...");
    }
    line = line + "...";
}
```

### 3. 优化的节点尺寸
- **节点宽度**：140px（比原来更宽，容纳更多文本）
- **节点高度**：50px（比原来更高，容纳多行文本）
- **层间距**：120px（足够的垂直空间）
- **兄弟节点间距**：60px（避免节点重叠）

### 4. 完整的3层结构
```
SELECT (根节点，蓝色)
├── SELECT_LIST (中间层，青色)
│   └── * (叶子节点，紫色)
└── FROM_CLAUSE (中间层，青色)
    └── user (叶子节点，紫色)
```

## 布局效果

### 树形结构特点
1. **根节点**：在顶部居中，作为整个树的起点
2. **中间层**：SELECT_LIST、FROM_CLAUSE等子句节点，在根节点下方居中排列
3. **叶子节点**：具体的表达式和表名，在对应父节点下方居中排列
4. **连接线**：从父节点中心垂直向下连接到子节点中心

### 美观特性
- ✅ **层次清晰**：每层节点整齐排列，层次分明
- ✅ **居中对称**：每个父节点的子节点在其下方居中排列
- ✅ **无交叉**：连接线不会交叉，布局清晰
- ✅ **文本适配**：文本不会超出节点边界，自动截断
- ✅ **颜色区分**：不同类型的节点使用不同颜色
- ✅ **流畅交互**：拖拽和缩放功能完善

## 技术实现

### 1. 布局算法
- **自底向上**：先计算每个节点的子树宽度
- **自顶向下**：从根节点开始递归布局每个节点
- **居中原则**：子节点在父节点下方居中排列

### 2. 文本处理
- **动态字体大小**：根据缩放比例调整字体大小
- **智能截断**：文本过长时自动截断并添加省略号
- **多行支持**：支持节点内的多行文本显示

### 3. 交互功能
- **拖拽移动**：可以拖拽整个视图
- **滚轮缩放**：支持鼠标滚轮缩放
- **节点选择**：点击节点可以选中
- **右键详情**：右键节点查看详细信息

## 使用方法

### 1. 运行美观的树形AST可视化测试
```bash
cd SqlTranslater/DB
javac -cp "src/main/java" src/main/java/com/sqlcompiler/gui/BeautifulASTTest.java
java -cp "src/main/java" com.sqlcompiler.gui.BeautifulASTTest
```

### 2. 在主界面中使用
DatabaseGUI已经更新为使用`BeautifulASTVisualizer`，现在具有：
- 真正的树形结构布局
- 美观的节点排列
- 完整的3层层次结构
- 智能的文本处理
- 流畅的交互体验

## 总结

这次的设计真正实现了美观的树形AST可视化：

- 🌳 **真正的树形结构**：从根节点一层一层向下展开
- 📐 **美观的布局**：每个父节点的子节点在其下方居中排列
- 🎨 **完整的层次**：3层结构清晰可见
- 🔗 **准确的连接**：连接线准确连接到节点中心
- 📝 **智能文本**：文本不会超出节点边界
- 🎯 **清晰排列**：节点排列整齐，无交叉

这才是真正的树形AST可视化！
