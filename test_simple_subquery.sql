-- 简单子查询测试
-- 创建测试表
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    dept_id INT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(50) NOT NULL,
    location VARCHAR(50)
);

CREATE TABLE employees (
    emp_id INT PRIMARY KEY AUTO_INCREMENT,
    emp_name VARCHAR(50) NOT NULL,
    dept_id INT,
    salary DECIMAL(10,2)
);

-- 插入测试数据
INSERT INTO departments (dept_name, location) VALUES 
('IT', 'New York'),
('HR', 'London'),
('Finance', 'Paris');

INSERT INTO employees (emp_name, dept_id, salary) VALUES 
('Alice', 1, 75000.00),
('Bob', 1, 80000.00),
('Charlie', 2, 65000.00),
('David', 2, 70000.00),
('Eve', 3, 85000.00);

-- 测试IN子查询 - 应该被检测到并优化
SELECT emp_name, salary 
FROM employees 
WHERE dept_id IN (
    SELECT dept_id 
    FROM departments 
    WHERE location = 'New York'
);

-- 测试EXISTS子查询 - 应该被检测到并优化
SELECT emp_name, salary 
FROM employees e 
WHERE EXISTS (
    SELECT 1 
    FROM departments d 
    WHERE d.dept_id = e.dept_id 
    AND d.location = 'London'
);

-- 测试标量子查询 - 应该被检测到并优化
SELECT emp_name, salary,
       (SELECT dept_name FROM departments WHERE dept_id = employees.dept_id) as dept_name
FROM employees 
WHERE salary > 70000;
