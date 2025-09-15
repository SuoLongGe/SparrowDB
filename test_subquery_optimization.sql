-- test_subquery_optimization.sql
-- 子查询优化测试脚本

-- 1. 创建测试表
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
    salary DECIMAL(10,2),
    hire_date DATE,
    FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
);

-- 2. 插入测试数据
INSERT INTO departments (dept_name, location) VALUES 
('IT', 'New York'),
('HR', 'London'),
('Finance', 'Paris'),
('Marketing', 'Tokyo'),
('Sales', 'Berlin');

INSERT INTO employees (emp_name, dept_id, salary, hire_date) VALUES 
('Alice', 1, 75000.00, '2020-01-15'),
('Bob', 1, 80000.00, '2019-03-20'),
('Charlie', 2, 65000.00, '2021-06-10'),
('David', 2, 70000.00, '2020-09-05'),
('Eve', 3, 85000.00, '2018-12-01'),
('Frank', 3, 90000.00, '2017-08-15'),
('Grace', 4, 60000.00, '2022-02-28'),
('Henry', 4, 55000.00, '2021-11-12'),
('Ivy', 5, 70000.00, '2020-04-18'),
('Jack', 5, 75000.00, '2019-07-25');

-- 3. 测试各种子查询优化场景

-- 3.1 IN子查询 - 应该可以改写为EXISTS或JOIN
SELECT emp_name, salary 
FROM employees 
WHERE dept_id IN (
    SELECT dept_id 
    FROM departments 
    WHERE location = 'New York'
);

-- 3.2 EXISTS子查询 - 应该可以改写为JOIN
SELECT emp_name, salary 
FROM employees e 
WHERE EXISTS (
    SELECT 1 
    FROM departments d 
    WHERE d.dept_id = e.dept_id 
    AND d.location = 'London'
);

-- 3.3 标量子查询 - 应该可以优化
SELECT emp_name, salary,
       (SELECT dept_name FROM departments WHERE dept_id = employees.dept_id) as dept_name
FROM employees 
WHERE salary > 70000;

-- 3.4 相关子查询 - 应该可以改写为JOIN
SELECT emp_name, salary 
FROM employees e1 
WHERE salary > (
    SELECT AVG(salary) 
    FROM employees e2 
    WHERE e2.dept_id = e1.dept_id
);

-- 3.5 复杂子查询 - 测试优化器的处理能力
SELECT emp_name, salary, dept_name
FROM employees e
WHERE dept_id IN (
    SELECT dept_id 
    FROM departments 
    WHERE location IN ('New York', 'London')
)
AND salary > (
    SELECT AVG(salary) 
    FROM employees 
    WHERE dept_id = e.dept_id
);

-- 3.6 嵌套子查询
SELECT emp_name 
FROM employees 
WHERE dept_id IN (
    SELECT dept_id 
    FROM departments 
    WHERE dept_name IN (
        SELECT dept_name 
        FROM departments 
        WHERE location = 'Paris'
    )
);

-- 3.7 子查询在HAVING子句中
SELECT dept_id, COUNT(*) as emp_count, AVG(salary) as avg_salary
FROM employees 
GROUP BY dept_id
HAVING AVG(salary) > (
    SELECT AVG(salary) 
    FROM employees
);

-- 3.8 子查询在SELECT列表中
SELECT emp_name, salary,
       (SELECT COUNT(*) FROM employees e2 WHERE e2.dept_id = employees.dept_id) as dept_emp_count,
       (SELECT dept_name FROM departments WHERE dept_id = employees.dept_id) as dept_name
FROM employees 
WHERE salary > 65000;

-- 4. 性能对比测试
-- 执行相同的查询，比较启用和禁用子查询优化时的性能差异

-- 5. 清理测试数据（可选）
-- DROP TABLE employees;
-- DROP TABLE departments;
