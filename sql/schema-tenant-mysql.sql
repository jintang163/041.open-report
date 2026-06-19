-- =====================================================
-- 多租户数据源映射 建表脚本 (MySQL 8.0)
-- =====================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 租户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_name` varchar(128) NOT NULL COMMENT '租户名称',
  `tenant_code` varchar(64) NOT NULL COMMENT '租户编码',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

-- ----------------------------
-- 租户数据源映射表
-- ----------------------------
DROP TABLE IF EXISTS `tenant_datasource_mapping`;
CREATE TABLE `tenant_datasource_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `original_ds_id` bigint NOT NULL COMMENT '原始数据源ID',
  `target_ds_id` bigint NOT NULL COMMENT '目标数据源ID（租户专用）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_original_ds` (`tenant_id`, `original_ds_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_original_ds_id` (`original_ds_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户数据源映射表';

-- ----------------------------
-- sys_user 添加 tenant_id 字段
-- ----------------------------
ALTER TABLE `sys_user` ADD COLUMN `tenant_id` bigint DEFAULT NULL COMMENT '租户ID' AFTER `dept_id`;
ALTER TABLE `sys_user` ADD INDEX `idx_tenant_id` (`tenant_id`);

-- ----------------------------
-- 初始租户数据
-- ----------------------------
INSERT INTO `sys_tenant` (`tenant_name`, `tenant_code`, `description`, `status`) VALUES
('默认租户', 'DEFAULT', '系统默认租户', 1);
