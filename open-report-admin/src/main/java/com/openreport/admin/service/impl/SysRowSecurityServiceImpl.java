package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.SysRowSecurity;
import com.openreport.admin.mapper.SysRowSecurityMapper;
import com.openreport.admin.service.SysRowSecurityService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysRowSecurityServiceImpl extends ServiceImpl<SysRowSecurityMapper, SysRowSecurity> implements SysRowSecurityService {

    @Override
    public List<SysRowSecurity> listByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<SysRowSecurity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysRowSecurity::getRoleId, roleIds);
        wrapper.eq(SysRowSecurity::getStatus, 1);
        return list(wrapper);
    }

    @Override
    public String buildRowFilterExpression(String tableName, List<Long> roleIds, Long deptId, Long userId) {
        List<SysRowSecurity> rules = listByRoleIds(roleIds);
        if (rules.isEmpty()) {
            return null;
        }

        List<SysRowSecurity> matchedRules = rules.stream()
                .filter(r -> "*".equals(r.getTableName()) || tableName.equalsIgnoreCase(r.getTableName()))
                .collect(Collectors.toList());

        if (matchedRules.isEmpty()) {
            return null;
        }

        String rawExpression = matchedRules.stream()
                .map(SysRowSecurity::getFilterExpression)
                .filter(expr -> expr != null && !expr.trim().isEmpty())
                .collect(Collectors.joining(" OR "));

        if (rawExpression.trim().isEmpty()) {
            return null;
        }

        String expression = rawExpression;
        if (deptId != null) {
            expression = expression.replace("{deptId}", String.valueOf(deptId));
            expression = expression.replace("{dept_id}", String.valueOf(deptId));
        }
        if (userId != null) {
            expression = expression.replace("{userId}", String.valueOf(userId));
            expression = expression.replace("{user_id}", String.valueOf(userId));
        }

        return expression;
    }
}
