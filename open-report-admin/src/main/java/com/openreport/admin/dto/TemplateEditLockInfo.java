package com.openreport.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateEditLockInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long templateId;

    private Long userId;

    private String userName;

    private Long lockTime;

    private Long expireTime;

    private String lockToken;
}
