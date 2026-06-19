-- 报表函数扩展仓库表
-- 函数主表
DROP TABLE IF EXISTS `report_function`;
CREATE TABLE `report_function` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `func_name` VARCHAR(100) NOT NULL COMMENT '函数名称（唯一标识，用于调用）',
  `func_label` VARCHAR(200) NOT NULL COMMENT '函数显示名称',
  `func_category` VARCHAR(50) NOT NULL DEFAULT 'CUSTOM' COMMENT '函数分类：SYSTEM-系统内置，CUSTOM-自定义，MATH-数学，DATE-日期，STRING-字符串，LOGIC-逻辑',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '函数描述',
  `param_config` TEXT COMMENT '参数配置JSON，包含参数名、类型、是否必填、描述等',
  `return_type` VARCHAR(50) DEFAULT NULL COMMENT '返回值类型',
  `example` VARCHAR(1000) DEFAULT NULL COMMENT '使用示例',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `current_version` INT NOT NULL DEFAULT 1 COMMENT '当前生效版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_func_name` (`func_name`, `deleted`),
  KEY `idx_category` (`func_category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表函数定义表';

-- 函数版本表
DROP TABLE IF EXISTS `report_function_version`;
CREATE TABLE `report_function_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `func_id` BIGINT NOT NULL COMMENT '函数ID',
  `version` INT NOT NULL COMMENT '版本号',
  `script_content` LONGTEXT COMMENT 'Groovy脚本内容',
  `script_type` VARCHAR(20) NOT NULL DEFAULT 'GROOVY' COMMENT '脚本类型：GROOVY',
  `change_log` VARCHAR(500) DEFAULT NULL COMMENT '版本变更说明',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_func_version` (`func_id`, `version`),
  KEY `idx_func_id` (`func_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表函数版本表';

-- 初始化内置函数数据
INSERT INTO `report_function` (`func_name`, `func_label`, `func_category`, `description`, `param_config`, `return_type`, `example`, `status`, `current_version`, `create_by`, `create_time`) VALUES
('SUM', 'SUM - 求和', 'MATH', '对数据集字段进行求和计算', '[{"name":"field","type":"String","required":true,"description":"数据集字段引用，如 dataset.fieldName"}]', 'Number', '${SUM(sales.amount)}', 1, 1, 1, NOW()),
('AVG', 'AVG - 平均值', 'MATH', '对数据集字段计算平均值', '[{"name":"field","type":"String","required":true,"description":"数据集字段引用，如 dataset.fieldName"}]', 'Number', '${AVG(sales.amount)}', 1, 1, 1, NOW()),
('COUNT', 'COUNT - 计数', 'MATH', '统计数据集字段非空值的数量', '[{"name":"field","type":"String","required":true,"description":"数据集字段引用，如 dataset.fieldName"}]', 'Number', '${COUNT(sales.id)}', 1, 1, 1, NOW()),
('MAX', 'MAX - 最大值', 'MATH', '获取数据集字段的最大值', '[{"name":"field","type":"String","required":true,"description":"数据集字段引用，如 dataset.fieldName"}]', 'Number', '${MAX(sales.amount)}', 1, 1, 1, NOW()),
('MIN', 'MIN - 最小值', 'MATH', '获取数据集字段的最小值', '[{"name":"field","type":"String","required":true,"description":"数据集字段引用，如 dataset.fieldName"}]', 'Number', '${MIN(sales.amount)}', 1, 1, 1, NOW()),
('IF', 'IF - 条件判断', 'LOGIC', '根据条件返回不同的值', '[{"name":"condition","type":"Expression","required":true,"description":"条件表达式"},{"name":"trueValue","type":"Object","required":true,"description":"条件为真时返回的值"},{"name":"falseValue","type":"Object","required":false,"description":"条件为假时返回的值，默认为null"}]', 'Object', '${IF(amount>100, "高", "低")}', 1, 1, 1, NOW()),
('CONCAT', 'CONCAT - 字符串拼接', 'STRING', '将多个值拼接为一个字符串', '[{"name":"values","type":"Object[]","required":true,"description":"要拼接的值列表，支持多个参数"}]', 'String', '${CONCAT(firstName, " ", lastName)}', 1, 1, 1, NOW()),
('ROUND', 'ROUND - 四舍五入', 'MATH', '对数值进行四舍五入', '[{"name":"value","type":"Number","required":true,"description":"要四舍五入的数值"},{"name":"scale","type":"Integer","required":false,"description":"保留小数位数，默认0"}]', 'Number', '${ROUND(3.14159, 2)}', 1, 1, 1, NOW()),
('DATE_FORMAT', 'DATE_FORMAT - 日期格式化', 'DATE', '将日期格式化为指定格式的字符串', '[{"name":"date","type":"Date","required":true,"description":"日期值"},{"name":"pattern","type":"String","required":true,"description":"日期格式，如 yyyy-MM-dd"}]', 'String', '${DATE_FORMAT(createTime, "yyyy-MM-dd")}', 1, 1, 1, NOW()),
('TODAY', 'TODAY - 当前日期', 'DATE', '获取当前日期，返回 yyyy-MM-dd 格式的字符串', '[]', 'String', '${TODAY()}', 1, 1, 1, NOW()),
('NOW', 'NOW - 当前时间', 'DATE', '获取当前日期时间，返回 yyyy-MM-dd HH:mm:ss 格式的字符串', '[]', 'String', '${NOW()}', 1, 1, 1, NOW()),
('UPPER', 'UPPER - 转大写', 'STRING', '将字符串转换为大写', '[{"name":"str","type":"String","required":true,"description":"要转换的字符串"}]', 'String', '${UPPER(name)}', 1, 1, 1, NOW()),
('LOWER', 'LOWER - 转小写', 'STRING', '将字符串转换为小写', '[{"name":"str","type":"String","required":true,"description":"要转换的字符串"}]', 'String', '${LOWER(name)}', 1, 1, 1, NOW()),
('SUBSTRING', 'SUBSTRING - 截取子串', 'STRING', '截取字符串的子串', '[{"name":"str","type":"String","required":true,"description":"原字符串"},{"name":"start","type":"Integer","required":true,"description":"起始位置（从1开始）"},{"name":"length","type":"Integer","required":false,"description":"截取长度，默认截取到末尾"}]', 'String', '${SUBSTRING(name, 1, 3)}', 1, 1, 1, NOW()),
('ABS', 'ABS - 绝对值', 'MATH', '计算数值的绝对值', '[{"name":"value","type":"Number","required":true,"description":"数值"}]', 'Number', '${ABS(-10)}', 1, 1, 1, NOW());

-- 为内置函数创建版本记录
INSERT INTO `report_function_version` (`func_id`, `version`, `script_content`, `script_type`, `change_log`, `create_by`, `create_time`)
SELECT id, 1, NULL, 'SYSTEM', '系统内置函数', 1, NOW() FROM report_function WHERE func_category != 'CUSTOM';
