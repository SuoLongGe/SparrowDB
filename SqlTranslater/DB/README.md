# SQL编译器

基于编译原理知识实现的SQL编译器，使用Java语言开发。该编译器支持CREATE TABLE、INSERT、SELECT、DELETE四种SQL语句的完整编译过程。

## 🚀 快速开始

### 当前实现状态
- ✅ **词法分析器** - 完整的Token识别和错误处理
- ✅ **语法分析器** - 递归下降分析，生成AST
- ✅ **语义分析器** - 类型检查、存在性验证、约束检查
- ✅ **执行计划生成器** - 输出标准化的执行计划
- ✅ **模式目录管理** - 维护数据库元数据
- 🔄 **数据库引擎** - 待实现（接收执行计划，进行查询优化）
- 🔄 **存储系统** - 待实现（数据存储、索引管理、持久化）

### 快速测试
```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 运行主程序
mvn exec:java -Dexec.mainClass="com.sqlcompiler.Main"
```

### 输出示例
编译器将SQL语句转换为标准化的执行计划，为数据库引擎提供输入：
```java
// 输入SQL
"SELECT * FROM users WHERE age > 18"

// 输出执行计划
SelectPlan {
    selectList: [IdentifierExpression("*")]
    fromClause: [TableReference("users")]
    whereClause: BinaryExpression(">", "age", 18)
}
```

## 功能特性

### 词法分析器 (Lexical Analyzer)
- 识别SQL关键字、标识符、常量、运算符、分隔符
- 输出格式：[种别码，词素值，行号，列号]
- 支持错误检测和位置报告
- 支持字符串转义、数字字面量、标识符等

### 语法分析器 (Syntax Analyzer)
- 使用递归下降分析法
- 支持CREATE TABLE、INSERT、SELECT、DELETE语句
- 生成抽象语法树(AST)
- 提供详细的语法错误报告和期望符号提示

### 语义分析器 (Semantic Analyzer)
- 存在性检查：验证表和列是否存在
- 类型一致性检查：验证数据类型兼容性
- 列数/列序检查：验证INSERT语句的值与列匹配
- 约束验证：检查主键、外键等约束
- 维护数据库模式目录(Catalog)

### 执行计划生成器 (Execution Plan Generator)
- 将AST转换为逻辑执行计划
- 支持多种输出格式：树形结构、JSON、S表达式
- 优化查询执行顺序

### 模式目录管理 (Catalog Management)
- 维护数据库表、列、约束等元数据
- 支持表信息查询和验证
- 提供目录信息摘要

## 项目结构

```
src/main/java/com/sqlcompiler/
├── lexer/                    # 词法分析器
│   ├── LexicalAnalyzer.java
│   ├── Token.java
│   ├── TokenType.java
│   └── Position.java
├── parser/                   # 语法分析器
│   └── SyntaxAnalyzer.java
├── ast/                      # 抽象语法树节点
│   ├── ASTNode.java
│   ├── ASTVisitor.java
│   ├── Statement.java
│   ├── CreateTableStatement.java
│   ├── InsertStatement.java
│   ├── SelectStatement.java
│   ├── DeleteStatement.java
│   └── ...
├── semantic/                 # 语义分析器
│   ├── SemanticAnalyzer.java
│   └── SemanticAnalysisResult.java
├── execution/                # 执行计划生成器
│   ├── ExecutionPlanGenerator.java
│   ├── ExecutionPlan.java
│   ├── CreateTablePlan.java
│   ├── InsertPlan.java
│   ├── SelectPlan.java
│   ├── DeletePlan.java
│   └── ...
├── catalog/                  # 模式目录管理
│   ├── Catalog.java
│   ├── TableInfo.java
│   ├── ColumnInfo.java
│   └── ConstraintInfo.java
├── exception/                # 异常处理
│   ├── CompilationException.java
│   ├── LexicalException.java
│   ├── SyntaxException.java
│   └── SemanticException.java
├── SQLCompiler.java          # 主编译器类
└── Main.java                 # 主程序入口
```

## 支持的SQL语法

### CREATE TABLE语句
```sql
CREATE TABLE table_name (
    column1 datatype constraints,
    column2 datatype constraints,
    ...
    CONSTRAINT constraint_name constraint_definition
);
```

支持的约束：
- PRIMARY KEY
- FOREIGN KEY
- UNIQUE
- NOT NULL
- DEFAULT
- AUTO_INCREMENT

### INSERT语句
```sql
INSERT INTO table_name (column1, column2, ...)
VALUES (value1, value2, ...), (value1, value2, ...);
```

### SELECT语句
```sql
SELECT [DISTINCT] column1, column2, ...
FROM table1 [AS alias1]
[JOIN table2 [AS alias2] ON condition]
[WHERE condition]
[GROUP BY column1, column2, ...]
[HAVING condition]
[ORDER BY column1 [ASC|DESC], column2 [ASC|DESC], ...]
[LIMIT count [OFFSET offset]];
```

### DELETE语句
```sql
DELETE FROM table_name [WHERE condition];
```

## 编译和运行

### 使用Maven编译
```bash
mvn clean compile
```

### 运行主程序
```bash
mvn exec:java -Dexec.mainClass="com.sqlcompiler.Main"
```

### 运行测试
```bash
mvn test
```

## 使用示例

### 1. 创建表
```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE,
    age INT DEFAULT 0
);
```

### 2. 插入数据
```sql
INSERT INTO users (id, name, email, age) 
VALUES (1, 'John Doe', 'john@example.com', 25);
```

### 3. 查询数据
```sql
SELECT id, name, email 
FROM users 
WHERE age > 18 
ORDER BY name ASC 
LIMIT 10;
```

### 4. 删除数据
```sql
DELETE FROM users WHERE id = 1;
```

## 输出格式

### 词法分析输出
```
[SELECT, SELECT, 1, 1]
[IDENTIFIER, id, 1, 8]
[COMMA, ,, 1, 10]
[IDENTIFIER, name, 1, 12]
[FROM, FROM, 1, 17]
[IDENTIFIER, users, 1, 22]
```

### 执行计划输出

#### 树形结构
```
SELECT
  SELECT_LIST
    IDENTIFIER
    IDENTIFIER
  FROM
    TABLE
  WHERE
    BINARY
```

#### JSON格式
```json
{
  "type": "SELECT",
  "selectList": [
    {
      "type": "IDENTIFIER",
      "name": "id"
    }
  ],
  "fromClause": [
    {
      "tableName": "users"
    }
  ]
}
```

#### S表达式格式
```
(SELECT (SELECT_LIST (IDENTIFIER "id") (IDENTIFIER "name")) 
 (FROM (TABLE "users")) 
 (WHERE (BINARY ">" (IDENTIFIER "age") (LITERAL "18" "NUMBER_LITERAL"))))
```

## 错误处理

编译器提供详细的错误信息，包括：
- 词法错误：非法字符、未闭合的字符串等
- 语法错误：缺少关键字、括号不匹配等
- 语义错误：表不存在、类型不匹配、列数不匹配等

错误格式：[错误类型，位置，原因说明]

## 技术特点

1. **完整的编译流程**：从词法分析到执行计划生成的完整编译过程
2. **模块化设计**：各组件职责清晰，易于维护和扩展
3. **错误处理**：提供详细的错误信息和位置定位
4. **多种输出格式**：支持树形结构、JSON、S表达式等多种执行计划输出格式
5. **类型安全**：使用Java强类型系统确保编译时类型检查
6. **测试覆盖**：包含完整的单元测试用例

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    SQL编译器 (当前实现)                        │
├─────────────────────────────────────────────────────────────┤
│  词法分析 → 语法分析 → 语义分析 → 执行计划生成                    │
│     ↓         ↓         ↓         ↓                        │
│   Token    AST     验证结果    ExecutionPlan                │
└─────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────┐
│                   数据库引擎 (待实现)                          │
├─────────────────────────────────────────────────────────────┤
│  查询优化 → 执行调度 → 事务管理 → 并发控制                      │
│     ↓         ↓         ↓         ↓                        │
│  优化计划   执行指令   事务状态   锁管理                       │
└─────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────┐
│                   存储系统 (待实现)                            │
├─────────────────────────────────────────────────────────────┤
│  数据存储 → 索引管理 → 缓存管理 → 持久化                       │
│     ↓         ↓         ↓         ↓                        │
│  文件系统    B+树     内存缓存   磁盘I/O                      │
└─────────────────────────────────────────────────────────────┘
```
<<<<<<< HEAD

=======
SQL语句 → 词法分析 → Token流
Token流 → 语法分析 → AST
AST → 语义分析 → 验证结果
AST → 执行计划生成 → ExecutionPlan
*****************************************************
>>>>>>> 62d958b6bcc46722dfbc5dd2897cfc16d17ca1d3
## 与数据库引擎和存储系统的接口

### 执行计划输出结构

编译器为后续的数据库引擎和存储系统提供了标准化的执行计划对象：

#### 1. 执行计划基类
```java
public abstract class ExecutionPlan {
    protected String planType;           // 计划类型：CREATE_TABLE, SELECT, INSERT, DELETE
    protected List<ExecutionPlan> children; // 子计划（用于复杂查询）
    
    // 输出格式
    public abstract String toJSON();     // JSON格式
    public abstract String toSExpression(); // S表达式格式
    public String toTreeString();        // 树形结构
}
```

#### 2. 具体执行计划类型

**CREATE TABLE计划：**
```java
CreateTablePlan {
    String tableName;                    // 表名
    List<ColumnPlan> columns;            // 列定义
    List<ConstraintPlan> constraints;    // 约束定义
}
```

**SELECT计划：**
```java
SelectPlan {
    boolean distinct;                    // 是否去重
    List<ExpressionPlan> selectList;     // 选择列表
    List<TablePlan> fromClause;          // FROM子句
    ExpressionPlan whereClause;          // WHERE条件
    List<ExpressionPlan> groupByClause;  // GROUP BY
    ExpressionPlan havingClause;         // HAVING条件
    List<OrderByItem> orderByClause;     // ORDER BY
    LimitPlan limitClause;               // LIMIT
}
```

**INSERT计划：**
```java
InsertPlan {
    String tableName;                    // 目标表
    List<String> columns;                // 插入列
    List<List<ExpressionPlan>> values;   // 插入值
}
```

**DELETE计划：**
```java
DeletePlan {
    String tableName;           // 目标表
    ExpressionPlan whereClause; // 删除条件
}
```

### 数据库引擎接口

数据库引擎可以直接使用执行计划对象：

```java
// 数据库引擎接口示例
public interface DatabaseEngine {
    // 表操作
    void createTable(CreateTablePlan plan);
    void dropTable(String tableName);
    
    // 数据操作
    ResultSet select(SelectPlan plan);
    int insert(InsertPlan plan);
    int delete(DeletePlan plan);
    
    // 索引操作
    void createIndex(String tableName, String columnName);
    void dropIndex(String indexName);
    
    // 事务操作
    void beginTransaction();
    void commit();
    void rollback();
}
```

### 存储系统接口

存储系统接收标准化的操作指令：

```java
// 表结构定义
TableSchema {
    String tableName;
    List<ColumnSchema> columns;
    List<Constraint> constraints;
}

// 数据操作指令
InsertOperation {
    String tableName;
    List<String> columns;
    List<Object[]> values;
}

SelectOperation {
    String tableName;
    List<String> columns;
    Expression condition;
    List<String> orderBy;
    Integer limit;
}
```

### 使用流程示例

```java
// 1. SQL编译器输出执行计划
ExecutionPlan plan = compiler.compile(sql);

// 2. 数据库引擎接收执行计划
DatabaseEngine engine = new DatabaseEngineImpl();

// 3. 根据计划类型执行相应操作
switch (plan.getPlanType()) {
    case "CREATE_TABLE":
        CreateTablePlan createPlan = (CreateTablePlan) plan;
        engine.createTable(createPlan);
        break;
        
    case "SELECT":
        SelectPlan selectPlan = (SelectPlan) plan;
        ResultSet result = engine.select(selectPlan);
        break;
        
    case "INSERT":
        InsertPlan insertPlan = (InsertPlan) plan;
        int rowsAffected = engine.insert(insertPlan);
        break;
}

// 4. 存储系统执行底层操作
StorageSystem storage = new StorageSystemImpl();
storage.createTable(createPlan.getTableName(), createPlan.getColumns());
storage.insertData(insertPlan.getTableName(), insertPlan.getValues());
```

### 输出格式优势

1. **JSON格式** - 便于跨语言调用和API接口
2. **S表达式格式** - 便于函数式处理和查询优化
3. **树形结构** - 便于调试和可视化
4. **强类型对象** - 便于Java程序直接使用

## 扩展性

该编译器设计具有良好的扩展性，可以轻松添加：
- 新的SQL语句类型
- 更多的数据类型
- 复杂的查询优化
- 更多的约束类型
- 索引支持等

## 团队协作指南

### 当前代码结构
- **编译器部分** (已完成) - 位于 `src/main/java/com/sqlcompiler/`
- **测试代码** (已完成) - 位于 `src/test/java/com/sqlcompiler/`
- **执行计划输出** (已完成) - 为数据库引擎提供标准化接口

### 下一步开发建议

#### 数据库引擎开发
1. **查询优化器** - 基于执行计划进行查询优化
2. **执行引擎** - 执行优化后的查询计划
3. **事务管理器** - 处理ACID事务
4. **并发控制器** - 处理多用户并发访问

#### 存储系统开发
1. **存储管理器** - 管理数据文件的读写
2. **索引管理器** - 实现B+树索引
3. **缓存管理器** - 实现LRU缓存机制
4. **恢复管理器** - 处理系统崩溃恢复

### 接口规范
- 数据库引擎应实现 `DatabaseEngine` 接口
- 存储系统应实现 `StorageSystem` 接口
- 所有接口定义在 `src/main/java/com/sqlcompiler/engine/` 和 `src/main/java/com/sqlcompiler/storage/`

### 开发环境
- **JDK版本**: 11+
- **构建工具**: Maven 3.6+
- **测试框架**: JUnit 5
- **代码风格**: 遵循Java标准命名规范

## 许可证

本项目仅用于学习和研究目的。
