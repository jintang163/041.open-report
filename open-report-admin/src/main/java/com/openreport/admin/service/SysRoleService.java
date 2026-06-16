package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.SysRole;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {

    Page<SysRole> pageList(Integer pageNum, Integer pageSize, String roleName);

    List<SysRole> listByUserId(Long userId);
}
