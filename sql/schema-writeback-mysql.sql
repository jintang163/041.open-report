-- =====================================================
-- Open Report 填报报表与数据回写 数据库建表脚本 (MySQL 8.0)
-- 编码: UTF-8
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 报表回写配置表
-- 存储报表中可编辑单元格的配置信息
-- ----------------------------
DROP TABLE IF EXISTS `report_writeback_config`;
CREATE TABLE `report_writeback_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` bigint NOT NULL COMMENT '报表模板ID',
  `data_source_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(128) NOT NULL COMMENT '目标数据表名',
  `primary_key_field` varchar(64) NOT NULL COMMENT '主键字段名',
  `primary_key_column` varchar(64) DEFAULT NULL COMMENT '主键列（单元格位置，如A1）',
  `version_field` varchar(64) DEFAULT NULL COMMENT '乐观锁版本字段',
  `logic_delete_field` varchar(64) DEFAULT NULL COMMENT '逻辑删除字段',
  `logic_delete_value` varchar(32) DEFAULT NULL COMMENT '逻辑删除值',
  `logic_not_delete_value` varchar(32) DEFAULT NULL COMMENT '逻辑未删除值',
  `batch_support` tinyint NOT NULL DEFAULT 1 COMMENT '是否支持批量提交 0-否 1-是',
  `transaction_enable` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用事务 0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_data_source_id` (`data_source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表回写配置表';

-- ----------------------------
-- 报表回写字段映射表
-- 存储可编辑单元格与数据表字段的映射关系
-- ----------------------------
DROP TABLE IF EXISTS `report_writeback_field`;
CREATE TABLE `report_writeback_field` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_id` bigint NOT NULL COMMENT '回写配置ID',
  `cell_position` varchar(16) NOT NULL COMMENT '单元格位置（如A1、B2）',
  `field_name` varchar(64) NOT NULL COMMENT '数据表字段名',
  `field_type` varchar(32) NOT NULL DEFAULT 'STRING' COMMENT '字段类型 STRING/NUMBER/DATE/DATETIME/BOOLEAN',
  `editable` tinyint NOT NULL DEFAULT 1 COMMENT '是否可编辑 0-否 1-是',
  `required` tinyint NOT NULL DEFAULT 0 COMMENT '是否必填 0-否 1-是',
  `default_value` varchar(255) DEFAULT NULL COMMENT '默认值',
  `validation_rule` varchar(512) DEFAULT NULL COMMENT '校验规则（正则表达式）',
  `validation_message` varchar(255) DEFAULT NULL COMMENT '校验失败提示信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_config_id` (`config_id`),
  UNIQUE KEY `uk_config_cell` (`config_id`, `cell_position`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表回写字段映射表';

-- ----------------------------
-- 报表数据提交历史表
-- 记录每次数据提交的主记录
-- ----------------------------
DROP TABLE IF EXISTS `report_writeback_history`;
CREATE TABLE `report_writeback_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` bigint NOT NULL COMMENT '报表模板ID',
  `config_id` bigint NOT NULL COMMENT '回写配置ID',
  `batch_no` varchar(64) NOT NULL COMMENT '提交批次号',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总记录数',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功数',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败数',
  `status` varchar(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '提交状态 PROCESSING-处理中 SUCCESS-成功 PARTIAL-部分成功 FAIL-失败',
  `execute_time` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `error_msg` text COMMENT '错误信息',
  `params` json DEFAULT NULL COMMENT '提交时的报表参数(JSON)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '提交人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_config_id` (`config_id`),
  KEY `idx_batch_no` (`batch_no`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表数据提交历史表';

-- ----------------------------
-- 报表数据提交明细表
-- 记录每次提交的每行数据变更详情
-- ----------------------------
DROP TABLE IF EXISTS `report_writeback_detail`;
CREATE TABLE `report_writeback_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `history_id` bigint NOT NULL COMMENT '提交历史ID',
  `row_index` int NOT NULL COMMENT '行号',
  `row_status` varchar(32) NOT NULL COMMENT '行状态 INSERT-新增 UPDATE-修改 DELETE-删除',
  `primary_key_value` varchar(255) DEFAULT NULL COMMENT '主键值',
  `old_data` json DEFAULT NULL COMMENT '变更前数据(JSON)',
  `new_data` json DEFAULT NULL COMMENT '变更后数据(JSON)',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '执行状态 PENDING-待执行 SUCCESS-成功 FAIL-失败',
  `execute_sql` text COMMENT '执行的SQL语句',
  `error_msg` varchar(1024) DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_history_id` (`history_id`),
  KEY `idx_row_index` (`row_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表数据提交明细表';

SET FOREIGN_KEY_CHECKS = 1;
