package com.openreport.admin.controller;

import com.openreport.admin.entity.SysMenu;
import com.openreport.admin.service.SysMenuService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Api(tags = "菜单管理")
@RestController
@RequestMapping("/sys/menu")
public class SysMenuController {

    @Autowired
    private SysMenuService sysMenuService;

    @ApiOperation("获取所有菜单")
    @GetMapping("/list")
    public Result<List<SysMenu>> list() {
        return Result.success(sysMenuService.listAll());
    }

    @ApiOperation("获取菜单详情")
    @GetMapping("/{id}")
    public Result<SysMenu> getById(@PathVariable Long id) {
        return Result.success(sysMenuService.getById(id));
    }

    @ApiOperation("根据角色ID获取菜单列表")
    @GetMapping("/role/{roleId}")
    public Result<List<SysMenu>> listByRoleId(@PathVariable Long roleId) {
        return Result.success(sysMenuService.listByRoleId(roleId));
    }

    @ApiOperation("根据用户ID获取菜单列表")
    @GetMapping("/user/{userId}")
    public Result<List<SysMenu>> listByUserId(@PathVariable Long userId) {
        return Result.success(sysMenuService.listByUserId(userId));
    }

    @ApiOperation("新增菜单")
    @PostMapping
    public Result<Void> add(@RequestBody SysMenu menu) {
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());
        menu.setDeleted(0);
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        sysMenuService.save(menu);
        return Result.success();
    }

    @ApiOperation("更新菜单")
    @PutMapping
    public Result<Void> update(@RequestBody SysMenu menu) {
        menu.setUpdateTime(LocalDateTime.now());
        sysMenuService.updateById(menu);
        return Result.success();
    }

    @ApiOperation("删除菜单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysMenuService.removeById(id);
        return Result.success();
    }
}
