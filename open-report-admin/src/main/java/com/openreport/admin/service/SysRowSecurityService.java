package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.SysRowSecurity;

import java.util.List;

public interface SysRowSecurityService extends IService<SysRowSecurity> {

    List<SysRowSecurity> listByRoleIds(List<Long> roleIds);

    String buildRowFilterExpression(String tableName, List<Long> roleIds, Long deptId, Long userId);
}
