-- 批量导入测试 - 第一个文件：初始化数据库结构
-- 文件名：01_init_schema.sql

-- 创建用户表
CREATE TABLE users (
    user_id INT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    created_date DATE
);

-- 创建产品表
CREATE TABLE products (
    product_id INT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2),
    category VARCHAR(50)
);

-- 创建订单表
CREATE TABLE orders (
    order_id INT PRIMARY KEY,
    user_id INT,
    order_date DATE,
    total_amount DECIMAL(12,2)
);
