# SparrowDB SQL文件管理功能使用指南

## 功能概述

SparrowDB 现在支持以下SQL文件管理功能：

1. **SQL文件导入和执行** - 从文件中读取并执行SQL语句
2. **数据库导出为SQL文件** - 将数据库结构和数据导出为SQL文件
3. **单表导出** - 导出指定表的结构和数据
4. **批量导入** - 从目录中批量导入多个SQL文件

## 使用方法

### 1. 在命令行界面中使用

启动SparrowDB应用：
```bash
java -cp "src/main/java;target/classes" com.database.SparrowDBApplication
```

可用的新命令：

#### 导入SQL文件
```
import <file_path>
```
例如：
```
import test_import.sql
import C:\data\backup.sql
```

#### 导出数据库
```
export <output_path>
```
例如：
```
export database_backup.sql
export C:\backup\full_database.sql
```

#### 导出单个表
```
export table <table_name> <output_path>
```
例如：
```
export table users users_backup.sql
export table products C:\backup\products.sql
```

#### 批量导入目录
```
import dir <directory_path> [file_pattern]
```
例如：
```
import dir C:\sql_scripts
import dir C:\sql_scripts *.sql
import dir /backup/scripts script_*.sql
```

### 2. 在代码中使用

```java
import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

// 初始化数据库引擎
DatabaseEngine engine = new DatabaseEngine("my_db", "data");
engine.initialize();

// 导入SQL文件
ExecutionResult result = engine.importSQLFile("backup.sql");
if (result.isSuccess()) {
    System.out.println("导入成功: " + result.getMessage());
} else {
    System.out.println("导入失败: " + result.getMessage());
}

// 导出数据库
result = engine.exportDatabaseToSQL("backup.sql");
if (result.isSuccess()) {
    System.out.println("导出成功: " + result.getMessage());
}

// 导出单个表
result = engine.exportTableToSQL("users", "users_backup.sql");

// 批量导入（容错模式）
result = engine.importSQLDirectory("C:\\sql_scripts", "*.sql", true);
```

## 功能特性

### SQL文件导入

- **多语句支持**：自动解析和分离多个SQL语句
- **注释处理**：自动移除单行注释（--）和多行注释（/* */）
- **容错模式**：可选择在遇到错误时继续执行后续语句
- **详细反馈**：提供每条语句的执行结果
- **编码支持**：支持UTF-8编码的SQL文件

### 数据库导出

- **完整导出**：包含表结构（CREATE TABLE）和数据（INSERT）
- **选择性导出**：可选择只导出结构或只导出数据
- **表过滤**：可指定要导出的表列表
- **格式化输出**：生成格式化的、可读性好的SQL文件
- **注释信息**：包含导出时间和表信息注释

### 批量处理

- **模式匹配**：支持文件名模式匹配（如 *.sql）
- **有序执行**：按文件名字母顺序执行
- **进度跟踪**：显示批量处理进度
- **错误处理**：支持容错模式和快速失败模式

## 导出的SQL文件格式示例

```sql
-- ================================================
-- SparrowDB 数据库导出文件
-- 导出时间: 2025-09-15 14:30:25
-- 生成工具: SparrowDB SQLFileManager
-- ================================================

-- 表: users
-- 导出时间: 2025-09-15 14:30:25

-- 删除表（如果存在）
DROP TABLE IF EXISTS users;

-- 创建表 users
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE,
    age INT
);

-- users 数据
INSERT INTO users (id, name, email, age) VALUES (1, '张三', 'zhangsan@example.com', 25);
INSERT INTO users (id, name, email, age) VALUES (2, '李四', 'lisi@example.com', 30);
```

## 错误处理

### 常见错误和解决方案

1. **文件不存在**
   - 检查文件路径是否正确
   - 确保文件存在且有读取权限

2. **SQL语法错误**
   - 检查SQL语句语法
   - 使用容错模式跳过错误语句

3. **表不存在**
   - 确保要导出的表存在于数据库中
   - 检查表名拼写

4. **权限问题**
   - 确保有读取源文件的权限
   - 确保有写入目标文件的权限

### 容错模式

在导入SQL文件时，可以选择容错模式：

- **快速失败模式**（默认）：遇到第一个错误时停止执行
- **容错模式**：记录错误但继续执行后续语句

## 性能考虑

1. **大文件处理**：对于大型SQL文件，建议分批处理
2. **事务处理**：每条SQL语句在独立事务中执行
3. **内存使用**：大量数据导出时注意内存使用
4. **磁盘空间**：确保有足够的磁盘空间存储导出文件

## 最佳实践

1. **备份策略**：定期使用导出功能创建数据库备份
2. **文件命名**：使用有意义的文件名和时间戳
3. **版本控制**：将重要的SQL脚本纳入版本控制
4. **测试导入**：在生产环境使用前先在测试环境验证
5. **增量备份**：对于大型数据库，考虑增量导出策略

## 命令行示例会话

```
SparrowDB> help
=== SparrowDB 命令帮助 ===
SQL 命令:
  SELECT * FROM table_name          - 查询数据
  INSERT INTO table VALUES (...)    - 插入数据
  DELETE FROM table WHERE ...       - 删除数据
  CREATE TABLE table (columns...)   - 创建表

系统命令:
  tables, show tables               - 显示所有表
  desc <table_name>                 - 描述表结构
  info, status                      - 显示数据库信息
  examples                          - 显示示例查询
  benchmark                         - 运行性能测试
  test functions, test-functions    - 测试函数功能
  test views, test-views           - 测试视图功能
  test all, test-all               - 测试所有功能

SQL文件管理:
  import <file_path>                - 导入并执行SQL文件
  export <output_path>              - 导出数据库为SQL文件
  export table <table> <path>      - 导出单个表为SQL文件
  import dir <dir> [pattern]        - 批量导入目录中的SQL文件

其他命令:
  help, h                          - 显示此帮助
  quit, exit, q                    - 退出程序

SparrowDB> export database_backup.sql

=== 导出数据库 ===
输出路径: database_backup.sql
是否包含表结构? (y/n, 默认: y): y
是否包含数据? (y/n, 默认: y): y
指定要导出的表 (逗号分隔，留空导出所有表): 

导出结果: 成功
消息: 数据库导出成功！导出了 5 个表，127 条记录
执行时间: 245ms

SparrowDB> import test_data.sql

=== 导入SQL文件 ===
文件路径: test_data.sql
是否在遇到错误时继续执行? (y/n, 默认: n): y

执行结果: 成功
消息: SQL文件执行完成: 23/25 语句执行成功
执行时间: 1024ms
详细结果: 25 条SQL语句
成功: 23/25
```

## 注意事项

1. 导出的SQL文件使用UTF-8编码
2. 自动处理SQL关键字转义
3. 支持各种数据类型的正确格式化
4. 保持与原数据库的完全一致性
5. 支持大小写敏感的表名和列名

通过这些新功能，SparrowDB 现在提供了完整的数据导入导出解决方案，方便数据库的备份、迁移和批量操作。

