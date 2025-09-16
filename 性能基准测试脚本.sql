-- =============================================
-- 数据库系统性能基准测试脚本
-- 用于量化测试各项性能提升
-- =============================================

-- 测试准备
-- =============================================

-- 创建大数据量测试表
CREATE TABLE perf_test_products (
    product_id INT PRIMARY KEY,
    product_name VARCHAR(100),
    category VARCHAR(50),
    price DECIMAL(10,2),
    stock_quantity INT,
    supplier VARCHAR(50),
    description TEXT,
    created_date DATE,
    last_updated TIMESTAMP
);

-- 插入大量测试数据（建议插入1000+条记录）
-- 可以通过批量插入或循环插入实现

-- 批量插入示例数据
INSERT INTO perf_test_products VALUES 
(1, 'Product 1', 'Electronics', 99.99, 10, 'Supplier A', 'Description 1', '2024-01-01', CURRENT_TIMESTAMP),
(2, 'Product 2', 'Books', 29.99, 20, 'Supplier B', 'Description 2', '2024-01-02', CURRENT_TIMESTAMP),
(3, 'Product 3', 'Electronics', 199.99, 5, 'Supplier C', 'Description 3', '2024-01-03', CURRENT_TIMESTAMP),
(4, 'Product 4', 'Clothing', 59.99, 15, 'Supplier A', 'Description 4', '2024-01-04', CURRENT_TIMESTAMP),
(5, 'Product 5', 'Electronics', 299.99, 8, 'Supplier D', 'Description 5', '2024-01-05', CURRENT_TIMESTAMP);
-- 继续插入更多数据...

-- =============================================
-- 1. 单表 vs 分片表性能对比测试
-- =============================================

SELECT '=== 1. 分片存储性能测试 ===' as test_section;

-- 1.1 单表基准测试
SELECT '--- 单表基准测试 ---' as test_name;

-- 单表全表扫描
-- 🕐 开始时间记录
SELECT COUNT(*) as total_records FROM perf_test_products;
-- 🕐 结束时间记录 - 记录为: single_full_scan_time

-- 单表条件查询
-- 🕐 开始时间记录
SELECT * FROM perf_test_products WHERE supplier = 'Supplier A';
-- 🕐 结束时间记录 - 记录为: single_condition_time

-- 单表聚合查询
-- 🕐 开始时间记录
SELECT supplier, COUNT(*) as count, AVG(price) as avg_price 
FROM perf_test_products 
GROUP BY supplier;
-- 🕐 结束时间记录 - 记录为: single_aggregate_time

-- 1.2 创建分片表
CREATE SHARD perf_test_products BY supplier USING RANGE (4);

-- 1.3 分片表性能测试
SELECT '--- 分片表性能测试 ---' as test_name;

-- 分片表全表扫描
-- 🕐 开始时间记录
SELECT COUNT(*) as total_records FROM perf_test_products;
-- 🕐 结束时间记录 - 记录为: shard_full_scan_time

-- 分片表条件查询（单分片查询）
-- 🕐 开始时间记录
SELECT * FROM perf_test_products WHERE supplier = 'Supplier A';
-- 🕐 结束时间记录 - 记录为: shard_condition_time

-- 分片表聚合查询
-- 🕐 开始时间记录
SELECT supplier, COUNT(*) as count, AVG(price) as avg_price 
FROM perf_test_products 
GROUP BY supplier;
-- 🕐 结束时间记录 - 记录为: shard_aggregate_time

-- =============================================
-- 2. 行式 vs 列式存储性能对比测试
-- =============================================

SELECT '=== 2. 存储引擎性能测试 ===' as test_section;

-- 2.1 创建行式存储表
CREATE TABLE perf_test_row_storage (
    id INT,
    category VARCHAR(50),
    amount DECIMAL(10,2),
    quarter VARCHAR(10),
    region VARCHAR(30),
    year INT,
    month INT
);

-- 2.2 创建列式存储表
CREATE TABLE perf_test_col_storage (
    id INT,
    category VARCHAR(50),
    amount DECIMAL(10,2),
    quarter VARCHAR(10),
    region VARCHAR(30),
    year INT,
    month INT
) ENGINE=COLUMNAR;

-- 2.3 插入相同的测试数据
INSERT INTO perf_test_row_storage VALUES 
(1, 'Electronics', 1500.00, '2024Q1', 'North', 2024, 1),
(2, 'Books', 850.00, '2024Q1', 'South', 2024, 1),
(3, 'Electronics', 1820.00, '2024Q1', 'East', 2024, 2),
(4, 'Books', 910.00, '2024Q1', 'West', 2024, 2),
(5, 'Electronics', 2200.00, '2024Q2', 'North', 2024, 4),
(6, 'Books', 780.00, '2024Q2', 'South', 2024, 4),
(7, 'Electronics', 1950.00, '2024Q2', 'East', 2024, 5),
(8, 'Books', 980.00, '2024Q2', 'West', 2024, 5);

INSERT INTO perf_test_col_storage VALUES 
(1, 'Electronics', 1500.00, '2024Q1', 'North', 2024, 1),
(2, 'Books', 850.00, '2024Q1', 'South', 2024, 1),
(3, 'Electronics', 1820.00, '2024Q1', 'East', 2024, 2),
(4, 'Books', 910.00, '2024Q1', 'West', 2024, 2),
(5, 'Electronics', 2200.00, '2024Q2', 'North', 2024, 4),
(6, 'Books', 780.00, '2024Q2', 'South', 2024, 4),
(7, 'Electronics', 1950.00, '2024Q2', 'East', 2024, 5),
(8, 'Books', 980.00, '2024Q2', 'West', 2024, 5);

-- 2.4 聚合查询性能对比
SELECT '--- 行式存储聚合查询 ---' as test_name;
-- 🕐 开始时间记录
SELECT category, SUM(amount) as total_amount, AVG(amount) as avg_amount
FROM perf_test_row_storage 
GROUP BY category;
-- 🕐 结束时间记录 - 记录为: row_aggregate_time

SELECT '--- 列式存储聚合查询 ---' as test_name;
-- 🕐 开始时间记录
SELECT category, SUM(amount) as total_amount, AVG(amount) as avg_amount
FROM perf_test_col_storage 
GROUP BY category;
-- 🕐 结束时间记录 - 记录为: col_aggregate_time

-- 2.5 列筛选查询性能对比
SELECT '--- 行式存储列筛选 ---' as test_name;
-- 🕐 开始时间记录
SELECT category, amount FROM perf_test_row_storage WHERE year = 2024;
-- 🕐 结束时间记录 - 记录为: row_column_select_time

SELECT '--- 列式存储列筛选 ---' as test_name;
-- 🕐 开始时间记录
SELECT category, amount FROM perf_test_col_storage WHERE year = 2024;
-- 🕐 结束时间记录 - 记录为: col_column_select_time

-- =============================================
-- 3. 查询优化性能测试
-- =============================================

SELECT '=== 3. 查询优化性能测试 ===' as test_section;

-- 3.1 谓词下推效果测试
SELECT '--- 谓词下推优化测试 ---' as test_name;
-- 观察控制台输出的优化信息
-- 🕐 开始时间记录
SELECT * FROM perf_test_products 
WHERE supplier = 'Supplier A' AND price > 50 AND category = 'Electronics';
-- 🕐 结束时间记录 - 记录为: predicate_pushdown_time
-- 📊 记录过滤率: _____%

-- 3.2 智能索引选择测试
SELECT '--- 智能索引选择测试 ---' as test_name;

-- 等值查询（应选择哈希索引）
-- 🕐 开始时间记录
SELECT * FROM perf_test_products WHERE product_id = 100;
-- 🕐 结束时间记录 - 记录为: hash_index_time

-- 范围查询（应选择B+树索引）
-- 🕐 开始时间记录
SELECT * FROM perf_test_products WHERE price BETWEEN 50 AND 200;
-- 🕐 结束时间记录 - 记录为: btree_range_time

-- 排序查询（应选择B+树索引）
-- 🕐 开始时间记录
SELECT * FROM perf_test_products ORDER BY price LIMIT 10;
-- 🕐 结束时间记录 - 记录为: btree_sort_time

-- =============================================
-- 4. 并发性能测试
-- =============================================

SELECT '=== 4. 并发性能测试说明 ===' as test_section;

/*
并发测试需要手动执行：

1. 打开5个查询窗口
2. 同时执行以下查询：

窗口1: SELECT * FROM perf_test_products WHERE supplier = 'Supplier A';
窗口2: SELECT * FROM perf_test_products WHERE supplier = 'Supplier B';  
窗口3: SELECT * FROM perf_test_products WHERE supplier = 'Supplier C';
窗口4: SELECT * FROM perf_test_products WHERE category = 'Electronics';
窗口5: SELECT COUNT(*) FROM perf_test_products;

3. 记录各窗口执行时间：
   - 窗口1时间: _____ms
   - 窗口2时间: _____ms  
   - 窗口3时间: _____ms
   - 窗口4时间: _____ms
   - 窗口5时间: _____ms

4. 计算平均响应时间和最大响应时间
*/

-- =============================================
-- 5. 批量操作性能测试
-- =============================================

SELECT '=== 5. 批量操作性能测试 ===' as test_section;

-- 5.1 批量插入性能测试
SELECT '--- 批量插入测试 ---' as test_name;
-- 🕐 开始时间记录
INSERT INTO perf_test_products VALUES 
(1001, 'Batch Product 1', 'Electronics', 50.00, 10, 'Supplier A', 'Batch Description 1', '2024-01-01', CURRENT_TIMESTAMP),
(1002, 'Batch Product 2', 'Books', 30.00, 15, 'Supplier B', 'Batch Description 2', '2024-01-02', CURRENT_TIMESTAMP),
(1003, 'Batch Product 3', 'Electronics', 70.00, 8, 'Supplier C', 'Batch Description 3', '2024-01-03', CURRENT_TIMESTAMP),
(1004, 'Batch Product 4', 'Clothing', 40.00, 20, 'Supplier A', 'Batch Description 4', '2024-01-04', CURRENT_TIMESTAMP),
(1005, 'Batch Product 5', 'Electronics', 90.00, 12, 'Supplier D', 'Batch Description 5', '2024-01-05', CURRENT_TIMESTAMP);
-- 🕐 结束时间记录 - 记录为: batch_insert_time

-- 5.2 批量更新性能测试
SELECT '--- 批量更新测试 ---' as test_name;
-- 🕐 开始时间记录
UPDATE perf_test_products SET stock_quantity = stock_quantity + 5 WHERE category = 'Electronics';
-- 🕐 结束时间记录 - 记录为: batch_update_time

-- 5.3 批量删除性能测试
SELECT '--- 批量删除测试 ---' as test_name;
-- 🕐 开始时间记录
DELETE FROM perf_test_products WHERE product_id > 1000;
-- 🕐 结束时间记录 - 记录为: batch_delete_time

-- =============================================
-- 6. 内存和I/O性能测试
-- =============================================

SELECT '=== 6. 内存和I/O性能测试 ===' as test_section;

-- 6.1 大数据集查询测试
SELECT '--- 大数据集查询测试 ---' as test_name;
-- 🕐 开始时间记录
SELECT supplier, category, COUNT(*) as count, SUM(price * stock_quantity) as total_value
FROM perf_test_products 
GROUP BY supplier, category
HAVING COUNT(*) > 0
ORDER BY total_value DESC;
-- 🕐 结束时间记录 - 记录为: large_dataset_time

-- 6.2 复杂JOIN查询测试（如果有相关表）
-- 可以根据实际情况添加JOIN测试

-- =============================================
-- 性能测试结果记录表
-- =============================================

/*
📊 性能测试结果记录：

1. 分片存储性能对比：
   - 单表全表扫描: _____ms
   - 分片表全表扫描: _____ms  
   - 性能提升: _____%
   
   - 单表条件查询: _____ms
   - 分片表条件查询: _____ms
   - 性能提升: _____%
   
   - 单表聚合查询: _____ms
   - 分片表聚合查询: _____ms
   - 性能提升: _____%

2. 存储引擎性能对比：
   - 行式存储聚合: _____ms
   - 列式存储聚合: _____ms
   - 性能提升: _____%
   
   - 行式存储列筛选: _____ms
   - 列式存储列筛选: _____ms  
   - 性能提升: _____%

3. 查询优化效果：
   - 谓词下推查询: _____ms
   - 数据过滤率: _____%
   
   - 哈希索引查询: _____ms
   - B+树范围查询: _____ms
   - B+树排序查询: _____ms

4. 并发性能：
   - 平均响应时间: _____ms
   - 最大响应时间: _____ms
   - 并发吞吐量: _____查询/秒

5. 批量操作性能：
   - 批量插入(5条): _____ms
   - 平均单条插入: _____ms
   - 批量更新: _____ms
   - 批量删除: _____ms

🎯 性能提升目标验证：
- □ 分片查询性能提升 ≥ 30%
- □ 列式聚合性能提升 ≥ 20%  
- □ 谓词下推过滤率 ≥ 50%
- □ 并发响应时间改善 ≥ 25%
*/

-- =============================================
-- 测试完成
-- =============================================

SELECT '=== 🏁 性能基准测试完成! ===' as completion;
SELECT '请根据记录的时间数据计算性能提升比例' as instruction;
SELECT '对比结果应显示明显的性能优势' as expectation;
