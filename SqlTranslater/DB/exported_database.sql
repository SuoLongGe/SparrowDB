-- ================================================
-- SparrowDB 数据库导出文件
-- 导出时间: 2025-09-15 11:22:22
-- 生成工具: SparrowDB SQLFileManager
-- ================================================


-- 表: simple_table
-- 导出时间: 2025-09-15 11:22:22

-- 删除表（如果存在）
DROP TABLE IF EXISTS simple_table;

-- 创建表 simple_table
CREATE TABLE simple_table (
    id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    value INT NOT NULL
);

-- simple_table 数据
INSERT INTO simple_table (id, name, value) VALUES (1, 'Item1', 100);
INSERT INTO simple_table (id, name, value) VALUES (2, 'Item2', 200);
INSERT INTO simple_table (id, name, value) VALUES (3, 'Item3', 50);


-- 表: test_categories
-- 导出时间: 2025-09-15 11:22:22

-- 删除表（如果存在）
DROP TABLE IF EXISTS test_categories;

-- 创建表 test_categories
CREATE TABLE test_categories (
    category_id INT PRIMARY KEY,
    category_name VARCHAR(30) NOT NULL,
    description TEXT
);

-- test_categories 数据
INSERT INTO test_categories (category_id, category_name, description) VALUES (1, '电子产品', '各种电子设备和配件');
INSERT INTO test_categories (category_id, category_name, description) VALUES (2, '服装', '男女服装和配饰');
INSERT INTO test_categories (category_id, category_name, description) VALUES (3, '图书', '各类图书和教材');


-- 表: __system_tables__
-- 导出时间: 2025-09-15 11:22:22

-- 删除表（如果存在）
DROP TABLE IF EXISTS __system_tables__;

-- 创建表 __system_tables__
CREATE TABLE __system_tables__ (
    table_name VARCHAR(255) PRIMARY KEY,
    create_time BIGINT NOT NULL,
    column_count INT NOT NULL,
    constraint_count INT NOT NULL
);


-- 表: __system_constraints__
-- 导出时间: 2025-09-15 11:22:22

-- 删除表（如果存在）
DROP TABLE IF EXISTS __system_constraints__;

-- 创建表 __system_constraints__
CREATE TABLE __system_constraints__ (
    table_name VARCHAR(255),
    constraint_name VARCHAR(255),
    constraint_type VARCHAR(50) NOT NULL,
    columns VARCHAR(1000) NOT NULL,
    referenced_table VARCHAR(255) NOT NULL,
    referenced_columns VARCHAR(1000) NOT NULL,
    default_value VARCHAR(255) NOT NULL
);


-- 表: __system_columns__
-- 导出时间: 2025-09-15 11:22:22

-- 删除表（如果存在）
DROP TABLE IF EXISTS __system_columns__;

-- 创建表 __system_columns__
CREATE TABLE __system_columns__ (
    table_name VARCHAR(255),
    column_name VARCHAR(255),
    data_type VARCHAR(50) NOT NULL,
    length INT NOT NULL,
    not_null BOOLEAN NOT NULL DEFAULT false,
    primary_key BOOLEAN NOT NULL DEFAULT false,
    unique BOOLEAN NOT NULL DEFAULT false,
    default_value VARCHAR(255) NOT NULL,
    auto_increment BOOLEAN NOT NULL DEFAULT false
);


-- 表: test_products
-- 导出时间: 2025-09-15 11:22:22

-- 删除表（如果存在）
DROP TABLE IF EXISTS test_products;

-- 创建表 test_products
CREATE TABLE test_products (
    id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    price DECIMAL NOT NULL,
    category VARCHAR(30) NOT NULL
);

-- test_products 数据
INSERT INTO test_products (id, name, price, category) VALUES (1, 'Laptop', 1299.99, 'Electronics');
INSERT INTO test_products (id, name, price, category) VALUES (2, 'Mouse', 25.50, 'Electronics');
INSERT INTO test_products (id, name, price, category) VALUES (3, 'Book', 15.99, 'Education');
INSERT INTO test_products (id, name, price, category) VALUES (4, 'Chair', 89.99, 'Furniture');
INSERT INTO test_products (id, name, price, category) VALUES (5, 'Monitor', 299.99, 'Electronics');


-- 表: test_sql_import
-- 导出时间: 2025-09-15 11:22:22

-- 删除表（如果存在）
DROP TABLE IF EXISTS test_sql_import;

-- 创建表 test_sql_import
CREATE TABLE test_sql_import (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    age INT,
    email VARCHAR(100)
);

-- test_sql_import 数据
INSERT INTO test_sql_import (id, name, age, email) VALUES (1, '张三', 25, 'zhangsan@example.com');
INSERT INTO test_sql_import (id, name, age, email) VALUES (2, '李四', 30, 'lisi@example.com');
INSERT INTO test_sql_import (id, name, age, email) VALUES (3, '王五', 28, 'wangwu@example.com');


-- 表: __system_functions__
-- 导出时间: 2025-09-15 11:22:22

-- 删除表（如果存在）
DROP TABLE IF EXISTS __system_functions__;

-- 创建表 __system_functions__
CREATE TABLE __system_functions__ (
    function_name VARCHAR(255),
    signature VARCHAR(500) NOT NULL,
    return_type VARCHAR(50) NOT NULL,
    body TEXT(10000) NOT NULL,
    is_permanent BOOLEAN NOT NULL DEFAULT false,
    create_time BIGINT NOT NULL
);


-- 表: employees
-- 导出时间: 2025-09-15 11:22:22

-- 删除表（如果存在）
DROP TABLE IF EXISTS employees;

-- 创建表 employees
CREATE TABLE employees (
    id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    department VARCHAR(50) NOT NULL,
    salary INT NOT NULL
);

-- employees 数据
INSERT INTO employees (id, name, department, salary) VALUES (1, 'Alice', 'Engineering', 75000);
INSERT INTO employees (id, name, department, salary) VALUES (2, 'Bob', 'Sales', 65000);
INSERT INTO employees (id, name, department, salary) VALUES (3, 'Charlie', 'Engineering', 80000);
INSERT INTO employees (id, name, department, salary) VALUES (1, 'Alice', 'Engineering', 75000);
INSERT INTO employees (id, name, department, salary) VALUES (2, 'Bob', 'Sales', 65000);
INSERT INTO employees (id, name, department, salary) VALUES (3, 'Charlie', 'Engineering', 80000);
INSERT INTO employees (id, name, department, salary) VALUES (1, 'Alice', 'Engineering', 75000);
INSERT INTO employees (id, name, department, salary) VALUES (2, 'Bob', 'Sales', 65000);
INSERT INTO employees (id, name, department, salary) VALUES (3, 'Charlie', 'Engineering', 80000);


-- 表: users
-- 导出时间: 2025-09-15 11:22:22

-- 删除表（如果存在）
DROP TABLE IF EXISTS users;

-- 创建表 users
CREATE TABLE users (
    id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    age INT NOT NULL
);

-- users 数据
INSERT INTO users (id, name, email, age) VALUES (1, 'Alice Johnson', 'alice@example.com', 28);
INSERT INTO users (id, name, email, age) VALUES (2, 'Bob Smith', 'bob@example.com', 32);
INSERT INTO users (id, name, email, age) VALUES (3, 'Charlie Brown', 'charlie@example.com', 25);
INSERT INTO users (id, name, email, age) VALUES (4, 'Diana Wilson', 'diana@example.com', 29);
INSERT INTO users (id, name, email, age) VALUES (5, 'Eve Davis', 'eve@example.com', 35);
INSERT INTO users (id, name, email, age) VALUES (6, 'Frank Miller', 'frank@example.com', 40);


-- 表: products
-- 导出时间: 2025-09-15 11:22:22

-- 删除表（如果存在）
DROP TABLE IF EXISTS products;

-- 创建表 products
CREATE TABLE products (
    id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    price DECIMAL NOT NULL,
    category VARCHAR(30) NOT NULL
);

-- products 数据
INSERT INTO products (id, name, price, category) VALUES (1, 'Laptop', 999.99, 'Electronics');
INSERT INTO products (id, name, price, category) VALUES (2, 'Mouse', 29.99, 'Electronics');
INSERT INTO products (id, name, price, category) VALUES (3, 'Book', 15.50, 'Education');


-- 表: test_users
-- 导出时间: 2025-09-15 11:22:22

-- 删除表（如果存在）
DROP TABLE IF EXISTS test_users;

-- 创建表 test_users
CREATE TABLE test_users (
    id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    age INT NOT NULL,
    department VARCHAR(30) NOT NULL
);

-- test_users 数据
INSERT INTO test_users (id, name, age, department) VALUES (1, 'Alice Johnson', 28, 'Engineering');
INSERT INTO test_users (id, name, age, department) VALUES (2, 'Bob Smith', 35, 'Marketing');
INSERT INTO test_users (id, name, age, department) VALUES (3, 'Charlie Brown', 22, 'Engineering');
INSERT INTO test_users (id, name, age, department) VALUES (4, 'Diana Prince', 30, 'HR');
INSERT INTO test_users (id, name, age, department) VALUES (5, 'Eve Wilson', 26, 'Engineering');

