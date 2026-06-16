package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);

    Page<SysUser> pageList(Integer pageNum, Integer pageSize, String username);

    boolean resetPassword(Long userId, String newPassword);
}
