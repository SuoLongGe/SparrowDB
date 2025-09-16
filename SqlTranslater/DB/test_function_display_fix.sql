-- 测试函数显示修复
-- 测试时间: 2025-09-15

-- 1. 首先确认当前在main数据库
SELECT DATABASE();

-- 2. 在main数据库中创建一个测试函数
CREATE FUNCTION test_main_func(x INT, y INT) RETURNS INT
PERMANENT
BEGIN
    RETURN x * y + 1;
END;

-- 3. 切换到smxx数据库
USE smxx;

-- 4. 确认切换成功
SELECT DATABASE();

-- 5. 在smxx数据库中创建一个测试函数
CREATE FUNCTION test_smxx_func(a INT, b INT) RETURNS INT
PERMANENT
BEGIN
    RETURN a + b * 2;
END;

-- 6. 测试调用smxx数据库中的函数
SELECT test_smxx_func(5, 3) AS result1;

-- 7. 切换回main数据库
USE main;

-- 8. 测试调用main数据库中的函数
SELECT test_main_func(4, 5) AS result2;

