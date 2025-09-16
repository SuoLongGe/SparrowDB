# 表点击功能修改测试

## 修改内容
1. 修改了 `showTableData` 方法，移除了自动填充SQL输入区域的逻辑
2. 创建了 `executeSQLInternal` 方法，用于内部执行查询而不修改用户输入区域

## 测试步骤
1. 启动DatabaseGUI程序
2. 在SQL输入区域输入一些文本（如 "SELECT * FROM users"）
3. 点击左侧数据库树中的表名
4. 验证：
   - SQL输入区域的文本没有被修改
   - 右侧结果区域显示了表的数据
   - 状态栏显示"正在显示表 XXX 的数据"

## 预期结果
- SQL输入区域保持原有文本不变
- 表数据正常显示在结果区域
- 不会自动填充 "SELECT * FROM 表名" 到输入区域

## 修改的代码位置
- 文件：`SqlTranslater/DB/src/main/java/com/sqlcompiler/DatabaseGUI.java`
- 方法：`showTableData` (第2115-2129行)
- 新增方法：`executeSQLInternal` (第968-1082行)
