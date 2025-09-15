-- ================================
-- 容错模式测试SQL文件
-- 用途: 测试GUI导入功能的错误处理能力
-- ================================

-- 正常的表创建
CREATE TABLE test_table (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    value INT
);

-- 正常的数据插入
INSERT INTO test_table (id, name, value) VALUES (1, '测试1', 100);

-- 故意的语法错误（缺少分号前的括号）
INSERT INTO test_table (id, name, value VALUES (2, '测试2', 200);

-- 这条语句应该能正常执行（测试容错模式是否继续）
INSERT INTO test_table (id, name, value) VALUES (3, '测试3', 300);

-- 重复主键错误
INSERT INTO test_table (id, name, value) VALUES (1, '重复ID', 400);

-- 正常语句（测试是否能继续执行）
INSERT INTO test_table (id, name, value) VALUES (4, '测试4', 400);

-- 不存在的表错误
INSERT INTO non_existing_table (col1, col2) VALUES ('test', 123);

-- 正常查询
SELECT * FROM test_table;

-- 错误的列名
SELECT id, name, wrong_column FROM test_table;

-- 最后的正常插入
INSERT INTO test_table (id, name, value) VALUES (5, '最终测试', 500);

-- 验证最终结果
SELECT COUNT(*) AS final_count FROM test_table;

-- ================================
-- 预期结果：
-- - 在容错模式下，正确的语句应该被执行
-- - 错误的语句会被跳过并记录
-- - 最终test_table应该包含id为1,3,4,5的记录
-- ================================
