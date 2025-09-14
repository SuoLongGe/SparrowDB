-- ============================================
-- SparrowDB 删除表操作指南
-- ============================================

-- 1. 查看当前所有表
-- 在GUI或命令行中输入以下命令查看现有表：
-- tables

-- 2. 删除products表的几种方法：

-- 方法1: 直接删除（如果确定表存在）
DROP TABLE products;

-- 方法2: 安全删除（推荐，不会因表不存在而报错）
DROP TABLE IF EXISTS products;

-- 方法3: 删除多个表
DROP TABLE IF EXISTS products, orders, customers;

-- 3. 删除后验证（检查表是否真的被删除）
-- 再次执行 tables 命令查看表列表

-- ============================================
-- 完整的清理和重建示例
-- ============================================

-- 步骤1: 删除可能存在的测试表
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS test_table;

-- 步骤2: 重新创建products表（如果需要）
CREATE TABLE products (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2),
    category VARCHAR(50),
    stock_quantity INT DEFAULT 0
);

-- 步骤3: 插入测试数据
INSERT INTO products (id, name, price, category, stock_quantity) VALUES 
(1, 'Laptop', 999.99, 'Electronics', 50),
(2, 'Phone', 599.99, 'Electronics', 100),
(3, 'Book', 29.99, 'Education', 200);

-- 步骤4: 验证数据
SELECT * FROM products;

-- ============================================
-- 注意事项
-- ============================================

/*
1. 删除表会永久删除所有数据，请谨慎操作
2. 建议使用 IF EXISTS 避免错误
3. 删除前可以先备份重要数据
4. 如果有外键约束，需要先删除相关约束
*/

-- 备份数据示例（删除前）：
-- CREATE TABLE products_backup AS SELECT * FROM products;

-- 删除视图（如果products表被视图引用）：
-- DROP VIEW IF EXISTS product_view;

-- 然后删除表：
-- DROP TABLE IF EXISTS products;
