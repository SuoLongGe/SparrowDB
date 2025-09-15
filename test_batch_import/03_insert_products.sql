-- 批量导入测试 - 第三个文件：插入产品数据
-- 文件名：03_insert_products.sql

-- 插入产品数据
INSERT INTO products (product_id, product_name, price, category) VALUES 
(101, '笔记本电脑', 5999.99, '电子产品'),
(102, '无线鼠标', 99.99, '电子产品'),
(103, '机械键盘', 299.99, '电子产品'),
(104, '显示器', 1299.99, '电子产品'),
(105, '耳机', 199.99, '电子产品');

INSERT INTO products (product_id, product_name, price, category) VALUES 
(201, '咖啡杯', 29.99, '生活用品'),
(202, '保温杯', 59.99, '生活用品'),
(203, '台灯', 89.99, '生活用品');

-- 验证插入结果
SELECT COUNT(*) AS products_count FROM products;
SELECT category, COUNT(*) AS count_per_category FROM products GROUP BY category;
