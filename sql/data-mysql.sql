-- =====================================================
-- Open Report 系统初始化数据脚本 (MySQL 8.0)
-- 编码: UTF-8
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 系统用户数据
-- 密码: 123456 (BCrypt 加密)
-- ----------------------------
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `avatar`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `deleted`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 'admin@example.com', '13800138000', NULL, 1, NOW(), NOW(), NULL, NULL, 0);

-- ----------------------------
-- 系统角色数据
-- ----------------------------
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`, `create_time`, `update_time`, `create_by`, `update_by`, `deleted`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '拥有系统所有权限', NOW(), NOW(), 1, 1, 0),
(2, 'REPORT_DESIGNER', '报表设计者', '可以设计和管理报表', NOW(), NOW(), 1, 1, 0),
(3, 'NORMAL_USER', '普通用户', '只能查看已发布的报表', NOW(), NOW(), 1, 1, 0);

-- ----------------------------
-- 用户角色关联
-- ----------------------------
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`, `create_time`) VALUES
(1, 1, 1, NOW());

-- ----------------------------
-- 系统菜单数据
-- ----------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `create_time`, `update_time`, `deleted`) VALUES
-- 一级目录
(1, 0, '系统管理', 'M', '/system', NULL, NULL, 'setting', 1, 1, NOW(), NOW(), 0),
(2, 0, '报表中心', 'M', '/report', NULL, NULL, 'chart', 2, 1, NOW(), NOW(), 0),
(3, 0, '数据管理', 'M', '/data', NULL, NULL, 'database', 3, 1, NOW(), NOW(), 0),

-- 系统管理子菜单
(100, 1, '用户管理', 'C', '/system/user', 'system/user/index', 'system:user:list', 'user', 1, 1, NOW(), NOW(), 0),
(101, 1, '角色管理', 'C', '/system/role', 'system/role/index', 'system:role:list', 'peoples', 2, 1, NOW(), NOW(), 0),
(102, 1, '菜单管理', 'C', '/system/menu', 'system/menu/index', 'system:menu:list', 'menu', 3, 1, NOW(), NOW(), 0),

-- 用户管理按钮
(1001, 100, '用户查询', 'F', NULL, NULL, 'system:user:query', NULL, 1, 1, NOW(), NOW(), 0),
(1002, 100, '用户新增', 'F', NULL, NULL, 'system:user:add', NULL, 2, 1, NOW(), NOW(), 0),
(1003, 100, '用户修改', 'F', NULL, NULL, 'system:user:edit', NULL, 3, 1, NOW(), NOW(), 0),
(1004, 100, '用户删除', 'F', NULL, NULL, 'system:user:remove', NULL, 4, 1, NOW(), NOW(), 0),
(1005, 100, '用户导出', 'F', NULL, NULL, 'system:user:export', NULL, 5, 1, NOW(), NOW(), 0),

-- 角色管理按钮
(1011, 101, '角色查询', 'F', NULL, NULL, 'system:role:query', NULL, 1, 1, NOW(), NOW(), 0),
(1012, 101, '角色新增', 'F', NULL, NULL, 'system:role:add', NULL, 2, 1, NOW(), NOW(), 0),
(1013, 101, '角色修改', 'F', NULL, NULL, 'system:role:edit', NULL, 3, 1, NOW(), NOW(), 0),
(1014, 101, '角色删除', 'F', NULL, NULL, 'system:role:remove', NULL, 4, 1, NOW(), NOW(), 0),

-- 菜单管理按钮
(1021, 102, '菜单查询', 'F', NULL, NULL, 'system:menu:query', NULL, 1, 1, NOW(), NOW(), 0),
(1022, 102, '菜单新增', 'F', NULL, NULL, 'system:menu:add', NULL, 2, 1, NOW(), NOW(), 0),
(1023, 102, '菜单修改', 'F', NULL, NULL, 'system:menu:edit', NULL, 3, 1, NOW(), NOW(), 0),
(1024, 102, '菜单删除', 'F', NULL, NULL, 'system:menu:remove', NULL, 4, 1, NOW(), NOW(), 0),

-- 报表中心子菜单
(200, 2, '报表设计', 'C', '/report/designer', 'report/designer/index', 'report:designer:list', 'edit', 1, 1, NOW(), NOW(), 0),
(201, 2, '报表管理', 'C', '/report/manage', 'report/manage/index', 'report:manage:list', 'list', 2, 1, NOW(), NOW(), 0),
(202, 2, '报表预览', 'C', '/report/preview', 'report/preview/index', 'report:preview:list', 'view', 3, 1, NOW(), NOW(), 0),
(203, 2, '调度任务', 'C', '/report/schedule', 'report/schedule/index', 'report:schedule:list', 'time', 4, 1, NOW(), NOW(), 0),
(204, 2, '执行日志', 'C', '/report/log', 'report/log/index', 'report:log:list', 'log', 5, 1, NOW(), NOW(), 0),

-- 报表设计按钮
(2001, 200, '报表创建', 'F', NULL, NULL, 'report:designer:add', NULL, 1, 1, NOW(), NOW(), 0),
(2002, 200, '报表编辑', 'F', NULL, NULL, 'report:designer:edit', NULL, 2, 1, NOW(), NOW(), 0),
(2003, 200, '报表删除', 'F', NULL, NULL, 'report:designer:remove', NULL, 3, 1, NOW(), NOW(), 0),
(2004, 200, '报表发布', 'F', NULL, NULL, 'report:designer:publish', NULL, 4, 1, NOW(), NOW(), 0),

-- 报表管理按钮
(2011, 201, '报表查询', 'F', NULL, NULL, 'report:manage:query', NULL, 1, 1, NOW(), NOW(), 0),
(2012, 201, '报表导出', 'F', NULL, NULL, 'report:manage:export', NULL, 2, 1, NOW(), NOW(), 0),

-- 调度任务按钮
(2031, 203, '调度查询', 'F', NULL, NULL, 'report:schedule:query', NULL, 1, 1, NOW(), NOW(), 0),
(2032, 203, '调度新增', 'F', NULL, NULL, 'report:schedule:add', NULL, 2, 1, NOW(), NOW(), 0),
(2033, 203, '调度修改', 'F', NULL, NULL, 'report:schedule:edit', NULL, 3, 1, NOW(), NOW(), 0),
(2034, 203, '调度删除', 'F', NULL, NULL, 'report:schedule:remove', NULL, 4, 1, NOW(), NOW(), 0),
(2035, 203, '调度启停', 'F', NULL, NULL, 'report:schedule:change', NULL, 5, 1, NOW(), NOW(), 0),

-- 执行日志按钮
(2041, 204, '日志查询', 'F', NULL, NULL, 'report:log:query', NULL, 1, 1, NOW(), NOW(), 0),
(2042, 204, '日志删除', 'F', NULL, NULL, 'report:log:remove', NULL, 2, 1, NOW(), NOW(), 0),

-- 数据管理子菜单
(300, 3, '数据源管理', 'C', '/data/source', 'data/source/index', 'data:source:list', 'data-source', 1, 1, NOW(), NOW(), 0),
(301, 3, '数据集管理', 'C', '/data/set', 'data/set/index', 'data:set:list', 'data-set', 2, 1, NOW(), NOW(), 0),

-- 数据源管理按钮
(3001, 300, '数据源查询', 'F', NULL, NULL, 'data:source:query', NULL, 1, 1, NOW(), NOW(), 0),
(3002, 300, '数据源新增', 'F', NULL, NULL, 'data:source:add', NULL, 2, 1, NOW(), NOW(), 0),
(3003, 300, '数据源修改', 'F', NULL, NULL, 'data:source:edit', NULL, 3, 1, NOW(), NOW(), 0),
(3004, 300, '数据源删除', 'F', NULL, NULL, 'data:source:remove', NULL, 4, 1, NOW(), NOW(), 0),
(3005, 300, '数据源测试', 'F', NULL, NULL, 'data:source:test', NULL, 5, 1, NOW(), NOW(), 0),

-- 数据集管理按钮
(3011, 301, '数据集查询', 'F', NULL, NULL, 'data:set:query', NULL, 1, 1, NOW(), NOW(), 0),
(3012, 301, '数据集新增', 'F', NULL, NULL, 'data:set:add', NULL, 2, 1, NOW(), NOW(), 0),
(3013, 301, '数据集修改', 'F', NULL, NULL, 'data:set:edit', NULL, 3, 1, NOW(), NOW(), 0),
(3014, 301, '数据集删除', 'F', NULL, NULL, 'data:set:remove', NULL, 4, 1, NOW(), NOW(), 0);

-- ----------------------------
-- 角色菜单关联 (超级管理员拥有所有菜单权限)
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`)
SELECT 1, id, NOW() FROM `sys_menu`;

-- ----------------------------
-- 报表设计者角色菜单权限
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`) VALUES
(2, 2, NOW()),
(2, 200, NOW()),
(2, 2001, NOW()),
(2, 2002, NOW()),
(2, 2003, NOW()),
(2, 2004, NOW()),
(2, 201, NOW()),
(2, 2011, NOW()),
(2, 2012, NOW()),
(2, 202, NOW()),
(2, 3, NOW()),
(2, 300, NOW()),
(2, 3001, NOW()),
(2, 3002, NOW()),
(2, 3003, NOW()),
(2, 3004, NOW()),
(2, 3005, NOW()),
(2, 301, NOW()),
(2, 3011, NOW()),
(2, 3012, NOW()),
(2, 3013, NOW()),
(2, 3014, NOW());

-- ----------------------------
-- 普通用户角色菜单权限
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`) VALUES
(3, 2, NOW()),
(3, 202, NOW()),
(3, 2011, NOW());

SET FOREIGN_KEY_CHECKS = 1;
