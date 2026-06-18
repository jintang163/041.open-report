-- =====================================================
-- Open Report 权限安全模块建表脚本 (MySQL 8.0)
-- 包含：部门表、行级安全规则表、字段级权限表
-- 编码: UTF-8
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 部门表
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父部门ID',
  `dept_name` varchar(64) NOT NULL COMMENT '部门名称',
  `dept_code` varchar(64) NOT NULL COMMENT '部门编码',
  `leader` varchar(64) DEFAULT NULL COMMENT '部门负责人',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- ----------------------------
-- 行级安全规则表
-- ----------------------------
DROP TABLE IF EXISTS `sys_row_security`;
CREATE TABLE `sys_row_security` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `table_name` varchar(128) NOT NULL DEFAULT '*' COMMENT '表名(*表示所有表)',
  `filter_expression` varchar(512) NOT NULL COMMENT '过滤表达式(支持{deptId},{userId}占位符)',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_table_name` (`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行级安全规则表';

-- ----------------------------
-- 字段级权限表
-- ----------------------------
DROP TABLE IF EXISTS `sys_field_permission`;
CREATE TABLE `sys_field_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `table_name` varchar(128) NOT NULL DEFAULT '*' COMMENT '表名(*表示所有表)',
  `field_name` varchar(128) NOT NULL COMMENT '字段名',
  `permission_type` varchar(32) NOT NULL DEFAULT 'HIDDEN' COMMENT '权限类型 HIDDEN-隐藏 MASKED-脱敏',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_table_field` (`table_name`, `field_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字段级权限表';

-- ----------------------------
-- sys_user 表增加 dept_id 字段
-- ----------------------------
ALTER TABLE `sys_user` ADD COLUMN `dept_id` bigint DEFAULT NULL COMMENT '部门ID' AFTER `avatar`;

-- ----------------------------
-- 部门初始化数据
-- ----------------------------
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `dept_code`, `leader`, `sort_order`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 0, '总公司', 'HQ', 'admin', 1, 1, NOW(), NOW(), 0),
(2, 1, '研发部', 'DEV', 'zhangsan', 1, 1, NOW(), NOW(), 0),
(3, 1, '销售部', 'SALES', 'lisi', 2, 1, NOW(), NOW(), 0),
(4, 1, '财务部', 'FIN', 'wangwu', 3, 1, NOW(), NOW(), 0),
(5, 2, '前端组', 'DEV_FE', 'zhaoliu', 1, 1, NOW(), NOW(), 0),
(6, 2, '后端组', 'DEV_BE', 'sunqi', 2, 1, NOW(), NOW(), 0);

-- ----------------------------
-- 更新admin用户部门
-- ----------------------------
UPDATE `sys_user` SET `dept_id` = 1 WHERE `username` = 'admin';

-- ----------------------------
-- 行级安全规则初始化数据
-- 普通用户只能查看本部门数据
-- ----------------------------
INSERT INTO `sys_row_security` (`role_id`, `table_name`, `filter_expression`, `description`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(3, '*', 'dept_id = {deptId}', '普通用户只能查看本部门数据', 1, NOW(), NOW(), 0),
(2, '*', 'dept_id = {deptId} OR create_by = {userId}', '报表设计者可查看本部门数据及自己创建的数据', 1, NOW(), NOW(), 0);

-- ----------------------------
-- 字段级权限初始化数据
-- 普通用户隐藏成本价、利润等敏感字段
-- ----------------------------
INSERT INTO `sys_field_permission` (`role_id`, `table_name`, `field_name`, `permission_type`, `description`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(3, '*', 'cost_price', 'HIDDEN', '普通用户隐藏成本价', 1, NOW(), NOW(), 0),
(3, '*', 'profit', 'HIDDEN', '普通用户隐藏利润', 1, NOW(), NOW(), 0),
(3, '*', 'cost', 'HIDDEN', '普通用户隐藏成本', 1, NOW(), NOW(), 0),
(3, '*', 'purchase_price', 'HIDDEN', '普通用户隐藏采购价', 1, NOW(), NOW(), 0),
(3, '*', 'phone', 'MASKED', '普通用户手机号脱敏', 1, NOW(), NOW(), 0),
(3, '*', 'email', 'MASKED', '普通用户邮箱脱敏', 1, NOW(), NOW(), 0);

-- ----------------------------
-- 安全管理菜单数据
-- 行级安全、字段权限菜单（与前端路由路径对齐）
-- ----------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `create_time`, `update_time`, `deleted`) VALUES
-- 系统管理子菜单：行级安全、字段权限
(103, 1, '行级安全', 'C', '/system/row-security', 'system/row-security/index', 'system:row-security:list', 'safety', 4, 1, NOW(), NOW(), 0),
(104, 1, '字段权限', 'C', '/system/field-permission', 'system/field-permission/index', 'system:field-permission:list', 'lock', 5, 1, NOW(), NOW(), 0);

-- 行级安全按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `create_time`, `update_time`, `deleted`) VALUES
(1031, 103, '行级安全查询', 'F', NULL, NULL, 'system:row-security:query', NULL, 1, 1, NOW(), NOW(), 0),
(1032, 103, '行级安全新增', 'F', NULL, NULL, 'system:row-security:add', NULL, 2, 1, NOW(), NOW(), 0),
(1033, 103, '行级安全修改', 'F', NULL, NULL, 'system:row-security:edit', NULL, 3, 1, NOW(), NOW(), 0),
(1034, 103, '行级安全删除', 'F', NULL, NULL, 'system:row-security:remove', NULL, 4, 1, NOW(), NOW(), 0);

-- 字段权限按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `create_time`, `update_time`, `deleted`) VALUES
(1041, 104, '字段权限查询', 'F', NULL, NULL, 'system:field-permission:query', NULL, 1, 1, NOW(), NOW(), 0),
(1042, 104, '字段权限新增', 'F', NULL, NULL, 'system:field-permission:add', NULL, 2, 1, NOW(), NOW(), 0),
(1043, 104, '字段权限修改', 'F', NULL, NULL, 'system:field-permission:edit', NULL, 3, 1, NOW(), NOW(), 0),
(1044, 104, '字段权限删除', 'F', NULL, NULL, 'system:field-permission:remove', NULL, 4, 1, NOW(), NOW(), 0);

-- ----------------------------
-- 交叉报表菜单数据
-- 与前端路由路径对齐
-- ----------------------------
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `create_time`, `update_time`, `deleted`) VALUES
(105, 0, '交叉报表', 'C', '/pivot-designer', 'pivot-designer/index', 'pivot:designer:list', 'table', 3, 1, NOW(), NOW(), 0);

-- 交叉报表按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `create_time`, `update_time`, `deleted`) VALUES
(1051, 105, '交叉报表查询', 'F', NULL, NULL, 'pivot:designer:query', NULL, 1, 1, NOW(), NOW(), 0),
(1052, 105, '交叉报表执行', 'F', NULL, NULL, 'pivot:designer:execute', NULL, 2, 1, NOW(), NOW(), 0),
(1053, 105, '交叉报表导出', 'F', NULL, NULL, 'pivot:designer:export', NULL, 3, 1, NOW(), NOW(), 0);

SET FOREIGN_KEY_CHECKS = 1;
