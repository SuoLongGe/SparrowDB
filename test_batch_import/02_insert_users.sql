-- 批量导入测试 - 第二个文件：插入用户数据
-- 文件名：02_insert_users.sql

-- 插入用户数据
INSERT INTO users (user_id, username, email, created_date) VALUES 
(1, 'admin', 'admin@test.com', '2024-01-01'),
(2, 'user1', 'user1@test.com', '2024-01-15'),
(3, 'user2', 'user2@test.com', '2024-02-01'),
(4, 'user3', 'user3@test.com', '2024-02-15'),
(5, 'guest', 'guest@test.com', '2024-03-01');

-- 验证插入结果
SELECT COUNT(*) AS users_count FROM users;
