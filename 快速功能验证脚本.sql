-- =============================================
-- 数据库系统快速功能验证脚本
-- 执行此脚本可快速验证系统主要功能
-- =============================================

-- 1. 基础功能验证
-- =============================================

-- 1.1 创建测试表
CREATE TABLE demo_products (
    product_id INT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price DECIMAL(10,2),
    stock_quantity INT,
    supplier VARCHAR(50),
    created_date DATE
);

-- 1.2 插入测试数据
INSERT INTO demo_products VALUES 
(1, 'Laptop Pro', 'Electronics', 1299.99, 10, 'TechCorp', '2024-01-01'),
(2, 'Wireless Mouse', 'Electronics', 29.99, 50, 'TechCorp', '2024-01-02'),
(3, 'Programming Book', 'Books', 59.99, 25, 'BookHouse', '2024-01-03'),
(4, 'Mechanical Keyboard', 'Electronics', 129.99, 15, 'TechCorp', '2024-01-04'),
(5, 'Design Guidebook', 'Books', 49.99, 30, 'BookHouse', '2024-01-05'),
(6, 'Gaming Monitor', 'Electronics', 299.99, 8, 'DisplayTech', '2024-01-06'),
(7, 'Database Theory', 'Books', 79.99, 20, 'BookHouse', '2024-01-07'),
(8, 'USB-C Hub', 'Electronics', 39.99, 35, 'TechCorp', '2024-01-08');

-- 1.3 基础查询验证
SELECT '=== 基础查询测试 ===' as test_name;
SELECT * FROM demo_products;

SELECT '=== 条件查询测试 ===' as test_name;
SELECT * FROM demo_products WHERE category = 'Electronics';

SELECT '=== 聚合查询测试 ===' as test_name;
SELECT supplier, COUNT(*) as product_count, AVG(price) as avg_price 
FROM demo_products 
GROUP BY supplier;

-- 2. 分片存储功能验证
-- =============================================

SELECT '=== 分片存储测试 ===' as test_name;

-- 2.1 创建分片表
CREATE SHARD demo_products BY supplier USING RANGE (4)

-- 2.2 查看分片信息
SHOW SHARDS;

-- 2.3 分片统计信息
SHARD STATS;

-- 2.4 分片查询测试
SELECT '=== 分片查询测试 ===' as test_name;
SELECT * FROM demo_products WHERE supplier = 'TechCorp';

-- 3. 用户自定义函数验证
-- =============================================

SELECT '=== 用户函数测试 ===' as test_name;

-- 3.1 创建计算折扣的函数
CREATE PERMANENT FUNCTION calculate_discount(original_price DECIMAL, discount_rate DECIMAL) 
RETURNS DECIMAL 
BEGIN 
    RETURN original_price * discount_rate;
END;


-- 3.3 使用函数查询
SELECT 
    product_name,
    price,
    calculate_discount(price, 0.1) as discounted_price
FROM demo_products 
WHERE category = 'Electronics';

-- 4. 列式存储验证
-- =============================================

SELECT '=== 列式存储测试 ===' as test_name;

-- 4.1 创建列式存储表
CREATE TABLE demo_analytics (
    id INT,
    product_type VARCHAR(50),
    sales_amount DECIMAL(10,2),
    quarter VARCHAR(10),
    region VARCHAR(30)
) ENGINE=COLUMNAR;

-- 4.2 插入分析数据
INSERT INTO demo_analytics VALUES 
(1, 'Electronics', 15000.00, '2024Q1', 'North'),
(2, 'Books', 8500.00, '2024Q1', 'South'),
(3, 'Electronics', 18200.00, '2024Q2', 'North'),
(4, 'Books', 9100.00, '2024Q2', 'South'),
(5, 'Electronics', 22000.00, '2024Q3', 'East'),
(6, 'Books', 7800.00, '2024Q3', 'West'),
(7, 'Electronics', 19500.00, '2024Q4', 'North'),
(8, 'Books', 9800.00, '2024Q4', 'South');

-- 4.3 列式存储查询测试
SELECT quarter, product_type, SUM(sales_amount) as total_sales 
FROM demo_analytics 
GROUP BY quarter, product_type 
ORDER BY quarter, product_type;

-- 5. 查询优化验证
-- =============================================

SELECT '=== 查询优化测试 ===' as test_name;

-- 5.1 谓词下推测试（观察控制台输出的优化信息）
SELECT * FROM demo_products 
WHERE supplier = 'TechCorp' AND price > 50;

-- 5.2 复杂查询优化测试
SELECT 
    supplier,
    category,
    COUNT(*) as product_count,
    AVG(price) as avg_price,
    SUM(stock_quantity) as total_stock
FROM demo_products 
WHERE price > 30 
GROUP BY supplier, category 
HAVING COUNT(*) > 1
ORDER BY category DESC;

-- 6. 性能验证查询
-- =============================================

SELECT '=== 性能验证测试 ===' as test_name;

-- 6.1 全表扫描性能        !!!!!!!!!!!!!!!!!!!!!!!!!!
SELECT COUNT(*) as total_products FROM demo_products;

-- 6.2 索引查询性能
SELECT * FROM demo_products WHERE product_id = 5;

-- 6.3 范围查询性能
SELECT * FROM demo_products WHERE price BETWEEN 50 AND 200;

-- 6.4 排序查询性能       !!!!!!!!!!!!!!!!!!!!!!!!!!
SELECT * FROM demo_products ORDER BY price DESC;

-- 7. 数据修改验证
-- =============================================

SELECT '=== 数据修改测试 ===' as test_name;

-- 7.1 更新测试           !!!!!!!!!!!!!!!!!!!!!!!!!!
UPDATE demo_products SET stock_quantity = stock_quantity + 5 WHERE category = 'Books';

-- 7.2 验证更新结果
SELECT product_name, stock_quantity FROM demo_products WHERE category = 'Books';

-- 7.3 插入新记录测试
INSERT INTO demo_products VALUES 
(9, 'Wireless Headphones', 'Electronics', 79.99, 20, 'AudioTech', '2024-01-09');

-- 7.4 验证插入结果
SELECT * FROM demo_products WHERE product_id = 9;

-- 8. 系统信息查询
-- =============================================

SELECT '=== 系统信息查询 ===' as test_name;

-- 8.1 查看所有表
SELECT '当前数据库表:' as info;

-- 8.2 查看分片信息
SHOW SHARDS;

-- 8.3 查看函数信息（在GUI侧边栏查看）

-- =============================================
-- 验证完成提示
-- =============================================

SELECT '=== 🎉 快速验证完成! ===' as completion_message;
SELECT '请检查以下几点:' as check_points;
SELECT '1. 所有查询都成功执行' as point_1;
SELECT '2. 分片信息正确显示' as point_2;
SELECT '3. 函数在GUI侧边栏显示' as point_3;
SELECT '4. 控制台显示优化信息' as point_4;
SELECT '5. 数据修改操作成功' as point_5;

-- =============================================
-- 性能测试建议
-- =============================================

/*
🚀 性能测试建议：

1. 记录每个查询的执行时间
2. 观察控制台输出的优化信息：
   - "谓词下推优化完成"
   - "数据过滤率: X%"
   - "使用XX索引查询"

3. 检查文件系统变化：
   - data/main/demo_products_shard_*.tbl (分片文件)
   - data/main/demo_analytics/ (列式存储目录)

4. GUI功能验证：
   - 侧边栏显示表和函数
   - 查询结果正确展示
   - 错误信息友好显示

5. 并发测试：
   - 打开多个查询窗口
   - 同时执行不同查询
   - 观察响应时间

📊 预期性能提升：
- 分片查询: 30-70% 性能提升
- 列式聚合: 20-50% 性能提升  
- 谓词下推: 50-80% 数据过滤率
- 并发处理: 25-40% 响应时间改善
*/
