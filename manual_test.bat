@echo off
echo === SparrowDB 自定义SQL语句后台测试 ===
echo.

echo 测试1: 基本SELECT查询
echo SELECT * FROM users | java -cp target/classes com.database.SparrowDBApplication
echo.

echo 测试2: 创建新表
echo CREATE TABLE test_employees (id INT PRIMARY KEY, name VARCHAR(50), salary DECIMAL(10)) | java -cp target/classes com.database.SparrowDBApplication
echo.

echo 测试3: 插入数据
echo INSERT INTO test_employees VALUES (1, 'John Doe', 75000.00) | java -cp target/classes com.database.SparrowDBApplication
echo.

echo 测试4: 条件查询
echo SELECT * FROM users WHERE age > 30 | java -cp target/classes com.database.SparrowDBApplication
echo.

echo 测试5: 错误处理测试
echo SELECT * FROM non_existing_table | java -cp target/classes com.database.SparrowDBApplication
echo.

pause
