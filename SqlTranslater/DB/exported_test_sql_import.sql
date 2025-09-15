-- ================================================
-- SparrowDB 数据库导出文件
-- 导出时间: 2025-09-15 11:22:22
-- 生成工具: SparrowDB SQLFileManager
-- ================================================


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

