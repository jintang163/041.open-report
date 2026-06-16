package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.SysMenu;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {

    List<SysMenu> listAll();

    List<SysMenu> listByRoleId(Long roleId);

    List<SysMenu> listByUserId(Long userId);
}
