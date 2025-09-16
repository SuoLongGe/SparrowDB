-- 简单的COUNT测试
CREATE TABLE test_table (id INT, name VARCHAR(50));
INSERT INTO test_table VALUES (1, 'Alice');
INSERT INTO test_table VALUES (2, 'Bob');
SELECT COUNT(*) FROM test_table;

