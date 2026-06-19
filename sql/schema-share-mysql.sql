-- =====================================================
-- Open Report 匿名分享功能数据库迁移脚本 (MySQL 8.0)
-- 编码: UTF-8
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 为 report_template 表添加分享相关字段
-- ----------------------------
ALTER TABLE `report_template`
ADD COLUMN `share_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否开启分享 0-关闭 1-开启' AFTER `status`,
ADD COLUMN `share_token` varchar(64) DEFAULT NULL COMMENT '分享访问令牌' AFTER `share_enabled`,
ADD COLUMN `share_expire_time` datetime DEFAULT NULL COMMENT '分享过期时间' AFTER `share_token`,
ADD COLUMN `share_password` varchar(64) DEFAULT NULL COMMENT '分享访问密码(加密存储)' AFTER `share_expire_time`,
ADD COLUMN `share_view_count` bigint NOT NULL DEFAULT 0 COMMENT '分享浏览次数' AFTER `share_password`,
ADD UNIQUE KEY `uk_share_token` (`share_token`);

SET FOREIGN_KEY_CHECKS = 1;
