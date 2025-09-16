# SparrowDB 数据库引擎

## 概述

这是SparrowDB项目的第三部分 - 数据库引擎，负责执行SQL操作、管理数据存储和系统目录。

## 架构组件

### 1. 执行引擎 (Executor)
- **功能**: 执行各种SQL操作
- **支持的操作**: CREATE TABLE, INSERT, SELECT, DELETE
- **特点**: 
  - 支持WHERE条件过滤
  - 支持SELECT列表投影
  - 支持ORDER BY排序
  - 支持LIMIT限制

### 2. 存储引擎 (StorageEngine)
- **功能**: 负责数据的物理存储和检索
- **特点**:
  - 实现行与页的映射
  - 支持页面分配和管理
  - 实现记录序列化/反序列化
  - 支持表统计信息

### 3. 目录管理器 (CatalogManager)
- **功能**: 维护元数据，作为特殊表存储
- **特点**:
  - 管理表、列、约束信息
  - 实现系统表存储
  - 支持元数据持久化
  - 提供统计信息查询

### 4. 数据库引擎主类 (DatabaseEngine)
- **功能**: 整合所有组件，提供统一接口
- **特点**:
  - 统一的SQL执行接口
  - 数据库生命周期管理
  - 统计信息查询

## 使用方法

### 基本使用

```java
// 创建数据库引擎
DatabaseEngine engine = new DatabaseEngine("./data");

// 创建表
List<ColumnPlan> columns = new ArrayList<>();
columns.add(new ColumnPlan("id", "INT", 4, true, true, false, null, true));
columns.add(new ColumnPlan("name", "VARCHAR", 50, true, false, false, null, false));

List<ConstraintPlan> constraints = new ArrayList<>();
ExecutionResult result = engine.createTable("users", columns, constraints);

// 插入数据
List<List<ExpressionPlan>> values = new ArrayList<>();
List<ExpressionPlan> row1 = new ArrayList<>();
row1.add(new LiteralExpressionPlan("1"));
row1.add(new LiteralExpressionPlan("张三"));
values.add(row1);

ExecutionResult insertResult = engine.insertData("users", values);

// 查询数据
List<ExpressionPlan> selectList = new ArrayList<>();
selectList.add(new IdentifierExpressionPlan("*"));

ExecutionResult selectResult = engine.selectData("users", selectList, null, null, null);

// 关闭数据库
engine.close();
```

### 测试运行

```bash
# 编译
javac -cp . DatabaseEngine/*.java

# 运行测试
java -cp . com.database.engine.TestDatabaseEngine
```

## 文件结构

```
DatabaseEngine/
├── Executor.java              # 执行引擎
├── StorageEngine.java         # 存储引擎
├── CatalogManager.java        # 目录管理器
├── DatabaseEngine.java        # 数据库引擎主类
├── ExecutionResult.java       # 执行结果类
├── TestDatabaseEngine.java    # 测试类
└── README.md                  # 说明文档
```

## 数据存储格式

### 表文件格式
```
# Table Metadata
TABLE_NAME=users
COLUMN_COUNT=2
COLUMN=id:INT:4
COLUMN=name:VARCHAR:50
# End Metadata

PAGE:1
id=1|name=张三
id=2|name=李四

PAGE:2
id=3|name=王五
```

### 系统表
- `__system_tables__`: 存储表基本信息
- `__system_columns__`: 存储列信息
- `__system_constraints__`: 存储约束信息

## 特性

### 支持的数据类型
- INT: 整数类型
- VARCHAR: 可变长度字符串
- BOOLEAN: 布尔类型
- BIGINT: 长整数类型

### 支持的约束
- PRIMARY KEY: 主键约束
- NOT NULL: 非空约束
- UNIQUE: 唯一约束
- FOREIGN KEY: 外键约束（基础支持）

### 支持的SQL操作
- CREATE TABLE: 创建表
- INSERT: 插入数据
- SELECT: 查询数据
- DELETE: 删除数据

### 查询功能
- WHERE条件过滤
- SELECT列表投影
- ORDER BY排序
- LIMIT限制结果数量

## 限制

1. 当前版本是简化实现，主要用于演示数据库引擎的基本功能
2. 不支持复杂的JOIN操作
3. 不支持UPDATE操作
4. 不支持索引
5. 事务支持有限

## 扩展建议

1. 添加索引支持以提高查询性能
2. 实现UPDATE操作
3. 添加事务管理
4. 支持更复杂的数据类型
5. 实现查询优化器
6. 添加并发控制

## 依赖

- Java 8+
- SQL编译器模块 (com.sqlcompiler.*)
- 存储系统模块 (com.storage.*)

## 注意事项

1. 数据目录会自动创建
2. 系统表会自动初始化
3. 关闭数据库时会自动保存状态
4. 建议在生产环境中添加更多的错误处理和日志记录
