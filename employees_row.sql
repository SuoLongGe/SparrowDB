CREATE TABLE demo_product (
                               product_id INT PRIMARY KEY,
                               product_name VARCHAR(100) NOT NULL,
                               category VARCHAR(50),
                               price DECIMAL(10,2),
                               stock_quantity INT,
                               supplier VARCHAR(50),
                               created_date DATE
);

-- 1.2 插入测试数据
INSERT INTO demo_product VALUES
                              (1, 'Laptop Pro', 'Electronics', 1299.99, 10, 'TechCorp', '2024-01-01'),
                              (2, 'Wireless Mouse', 'Electronics', 29.99, 50, 'TechCorp', '2024-01-02'),
                              (3, 'Programming Book', 'Books', 59.99, 25, 'BookHouse', '2024-01-03'),
                              (4, 'Mechanical Keyboard', 'Electronics', 129.99, 15, 'TechCorp', '2024-01-04'),
                              (5, 'Design Guidebook', 'Books', 49.99, 30, 'BookHouse', '2024-01-05'),
                              (6, 'Gaming Monitor', 'Electronics', 299.99, 8, 'DisplayTech', '2024-01-06'),
                              (7, 'Database Theory', 'Books', 79.99, 20, 'BookHouse', '2024-01-07'),
                              (8, 'USB-C Hub', 'Electronics', 39.99, 35, 'TechCorp', '2024-01-08');