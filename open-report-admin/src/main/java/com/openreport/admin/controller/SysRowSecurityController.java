package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.entity.SysRowSecurity;
import com.openreport.admin.service.SysRowSecurityService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Api(tags = "行级安全配置")
@RestController
@RequestMapping("/sys/row-security")
public class SysRowSecurityController {

    @Autowired
    private SysRowSecurityService sysRowSecurityService;

    @ApiOperation("查询所有行级安全规则")
    @GetMapping("/list")
    @RequirePerms("system:row-security:list")
    public Result<List<SysRowSecurity>> list(@RequestParam(required = false) Long roleId) {
        if (roleId != null) {
            return Result.success(sysRowSecurityService.listByRoleIds(List.of(roleId)));
        }
        return Result.success(sysRowSecurityService.list());
    }

    @ApiOperation("分页查询行级安全规则")
    @GetMapping("/page")
    @RequirePerms("system:row-security:list")
    public Result<Page<SysRowSecurity>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long roleId) {
        Page<SysRowSecurity> page = new Page<>(pageNum, pageSize);
        return Result.success(sysRowSecurityService.page(page));
    }

    @ApiOperation("新增行级安全规则")
    @PostMapping
    @RequirePerms("system:row-security:add")
    public Result<Void> add(@RequestBody SysRowSecurity rule) {
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        rule.setDeleted(0);
        if (rule.getStatus() == null) {
            rule.setStatus(1);
        }
        sysRowSecurityService.save(rule);
        return Result.success();
    }

    @ApiOperation("更新行级安全规则")
    @PutMapping
    @RequirePerms("system:row-security:edit")
    public Result<Void> update(@RequestBody SysRowSecurity rule) {
        rule.setUpdateTime(LocalDateTime.now());
        sysRowSecurityService.updateById(rule);
        return Result.success();
    }

    @ApiOperation("删除行级安全规则")
    @DeleteMapping("/{id}")
    @RequirePerms("system:row-security:remove")
    public Result<Void> delete(@PathVariable Long id) {
        sysRowSecurityService.removeById(id);
        return Result.success();
    }
}
