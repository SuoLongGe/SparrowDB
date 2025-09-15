-- ================================
-- SparrowDB 导入功能测试SQL文件
-- 创建时间: 2025-09-15
-- 用途: 测试GUI界面的SQL文件导入功能
-- ================================

-- 创建学生信息表
CREATE TABLE student (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    age INT,
    grade VARCHAR(20),
    email VARCHAR(100)
);

-- 创建课程表
CREATE TABLE course (
    course_id INT PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    credits INT,
    instructor VARCHAR(50)
);

-- 创建选课表
CREATE TABLE enrollments (
    student_id INT,
    course_id INT,
    enrollment_date DATE,
    score DECIMAL(5,2),
    PRIMARY KEY(student_id, course_id)
);

-- 插入学生数据
INSERT INTO student (id, name, age, grade, email) VALUES 
(1, '张三', 20, '大三', 'zhangsan@example.com'),
(2, '李四', 19, '大二', 'lisi@example.com'),
(3, '王五', 21, '大四', 'wangwu@example.com'),
(4, '赵六', 18, '大一', 'zhaoliu@example.com'),
(5, '钱七', 22, '研一', 'qianqi@example.com');

-- 插入课程数据
INSERT INTO course (course_id, course_name, credits, instructor) VALUES 
(101, '数据库系统原理', 3, '陈教授'),
(102, '算法与数据结构', 4, '李教授'),
(103, '计算机网络', 3, '王教授'),
(104, '操作系统', 3, '刘教授'),
(105, '软件工程', 2, '赵教授');

-- 插入选课数据
INSERT INTO enrollments (student_id, course_id, enrollment_date, score) VALUES 
(1, 101, '2024-09-01', 88.5),
(1, 102, '2024-09-01', 92.0),
(2, 101, '2024-09-01', 85.0),
(2, 103, '2024-09-01', 78.5),
(3, 102, '2024-09-01', 95.5),
(3, 104, '2024-09-01', 87.0),
(4, 103, '2024-09-01', 82.5),
(4, 105, '2024-09-01', 90.0),
(5, 104, '2024-09-01', 93.0),
(5, 105, '2024-09-01', 89.5);

-- 创建部门表（用于测试更复杂的数据结构）
CREATE TABLE departments (
    dept_id INT PRIMARY KEY,
    dept_name VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    budget DECIMAL(12,2)
);

-- 插入部门数据
INSERT INTO departments (dept_id, dept_name, location, budget) VALUES 
(1, '计算机科学与技术学院', '主楼A区', 5000000.00),
(2, '软件学院', '主楼B区', 3000000.00),
(3, '信息安全学院', '主楼C区', 2500000.00);

-- 创建教师表
CREATE TABLE teachers (
    teacher_id INT PRIMARY KEY,
    teacher_name VARCHAR(50) NOT NULL,
    dept_id INT,
    title VARCHAR(20),
    salary DECIMAL(10,2)
);

-- 插入教师数据
INSERT INTO teachers (teacher_id, teacher_name, dept_id, title, salary) VALUES 
(1001, '陈教授', 1, '教授', 15000.00),
(1002, '李教授', 1, '副教授', 12000.00),
(1003, '王教授', 2, '讲师', 8000.00),
(1004, '刘教授', 1, '教授', 16000.00),
(1005, '赵教授', 3, '副教授', 11000.00);

-- 更新一些数据（测试UPDATE语句）
UPDATE student SET grade = '大四' WHERE id = 2;
UPDATE course SET credits = 4 WHERE course_id = 103;

-- 测试一些查询语句（这些会在导入时执行，但不会影响数据）
SELECT COUNT(*) AS total_students FROM student;
SELECT course_name, credits FROM course WHERE credits >= 3;

-- 创建一个视图（如果支持）
CREATE VIEW student_course_view AS
SELECT s.name AS student_name, c.course_name, e.score
FROM student s
JOIN enrollments e ON s.id = e.student_id
JOIN course c ON e.course_id = c.course_id;

-- 测试删除操作
DELETE FROM enrollments WHERE score < 80;

-- 再插入一些测试数据
INSERT INTO student (id, name, age, grade, email) VALUES 
(6, '孙八', 20, '大三', 'sunba@example.com'),
(7, '周九', 19, '大二', 'zhoujiu@example.com');

-- 创建索引（如果支持）
-- CREATE INDEX idx_student_name ON student(name);
-- CREATE INDEX idx_course_name ON course(course_name);

-- 测试事务相关操作
-- BEGIN TRANSACTION;
INSERT INTO student (id, name, age, grade, email) VALUES (8, '吴十', 21, '大三', 'wushi@example.com');
INSERT INTO course (course_id, course_name, credits, instructor) VALUES (106, 'Web开发技术', 3, '新教授');
-- COMMIT;

-- 最后的数据验证查询
SELECT 'Students Count' AS table_name, COUNT(*) AS record_count FROM student
UNION ALL
SELECT 'Courses Count', COUNT(*) FROM course
UNION ALL
SELECT 'Enrollments Count', COUNT(*) FROM enrollments
UNION ALL
SELECT 'Departments Count', COUNT(*) FROM departments
UNION ALL
SELECT 'Teachers Count', COUNT(*) FROM teachers;

-- ================================
-- 测试完成提示
-- ================================
-- 此SQL文件包含了多种类型的SQL语句：
-- 1. CREATE TABLE - 创建表结构
-- 2. INSERT - 插入数据
-- 3. UPDATE - 更新数据  
-- 4. DELETE - 删除数据
-- 5. SELECT - 查询数据
-- 6. CREATE VIEW - 创建视图
-- 7. 复杂查询和统计
-- 
-- 导入成功后，数据库中应该包含：
-- - 5个表：student, course, enrollments, departments, teachers
-- - 1个视图：student_course_view
-- - 约30+条记录分布在各个表中
-- ================================