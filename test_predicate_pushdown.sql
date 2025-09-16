-- 谓词下推优化测试脚本
-- 这个脚本用于测试谓词下推功能在大数据量查询中的性能提升

-- 1. 创建测试表
CREATE TABLE test_large_table (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    age INT,
    salary DECIMAL(10,2),
    department VARCHAR(50),
    city VARCHAR(50)
);

-- 2. 插入大量测试数据（模拟大数据量）
INSERT INTO test_large_table VALUES 
(1, '张三', 25, 5000.00, '技术部', '北京'),
(2, '李四', 30, 6000.00, '销售部', '上海'),
(3, '王五', 35, 7000.00, '技术部', '北京'),
(4, '赵六', 28, 5500.00, '人事部', '广州'),
(5, '钱七', 32, 6500.00, '技术部', '深圳'),
(6, '孙八', 27, 5200.00, '销售部', '北京'),
(7, '周九', 29, 5800.00, '技术部', '上海'),
(8, '吴十', 31, 6200.00, '人事部', '北京'),
(9, '郑一', 26, 5100.00, '销售部', '广州'),
(10, '王二', 33, 6800.00, '技术部', '深圳'),
(11, '李三', 24, 4800.00, '人事部', '北京'),
(12, '张四', 36, 7200.00, '技术部', '上海'),
(13, '刘五', 28, 5600.00, '销售部', '广州'),
(14, '陈六', 30, 6100.00, '人事部', '深圳'),
(15, '杨七', 25, 4900.00, '技术部', '北京'),
(16, '黄八', 34, 6900.00, '销售部', '上海'),
(17, '赵九', 27, 5300.00, '人事部', '广州'),
(18, '周十', 29, 5700.00, '技术部', '深圳'),
(19, '吴一', 32, 6400.00, '销售部', '北京'),
(20, '郑二', 26, 5000.00, '人事部', '上海');

-- 3. 测试谓词下推优化 - 简单条件查询
-- 这个查询应该能够使用谓词下推优化
SELECT * FROM test_large_table WHERE age > 30;

-- 4. 测试谓词下推优化 - 复合条件查询
-- 这个查询应该能够将部分条件下推
SELECT * FROM test_large_table WHERE age > 25 AND department = '技术部';

-- 5. 测试谓词下推优化 - 范围查询
-- 这个查询应该能够使用谓词下推优化
SELECT * FROM test_large_table WHERE salary BETWEEN 5000 AND 6000;

-- 6. 测试谓词下推优化 - 字符串匹配查询
-- 这个查询应该能够使用谓词下推优化
SELECT * FROM test_large_table WHERE city = '北京';

-- 7. 测试谓词下推优化 - 多条件AND查询
-- 这个查询应该能够将多个条件都下推
SELECT * FROM test_large_table WHERE age > 25 AND salary > 5500 AND department = '技术部';

-- 8. 测试谓词下推优化 - 聚合查询
-- 这个查询应该能够将WHERE条件下推
SELECT department, COUNT(*), AVG(salary) 
FROM test_large_table 
WHERE age > 28 
GROUP BY department;

-- 9. 测试谓词下推优化 - 排序查询
-- 这个查询应该能够将WHERE条件下推
SELECT * FROM test_large_table 
WHERE salary > 6000 
ORDER BY age DESC;

-- 10. 测试谓词下推优化 - 限制查询
-- 这个查询应该能够将WHERE条件下推
SELECT * FROM test_large_table 
WHERE department = '技术部' 
LIMIT 5;
