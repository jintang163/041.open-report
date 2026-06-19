package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.SysTenant;

public interface SysTenantService extends IService<SysTenant> {

    Page<SysTenant> pageList(Integer pageNum, Integer pageSize, String tenantName);

    SysTenant getByCode(String tenantCode);
}
