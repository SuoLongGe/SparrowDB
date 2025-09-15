-- =====================================================
-- SparrowDB 简化功能验证测试文件
-- 专注于核心功能，避免复杂语法问题
-- 创建时间：2025-09-15
-- =====================================================

-- 清理可能存在的测试数据
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS customers;

-- =====================================================
-- 1. 基本表创建测试
-- =====================================================

-- 创建客户表
CREATE TABLE customers (
    customer_id INT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    city VARCHAR(50),
    total_spent DECIMAL(10,2) DEFAULT 0.00,
    is_active BOOLEAN DEFAULT TRUE
);

-- 创建产品表
CREATE TABLE products (
    product_id INT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price DECIMAL(8,2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    supplier VARCHAR(50)
);

-- 创建订单表
CREATE TABLE orders (
    order_id INT PRIMARY KEY,
    customer_id INT,
    product_id INT,
    quantity INT NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    discount_rate DECIMAL(3,2) DEFAULT 0.00
);

-- =====================================================
-- 2. 基本数据插入测试
-- =====================================================

-- 插入客户数据
INSERT INTO customers (customer_id, first_name, last_name, email, phone, city, total_spent, is_active) VALUES
(1, '张', '三', 'zhang.san@email.com', '13800138001', '北京', 2500.00, TRUE);

INSERT INTO customers (customer_id, first_name, last_name, email, phone, city, total_spent, is_active) VALUES
(2, '李', '四', 'li.si@email.com', '13800138002', '上海', 1800.00, TRUE);

INSERT INTO customers (customer_id, first_name, last_name, email, phone, city, total_spent, is_active) VALUES
(3, '王', '五', 'wang.wu@email.com', '13800138003', '广州', 3200.00, TRUE);

-- 插入产品数据
INSERT INTO products (product_id, product_name, category, price, stock_quantity, supplier) VALUES
(101, '智能手机Pro', '电子产品', 3999.00, 50, '科技公司A');

INSERT INTO products (product_id, product_name, category, price, stock_quantity, supplier) VALUES
(102, '无线耳机', '电子产品', 299.00, 200, '科技公司B');

INSERT INTO products (product_id, product_name, category, price, stock_quantity, supplier) VALUES
(103, '笔记本电脑', '电子产品', 6999.00, 30, '科技公司C');

-- 插入订单数据
INSERT INTO orders (order_id, customer_id, product_id, quantity, status, discount_rate) VALUES
(1001, 1, 101, 1, 'completed', 0.05);

INSERT INTO orders (order_id, customer_id, product_id, quantity, status, discount_rate) VALUES
(1002, 2, 102, 2, 'completed', 0.00);

INSERT INTO orders (order_id, customer_id, product_id, quantity, status, discount_rate) VALUES
(1003, 1, 103, 1, 'shipped', 0.10);

-- =====================================================
-- 3. 基本查询测试
-- =====================================================

-- 简单查询
SELECT * FROM customers WHERE city = '北京';

-- 聚合查询
SELECT category, COUNT(*) as product_count, AVG(price) as avg_price 
FROM products 
GROUP BY category;

-- 计算统计信息
SELECT 
    COUNT(*) as total_customers,
    SUM(total_spent) as total_revenue,
    AVG(total_spent) as avg_customer_value
FROM customers 
WHERE is_active = TRUE;

-- =====================================================
-- 4. 更新和删除测试
-- =====================================================

-- 更新客户总消费金额
UPDATE customers 
SET total_spent = total_spent + 500.00 
WHERE customer_id = 1;

-- 更新产品库存
UPDATE products 
SET stock_quantity = stock_quantity - 10 
WHERE category = '电子产品' AND stock_quantity > 50;

-- =====================================================
-- 5. 函数使用测试
-- =====================================================

-- 数学函数测试
SELECT 
    product_name,
    price,
    ROUND(price * 0.9, 2) as discounted_price,
    ABS(stock_quantity - 100) as stock_diff
FROM products;

-- 字符串函数测试
SELECT 
    customer_id,
    UPPER(first_name) as upper_name,
    LOWER(last_name) as lower_name,
    LENGTH(email) as email_length,
    CONCAT(first_name, ' ', last_name) as full_name
FROM customers;

-- =====================================================
-- 6. 连接查询测试
-- =====================================================

-- 内连接查询
SELECT 
    o.order_id,
    c.first_name,
    c.last_name,
    p.product_name,
    o.quantity,
    p.price
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
JOIN products p ON o.product_id = p.product_id
WHERE o.status = 'completed';

-- 左连接查询
SELECT 
    c.customer_id,
    c.first_name,
    c.last_name,
    COUNT(o.order_id) as order_count
FROM customers c
LEFT JOIN orders o ON c.customer_id = o.customer_id
GROUP BY c.customer_id, c.first_name, c.last_name;

-- =====================================================
-- 7. 条件查询和排序测试
-- =====================================================

-- 复杂WHERE条件
SELECT 
    customer_id,
    first_name,
    last_name,
    city,
    total_spent
FROM customers 
WHERE (city = '北京' OR city = '上海') 
  AND total_spent BETWEEN 1000 AND 3000
  AND is_active = TRUE
ORDER BY total_spent DESC;

-- 分组和HAVING子句
SELECT 
    category,
    COUNT(*) as product_count,
    SUM(stock_quantity) as total_stock,
    AVG(price) as avg_price,
    MIN(price) as min_price,
    MAX(price) as max_price
FROM products 
GROUP BY category
HAVING COUNT(*) >= 2 AND AVG(price) > 1000;

-- =====================================================
-- 8. 子查询测试
-- =====================================================

-- 简单子查询
SELECT 
    product_name,
    price,
    stock_quantity
FROM products 
WHERE price > (SELECT AVG(price) FROM products);

-- IN子查询
SELECT 
    customer_id,
    first_name,
    last_name
FROM customers 
WHERE customer_id IN (SELECT DISTINCT customer_id FROM orders WHERE status = 'completed');

-- =====================================================
-- 9. 数据验证查询
-- =====================================================

-- 检查客户表记录数
SELECT 'customers' as table_name, COUNT(*) as record_count FROM customers;

-- 检查产品表记录数
SELECT 'products' as table_name, COUNT(*) as record_count FROM products;

-- 检查订单表记录数
SELECT 'orders' as table_name, COUNT(*) as record_count FROM orders;

-- 检查活跃客户数
SELECT 'active_customers' as type, COUNT(*) as count FROM customers WHERE is_active = TRUE;

-- 检查高价值客户数
SELECT 'high_value_customers' as type, COUNT(*) as count FROM customers WHERE total_spent > 2000;

-- 检查库存充足的产品数
SELECT 'well_stocked_products' as type, COUNT(*) as count FROM products WHERE stock_quantity > 100;

-- =====================================================
-- 测试完成标记
-- =====================================================
-- 显示测试完成信息
SELECT 'SparrowDB 简化功能验证测试完成' as test_status FROM customers LIMIT 1;
