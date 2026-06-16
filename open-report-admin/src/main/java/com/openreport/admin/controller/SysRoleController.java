package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.entity.SysRole;
import com.openreport.admin.service.SysRoleService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Api(tags = "角色管理")
@RestController
@RequestMapping("/sys/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @ApiOperation("分页查询角色列表")
    @GetMapping("/page")
    public Result<Page<SysRole>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName) {
        return Result.success(sysRoleService.pageList(pageNum, pageSize, roleName));
    }

    @ApiOperation("获取所有角色列表")
    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.success(sysRoleService.list());
    }

    @ApiOperation("获取角色详情")
    @GetMapping("/{id}")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.success(sysRoleService.getById(id));
    }

    @ApiOperation("根据用户ID获取角色列表")
    @GetMapping("/user/{userId}")
    public Result<List<SysRole>> listByUserId(@PathVariable Long userId) {
        return Result.success(sysRoleService.listByUserId(userId));
    }

    @ApiOperation("新增角色")
    @PostMapping
    public Result<Void> add(@RequestBody SysRole role) {
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        role.setDeleted(0);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        sysRoleService.save(role);
        return Result.success();
    }

    @ApiOperation("更新角色")
    @PutMapping
    public Result<Void> update(@RequestBody SysRole role) {
        role.setUpdateTime(LocalDateTime.now());
        sysRoleService.updateById(role);
        return Result.success();
    }

    @ApiOperation("删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleService.removeById(id);
        return Result.success();
    }
}
