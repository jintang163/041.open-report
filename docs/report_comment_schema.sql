CREATE TABLE IF NOT EXISTS `report_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `template_id` BIGINT NOT NULL COMMENT '报表模板ID',
    `template_name` VARCHAR(200) DEFAULT NULL COMMENT '报表模板名称',
    `snapshot_version` INT DEFAULT NULL COMMENT '报表快照版本号',
    `cell_ref` VARCHAR(100) DEFAULT NULL COMMENT '单元格引用（如A1、B3）',
    `chart_id` VARCHAR(100) DEFAULT NULL COMMENT '图表ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID（为空表示顶级评论）',
    `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '回复目标用户ID',
    `reply_to_user_name` VARCHAR(100) DEFAULT NULL COMMENT '回复目标用户名',
    `mention_user_ids` VARCHAR(500) DEFAULT NULL COMMENT '@提及用户ID列表（逗号分隔）',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `reply_count` INT DEFAULT 0 COMMENT '回复数',
    `create_by` BIGINT DEFAULT NULL COMMENT '评论者ID',
    `create_by_name` VARCHAR(100) DEFAULT NULL COMMENT '评论者名称',
    `create_by_avatar` VARCHAR(500) DEFAULT NULL COMMENT '评论者头像',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_template_version` (`template_id`, `snapshot_version`),
    KEY `idx_cell_ref` (`template_id`, `cell_ref`),
    KEY `idx_chart_id` (`template_id`, `chart_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表评论表';

CREATE TABLE IF NOT EXISTS `report_comment_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
    `user_name` VARCHAR(100) DEFAULT NULL COMMENT '点赞用户名',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';

INSERT INTO `sys_menu` (`parent_id`, `name`, `path`, `component`, `perms`, `icon`, `menu_type`, `sort`)
SELECT id, '评论管理', '', '', 'report:comment:list', 'CommentOutlined', 2, 8
FROM `sys_menu` WHERE `perms` = 'report:view' LIMIT 1;

INSERT INTO `sys_menu` (`parent_id`, `name`, `path`, `component`, `perms`, `icon`, `menu_type`, `sort`)
SELECT id, '添加评论', '', '', 'report:comment:add', '', 3, 1
FROM `sys_menu` WHERE `perms` = 'report:comment:list' LIMIT 1;

INSERT INTO `sys_menu` (`parent_id`, `name`, `path`, `component`, `perms`, `icon`, `menu_type`, `sort`)
SELECT id, '删除评论', '', '', 'report:comment:delete', '', 3, 2
FROM `sys_menu` WHERE `perms` = 'report:comment:list' LIMIT 1;

INSERT INTO `sys_menu` (`parent_id`, `name`, `path`, `component`, `perms`, `icon`, `menu_type`, `sort`)
SELECT id, '评论点赞', '', '', 'report:comment:like', '', 3, 3
FROM `sys_menu` WHERE `perms` = 'report:comment:list' LIMIT 1;
