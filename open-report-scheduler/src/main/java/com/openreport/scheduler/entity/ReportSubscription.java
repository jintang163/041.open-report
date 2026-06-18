package com.openreport.scheduler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.openreport.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_subscription")
public class ReportSubscription extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField("name")
    private String name;

    @TableField("report_id")
    private Long reportId;

    @TableField("channels")
    private String channels;

    @TableField("frequency")
    private String frequency;

    @TableField("push_time")
    private LocalTime pushTime;

    @TableField("push_day_of_week")
    private Integer pushDayOfWeek;

    @TableField("push_day_of_month")
    private Integer pushDayOfMonth;

    @TableField("dingtalk_webhook")
    private String dingtalkWebhook;

    @TableField("dingtalk_secret")
    private String dingtalkSecret;

    @TableField("wecom_webhook")
    private String wecomWebhook;

    @TableField("email_list")
    private String emailList;

    @TableField("email_cc_list")
    private String emailCcList;

    @TableField("email_subject")
    private String emailSubject;

    @TableField("message_format")
    private String messageFormat;

    @TableField("content_template")
    private String contentTemplate;

    @TableField("include_chart")
    private Boolean includeChart;

    @TableField("include_attachment")
    private Boolean includeAttachment;

    @TableField("attachment_type")
    private String attachmentType;

    @TableField("params")
    private String params;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("max_retry_count")
    private Integer maxRetryCount;

    @TableField("status")
    private Integer status;

    @TableField("last_push_time")
    private LocalDateTime lastPushTime;

    @TableField("next_push_time")
    private LocalDateTime nextPushTime;
}
