-- =============================================
-- UPDATE 和 DELETE 语句演示
-- 基于 demo_products 表的各种修改操作
-- =============================================

-- 确保表存在并有数据
SELECT '=== 初始数据展示 ===' as demo_section;
SELECT * FROM demo_products ORDER BY product_id;

-- =============================================
-- UPDATE 语句示例
-- =============================================

SELECT '=== UPDATE 语句演示 ===' as demo_section;

-- 1. 单条记录更新 - 更新指定产品的价格
SELECT '--- 1. 单条记录价格更新 ---' as operation;
UPDATE demo_products 
SET price = 1399.99 
WHERE product_id = 1;

-- 验证更新结果
SELECT * FROM demo_products WHERE product_id = 1;

-- 2. 多字段同时更新 - 更新产品信息
SELECT '--- 2. 多字段同时更新 ---' as operation;
UPDATE demo_products 
SET price = 34.99, stock_quantity = 60, supplier = 'TechCorp Pro'
WHERE product_name = 'Wireless Mouse';

-- 验证更新结果
SELECT * FROM demo_products WHERE product_name = 'Wireless Mouse';

-- 3. 条件批量更新 - 所有电子产品降价10%
SELECT '--- 3. 批量价格调整 ---' as operation;
UPDATE demo_products 
SET price = price * 0.9 
WHERE category = 'Electronics';

-- 验证更新结果
SELECT product_name, category, price FROM demo_products WHERE category = 'Electronics';

-- 4. 基于计算的更新 - 库存不足的产品补货
SELECT '--- 4. 条件性库存补充 ---' as operation;
UPDATE demo_products 
SET stock_quantity = stock_quantity + 20 
WHERE stock_quantity < 15;

-- 验证更新结果
SELECT product_name, stock_quantity FROM demo_products WHERE stock_quantity >= 20;

-- 5. 字符串字段更新 - 统一供应商名称
SELECT '--- 5. 字符串字段标准化 ---' as operation;
UPDATE demo_products 
SET supplier = 'TechCorporation' 
WHERE supplier LIKE '%TechCorp%';

-- 验证更新结果
SELECT DISTINCT supplier FROM demo_products;

-- 6. 复杂条件更新 - 高价值产品标记
SELECT '--- 6. 复杂条件更新 ---' as operation;
UPDATE demo_products 
SET product_name = product_name + ' [Premium]'
WHERE price > 100 AND category = 'Electronics';

-- 验证更新结果
SELECT product_name, price, category FROM demo_products WHERE price > 100;

-- =============================================
-- DELETE 语句示例
-- =============================================

SELECT '=== DELETE 语句演示 ===' as demo_section;

-- 先插入一些测试数据用于删除演示
INSERT INTO demo_products VALUES 
(10, 'Test Product 1', 'Test', 9.99, 1, 'TestSupplier', '2024-01-10'),
(11, 'Test Product 2', 'Test', 19.99, 2, 'TestSupplier', '2024-01-11'),
(12, 'Obsolete Item', 'Obsolete', 5.99, 0, 'OldSupplier', '2024-01-12'),
(13, 'Expired Product', 'Test', 15.99, 0, 'TestSupplier', '2024-01-13');

-- 显示插入的测试数据
SELECT '--- 插入的测试数据 ---' as operation;
SELECT * FROM demo_products WHERE product_id >= 10;

-- 1. 单条记录删除 - 删除指定ID的产品
SELECT '--- 1. 单条记录删除 ---' as operation;
DELETE FROM demo_products WHERE product_id = 10;

-- 验证删除结果
SELECT COUNT(*) as remaining_count FROM demo_products WHERE product_id = 10;

-- 2. 条件批量删除 - 删除库存为0的产品
SELECT '--- 2. 批量删除零库存产品 ---' as operation;
DELETE FROM demo_products WHERE stock_quantity = 0;

-- 验证删除结果
SELECT product_name, stock_quantity FROM demo_products WHERE stock_quantity = 0;

-- 3. 基于字符串匹配删除 - 删除测试类别的产品
SELECT '--- 3. 按类别批量删除 ---' as operation;
DELETE FROM demo_products WHERE category = 'Test';

-- 验证删除结果
SELECT COUNT(*) as test_category_count FROM demo_products WHERE category = 'Test';

-- 4. 复杂条件删除 - 删除低价且库存少的产品
SELECT '--- 4. 复杂条件删除 ---' as operation;
DELETE FROM demo_products 
WHERE price < 20 AND stock_quantity < 5;

-- 验证删除结果
SELECT product_name, price, stock_quantity 
FROM demo_products 
WHERE price < 20 AND stock_quantity < 5;

-- 5. 基于供应商删除
SELECT '--- 5. 按供应商删除 ---' as operation;
DELETE FROM demo_products WHERE supplier = 'OldSupplier';

-- 验证删除结果
SELECT COUNT(*) as old_supplier_count FROM demo_products WHERE supplier = 'OldSupplier';

-- =============================================
-- 组合操作示例
-- =============================================

SELECT '=== 组合操作演示 ===' as demo_section;

-- 先添加一些新数据
INSERT INTO demo_products VALUES 
(14, 'New Electronics', 'Electronics', 199.99, 25, 'NewTech', '2024-01-14'),
(15, 'Budget Book', 'Books', 12.99, 100, 'CheapBooks', '2024-01-15');

-- 1. 更新后查询验证
SELECT '--- 更新操作 + 查询验证 ---' as operation;
UPDATE demo_products SET price = price * 1.1 WHERE supplier = 'NewTech';
SELECT product_name, price, supplier FROM demo_products WHERE supplier = 'NewTech';

-- 2. 条件删除后统计
SELECT '--- 删除操作 + 统计验证 ---' as operation;
DELETE FROM demo_products WHERE price < 15;
SELECT category, COUNT(*) as product_count, AVG(price) as avg_price 
FROM demo_products 
GROUP BY category;

-- =============================================
-- 最终数据状态展示
-- =============================================

SELECT '=== 最终数据状态 ===' as demo_section;
SELECT * FROM demo_products ORDER BY product_id;

SELECT '=== 数据统计信息 ===' as demo_section;
SELECT 
    COUNT(*) as total_products,
    COUNT(DISTINCT category) as categories,
    COUNT(DISTINCT supplier) as suppliers,
    MIN(price) as min_price,
    MAX(price) as max_price,
    AVG(price) as avg_price,
    SUM(stock_quantity) as total_stock
FROM demo_products;

-- =============================================
-- 操作说明和最佳实践
-- =============================================

/*
📝 UPDATE 和 DELETE 操作说明：

UPDATE 语句语法：
UPDATE table_name 
SET column1 = value1, column2 = value2, ...
WHERE condition;

DELETE 语句语法：
DELETE FROM table_name 
WHERE condition;

⚠️ 重要注意事项：

1. 始终使用 WHERE 条件
   - 没有 WHERE 的 UPDATE 会更新所有记录
   - 没有 WHERE 的 DELETE 会删除所有记录

2. 先查询后操作
   - 执行 UPDATE/DELETE 前先用 SELECT 验证条件
   - 确保 WHERE 条件匹配预期的记录数

3. 备份重要数据
   - 在生产环境中执行前先备份
   - 测试环境验证后再在生产环境执行

4. 使用事务（如果支持）
   - 复杂操作使用事务确保数据一致性
   - 出错时可以回滚

5. 性能考虑
   - 大批量操作考虑分批执行
   - 在 WHERE 条件中使用索引字段

🔧 常见使用场景：

UPDATE 场景：
- 价格调整
- 库存更新
- 状态变更
- 数据标准化
- 批量修正

DELETE 场景：
- 清理过期数据
- 删除测试数据
- 移除无效记录
- 数据归档前清理
- 维护数据质量
*/
