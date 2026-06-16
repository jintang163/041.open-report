package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.entity.SysUser;
import com.openreport.admin.service.SysUserService;
import com.openreport.common.result.Result;
import cn.hutool.crypto.digest.BCrypt;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Api(tags = "用户管理")
@RestController
@RequestMapping("/sys/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @ApiOperation("分页查询用户列表")
    @GetMapping("/page")
    public Result<Page<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username) {
        return Result.success(sysUserService.pageList(pageNum, pageSize, username));
    }

    @ApiOperation("获取用户详情")
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @ApiOperation("新增用户")
    @PostMapping
    public Result<Void> add(@RequestBody SysUser user) {
        user.setPassword(PasswordUtils.encrypt(user.getPassword() != null ? user.getPassword() : "123456"));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleted(0);
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        sysUserService.save(user);
        return Result.success();
    }

    @ApiOperation("更新用户")
    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        user.setPassword(null);
        user.setUpdateTime(LocalDateTime.now());
        sysUserService.updateById(user);
        return Result.success();
    }

    @ApiOperation("删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.removeById(id);
        return Result.success();
    }

    @ApiOperation("重置密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestParam Long userId, @RequestParam String newPassword) {
        sysUserService.resetPassword(userId, newPassword);
        return Result.success();
    }
}
