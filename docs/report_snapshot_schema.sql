-- ============================================
-- 报表快照功能表结构
-- 创建时间: 2026-06-19
-- 说明: 支持周期性快照生成、快照存储、快照对比、过期清理
-- ============================================

-- ============================================
-- 1. 快照配置表
-- ============================================
DROP TABLE IF EXISTS `report_snapshot_config`;
CREATE TABLE `report_snapshot_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` BIGINT NOT NULL COMMENT '报表ID',
  `report_name` VARCHAR(255) DEFAULT NULL COMMENT '报表名称',
  `enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 1-启用 0-停用',
  `cron_expression` VARCHAR(100) NOT NULL COMMENT 'Cron表达式，如: 0 0 2 * * ? 每天凌晨2点',
  `retention_days` INT DEFAULT 30 COMMENT '快照保留天数，超过自动清理',
  `snapshot_type` VARCHAR(20) DEFAULT 'FULL' COMMENT '快照类型: FULL-完整快照 INCREMENTAL-增量快照',
  `storage_type` VARCHAR(20) DEFAULT 'MYSQL' COMMENT '存储类型: MYSQL/CLICKHOUSE',
  `params_json` TEXT COMMENT '快照生成时使用的参数JSON',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '配置说明',
  `last_snapshot_time` DATETIME DEFAULT NULL COMMENT '最近快照生成时间',
  `last_snapshot_id` BIGINT DEFAULT NULL COMMENT '最近一次快照ID',
  `snapshot_count` INT DEFAULT 0 COMMENT '已生成快照数量',
  `max_snapshots` INT DEFAULT 100 COMMENT '最大快照数量，超过删除最旧的',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-停用',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
  `create_by_name` VARCHAR(100) DEFAULT NULL COMMENT '创建人姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 1-删除 0-未删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_enabled_status` (`enabled`, `status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表快照配置表';

-- ============================================
-- 2. 数据快照表
-- ============================================
DROP TABLE IF EXISTS `report_data_snapshot`;
CREATE TABLE `report_data_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` BIGINT NOT NULL COMMENT '报表ID',
  `report_name` VARCHAR(255) DEFAULT NULL COMMENT '报表名称',
  `config_id` BIGINT DEFAULT NULL COMMENT '快照配置ID',
  `snapshot_name` VARCHAR(255) NOT NULL COMMENT '快照名称',
  `snapshot_type` VARCHAR(20) DEFAULT 'FULL' COMMENT '快照类型: FULL-完整快照 INCREMENTAL-增量快照',
  `storage_type` VARCHAR(20) DEFAULT 'MYSQL' COMMENT '存储类型: MYSQL/CLICKHOUSE',
  `data_version` VARCHAR(50) DEFAULT NULL COMMENT '数据版本号（UUID短码）',
  `params_json` TEXT COMMENT '生成快照时的参数JSON',
  `data_json` LONGTEXT COMMENT '快照数据JSON',
  `data_size` BIGINT DEFAULT 0 COMMENT '数据大小（字节）',
  `row_count` BIGINT DEFAULT 0 COMMENT '数据总行数',
  `table_count` INT DEFAULT 0 COMMENT '数据集数量',
  `execute_time` BIGINT DEFAULT 0 COMMENT '快照生成耗时（毫秒）',
  `data_hash` VARCHAR(64) DEFAULT NULL COMMENT '数据内容SHA256哈希，用于快速比对',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间，到期自动清理',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-生成中 1-成功 -1-失败',
  `error_msg` TEXT COMMENT '错误信息（生成失败时）',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
  `create_by_name` VARCHAR(100) DEFAULT NULL COMMENT '创建人姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 1-删除 0-未删除',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_config_id` (`config_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_expire_time` (`expire_time`),
  KEY `idx_status` (`status`),
  KEY `idx_report_create` (`report_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表数据快照表';

-- ============================================
-- 3. ClickHouse 可选存储表（如使用 ClickHouse）
-- ============================================
-- 说明: 如果启用 ClickHouse 存储，可创建以下 MergeTree 表存储数据
--       数据 JSON 可直接写入，MySQL 表仅存元数据

-- 可选: 创建 ClickHouse 数据库
-- CREATE DATABASE IF NOT EXISTS open_report_snapshots ON CLUSTER default_cluster;

-- 可选: ClickHouse 数据快照明细表
-- CREATE TABLE IF NOT EXISTS open_report_snapshots.report_snapshot_data ON CLUSTER default_cluster
-- (
--     snapshot_id UInt64 COMMENT '快照ID，关联MySQL',
--     report_id UInt64 COMMENT '报表ID',
--     config_id UInt64 COMMENT '配置ID',
--     data_version String COMMENT '数据版本',
--     bind_name String COMMENT '数据集绑定名称',
--     dataset_id UInt64 COMMENT '数据集ID',
--     row_data String COMMENT '单行数据JSON',
--     row_index UInt32 COMMENT '行号',
--     create_time DateTime DEFAULT now() COMMENT '创建时间',
--     partition_date Date DEFAULT toDate(create_time) COMMENT '分区日期'
-- )
-- ENGINE = ReplicatedMergeTree('/clickhouse/tables/{layer}-{shard}/report_snapshot_data', '{replica}')
-- PARTITION BY toYYYYMM(partition_date)
-- ORDER BY (snapshot_id, report_id, bind_name, row_index)
-- TTL create_time + INTERVAL 30 DAY
-- SETTINGS index_granularity = 8192;
-- COMMENT '报表快照明细数据表（ClickHouse存储）';

-- ============================================
-- 4. 初始化示例数据（可选）
-- ============================================
-- 示例: 为报表ID=1配置每天凌晨2点的快照，保留30天
-- INSERT INTO `report_snapshot_config`
--   (`report_id`, `report_name`, `enabled`, `cron_expression`, `retention_days`, `snapshot_type`, `description`, `max_snapshots`)
-- VALUES
--   (1, '销售日报表', 1, '0 0 2 * * ?', 30, 'FULL', '每日凌晨2点生成销售日报快照，保留30天', 100);
