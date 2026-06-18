package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.config.SecurityContextHolder;
import com.openreport.admin.entity.SysApiKey;
import com.openreport.admin.service.SysApiKeyService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "API Key管理")
@RestController
@RequestMapping("/api-key")
public class SysApiKeyController {

    @Autowired
    private SysApiKeyService apiKeyService;

    @ApiOperation("分页查询我的API Key")
    @GetMapping("/page")
    @RequirePerms("api:key:list")
    public Result<Page<SysApiKey>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(apiKeyService.pageMyKeys(userId, pageNum, pageSize, keyword));
    }

    @ApiOperation("获取API Key详情")
    @GetMapping("/{id}")
    @RequirePerms("api:key:list")
    public Result<SysApiKey> getById(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        SysApiKey key = apiKeyService.getById(id);
        if (key != null && key.getUserId().equals(userId)) {
            key.setApiSecret("********");
        }
        return Result.success(key);
    }

    @ApiOperation("创建API Key")
    @PostMapping
    @RequirePerms("api:key:add")
    public Result<SysApiKey> create(@RequestBody Map<String, Object> params) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        String appName = params.get("appName") != null ? params.get("appName").toString() : "默认应用";
        String description = params.get("description") != null ? params.get("description").toString() : null;
        Integer rateLimit = params.get("rateLimit") != null
                ? Integer.valueOf(params.get("rateLimit").toString()) : null;
        String ipWhitelist = params.get("ipWhitelist") != null ? params.get("ipWhitelist").toString() : null;
        Integer expireDays = params.get("expireDays") != null
                ? Integer.valueOf(params.get("expireDays").toString()) : null;
        SysApiKey result = apiKeyService.createKey(userId, userName, appName, description,
                rateLimit, ipWhitelist, expireDays);
        return Result.success(result);
    }

    @ApiOperation("作废API Key")
    @PostMapping("/{id}/revoke")
    @RequirePerms("api:key:edit")
    public Result<Void> revoke(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        boolean result = apiKeyService.revokeKey(id, userId);
        return result ? Result.success() : Result.failure(com.openreport.common.result.ResultCode.DATA_NOT_FOUND);
    }

    @ApiOperation("重置API Secret")
    @PostMapping("/{id}/rotate")
    @RequirePerms("api:key:edit")
    public Result<SysApiKey> rotate(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        boolean result = apiKeyService.rotateKey(id, userId);
        if (result) {
            return Result.success(apiKeyService.getById(id));
        }
        return Result.failure(com.openreport.common.result.ResultCode.DATA_NOT_FOUND);
    }

    @ApiOperation("校验API Key有效性")
    @PostMapping("/validate")
    public Result<Map<String, Object>> validate(
            @ApiParam("API Key") @RequestParam String apiKey,
            @ApiParam("客户端IP") @RequestParam(required = false) String clientIp) {
        try {
            SysApiKey key = apiKeyService.validateKey(apiKey, clientIp);
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("valid", true);
            result.put("appName", key.getAppName());
            result.put("rateLimit", key.getRateLimit());
            result.put("expireTime", key.getExpireTime());
            return Result.success(result);
        } catch (RuntimeException e) {
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("valid", false);
            result.put("message", e.getMessage());
            return Result.success(result);
        }
    }
}
