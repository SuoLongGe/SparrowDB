-- 创建demo_products表和测试COUNT函数
CREATE TABLE demo_products (
    product_id INT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price DECIMAL(10,2),
    stock_quantity INT,
    supplier VARCHAR(50),
    created_date DATE
);

-- 插入测试数据
INSERT INTO demo_products VALUES 
(1, 'Laptop Pro', 'Electronics', 1299.99, 10, 'TechCorp', '2024-01-01'),
(2, 'Wireless Mouse', 'Electronics', 29.99, 50, 'TechCorp', '2024-01-02'),
(3, 'Programming Book', 'Books', 59.99, 25, 'BookHouse', '2024-01-03');

-- 测试COUNT函数
SELECT COUNT(*) as total_products FROM demo_products;

