# AST可视化功能说明

## 概述

AST（抽象语法树）可视化功能为SparrowDB提供了直观的图形化界面来展示SQL语句的语法结构。相比传统的文本形式AST显示，图形化可视化更加直观、生动，便于理解和调试。

## 功能特性

### 🎨 图形化展示
- **树形结构**：以节点和连接线的形式展示AST的层次结构
- **颜色编码**：不同类型的节点使用不同颜色区分
  - 🔵 蓝色：语句节点（Statement）
  - 🟢 绿色：表达式节点（Expression）
  - 🟡 黄色：字面量节点（Literal）
  - 🟣 紫色：标识符节点（Identifier）
  - 🟠 橙色：子句节点（Clause）
  - 🔴 红色：选中节点

### 🖱️ 交互功能
- **拖拽移动**：鼠标拖拽可以移动整个视图
- **滚轮缩放**：鼠标滚轮可以放大/缩小视图
- **节点选择**：单击节点可以选中并高亮显示
- **详细信息**：双击节点可以查看节点的详细信息

### 🛠️ 控制功能
- **放大按钮（+）**：放大AST视图
- **缩小按钮（-）**：缩小AST视图
- **适应按钮**：自动调整视图以适应窗口大小并居中
- **自动适应**：每次生成AST时自动适应窗口大小

## 使用方法

### 在主界面中使用

1. **输入SQL语句**：在SQL输入区域输入要分析的SQL语句
2. **点击执行SQL按钮**：点击绿色的"执行SQL"按钮
3. **查看图形化AST**：系统会在右下角的"AST可视化"区域直接显示SQL语句的图形化语法树

### 独立测试程序

运行`IntegratedASTTest.java`可以独立测试集成后的AST可视化功能：

```bash
cd SqlTranslater/DB
javac -cp "src/main/java" src/main/java/com/sqlcompiler/gui/IntegratedASTTest.java
java -cp "src/main/java" com.sqlcompiler.gui.IntegratedASTTest
```

## 支持的SQL语句类型

### SELECT语句
```sql
SELECT name, age FROM students WHERE age > 18 ORDER BY name
```
- 显示SELECT列表、FROM子句、WHERE条件、ORDER BY等

### CREATE TABLE语句
```sql
CREATE TABLE users (id INT, name VARCHAR(50), email VARCHAR(100))
```
- 显示表名、列定义、数据类型等

### INSERT语句
```sql
INSERT INTO students (name, age) VALUES ('张三', 20), ('李四', 22)
```
- 显示表名、列名、插入的值等

### UPDATE语句
```sql
UPDATE students SET age = age + 1 WHERE grade = 'A'
```
- 显示表名、SET子句、WHERE条件等

### DELETE语句
```sql
DELETE FROM students WHERE age < 18
```
- 显示表名、WHERE条件等

## 技术实现

### 核心组件

1. **ASTVisualizer**：核心可视化组件
   - 负责绘制AST节点和连接线
   - 处理鼠标交互事件
   - 管理视图的缩放和平移

2. **DatabaseGUI集成**：主界面集成
   - 将AST可视化直接集成到右下角区域
   - 与SQL编译器集成，自动显示AST
   - 错误处理和用户提示

### 布局算法

使用简单的层次布局算法：
- 按AST的层次结构分配节点到不同层级
- 同一层级的节点水平排列
- 自动计算节点间距，避免重叠

### 渲染技术

- 使用Java 2D Graphics API进行绘制
- 支持抗锯齿渲染，提供平滑的视觉效果
- 使用圆角矩形绘制节点，美观大方

## 使用技巧

### 查看大型AST
- 使用底部的放大/缩小按钮调整视图
- 点击"适应"按钮自动调整到最佳大小
- 利用滚轮缩放功能查看细节
- 拖拽移动视图查看不同部分
- 使用滚动条查看超出显示区域的部分

### 理解SQL结构
- 观察节点的颜色来理解不同类型的语法元素
- 双击节点查看详细信息
- 从根节点开始，沿着连接线理解SQL的执行逻辑

### 调试SQL语句
- 当SQL编译失败时，可视化可以帮助理解语法结构
- 通过图形化展示更容易发现语法错误
- 对比不同SQL语句的AST结构

## 注意事项

1. **性能考虑**：对于非常复杂的SQL语句，AST可能包含大量节点，建议使用缩放功能查看
2. **界面布局**：
   - 执行结果占左侧全部区域
   - 右侧分为上下两部分：Token列表在上（占2/5高度），AST可视化在下（占3/5高度）
   - AST可视化区域底部有控制按钮
   - 布局比例优化，为AST可视化提供更多显示空间
3. **自动适应**：每次生成AST时会自动适应窗口大小并居中显示
4. **错误处理**：如果SQL编译失败，会显示错误信息而不是AST可视化

## 未来改进

- [ ] 支持AST节点的折叠/展开功能
- [ ] 添加搜索和过滤功能
- [ ] 支持AST的导出（PNG、SVG格式）
- [ ] 添加动画效果，展示AST的构建过程
- [ ] 支持AST的编辑和修改功能

## 总结

AST可视化功能大大提升了SparrowDB的用户体验，使得SQL语句的语法结构更加直观易懂。无论是学习SQL语法、调试复杂查询，还是理解数据库系统的内部工作原理，这个功能都能提供极大的帮助。
