-- =============================================
-- DROP TABLE 功能验证脚本
-- 测试DROP TABLE是否正确清理系统表记录
-- =============================================

-- 1. 创建测试表
SELECT '=== 创建测试表 ===' as test_name;

CREATE TABLE test_drop_table (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    age INT
);

-- 2. 插入一些测试数据
INSERT INTO test_drop_table VALUES (1, 'Alice', 25);
INSERT INTO test_drop_table VALUES (2, 'Bob', 30);

-- 3. 验证表已创建
SELECT '=== 验证表已创建 ===' as test_name;
SELECT * FROM test_drop_table;

-- 4. 查看系统表中的记录（在删除之前）
SELECT '=== 删除前系统表记录 ===' as test_name;
SELECT table_name FROM __system_tables__ WHERE table_name = 'test_drop_table';
SELECT table_name, column_name FROM __system_columns__ WHERE table_name = 'test_drop_table';

-- 5. 删除表
SELECT '=== 执行DROP TABLE ===' as test_name;
DROP TABLE test_drop_table;

-- 6. 验证表已删除
SELECT '=== 验证表已删除 ===' as test_name;
-- 这个查询应该失败，因为表已经不存在了
-- SELECT * FROM test_drop_table;

-- 7. 验证系统表记录已清理
SELECT '=== 删除后系统表记录检查 ===' as test_name;
SELECT table_name FROM __system_tables__ WHERE table_name = 'test_drop_table';
SELECT table_name, column_name FROM __system_columns__ WHERE table_name = 'test_drop_table';

-- 8. 如果上面两个查询都没有返回结果，则说明DROP TABLE正确清理了系统表记录
SELECT '=== DROP TABLE 测试完成 ===' as test_name;



