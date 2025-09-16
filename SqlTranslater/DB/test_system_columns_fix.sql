-- 测试系统列表自动创建和更新功能
-- 文件：test_system_columns_fix.sql

-- 1. 切换到column_expe数据库（测试数据库初始化时系统表创建）
USE column_expe;

-- 2. 查看是否已自动创建系统列表
SELECT '检查系统列表是否存在' AS test_step;
SELECT * FROM __system_columns__;

-- 3. 创建新表测试系统列表自动更新
SELECT '创建新表测试系统列表更新' AS test_step;

CREATE TABLE test_new_table (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE,
    age INT,
    created_at DATE DEFAULT '2025-01-01'
);

-- 4. 验证系统列表是否包含新表的列信息
SELECT '验证新表列信息是否已添加到系统列表' AS test_step;
SELECT * FROM __system_columns__ WHERE table_name = 'test_new_table';

-- 5. 再创建一个表进一步测试
CREATE TABLE test_second_table (
    product_id INT PRIMARY KEY,
    product_name VARCHAR(200) NOT NULL,
    price DECIMAL(10,2),
    category VARCHAR(50)
);

-- 6. 查看所有系统列表内容
SELECT '查看完整系统列表' AS test_step;
SELECT table_name, column_name, data_type, primary_key, not_null, unique_key 
FROM __system_columns__ 
ORDER BY table_name, column_name;

-- 7. 统计各表的列数
SELECT '统计各表列数' AS test_step;
SELECT table_name, COUNT(*) as column_count 
FROM __system_columns__ 
GROUP BY table_name 
ORDER BY table_name;

-- 8. 验证主键约束信息
SELECT '验证主键约束信息' AS test_step;
SELECT table_name, column_name 
FROM __system_columns__ 
WHERE primary_key = 'true' 
ORDER BY table_name;

-- 9. 验证非空约束信息
SELECT '验证非空约束信息' AS test_step;
SELECT table_name, column_name 
FROM __system_columns__ 
WHERE not_null = 'true' 
ORDER BY table_name, column_name;

-- 10. 验证唯一约束信息
SELECT '验证唯一约束信息' AS test_step;
SELECT table_name, column_name 
FROM __system_columns__ 
WHERE unique_key = 'true' 
ORDER BY table_name, column_name;




