-- =====================================================
-- SparrowDB 功能验证测试文件
-- 包含：基本SQL语句、函数、视图创建等功能测试
-- 创建时间：2025-09-15
-- =====================================================

-- 清理可能存在的测试数据
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS customers;
DROP VIEW IF EXISTS customer_order_summary;
DROP FUNCTION IF EXISTS calculate_total_price;
DROP FUNCTION IF EXISTS get_customer_level;

-- =====================================================
-- 1. 基本表创建测试
-- =====================================================

-- 创建客户表
CREATE TABLE customers (
    customer_id INT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    city VARCHAR(50),
    registration_date DATE DEFAULT '2025-01-01',
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
    supplier VARCHAR(50),
    created_date DATE DEFAULT '2025-01-01'
);

-- 创建订单表
CREATE TABLE orders (
    order_id INT PRIMARY KEY,
    customer_id INT,
    product_id INT,
    quantity INT NOT NULL,
    order_date DATE DEFAULT '2025-01-01',
    status VARCHAR(20) DEFAULT 'pending',
    discount_rate DECIMAL(3,2) DEFAULT 0.00,
    notes TEXT
);

-- =====================================================
-- 2. 基本数据插入测试
-- =====================================================

-- 插入客户数据
INSERT INTO customers (customer_id, first_name, last_name, email, phone, city, registration_date, total_spent, is_active) VALUES
(1, '张', '三', 'zhang.san@email.com', '13800138001', '北京', '2025-01-15', 2500.00, TRUE),
(2, '李', '四', 'li.si@email.com', '13800138002', '上海', '2025-02-01', 1800.00, TRUE),
(3, '王', '五', 'wang.wu@email.com', '13800138003', '广州', '2025-02-15', 3200.00, TRUE),
(4, '赵', '六', 'zhao.liu@email.com', '13800138004', '深圳', '2025-03-01', 800.00, FALSE),
(5, '陈', '七', 'chen.qi@email.com', '13800138005', '杭州', '2025-03-15', 1500.00, TRUE);

-- 插入产品数据
INSERT INTO products (product_id, product_name, category, price, stock_quantity, supplier, created_date) VALUES
(101, '智能手机Pro', '电子产品', 3999.00, 50, '科技公司A', '2025-01-10'),
(102, '无线耳机', '电子产品', 299.00, 200, '科技公司B', '2025-01-15'),
(103, '笔记本电脑', '电子产品', 6999.00, 30, '科技公司C', '2025-01-20'),
(104, '智能手表', '电子产品', 1999.00, 80, '科技公司A', '2025-02-01'),
(105, '平板电脑', '电子产品', 2999.00, 40, '科技公司D', '2025-02-15'),
(106, '蓝牙音箱', '电子产品', 199.00, 150, '科技公司B', '2025-03-01');

-- 插入订单数据
INSERT INTO orders (order_id, customer_id, product_id, quantity, order_date, status, discount_rate, notes) VALUES
(1001, 1, 101, 1, '2025-03-01', 'completed', 0.05, '首次购买优惠'),
(1002, 2, 102, 2, '2025-03-02', 'completed', 0.00, ''),
(1003, 1, 103, 1, '2025-03-05', 'shipped', 0.10, 'VIP客户折扣'),
(1004, 3, 104, 1, '2025-03-08', 'completed', 0.00, ''),
(1005, 3, 105, 1, '2025-03-10', 'processing', 0.15, '批量购买折扣'),
(1006, 5, 106, 3, '2025-03-12', 'pending', 0.00, '团购订单'),
(1007, 2, 101, 1, '2025-03-15', 'cancelled', 0.00, '客户取消'),
(1008, 4, 102, 1, '2025-03-18', 'completed', 0.20, '清仓促销');

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

-- 删除已取消的订单
DELETE FROM orders WHERE status = 'cancelled';

-- =====================================================
-- 5. 函数创建测试
-- =====================================================

-- 创建计算订单总价的函数
CREATE FUNCTION calculate_total_price(product_price DECIMAL, quantity INT, discount DECIMAL)
RETURNS DECIMAL
BEGIN
    DECLARE total DECIMAL;
    SET total = product_price * quantity * (1 - discount);
    RETURN total;
END;

-- 创建获取客户等级的函数
CREATE FUNCTION get_customer_level(spent_amount DECIMAL)
RETURNS VARCHAR
BEGIN
    IF spent_amount >= 3000 THEN
        RETURN 'VIP';
    ELSEIF spent_amount >= 1500 THEN
        RETURN 'Gold';
    ELSEIF spent_amount >= 500 THEN
        RETURN 'Silver';
    ELSE
        RETURN 'Bronze';
    END IF;
END;

-- =====================================================
-- 6. 视图创建测试
-- =====================================================

-- 创建客户订单汇总视图
CREATE VIEW customer_order_summary AS
SELECT 
    c.customer_id,
    c.first_name,
    c.last_name,
    c.email,
    c.city,
    COUNT(o.order_id) as total_orders,
    SUM(p.price * o.quantity * (1 - o.discount_rate)) as total_order_value,
    AVG(p.price * o.quantity * (1 - o.discount_rate)) as avg_order_value,
    MAX(o.order_date) as last_order_date
FROM customers c
LEFT JOIN orders o ON c.customer_id = o.customer_id
LEFT JOIN products p ON o.product_id = p.product_id
WHERE o.status != 'cancelled' OR o.status IS NULL
GROUP BY c.customer_id, c.first_name, c.last_name, c.email, c.city;

-- =====================================================
-- 7. 复杂查询测试
-- =====================================================

-- 使用函数的查询
SELECT 
    o.order_id,
    CONCAT(c.first_name, ' ', c.last_name) as customer_name,
    p.product_name,
    o.quantity,
    p.price,
    o.discount_rate,
    calculate_total_price(p.price, o.quantity, o.discount_rate) as total_price
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
JOIN products p ON o.product_id = p.product_id
WHERE o.status = 'completed';

-- 使用视图的查询
SELECT 
    customer_id,
    CONCAT(first_name, ' ', last_name) as full_name,
    city,
    total_orders,
    total_order_value,
    get_customer_level(total_order_value) as customer_level
FROM customer_order_summary
WHERE total_orders > 0
ORDER BY total_order_value DESC;

-- 子查询测试
SELECT 
    p.product_name,
    p.price,
    p.stock_quantity,
    (SELECT COUNT(*) FROM orders o WHERE o.product_id = p.product_id AND o.status != 'cancelled') as times_ordered
FROM products p
WHERE p.price > (SELECT AVG(price) FROM products);

-- 高价值客户查询
SELECT 'High Value Customer' as type, first_name, last_name, total_spent as amount
FROM customers 
WHERE total_spent > 2000;

-- 高库存产品查询
SELECT 'High Stock Product' as type, product_name, category, stock_quantity as amount
FROM products 
WHERE stock_quantity > 100;

-- =====================================================
-- 8. 条件查询和排序测试
-- =====================================================

-- 复杂WHERE条件
SELECT 
    c.customer_id,
    CONCAT(c.first_name, ' ', c.last_name) as full_name,
    c.city,
    c.total_spent,
    c.registration_date
FROM customers c
WHERE (c.city = '北京' OR c.city = '上海') 
  AND c.total_spent BETWEEN 1000 AND 3000
  AND c.is_active = TRUE
ORDER BY c.total_spent DESC, c.registration_date ASC;

-- 分组和HAVING子句
SELECT 
    p.category,
    COUNT(*) as product_count,
    SUM(p.stock_quantity) as total_stock,
    AVG(p.price) as avg_price,
    MIN(p.price) as min_price,
    MAX(p.price) as max_price
FROM products p
GROUP BY p.category
HAVING COUNT(*) >= 2 AND AVG(p.price) > 1000;

-- =====================================================
-- 9. 数据验证查询
-- =====================================================

-- 检查表记录数
SELECT 'customers' as table_name, COUNT(*) as record_count FROM customers;
SELECT 'products' as table_name, COUNT(*) as record_count FROM products;
SELECT 'orders' as table_name, COUNT(*) as record_count FROM orders;

-- 检查数据完整性
SELECT 
    'Orders without customers' as check_type,
    COUNT(*) as issue_count
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.customer_id
WHERE c.customer_id IS NULL;

SELECT 
    'Orders without products' as check_type,
    COUNT(*) as issue_count
FROM orders o
LEFT JOIN products p ON o.product_id = p.product_id
WHERE p.product_id IS NULL;

-- =====================================================
-- 测试完成标记
-- =====================================================
-- 显示测试完成信息（使用虚拟表）
SELECT 'SparrowDB 功能验证测试完成' as test_status FROM customers LIMIT 1;
