package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.SysTenant;
import com.openreport.admin.mapper.SysTenantMapper;
import com.openreport.admin.service.SysTenantService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class SysTenantServiceImpl extends ServiceImpl<SysTenantMapper, SysTenant> implements SysTenantService {

    @Override
    public Page<SysTenant> pageList(Integer pageNum, Integer pageSize, String tenantName) {
        Page<SysTenant> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(tenantName)) {
            wrapper.like(SysTenant::getTenantName, tenantName);
        }
        wrapper.orderByDesc(SysTenant::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public SysTenant getByCode(String tenantCode) {
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysTenant::getTenantCode, tenantCode);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }
}
