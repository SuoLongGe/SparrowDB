-- 数据库切换和CHECK约束测试
-- 测试时间: 2025-09-15

-- 1. 显示所有数据库
SHOW DATABASES;

-- 2. 切换到smxx数据库（如果不存在会自动创建）
USE smxx;

-- 3. 创建带有CHECK约束的Students表（修复版本）
CREATE TABLE Students (
    student_id INT PRIMARY KEY,       
    name VARCHAR(100) NOT NULL,        
    age INT CHECK (age >= 0 AND age <= 150),
    grade VARCHAR(10) CHECK (grade IN ('A', 'B', 'C', 'D', 'F')),
    email VARCHAR(100)
);

-- 4. 验证表创建成功
SHOW TABLES;

-- 5. 插入测试数据（正常数据）
INSERT INTO Students (student_id, name, age, grade, email) 
VALUES (1, '张三', 20, 'A', 'zhangsan@example.com');

INSERT INTO Students (student_id, name, age, grade, email) 
VALUES (2, '李四', 22, 'B', 'lisi@example.com');

-- 6. 查询数据验证
SELECT * FROM Students;

-- 7. 切换回main数据库
USE main;

-- 8. 验证main数据库的表（应该看到原有数据）
SHOW TABLES;
SELECT COUNT(*) as table_count FROM customers;

-- 9. 再次切换到smxx数据库
USE smxx;

-- 10. 验证smxx数据库的数据依然存在
SELECT * FROM Students;

-- 11. 创建另一个表测试数据库隔离
CREATE TABLE Courses (
    course_id INT PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    credits INT CHECK (credits > 0 AND credits <= 10)
);

-- 12. 插入课程数据
INSERT INTO Courses VALUES (1, '数据库原理', 4);
INSERT INTO Courses VALUES (2, '算法设计', 3);

-- 13. 最终验证
SELECT 'smxx数据库表数量:' as info, COUNT(*) as count FROM information_schema.tables WHERE table_schema = 'smxx';
SELECT * FROM Students;
SELECT * FROM Courses;

-- 测试说明：
-- 1. 窗口标题应显示: SparrowDB - 当前数据库: smxx
-- 2. 状态栏应显示: 已切换到数据库: smxx  
-- 3. CHECK约束应该正常解析，不再报"未知的约束类型"错误
-- 4. 数据库切换后，操作只影响当前数据库
-- 5. 左侧数据库树应显示对应数据库的表结构
