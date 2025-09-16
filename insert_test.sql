-- 测试单行INSERT语句
CREATE TABLE test_insert (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    value DECIMAL(10,2)
);

-- 单行INSERT测试
INSERT INTO test_insert VALUES (1, 'Test Name', 99.99);

-- 查询验证
SELECT * FROM test_insert;

