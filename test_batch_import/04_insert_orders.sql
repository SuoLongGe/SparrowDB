-- 批量导入测试 - 第四个文件：插入订单数据
-- 文件名：04_insert_orders.sql

-- 插入订单数据
INSERT INTO orders (order_id, user_id, order_date, total_amount) VALUES 
(1001, 1, '2024-03-01', 6399.97),
(1002, 2, '2024-03-02', 399.98),
(1003, 3, '2024-03-03', 1299.99),
(1004, 4, '2024-03-04', 89.98),
(1005, 5, '2024-03-05', 199.99);

INSERT INTO orders (order_id, user_id, order_date, total_amount) VALUES 
(1006, 1, '2024-03-10', 159.98),
(1007, 2, '2024-03-11', 29.99),
(1008, 3, '2024-03-12', 299.99);

-- 验证插入结果
SELECT COUNT(*) AS orders_count FROM orders;
SELECT user_id, COUNT(*) AS orders_per_user FROM orders GROUP BY user_id;
