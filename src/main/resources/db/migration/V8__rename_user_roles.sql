-- 将全局角色 TEACHER 和 STUDENT 转换为 USER，ADMIN 保持不变
-- 迁移后，全局角色只有 USER 和 ADMIN
UPDATE app_users SET role = 'USER' WHERE role IN ('TEACHER', 'STUDENT');

-- 注释：role 列是 varchar(20)，无需修改约束
-- 前端和后端代码将更新为处理新的角色枚举值