-- ============================================
-- 血缘分析与影响分析表结构
-- 创建时间: 2026-06-19
-- 说明: 支持报表字段到数据库表字段的血缘追溯，以及数据库变更时的影响分析
-- ============================================

-- ============================================
-- 1. 数据血缘关系表
-- ============================================
DROP TABLE IF EXISTS `data_lineage`;
CREATE TABLE `data_lineage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` BIGINT NOT NULL COMMENT '报表ID',
  `report_name` VARCHAR(255) DEFAULT NULL COMMENT '报表名称',
  `report_field` VARCHAR(100) NOT NULL COMMENT '报表字段名',
  `report_field_title` VARCHAR(255) DEFAULT NULL COMMENT '报表字段显示名',
  `data_set_id` BIGINT NOT NULL COMMENT '数据集ID',
  `data_set_name` VARCHAR(255) DEFAULT NULL COMMENT '数据集名称',
  `data_set_field` VARCHAR(100) NOT NULL COMMENT '数据集字段名',
  `bind_name` VARCHAR(100) DEFAULT 'default' COMMENT '数据集绑定名称',
  `expression` TEXT COMMENT 'SQL表达式或字段映射关系',
  `lineage_type` VARCHAR(20) DEFAULT 'DIRECT' COMMENT '血缘类型: DIRECT-直接映射 EXPRESSION-表达式计算 AGGREGATION-聚合计算',
  `datasource_id` BIGINT DEFAULT NULL COMMENT '数据源ID',
  `datasource_name` VARCHAR(255) DEFAULT NULL COMMENT '数据源名称',
  `datasource_type` VARCHAR(20) DEFAULT NULL COMMENT '数据源类型: MYSQL/POSTGRESQL等',
  `database_name` VARCHAR(100) DEFAULT NULL COMMENT '数据库名',
  `schema_name` VARCHAR(100) DEFAULT NULL COMMENT 'Schema名',
  `table_name` VARCHAR(100) DEFAULT NULL COMMENT '数据库表名',
  `column_name` VARCHAR(100) DEFAULT NULL COMMENT '数据库字段名',
  `source_tables` TEXT COMMENT '涉及的源表JSON数组',
  `source_columns` TEXT COMMENT '涉及的源字段JSON数组',
  `sql_text` TEXT COMMENT '原始SQL片段',
  `lineage_hash` VARCHAR(64) DEFAULT NULL COMMENT '血缘哈希，用于去重',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1-有效 0-无效',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 1-删除 0-未删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_dataset_id` (`data_set_id`),
  KEY `idx_datasource_id` (`datasource_id`),
  KEY `idx_table_column` (`table_name`, `column_name`),
  KEY `idx_lineage_hash` (`lineage_hash`),
  KEY `idx_report_field` (`report_id`, `report_field`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据血缘关系表';

-- ============================================
-- 2. SQL解析结果缓存表（可选，用于性能优化）
-- ============================================
DROP TABLE IF EXISTS `sql_parse_cache`;
CREATE TABLE `sql_parse_cache` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sql_hash` VARCHAR(64) NOT NULL COMMENT 'SQL哈希值',
  `sql_text` TEXT COMMENT '原始SQL',
  `parse_result` TEXT COMMENT '解析结果JSON',
  `tables_json` TEXT COMMENT '涉及的表JSON',
  `columns_json` TEXT COMMENT '涉及的字段JSON',
  `parse_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '解析时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sql_hash` (`sql_hash`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SQL解析结果缓存表';

-- ============================================
-- 3. 索引说明
-- ============================================
-- 血缘查询: report_id → 报表字段 → 数据集 → 数据库表字段
-- 影响分析: datasource_id + table_name + column_name → 数据集 → 报表
