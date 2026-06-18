package com.openreport.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_api_key")
public class SysApiKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("api_key")
    private String apiKey;

    @TableField("api_secret")
    private String apiSecret;

    @TableField("app_name")
    private String appName;

    @TableField("description")
    private String description;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("status")
    private Integer status;

    @TableField("rate_limit")
    private Integer rateLimit;

    @TableField("ip_whitelist")
    private String ipWhitelist;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("call_count")
    private Long callCount;

    @TableField("last_call_time")
    private LocalDateTime lastCallTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
