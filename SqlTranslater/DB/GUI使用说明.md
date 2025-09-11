# SparrowDB GUI界面使用说明

## 概述

SparrowDB GUI是一个基于Java Swing的图形用户界面，为SparrowDB迷你数据库提供友好的操作界面。该界面包含SQL输入、结果显示、Token分析和AST显示等功能。

## 界面布局

### 1. 顶部区域 - SQL输入
- **SQL输入区域**：用于输入SQL语句
- **执行按钮**：执行输入的SQL语句
- **清空按钮**：清空所有显示区域
- **查看目录按钮**：显示数据库目录信息
- **状态标签**：显示当前操作状态

### 2. 底部左侧 - 执行结果
- 显示SQL执行结果
- 显示错误信息
- 显示编译状态

### 3. 底部右侧 - 分析信息
- **Token列表**：显示SQL语句的词法分析结果
- **AST结构**：显示抽象语法树和执行计划

## 功能特性

### 支持的SQL语句
- `CREATE TABLE`：创建表
- `INSERT INTO`：插入数据
- `SELECT`：查询数据
- `DELETE FROM`：删除数据

### 特殊命令
- `catalog`：查看数据库目录信息
- `clear`：清空数据库目录
- `exit`：退出程序

### 快捷键
- `Ctrl + Enter`：执行SQL语句

## 运行方法

### 方法一：使用脚本运行（推荐）

#### Windows系统
```bash
# 双击运行或在命令行执行
run_gui.bat
```

#### Linux/Mac系统
```bash
# 在终端执行
./run_gui.sh
```

### 方法二：手动编译运行

#### 1. 编译Java文件
```bash
# 进入项目目录
cd SqlTranslater/DB

# 编译GUI相关文件
javac -cp "target/classes" -d target/classes src/main/java/com/sqlcompiler/DatabaseGUI.java src/main/java/com/sqlcompiler/EnhancedSQLCompiler.java
```

#### 2. 运行GUI程序
```bash
java -cp "target/classes" com.sqlcompiler.DatabaseGUI
```

## 使用示例

### 1. 创建表
```sql
CREATE TABLE students (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    age INT
);
```

### 2. 插入数据
```sql
INSERT INTO students VALUES (1, '张三', 20);
INSERT INTO students VALUES (2, '李四', 21);
```

### 3. 查询数据
```sql
SELECT * FROM students;
SELECT name, age FROM students WHERE age > 20;
```

### 4. 删除数据
```sql
DELETE FROM students WHERE id = 1;
```

## 界面说明

### 状态指示
- **绿色**：操作成功
- **红色**：操作失败或错误
- **蓝色**：正在处理
- **橙色**：警告信息

### 显示区域
- **执行结果**：显示SQL执行的结果或错误信息
- **Token列表**：显示词法分析器生成的Token序列
- **AST结构**：显示抽象语法树和执行计划的详细信息

## 注意事项

1. **数据库引擎状态**：当前数据库引擎功能尚未完全实现，主要展示SQL编译和分析功能
2. **文件路径**：确保在正确的项目目录下运行程序
3. **Java版本**：需要Java 8或更高版本
4. **依赖关系**：确保所有依赖的类文件都已编译

## 故障排除

### 编译错误
- 检查Java版本是否支持
- 确保所有依赖类文件存在
- 检查文件路径是否正确

### 运行时错误
- 检查数据库目录是否存在
- 确保有足够的文件权限
- 查看控制台输出的错误信息

### 界面问题
- 如果界面显示异常，尝试调整窗口大小
- 检查系统是否支持Java Swing

## 技术架构

### 主要类文件
- `DatabaseGUI.java`：主界面类
- `EnhancedSQLCompiler.java`：增强版SQL编译器
- `SQLCompiler.java`：原始SQL编译器
- `DatabaseEngine.java`：数据库引擎

### 界面组件
- 使用Java Swing构建
- 采用BorderLayout布局
- 支持滚动条和文本换行

## 扩展功能

该GUI界面设计为可扩展的架构，未来可以添加：
- 语法高亮
- 自动补全
- 查询历史
- 数据可视化
- 多数据库连接

## 联系信息

如有问题或建议，请查看项目文档或联系开发团队。
