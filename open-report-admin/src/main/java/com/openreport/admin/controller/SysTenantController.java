package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.config.SecurityContextHolder;
import com.openreport.admin.entity.SysTenant;
import com.openreport.admin.service.SysTenantService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Api(tags = "租户管理")
@RestController
@RequestMapping("/tenant")
public class SysTenantController {

    @Autowired
    private SysTenantService sysTenantService;

    @ApiOperation("分页查询租户列表")
    @GetMapping("/page")
    @RequirePerms("system:tenant:list")
    public Result<Page<SysTenant>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String tenantName) {
        return Result.success(sysTenantService.pageList(pageNum, pageSize, tenantName));
    }

    @ApiOperation("获取所有租户列表")
    @GetMapping("/list")
    @RequirePerms("system:tenant:list")
    public Result<List<SysTenant>> list() {
        return Result.success(sysTenantService.list());
    }

    @ApiOperation("获取租户详情")
    @GetMapping("/{id}")
    @RequirePerms("system:tenant:list")
    public Result<SysTenant> getById(@PathVariable Long id) {
        return Result.success(sysTenantService.getById(id));
    }

    @ApiOperation("新增租户")
    @PostMapping
    @RequirePerms("system:tenant:add")
    public Result<Void> add(@RequestBody SysTenant tenant) {
        if (tenant.getStatus() == null) {
            tenant.setStatus(1);
        }
        tenant.setCreateTime(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());
        sysTenantService.save(tenant);
        return Result.success();
    }

    @ApiOperation("更新租户")
    @PutMapping
    @RequirePerms("system:tenant:edit")
    public Result<Void> update(@RequestBody SysTenant tenant) {
        tenant.setUpdateTime(LocalDateTime.now());
        sysTenantService.updateById(tenant);
        return Result.success();
    }

    @ApiOperation("删除租户")
    @DeleteMapping("/{id}")
    @RequirePerms("system:tenant:remove")
    public Result<Void> delete(@PathVariable Long id) {
        sysTenantService.removeById(id);
        return Result.success();
    }

    @ApiOperation("获取当前用户的租户信息")
    @GetMapping("/current")
    public Result<SysTenant> getCurrentTenant() {
        Long tenantId = SecurityContextHolder.getTenantId();
        if (tenantId == null) {
            return Result.success(null);
        }
        return Result.success(sysTenantService.getById(tenantId));
    }
}
