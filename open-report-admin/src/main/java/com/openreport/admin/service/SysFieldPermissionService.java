package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.SysFieldPermission;

import java.util.List;
import java.util.Set;

public interface SysFieldPermissionService extends IService<SysFieldPermission> {

    List<SysFieldPermission> listByRoleIds(List<Long> roleIds);

    Set<String> getHiddenFields(String tableName, List<Long> roleIds);

    Set<String> getMaskedFields(String tableName, List<Long> roleIds);
}
