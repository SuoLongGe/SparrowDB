-- ============================================
-- GUI界面删除products表演示
-- ============================================

-- 在GUI界面的SQL输入框中，按顺序执行以下SQL语句：

-- 1. 首先查看当前有哪些表
-- （在GUI中输入下面的命令，虽然这不是标准SQL，但应用可能支持）
-- 或者执行一个简单查询来确认表存在：
SELECT COUNT(*) FROM products;

-- 2. 安全删除products表
DROP TABLE IF EXISTS products;

-- 3. 验证删除结果（这个查询应该会报错，说明表已被删除）
-- SELECT * FROM products;

-- 4. 如果你想重新创建products表：
CREATE TABLE products (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2),
    category VARCHAR(50),
    stock_quantity INT DEFAULT 0
);

-- 5. 插入一些测试数据
INSERT INTO products VALUES (1, 'Test Product', 99.99, 'Test', 10);

-- 6. 验证表重新创建成功
SELECT * FROM products;
