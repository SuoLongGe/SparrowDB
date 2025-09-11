# B+树索引系统

## 概述

本B+树索引系统为Java存储系统提供了高效的索引功能，支持整数和字符串类型的键值，实现了完整的B+树操作包括插入、删除、查找和范围查询。

## 主要特性

- **完整的B+树实现**: 支持内部节点和叶子节点的分裂、合并操作
- **多数据类型支持**: 支持整数(IntegerKey)和字符串(StringKey)键值
- **持久化存储**: 索引数据持久化到磁盘，支持数据库重启后恢复
- **范围查询**: 支持高效的范围查询操作
- **缓存集成**: 与现有的缓冲池管理器完全集成
- **并发安全**: 使用读写锁保证并发访问安全

## 核心组件

### 1. BPlusTreeKey接口
定义键值的基本操作：
- `toBytes()`: 序列化为字节数组
- `fromBytes()`: 从字节数组反序列化
- `compareTo()`: 键值比较
- `getSize()`: 获取键值占用的字节数

### 2. 键值实现类
- **IntegerKey**: 整数键值实现
- **StringKey**: 字符串键值实现（最大255字符）

### 3. BPlusTreeNode节点类
- **BPlusTreeInternalNode**: 内部节点，存储键值和子节点指针
- **BPlusTreeLeafNode**: 叶子节点，存储键值和记录页面ID，维护叶子节点链表

### 4. BPlusTree核心类
实现B+树的主要操作：
- 插入操作（支持节点分裂）
- 删除操作（支持节点合并）
- 查找操作
- 范围查询
- 树结构维护

### 5. IndexManager索引管理器
管理多个B+树索引：
- 创建/删除索引
- 索引操作封装
- 键值类型管理

## 使用方法

### 1. 基本使用

```java
// 创建存储引擎
StorageEngine engine = new StorageEngine(10, "test.db", ReplacementPolicy.LRU);

// 创建整数索引
engine.createIntegerIndex("student_id_index", 10);

// 插入数据
engine.insertToIndex("student_id_index", 1001, 5);  // 学号1001对应页面5
engine.insertToIndex("student_id_index", 1002, 6);  // 学号1002对应页面6

// 查找数据
int pageId = engine.searchIndex("student_id_index", 1001);  // 返回5

// 范围查询
List<Integer> results = engine.rangeSearchIndex("student_id_index", 1000, 1010);

// 删除数据
engine.deleteFromIndex("student_id_index", 1001);
```

### 2. 字符串索引

```java
// 创建字符串索引
engine.createStringIndex("name_index", 10);

// 插入字符串数据
engine.insertToIndex("name_index", "Alice", 10);
engine.insertToIndex("name_index", "Bob", 11);

// 查找字符串
int pageId = engine.searchIndex("name_index", "Alice");  // 返回10
```

### 3. 索引管理

```java
// 检查索引是否存在
boolean exists = engine.hasIndex("student_id_index");

// 显示所有索引信息
engine.printAllIndexes();

// 显示特定索引信息
engine.printIndexInfo("student_id_index");

// 显示索引结构
engine.printIndexStructure("student_id_index");

// 删除索引
engine.dropIndex("student_id_index");
```

## 性能特点

### 时间复杂度
- **查找**: O(log n)
- **插入**: O(log n)
- **删除**: O(log n)
- **范围查询**: O(log n + k)，其中k是结果数量

### 空间复杂度
- **节点存储**: 每个节点最多存储maxKeys个键值
- **页面利用**: 充分利用4KB页面空间
- **索引开销**: 相比全表扫描，索引查询大幅减少I/O操作

## 配置参数

### 1. 最大键值数量 (maxKeys)
- **推荐值**: 5-20
- **影响**: 影响树的深度和节点分裂频率
- **权衡**: 较大的值减少树深度但增加节点分裂开销

### 2. 缓冲池大小
- **推荐值**: 10-100个页面
- **影响**: 影响索引操作的缓存命中率
- **建议**: 根据可用内存和索引大小调整

## 测试和演示

### 1. 运行测试
```bash
# 编译所有文件
javac *.java

# 运行B+树测试
java BPlusTreeTest

# 运行演示程序
java BPlusTreeDemo
```

### 2. 测试内容
- **基本操作测试**: 插入、查找、删除
- **大量数据测试**: 1000个随机数据插入和查找
- **范围查询测试**: 验证范围查询正确性
- **删除操作测试**: 验证删除后的树结构
- **性能测试**: 测量插入和查找性能
- **字符串索引测试**: 验证字符串键值功能

### 3. 演示程序功能
- 交互式菜单操作
- 索引创建和管理
- 数据插入和查询
- 批量操作演示
- 性能测试

## 文件结构

```
JavaStorageSystem/
├── BPlusTreeKey.java          # 键值接口和实现
├── BPlusTreeNode.java         # B+树节点类
├── BPlusTree.java            # B+树核心实现
├── IndexManager.java         # 索引管理器
├── StorageEngine.java        # 存储引擎（已更新）
├── BPlusTreeTest.java        # 测试程序
├── BPlusTreeDemo.java        # 演示程序
├── compile_and_run.bat       # 编译运行脚本（已更新）
└── BPlusTree_README.md       # 本文档
```

## 注意事项

1. **键值唯一性**: 每个键值在索引中必须唯一
2. **页面管理**: 索引节点占用存储页面，需要合理管理
3. **内存使用**: 大量索引会占用较多内存
4. **并发访问**: 使用读写锁保证线程安全
5. **数据持久化**: 索引数据自动持久化到磁盘

## 扩展功能

### 可能的改进方向
1. **复合索引**: 支持多列组合索引
2. **部分索引**: 支持条件索引
3. **索引统计**: 收集索引使用统计信息
4. **自动优化**: 根据查询模式自动调整索引
5. **压缩存储**: 优化存储空间使用

## 故障排除

### 常见问题
1. **编译错误**: 确保所有依赖文件都已编译
2. **运行时错误**: 检查数据库文件权限和磁盘空间
3. **性能问题**: 调整缓冲池大小和maxKeys参数
4. **内存不足**: 减少缓冲池大小或maxKeys值

### 调试建议
1. 使用`printIndexStructure()`查看树结构
2. 使用`printCacheStats()`查看缓存统计
3. 启用详细日志输出
4. 使用小数据集测试功能正确性
