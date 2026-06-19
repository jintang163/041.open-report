-- =====================================================
-- Open Report 系统数据库建表脚本 (MySQL 8.0)
-- 编码: UTF-8
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 系统用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ----------------------------
-- 系统角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `role_name` varchar(64) NOT NULL COMMENT '角色名称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- ----------------------------
-- 系统菜单表
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父菜单ID',
  `menu_name` varchar(64) NOT NULL COMMENT '菜单名称',
  `menu_type` char(1) NOT NULL COMMENT '菜单类型 M-目录 C-菜单 F-按钮',
  `path` varchar(255) DEFAULT NULL COMMENT '路由路径',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `perms` varchar(128) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(64) DEFAULT NULL COMMENT '菜单图标',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `visible` tinyint NOT NULL DEFAULT 1 COMMENT '显示状态 0-隐藏 1-显示',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单表';

-- ----------------------------
-- 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ----------------------------
-- 角色菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- ----------------------------
-- 数据源配置表
-- ----------------------------
DROP TABLE IF EXISTS `data_source_config`;
CREATE TABLE `data_source_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(128) NOT NULL COMMENT '数据源名称',
  `code` varchar(64) NOT NULL COMMENT '数据源编码',
  `source_type` varchar(32) NOT NULL COMMENT '数据源类型 MYSQL/ORACLE/POSTGRESQL/SQLSERVER/DM/API',
  `host` varchar(128) DEFAULT NULL COMMENT '主机地址',
  `port` int DEFAULT NULL COMMENT '端口',
  `database_name` varchar(128) DEFAULT NULL COMMENT '数据库名称',
  `username` varchar(64) DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) DEFAULT NULL COMMENT '密码',
  `jdbc_url` varchar(512) DEFAULT NULL COMMENT 'JDBC连接URL',
  `driver_class` varchar(255) DEFAULT NULL COMMENT '驱动类名',
  `api_url` varchar(512) DEFAULT NULL COMMENT 'API地址',
  `api_method` varchar(16) DEFAULT NULL COMMENT 'API请求方法 GET/POST',
  `api_headers` json DEFAULT NULL COMMENT 'API请求头(JSON)',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源配置表';

-- ----------------------------
-- 数据集表
-- ----------------------------
DROP TABLE IF EXISTS `data_set`;
CREATE TABLE `data_set` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(128) NOT NULL COMMENT '数据集名称',
  `code` varchar(64) NOT NULL COMMENT '数据集编码',
  `data_source_id` bigint NOT NULL COMMENT '数据源ID',
  `sql_text` text COMMENT 'SQL语句',
  `params` json DEFAULT NULL COMMENT '参数定义(JSON)',
  `fields` json DEFAULT NULL COMMENT '字段定义(JSON)',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_data_source_id` (`data_source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据集表';

-- ----------------------------
-- 报表模板表
-- ----------------------------
DROP TABLE IF EXISTS `report_template`;
CREATE TABLE `report_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(128) NOT NULL COMMENT '报表名称',
  `code` varchar(64) NOT NULL COMMENT '报表编码',
  `type` varchar(32) NOT NULL COMMENT '报表类型',
  `template_json` longtext COMMENT '报表模板JSON',
  `thumbnail` varchar(512) DEFAULT NULL COMMENT '缩略图',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `params` json DEFAULT NULL COMMENT '参数定义(JSON)',
  `data_sets` json DEFAULT NULL COMMENT '数据集配置(JSON)',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT-草稿 PUBLISHED-已发布',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本号',
  `view_count` bigint NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表模板表';

-- ----------------------------
-- 报表调度表
-- ----------------------------
DROP TABLE IF EXISTS `report_schedule`;
CREATE TABLE `report_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(128) NOT NULL COMMENT '任务名称',
  `report_id` bigint NOT NULL COMMENT '报表ID',
  `cron_expression` varchar(128) NOT NULL COMMENT 'Cron表达式',
  `params` json DEFAULT NULL COMMENT '执行参数(JSON)',
  `output_type` varchar(32) NOT NULL COMMENT '输出类型 PDF/EXCEL/IMAGE/EMAIL',
  `email_list` varchar(1024) DEFAULT NULL COMMENT '邮件收件人列表(逗号分隔)',
  `email_cc_list` varchar(1024) DEFAULT NULL COMMENT '邮件抄送列表(逗号分隔)',
  `email_subject` varchar(256) DEFAULT NULL COMMENT '邮件主题',
  `email_content` text COMMENT '邮件正文',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `max_retry_count` int NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
  `last_execute_time` datetime DEFAULT NULL COMMENT '上次执行时间',
  `next_execute_time` datetime DEFAULT NULL COMMENT '下次执行时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表调度表';

-- ----------------------------
-- 报表执行日志表
-- ----------------------------
DROP TABLE IF EXISTS `report_log`;
CREATE TABLE `report_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `schedule_id` bigint DEFAULT NULL COMMENT '调度任务ID',
  `report_id` bigint NOT NULL COMMENT '报表ID',
  `execute_type` varchar(32) NOT NULL COMMENT '执行类型 MANUAL-手动 SCHEDULE-定时 RETRY-重试',
  `params` json DEFAULT NULL COMMENT '执行参数(JSON)',
  `status` varchar(32) NOT NULL COMMENT '执行状态 RUNNING-执行中 SUCCESS-成功 FAIL-失败',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `cost_time` bigint DEFAULT NULL COMMENT '耗时(毫秒)',
  `error_msg` text COMMENT '错误信息',
  `output_path` varchar(512) DEFAULT NULL COMMENT '输出文件路径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_schedule_id` (`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表执行日志表';

-- ----------------------------
-- 可视化大屏表
-- ----------------------------
DROP TABLE IF EXISTS `chart_dashboard`;
CREATE TABLE `chart_dashboard` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(128) NOT NULL COMMENT '大屏名称',
  `code` varchar(64) NOT NULL COMMENT '大屏编码',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `canvas_width` int NOT NULL DEFAULT 1920 COMMENT '画布宽度',
  `canvas_height` int NOT NULL DEFAULT 1080 COMMENT '画布高度',
  `background_color` varchar(32) DEFAULT '#0d1b2a' COMMENT '背景色',
  `refresh_interval` int DEFAULT 0 COMMENT '自动刷新间隔(秒) 0-不刷新',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可视化大屏表';

-- ----------------------------
-- 大屏图表组件表
-- ----------------------------
DROP TABLE IF EXISTS `chart_dashboard_item`;
CREATE TABLE `chart_dashboard_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dashboard_id` bigint NOT NULL COMMENT '大屏ID',
  `title` varchar(128) DEFAULT NULL COMMENT '图表标题',
  `chart_type` varchar(32) NOT NULL COMMENT '图表类型 bar/line/pie/radar/scatter',
  `dataset_id` bigint DEFAULT NULL COMMENT '数据集ID',
  `x_field` varchar(128) DEFAULT NULL COMMENT '分类轴字段',
  `y_fields` json DEFAULT NULL COMMENT '数值轴字段(JSON数组)',
  `linkage_field` varchar(128) DEFAULT NULL COMMENT '联动字段',
  `linkage_target_id` bigint DEFAULT NULL COMMENT '联动目标组件ID',
  `position_x` int NOT NULL DEFAULT 0 COMMENT 'X坐标',
  `position_y` int NOT NULL DEFAULT 0 COMMENT 'Y坐标',
  `width` int NOT NULL DEFAULT 400 COMMENT '宽度',
  `height` int NOT NULL DEFAULT 300 COMMENT '高度',
  `chart_config` json DEFAULT NULL COMMENT '图表扩展配置(JSON)',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_dashboard_id` (`dashboard_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大屏图表组件表';

-- ----------------------------
-- 数据血缘关系表
-- ----------------------------
DROP TABLE IF EXISTS `data_lineage`;
CREATE TABLE `data_lineage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` bigint NOT NULL COMMENT '报表ID',
  `report_name` varchar(255) DEFAULT NULL COMMENT '报表名称',
  `report_field` varchar(100) NOT NULL COMMENT '报表字段名',
  `report_field_title` varchar(255) DEFAULT NULL COMMENT '报表字段显示名',
  `data_set_id` bigint NOT NULL COMMENT '数据集ID',
  `data_set_name` varchar(255) DEFAULT NULL COMMENT '数据集名称',
  `data_set_field` varchar(100) NOT NULL COMMENT '数据集字段名',
  `bind_name` varchar(100) DEFAULT 'default' COMMENT '数据集绑定名称',
  `expression` text COMMENT 'SQL表达式或字段映射关系',
  `lineage_type` varchar(20) DEFAULT 'DIRECT' COMMENT '血缘类型: DIRECT-直接映射 EXPRESSION-表达式计算 AGGREGATION-聚合计算',
  `datasource_id` bigint DEFAULT NULL COMMENT '数据源ID',
  `datasource_name` varchar(255) DEFAULT NULL COMMENT '数据源名称',
  `datasource_type` varchar(20) DEFAULT NULL COMMENT '数据源类型',
  `database_name` varchar(100) DEFAULT NULL COMMENT '数据库名',
  `schema_name` varchar(100) DEFAULT NULL COMMENT 'Schema名',
  `table_name` varchar(100) DEFAULT NULL COMMENT '数据库表名',
  `column_name` varchar(100) DEFAULT NULL COMMENT '数据库字段名',
  `source_tables` text COMMENT '涉及的源表JSON数组',
  `source_columns` text COMMENT '涉及的源字段JSON数组',
  `sql_text` text COMMENT '原始SQL片段',
  `lineage_hash` varchar(64) DEFAULT NULL COMMENT '血缘哈希，用于去重',
  `status` tinyint DEFAULT 1 COMMENT '状态: 1-有效 0-无效',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除: 1-删除 0-未删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_dataset_id` (`data_set_id`),
  KEY `idx_datasource_id` (`datasource_id`),
  KEY `idx_table_column` (`table_name`, `column_name`),
  KEY `idx_lineage_hash` (`lineage_hash`),
  KEY `idx_report_field` (`report_id`, `report_field`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据血缘关系表';

-- ----------------------------
-- 报表评论表
-- ----------------------------
DROP TABLE IF EXISTS `report_comment`;
CREATE TABLE `report_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '报表模板ID',
  `template_name` varchar(200) DEFAULT NULL COMMENT '报表模板名称',
  `snapshot_version` int DEFAULT NULL COMMENT '报表快照版本号',
  `cell_ref` varchar(100) DEFAULT NULL COMMENT '单元格引用（如A1、B3）',
  `chart_id` varchar(100) DEFAULT NULL COMMENT '图表ID',
  `content` text NOT NULL COMMENT '评论内容',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID（为空表示顶级评论）',
  `reply_to_user_id` bigint DEFAULT NULL COMMENT '回复目标用户ID',
  `reply_to_user_name` varchar(100) DEFAULT NULL COMMENT '回复目标用户名',
  `mention_user_ids` varchar(500) DEFAULT NULL COMMENT '@提及用户ID列表（逗号分隔）',
  `like_count` int DEFAULT 0 COMMENT '点赞数',
  `reply_count` int DEFAULT 0 COMMENT '回复数',
  `create_by` bigint DEFAULT NULL COMMENT '评论者ID',
  `create_by_name` varchar(100) DEFAULT NULL COMMENT '评论者名称',
  `create_by_avatar` varchar(500) DEFAULT NULL COMMENT '评论者头像',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除: 1-删除 0-未删除',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_template_version` (`template_id`, `snapshot_version`),
  KEY `idx_cell_ref` (`template_id`, `cell_ref`),
  KEY `idx_chart_id` (`template_id`, `chart_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表评论表';

-- ----------------------------
-- 评论点赞表
-- ----------------------------
DROP TABLE IF EXISTS `report_comment_like`;
CREATE TABLE `report_comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `comment_id` bigint NOT NULL COMMENT '评论ID',
  `user_id` bigint NOT NULL COMMENT '点赞用户ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '点赞用户名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

SET FOREIGN_KEY_CHECKS = 1;
