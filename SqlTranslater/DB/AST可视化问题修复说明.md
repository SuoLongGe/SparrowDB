# AST可视化问题修复说明

## 修复的问题

您指出的两个关键问题已经修复：

### 1. 文字超出节点范围问题

**问题**：当缩放很小时，文字会暴露在节点范围外，很丑

**修复方案**：
```java
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
        // ... 绘制文字逻辑
        
        // 检查文字是否在节点范围内
        if (lineY >= y + 5 && lineY <= y + height - 5) {
            g2d.drawString(line, lineX, lineY);
        }
    }
}
```

**修复效果**：
- ✅ 只有当节点足够大时才显示文字
- ✅ 文字完全在节点范围内，不会超出边界
- ✅ 当缩放很小时，文字会消失而不是暴露在外
- ✅ 文字位置经过精确计算，确保在节点内

### 2. 节点重叠问题

**问题**：节点之间会有重叠，影响美观

**修复方案**：

#### 增加节点间距
```java
private static final int MIN_SIBLING_DISTANCE = 80;  // 兄弟节点之间的最小距离（增加避免重叠）
```

#### 添加重叠检测和修复算法
```java
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
```

**修复效果**：
- ✅ 节点之间有足够的间距，不会重叠
- ✅ 自动检测和修复任何重叠的节点
- ✅ 保持树形结构的完整性
- ✅ 美观的节点排列

## 修复后的特性

### 1. 智能文字显示
- **条件显示**：只有当节点足够大时才显示文字
- **边界检查**：文字完全在节点范围内
- **动态消失**：当缩放很小时，文字会消失而不是暴露在外
- **精确位置**：文字位置经过精确计算

### 2. 无重叠布局
- **增加间距**：兄弟节点之间的最小距离增加到80px
- **重叠检测**：自动检测每一层的节点重叠
- **自动修复**：自动移动重叠的节点到合适位置
- **递归调整**：调整一个节点时，会递归调整后续节点

### 3. 美观的树形结构
- **真正的树形**：从根节点一层一层向下展开
- **居中排列**：子节点在父节点下方居中排列
- **清晰层次**：每层节点整齐排列，无重叠
- **准确连接**：连接线准确连接到节点中心

## 技术实现

### 文字显示控制
```java
// 只有当节点足够大时才显示文字
if (width > 20 && height > 15) {
    // 检查是否有足够空间显示文字
    if (totalTextHeight <= height - 8 && lineHeight > 0) {
        // 检查文字是否在节点范围内
        if (lineY >= y + 5 && lineY <= y + height - 5) {
            g2d.drawString(line, lineX, lineY);
        }
    }
}
```

### 重叠检测算法
```java
// 检查是否重叠
int prevRight = prevInfo.x + NODE_WIDTH;
int currLeft = currInfo.x;

if (prevRight > currLeft) {
    // 有重叠，移动当前节点
    int overlap = prevRight - currLeft;
    int newX = prevRight + MIN_SIBLING_DISTANCE;
    currInfo.x = newX;
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
- 智能的文字显示控制
- 无重叠的节点布局
- 美观的树形结构
- 流畅的交互体验

## 总结

修复后的AST可视化现在具有：

- ✅ **智能文字显示**：文字不会超出节点范围，缩放小时会消失
- ✅ **无重叠布局**：节点之间有足够间距，不会重叠
- ✅ **美观的树形结构**：真正的树形布局，层次清晰
- ✅ **准确的连接线**：连接线准确连接到节点中心
- ✅ **流畅的交互**：拖拽和缩放功能完善

现在AST可视化既美观又实用！
