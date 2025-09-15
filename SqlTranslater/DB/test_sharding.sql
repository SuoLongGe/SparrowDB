-- 分片功能测试SQL文件
-- 创建测试表
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    email VARCHAR(100),
    age INT,
    city VARCHAR(50)
);

CREATE TABLE orders (
    order_id INT PRIMARY KEY,
    user_id INT,
    order_date DATE,
    amount DECIMAL(10,2),
    status VARCHAR(20)
);

-- 插入测试数据
INSERT INTO users VALUES (1, '张三', 'zhangsan@email.com', 25, '北京');
INSERT INTO users VALUES (2, '李四', 'lisi@email.com', 30, '上海');
INSERT INTO users VALUES (3, '王五', 'wangwu@email.com', 28, '广州');
INSERT INTO users VALUES (4, '赵六', 'zhaoliu@email.com', 35, '深圳');
INSERT INTO users VALUES (5, '钱七', 'qianqi@email.com', 22, '杭州');
INSERT INTO users VALUES (6, '孙八', 'sunba@email.com', 29, '南京');
INSERT INTO users VALUES (7, '周九', 'zhoujiu@email.com', 31, '武汉');
INSERT INTO users VALUES (8, '吴十', 'wushi@email.com', 27, '成都');

INSERT INTO orders VALUES (1, 1, '2024-01-15', 299.99, '已完成');
INSERT INTO orders VALUES (2, 2, '2024-01-16', 199.50, '已完成');
INSERT INTO orders VALUES (3, 3, '2024-01-17', 399.00, '处理中');
INSERT INTO orders VALUES (4, 4, '2024-01-18', 150.75, '已完成');
INSERT INTO orders VALUES (5, 5, '2024-01-19', 599.99, '已完成');
INSERT INTO orders VALUES (6, 6, '2024-01-20', 89.99, '处理中');
INSERT INTO orders VALUES (7, 7, '2024-01-21', 799.50, '已完成');
INSERT INTO orders VALUES (8, 8, '2024-01-22', 249.99, '已完成');

-- 显示当前表
SHOW TABLES;

-- 显示用户表数据
SELECT * FROM users;

-- 显示订单表数据
SELECT * FROM orders;

-- 创建分片示例
-- 为users表创建哈希分片（按id列分4个分片）
CREATE SHARD users BY id USING HASH (4);

-- 为orders表创建范围分片（按order_date列分3个分片）
CREATE SHARD orders BY order_date USING RANGE (3);

-- 查看分片信息
SHOW SHARDS;

-- 查看特定表的分片信息
SHOW SHARDS users;
SHOW SHARDS orders;

-- 查看分片统计信息
SHARD STATS users;
SHARD STATS orders;

-- 测试分片查询（这些查询会自动路由到相应的分片）
SELECT * FROM users WHERE id = 1;
SELECT * FROM users WHERE id = 5;
SELECT * FROM orders WHERE order_date = '2024-01-15';
SELECT * FROM orders WHERE order_date = '2024-01-20';

-- 删除分片示例（取消注释以测试）
-- DROP SHARD users;
-- DROP SHARD orders;
