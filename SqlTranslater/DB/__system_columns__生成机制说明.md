# `__system_columns__.tbl` 生成机制详解

## 📋 概述

`__system_columns__.tbl` 是 SparrowDB 的系统元数据表，用于存储所有表的列信息。这个文件在数据库初始化时自动生成，并在每次创建或删除表时更新。

## 🔄 生成流程

### 1. 数据库启动时的初始化

#### 1.1 GUI启动流程
```
DatabaseGUI.initializeDatabase()
    ↓
MultiDatabaseManager(baseDataDirectory)
    ↓
DatabaseEngine(databaseName, dataPath)
    ↓
DatabaseEngine.initialize()
    ↓
CatalogManager.loadFromStorage()
```

#### 1.2 系统表创建
在 `CatalogManager.initializeSystemTables()` 中：

```java
// 创建系统列表结构
TableInfo systemColumnsInfo = new TableInfo(systemColumnsName);
systemColumnsInfo.addColumn(new ColumnInfo("table_name", "VARCHAR", 255, true, false, false, false, null, false));
systemColumnsInfo.addColumn(new ColumnInfo("column_name", "VARCHAR", 255, true, false, false, false, null, false));
systemColumnsInfo.addColumn(new ColumnInfo("data_type", "VARCHAR", 50, false, false, false, false, null, false));
systemColumnsInfo.addColumn(new ColumnInfo("length", "INT", 4, false, false, false, false, null, false));
systemColumnsInfo.addColumn(new ColumnInfo("not_null", "BOOLEAN", 1, false, false, false, false, "false", false));
systemColumnsInfo.addColumn(new ColumnInfo("primary_key", "BOOLEAN", 1, false, false, false, false, "false", false));
systemColumnsInfo.addColumn(new ColumnInfo("unique", "BOOLEAN", 1, false, false, false, false, "false", false));
systemColumnsInfo.addColumn(new ColumnInfo("default_value", "VARCHAR", 255, false, false, false, false, null, false));
systemColumnsInfo.addColumn(new ColumnInfo("auto_increment", "BOOLEAN", 1, false, false, false, false, "false", false));

// 创建物理存储
storageEngine.createTableStorage(systemColumnsName, systemColumnsInfo);
```

### 2. 文件结构生成

#### 2.1 元数据头部
在 `StorageEngine.writeTableMetadata()` 中生成：

```
# Table Metadata
TABLE_NAME=__system_columns__
COLUMN_COUNT=9
COLUMN=table_name:VARCHAR:255
COLUMN=column_name:VARCHAR:255
COLUMN=data_type:VARCHAR:50
COLUMN=length:INT:4
COLUMN=not_null:BOOLEAN:1
COLUMN=primary_key:BOOLEAN:1
COLUMN=unique:BOOLEAN:1
COLUMN=default_value:VARCHAR:255
COLUMN=auto_increment:BOOLEAN:1
# End Metadata
```

#### 2.2 数据页结构
```
PAGE:1
[记录数据...]

PAGE:2
[更多记录数据...]
```

### 3. 数据插入机制

#### 3.1 创建表时自动插入
当执行 `CREATE TABLE` 时：

```java
// CatalogManager.addTable()
public void addTable(TableInfo tableInfo) {
    catalog.addTable(tableInfo);
    persistTableMetadata(tableInfo);  // 触发系统表更新
}

// CatalogManager.persistTableMetadata()
private void persistTableMetadata(TableInfo tableInfo) {
    // 保存列信息到 __system_columns__
    for (ColumnInfo column : tableInfo.getColumns()) {
        Map<String, Object> columnRecord = new HashMap<>();
        columnRecord.put("table_name", tableInfo.getName());
        columnRecord.put("column_name", column.getName());
        columnRecord.put("data_type", column.getDataType());
        columnRecord.put("length", column.getLength());
        columnRecord.put("not_null", column.isNotNull());
        columnRecord.put("primary_key", column.isPrimaryKey());
        columnRecord.put("unique", column.isUnique());
        columnRecord.put("default_value", column.getDefaultValue());
        columnRecord.put("auto_increment", column.isAutoIncrement());
        
        // 插入到系统列表
        storageEngine.insertRecord(systemColumnsName, columnRecord);
    }
}
```

#### 3.2 记录序列化格式
每条记录按照以下格式存储：
```
auto_increment=false|not_null=false|unique=false|column_name=name|data_type=VARCHAR|length=50|default_value=null|table_name=test_table|primary_key=false
```

## 🗂️ 文件内容分析

基于您提供的 `__system_columns__.tbl` 文件内容：

### 文件头部
```
# Table Metadata
TABLE_NAME=__system_columns__
COLUMN_COUNT=9
COLUMN=table_name:VARCHAR:255
COLUMN=column_name:VARCHAR:255
COLUMN=data_type:VARCHAR:50
COLUMN=length:INT:4
COLUMN=not_null:BOOLEAN:1
COLUMN=primary_key:BOOLEAN:1
COLUMN=unique:BOOLEAN:1
COLUMN=default_value:VARCHAR:255
COLUMN=auto_increment:BOOLEAN:1
# End Metadata
```

### 数据记录
包含以下表的列信息：
- `test_row_table`: name(VARCHAR)
- `test_table`: id(INT), name(VARCHAR), age(INT)
- `test_table2`: id(INT), name(VARCHAR), age(INT), score(DECIMAL)
- `test_unified`: id(INT), name(VARCHAR)
- `hjy`: age(VARCHAR), ID(VARCHAR)

## 🔧 创建数据库时的生成步骤

### 步骤1: 目录创建
```java
// MultiDatabaseManager.createDatabase()
String dbPath = baseDataDirectory + File.separator + databaseName;
File dbDir = new File(dbPath);
dbDir.mkdirs();
```

### 步骤2: 数据库引擎初始化
```java
DatabaseEngine engine = new DatabaseEngine(databaseName, dbPath);
engine.initialize();
```

### 步骤3: 系统表自动创建
```java
// DatabaseEngine构造函数中
this.catalogManager = new CatalogManager(storageEngine);
catalogManager.setStorageAdapter(storageAdapter);  // 触发initializeSystemTables()
```

### 步骤4: 物理文件生成
```java
// StorageEngine.createTableStorage()
String tableFile = getTableFilePath("__system_columns__");  // 生成 __system_columns__.tbl
File file = new File(tableFile);
file.createNewFile();
writeTableMetadata(tableFile, systemColumnsInfo);
```

## 📁 文件位置

系统表文件位于每个数据库的目录中：
```
data/
├── main/                    # 默认数据库
│   ├── __system_tables__.tbl
│   ├── __system_columns__.tbl    ← 这里
│   ├── __system_constraints__.tbl
│   └── __system_functions__.tbl
├── test_db/                 # 其他数据库
│   ├── __system_tables__.tbl
│   ├── __system_columns__.tbl    ← 这里
│   └── ...
└── ...
```

## ⚙️ 配置参数

相关配置在 `DatabaseConfig.java` 中：
```java
public static final String SYSTEM_COLUMNS_FILE = "__system_columns__.tbl";
```

## 🔍 维护和更新

### 表创建时
- 自动向 `__system_columns__.tbl` 插入新表的所有列信息

### 表删除时
```java
// CatalogManager.removeTableMetadata()
Map<String, Object> columnRecord = new HashMap<>();
columnRecord.put("table_name", tableName);
storageEngine.deleteRecord(systemColumnsName, columnRecord);
```

### 列修改时
- 目前不支持 `ALTER TABLE`，如需修改需要重建表

## 🚀 手动创建示例

如果需要手动创建数据库，可以参考以下步骤：

```java
// 1. 创建数据库目录
File dbDir = new File("data/my_database");
dbDir.mkdirs();

// 2. 初始化数据库引擎
DatabaseEngine engine = new DatabaseEngine("my_database", "data/my_database");
boolean success = engine.initialize();

// 3. __system_columns__.tbl 将自动生成
```

## 📊 监控和诊断

检查系统表是否正确生成：
```java
// 查看所有系统表
File systemColumnsFile = new File(dataDir + File.separator + "__system_columns__.tbl");
boolean exists = systemColumnsFile.exists();
long size = systemColumnsFile.length();
```

## 🔄 总结

`__system_columns__.tbl` 的生成是完全自动化的过程：

1. **启动时**: 数据库引擎初始化时自动创建
2. **运行时**: 每次创建/删除表时自动更新
3. **结构**: 固定的9列结构存储列元数据
4. **位置**: 每个数据库目录中独立维护
5. **格式**: 标准的表文件格式（元数据头+数据页）

这确保了数据库的元数据始终保持一致和完整。

