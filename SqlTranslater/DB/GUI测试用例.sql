-- SparrowDB GUI界面测试用例
-- 在GUI界面中依次复制粘贴以下SQL语句进行测试

-- ===== 基础表创建和数据插入 =====

-- 1. 创建测试表
CREATE TABLE test_employees (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    department VARCHAR(50), 
    salary INT,
    age INT
);

-- 2. 插入测试数据
INSERT INTO test_employees VALUES (1, 'Alice', 'Engineering', 75000, 28);
INSERT INTO test_employees VALUES (2, 'Bob', 'Sales', 65000, 32);
INSERT INTO test_employees VALUES (3, 'Charlie', 'Engineering', 80000, 25);
INSERT INTO test_employees VALUES (4, 'Diana', 'HR', 70000, 29);

-- ===== 数学函数测试 =====

-- 3. ABS绝对值函数
SELECT name, salary, ABS(salary - 70000) AS salary_diff 
FROM test_employees;

-- 4. ROUND四舍五入函数
SELECT name, ROUND(salary / 12, 2) AS monthly_salary 
FROM test_employees;

-- 5. SQRT平方根函数
SELECT name, SQRT(age) AS age_sqrt 
FROM test_employees;

-- 6. POWER幂函数
SELECT name, POWER(age, 2) AS age_squared 
FROM test_employees;

-- 7. MOD取模函数
SELECT name, MOD(salary, 1000) AS salary_mod 
FROM test_employees;

-- ===== 字符串函数测试 =====

-- 8. UPPER转大写函数
SELECT UPPER(name) AS upper_name, department 
FROM test_employees;

-- 9. LOWER转小写函数
SELECT name, LOWER(department) AS lower_dept 
FROM test_employees;

-- 10. LENGTH字符串长度函数
SELECT name, LENGTH(name) AS name_length 
FROM test_employees;

-- 11. CONCAT字符串连接函数
SELECT CONCAT(name, ' - ', department) AS employee_info 
FROM test_employees;

-- 12. TRIM去空格函数（常量测试）
SELECT TRIM('  Hello World  ') AS trimmed_text 
FROM test_employees LIMIT 1;

-- ===== 视图功能测试 =====

-- 13. 创建简单视图
CREATE VIEW engineering_staff AS 
SELECT name, salary FROM test_employees 
WHERE department = 'Engineering';

-- 14. 创建复杂视图
CREATE VIEW high_earners AS 
SELECT name, department, salary 
FROM test_employees 
WHERE salary > 70000;

-- 15. 创建带函数的视图
CREATE VIEW employee_summary AS 
SELECT 
    UPPER(name) AS employee_name,
    department,
    ROUND(salary / 12, 2) AS monthly_salary,
    ABS(age - 30) AS age_diff
FROM test_employees;

-- 16. 创建复合函数视图
CREATE VIEW formatted_employees AS 
SELECT 
    CONCAT(UPPER(name), ' (', department, ')') AS full_info,
    ROUND(salary / 1000, 1) AS salary_k,
    LENGTH(name) AS name_len
FROM test_employees;

-- ===== 视图删除测试 =====

-- 17. 删除存在的视图
DROP VIEW engineering_staff;

-- 18. 条件删除视图（存在则删除）
DROP VIEW IF EXISTS non_existent_view;

-- 19. 删除其他测试视图
DROP VIEW IF EXISTS high_earners;
DROP VIEW IF EXISTS employee_summary;
DROP VIEW IF EXISTS formatted_employees;

-- ===== 复合查询测试 =====

-- 20. 复杂的函数组合查询
SELECT 
    CONCAT(UPPER(name), ' - Age:', age) AS info,
    ROUND(ABS(salary - 70000) / 1000, 2) AS salary_diff_k,
    MOD(LENGTH(name), 3) AS name_mod
FROM test_employees 
WHERE LENGTH(name) > 3;

-- ===== 清理测试数据 =====

-- 21. 清理测试表（可选）
-- DROP TABLE IF EXISTS test_employees;

-- ===== GUI界面验证要点 =====
-- 
-- 在执行以上SQL时，请注意观察：
-- 
-- 1. 语法高亮：关键字、字符串、数字应有不同颜色
-- 2. Token列表：显示词法分析的结果
-- 3. AST可视化：显示语法树的图形结构  
-- 4. 执行结果：显示SQL执行的结果和错误信息
-- 5. 执行时间：显示SQL执行耗时
-- 6. 状态栏：显示当前操作状态
-- 
-- 测试技巧：
-- - 使用F5执行选中的SQL语句
-- - 使用F9或Ctrl+Enter执行全部SQL
-- - 使用放大/缩小按钮调整AST可视化
-- - 观察不同索引方式的性能差异
