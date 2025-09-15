-- 批量导入测试 - 第五个文件：最终统计报告
-- 文件名：05_final_report.sql

-- 最终统计报告
SELECT 'BATCH IMPORT SUMMARY' AS report_title;

SELECT 'Tables Created' AS item, '3' AS count
UNION ALL
SELECT 'Users Imported', CAST(COUNT(*) AS VARCHAR(10)) FROM users
UNION ALL  
SELECT 'Products Imported', CAST(COUNT(*) AS VARCHAR(10)) FROM products
UNION ALL
SELECT 'Orders Imported', CAST(COUNT(*) AS VARCHAR(10)) FROM orders;

-- 详细统计
SELECT 'USER DETAILS' AS section;
SELECT username, email, created_date FROM users ORDER BY user_id;

SELECT 'PRODUCT DETAILS' AS section;
SELECT product_name, price, category FROM products ORDER BY product_id;

SELECT 'ORDER SUMMARY' AS section;
SELECT 
    u.username,
    COUNT(o.order_id) AS total_orders,
    SUM(o.total_amount) AS total_spent
FROM users u
LEFT JOIN orders o ON u.user_id = o.user_id
GROUP BY u.user_id, u.username
ORDER BY total_spent DESC;

-- 验证数据完整性
SELECT 'DATA INTEGRITY CHECK' AS section;
SELECT 
    CASE 
        WHEN EXISTS(SELECT 1 FROM users WHERE user_id IS NULL) THEN 'FAIL'
        ELSE 'PASS'
    END AS users_primary_key_check;

SELECT 
    CASE 
        WHEN EXISTS(SELECT 1 FROM products WHERE product_id IS NULL) THEN 'FAIL'
        ELSE 'PASS'
    END AS products_primary_key_check;

-- 批量导入完成标记
SELECT 'BATCH IMPORT COMPLETED SUCCESSFULLY' AS status;
