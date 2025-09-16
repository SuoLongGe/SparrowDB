-- =============================================
-- 子查询优化器测试脚本
-- 基于快速功能验证脚本中建好的表，创建能触发子查询优化的嵌套查询
-- =============================================

-- 前置说明：请先运行快速功能验证脚本.sql 创建基础表和数据
-- 本脚本将展示不同类型的嵌套子查询，触发已实现的SQL重写器优化

SELECT '=== 🔧 子查询优化器测试开始 ===' as test_header;

-- =============================================
-- 1. IN子查询优化测试 (会被改写为EXISTS或LEFT JOIN)
-- =============================================

SELECT '=== 测试1: IN子查询优化 ===' as test_name;

-- 1.1 查找有高价产品的供应商的所有产品
SELECT product_name, supplier, price 
FROM demo_products 
WHERE supplier IN (
    SELECT supplier 
    FROM demo_products 
    WHERE price > 100
);

-- 1.2 查找有电子产品的供应商的所有图书产品
SELECT product_name, supplier, category, price
FROM demo_products 
WHERE supplier IN (
    SELECT DISTINCT supplier 
    FROM demo_products 
    WHERE category = 'Electronics'
) AND category = 'Books';

-- =============================================
-- 2. EXISTS子查询优化测试 (会被改写为INNER JOIN)
-- =============================================

SELECT '=== 测试2: EXISTS子查询优化 ===' as test_name;

-- 2.1 查找存在高价产品的供应商的所有产品
SELECT p1.product_name, p1.supplier, p1.price
FROM demo_products p1
WHERE EXISTS (
    SELECT 1 
    FROM demo_products p2 
    WHERE p2.supplier = p1.supplier 
    AND p2.price > 100
);

-- 2.2 查找存在库存不足(小于20)产品的供应商
SELECT DISTINCT p1.supplier, COUNT(*) as total_products
FROM demo_products p1
WHERE EXISTS (
    SELECT 1 
    FROM demo_products p2 
    WHERE p2.supplier = p1.supplier 
    AND p2.stock_quantity < 20
)
GROUP BY p1.supplier;

-- =============================================
-- 3. 标量子查询优化测试 (比较操作中的子查询)
-- =============================================

SELECT '=== 测试3: 标量子查询优化 ===' as test_name;

-- 3.1 查找价格高于平均价格的产品
SELECT product_name, price, 
       (price - (SELECT AVG(price) FROM demo_products)) as price_diff
FROM demo_products 
WHERE price > (SELECT AVG(price) FROM demo_products);

-- 3.2 查找价格高于同类别最低价格的产品
SELECT p1.product_name, p1.category, p1.price
FROM demo_products p1
WHERE p1.price > (
    SELECT MIN(p2.price) 
    FROM demo_products p2 
    WHERE p2.category = p1.category
);

-- =============================================
-- 4. SELECT列表中的子查询优化测试
-- =============================================

SELECT '=== 测试4: SELECT列表子查询优化 ===' as test_name;

-- 4.1 查询每个产品及其供应商的总产品数
SELECT 
    product_name,
    supplier,
    price,
    (SELECT COUNT(*) 
     FROM demo_products p2 
     WHERE p2.supplier = demo_products.supplier) as supplier_product_count
FROM demo_products;

-- 4.2 查询每个产品在同类别中的价格排名
SELECT 
    product_name,
    category,
    price,
    (SELECT COUNT(*) + 1
     FROM demo_products p2 
     WHERE p2.category = demo_products.category 
     AND p2.price > demo_products.price) as price_rank_in_category
FROM demo_products
ORDER BY category, price DESC;

-- =============================================
-- 5. HAVING子句中的子查询优化测试
-- =============================================

SELECT '=== 测试5: HAVING子句子查询优化 ===' as test_name;

-- 5.1 查找平均价格高于全局平均价格的供应商
SELECT 
    supplier,
    COUNT(*) as product_count,
    AVG(price) as avg_price
FROM demo_products 
GROUP BY supplier
HAVING AVG(price) > (SELECT AVG(price) FROM demo_products);

-- 5.2 查找产品数量超过电子产品类别平均产品数的供应商
SELECT 
    supplier,
    COUNT(*) as product_count,
    AVG(stock_quantity) as avg_stock
FROM demo_products 
GROUP BY supplier
HAVING COUNT(*) > (
    SELECT AVG(category_count) 
    FROM (
        SELECT COUNT(*) as category_count 
        FROM demo_products 
        WHERE category = 'Electronics' 
        GROUP BY supplier
    ) as electronics_stats
);

-- =============================================
-- 6. 复杂嵌套子查询测试 (多层嵌套)
-- =============================================

SELECT '=== 测试6: 复杂嵌套子查询优化 ===' as test_name;

-- 6.1 三层嵌套查询：查找价格在前50%的供应商的所有产品
SELECT product_name, supplier, price
FROM demo_products 
WHERE supplier IN (
    SELECT supplier 
    FROM demo_products 
    WHERE price > (
        SELECT AVG(price) 
        FROM demo_products 
        WHERE category IN (
            SELECT DISTINCT category 
            FROM demo_products 
            WHERE stock_quantity > 20
        )
    )
);

-- 6.2 相关子查询：查找每个供应商价格最高的产品
SELECT p1.product_name, p1.supplier, p1.price
FROM demo_products p1
WHERE p1.price = (
    SELECT MAX(p2.price) 
    FROM demo_products p2 
    WHERE p2.supplier = p1.supplier
);

-- =============================================
-- 7. 子查询与分片表的组合测试
-- =============================================

SELECT '=== 测试7: 分片表子查询优化 ===' as test_name;

-- 注意：这些查询在分片表上执行，测试子查询优化与分片的协同工作

-- 7.1 在分片表上使用IN子查询
SELECT product_name, supplier, price 
FROM demo_products 
WHERE supplier IN (
    SELECT supplier 
    FROM demo_products 
    WHERE stock_quantity > 25
);

-- 7.2 在分片表上使用EXISTS子查询
SELECT p1.product_name, p1.supplier
FROM demo_products p1
WHERE EXISTS (
    SELECT 1 
    FROM demo_products p2 
    WHERE p2.supplier = p1.supplier 
    AND p2.category = 'Electronics'
);

-- =============================================
-- 8. 与用户自定义函数结合的子查询测试
-- =============================================

SELECT '=== 测试8: 函数与子查询组合优化 ===' as test_name;

-- 8.1 使用自定义函数的子查询
SELECT 
    product_name,
    price,
    calculate_discount(price, 0.1) as discounted_price
FROM demo_products 
WHERE price > (
    SELECT AVG(calculate_discount(price, 0.1)) 
    FROM demo_products 
    WHERE category = 'Electronics'
);

-- 8.2 子查询中使用函数
SELECT product_name, supplier, price
FROM demo_products 
WHERE supplier IN (
    SELECT supplier 
    FROM demo_products 
    WHERE calculate_discount(price, 0.2) > 20
);

-- =============================================
-- 验证和观察说明
-- =============================================

SELECT '=== 🎯 子查询优化验证完成! ===' as completion_message;

/*
🔍 观察要点：

1. 控制台输出检查：
   - "=== 子查询改写器开始工作 ==="
   - "检测到IN子查询，可改写为EXISTS"
   - "检测到IN子查询，可改写为LEFT JOIN"
   - "检测到EXISTS子查询，可改写为INNER JOIN"
   - "检测到标量子查询，可进行优化"

2. 优化信息显示：
   - "=== 子查询优化检测 ==="
   - "原始SQL: [原始查询]"
   - "优化类型: [具体的优化类型]"
   - "改写后SQL: [重写后的SQL]"
   - "=== 子查询优化完成 ==="

3. 性能对比：
   - 记录每个查询的执行时间
   - 观察优化前后的性能差异
   - 检查是否正确触发了子查询重写

4. 重写SQL检查：
   - IN子查询是否改写为JOIN
   - EXISTS子查询是否改写为INNER JOIN
   - 标量子查询是否进行了优化
   - 复杂嵌套是否正确处理

📊 预期优化效果：
- IN子查询 → EXISTS/JOIN: 20-50% 性能提升
- EXISTS子查询 → INNER JOIN: 30-60% 性能提升
- 标量子查询优化: 10-30% 性能提升
- 复杂嵌套优化: 40-70% 性能提升

🚀 测试建议：
1. 逐个执行查询，观察控制台输出
2. 对比优化前后的执行时间
3. 检查重写后的SQL是否合理
4. 验证查询结果的正确性
5. 测试在分片表上的优化效果
*/
