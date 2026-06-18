package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.entity.SysFieldPermission;
import com.openreport.admin.service.SysFieldPermissionService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Api(tags = "字段级权限配置")
@RestController
@RequestMapping("/sys/field-permission")
public class SysFieldPermissionController {

    @Autowired
    private SysFieldPermissionService sysFieldPermissionService;

    @ApiOperation("查询所有字段权限规则")
    @GetMapping("/list")
    public Result<List<SysFieldPermission>> list(@RequestParam(required = false) Long roleId) {
        if (roleId != null) {
            return Result.success(sysFieldPermissionService.listByRoleIds(List.of(roleId)));
        }
        return Result.success(sysFieldPermissionService.list());
    }

    @ApiOperation("分页查询字段权限规则")
    @GetMapping("/page")
    public Result<Page<SysFieldPermission>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long roleId) {
        Page<SysFieldPermission> page = new Page<>(pageNum, pageSize);
        return Result.success(sysFieldPermissionService.page(page));
    }

    @ApiOperation("新增字段权限规则")
    @PostMapping
    public Result<Void> add(@RequestBody SysFieldPermission rule) {
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        rule.setDeleted(0);
        if (rule.getStatus() == null) {
            rule.setStatus(1);
        }
        sysFieldPermissionService.save(rule);
        return Result.success();
    }

    @ApiOperation("更新字段权限规则")
    @PutMapping
    public Result<Void> update(@RequestBody SysFieldPermission rule) {
        rule.setUpdateTime(LocalDateTime.now());
        sysFieldPermissionService.updateById(rule);
        return Result.success();
    }

    @ApiOperation("删除字段权限规则")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysFieldPermissionService.removeById(id);
        return Result.success();
    }
}
