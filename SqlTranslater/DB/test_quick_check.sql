-- 快速测试：数据库切换和CHECK约束
-- 测试时间: 2025-09-15

-- 1. 切换到smxx数据库
USE smxx;

-- 2. 创建简单的CHECK约束表
CREATE TABLE TestTable (
    id INT PRIMARY KEY,
    score INT CHECK (score >= 0 AND score <= 100),
    status VARCHAR(10) CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- 3. 验证表创建
SHOW TABLES;

-- 4. 插入测试数据
INSERT INTO TestTable VALUES (1, 85, 'ACTIVE');
INSERT INTO TestTable VALUES (2, 92, 'INACTIVE');

-- 5. 查询验证
SELECT * FROM TestTable;
