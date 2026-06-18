package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.SysFieldPermission;
import com.openreport.admin.mapper.SysFieldPermissionMapper;
import com.openreport.admin.service.SysFieldPermissionService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysFieldPermissionServiceImpl extends ServiceImpl<SysFieldPermissionMapper, SysFieldPermission> implements SysFieldPermissionService {

    @Override
    public List<SysFieldPermission> listByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<SysFieldPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysFieldPermission::getRoleId, roleIds);
        wrapper.eq(SysFieldPermission::getStatus, 1);
        return list(wrapper);
    }

    @Override
    public Set<String> getHiddenFields(String tableName, List<Long> roleIds) {
        List<SysFieldPermission> permissions = listByRoleIds(roleIds);
        Set<String> hiddenFields = new HashSet<>();
        for (SysFieldPermission perm : permissions) {
            if (("*".equals(perm.getTableName()) || tableName.equalsIgnoreCase(perm.getTableName()))
                    && "HIDDEN".equalsIgnoreCase(perm.getPermissionType())) {
                hiddenFields.add(perm.getFieldName());
            }
        }
        return hiddenFields;
    }

    @Override
    public Set<String> getMaskedFields(String tableName, List<Long> roleIds) {
        List<SysFieldPermission> permissions = listByRoleIds(roleIds);
        Set<String> maskedFields = new HashSet<>();
        for (SysFieldPermission perm : permissions) {
            if (("*".equals(perm.getTableName()) || tableName.equalsIgnoreCase(perm.getTableName()))
                    && "MASKED".equalsIgnoreCase(perm.getPermissionType())) {
                maskedFields.add(perm.getFieldName());
            }
        }
        return maskedFields;
    }
}
