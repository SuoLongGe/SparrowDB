-- 测试视图功能修复
-- 测试时间: 2025-09-15

-- 1. 创建基础表
CREATE TABLE test_users (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    age INT,
    email VARCHAR(100)
);

-- 2. 插入测试数据
INSERT INTO test_users VALUES (1, 'Alice', 25, 'alice@test.com');
INSERT INTO test_users VALUES (2, 'Bob', 35, 'bob@test.com');
INSERT INTO test_users VALUES (3, 'Charlie', 22, 'charlie@test.com');
INSERT INTO test_users VALUES (4, 'Diana', 30, 'diana@test.com');

-- 3. 创建视图
CREATE VIEW young_users AS 
SELECT id, name, age 
FROM test_users 
WHERE age < 30;

-- 4. 测试视图查询
SELECT * FROM young_users;

-- 5. 创建另一个视图
CREATE VIEW adult_users AS 
SELECT id, name, email 
FROM test_users 
WHERE age >= 25;

-- 6. 测试第二个视图
SELECT * FROM adult_users;

-- 7. 测试条件删除视图
DROP VIEW IF EXISTS nonexistent_view;

-- 8. 删除视图
DROP VIEW young_users;

-- 9. 再次测试视图查询（应该失败）
-- SELECT * FROM young_users; -- 这行会失败因为视图已删除

-- 10. 测试剩余视图
SELECT * FROM adult_users;

