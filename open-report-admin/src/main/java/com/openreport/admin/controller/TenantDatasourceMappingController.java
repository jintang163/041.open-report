package com.openreport.admin.controller;

import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.config.SecurityContextHolder;
import com.openreport.admin.entity.DataSourceConfig;
import com.openreport.admin.entity.TenantDatasourceMapping;
import com.openreport.admin.service.DataSourceConfigService;
import com.openreport.admin.service.TenantDatasourceMappingService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "租户数据源映射管理")
@RestController
@RequestMapping("/tenant-datasource")
public class TenantDatasourceMappingController {

    @Autowired
    private TenantDatasourceMappingService tenantDatasourceMappingService;

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @ApiOperation("获取租户的所有数据源映射")
    @GetMapping("/tenant/{tenantId}")
    @RequirePerms("system:tenant:list")
    public Result<List<Map<String, Object>>> listByTenant(@PathVariable Long tenantId) {
        List<TenantDatasourceMapping> mappings = tenantDatasourceMappingService.listByTenantId(tenantId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TenantDatasourceMapping mapping : mappings) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", mapping.getId());
            item.put("tenantId", mapping.getTenantId());
            item.put("originalDsId", mapping.getOriginalDsId());
            item.put("targetDsId", mapping.getTargetDsId());
            item.put("status", mapping.getStatus());
            item.put("createTime", mapping.getCreateTime());
            item.put("updateTime", mapping.getUpdateTime());

            DataSourceConfig originalDs = dataSourceConfigService.getById(mapping.getOriginalDsId());
            if (originalDs != null) {
                item.put("originalDsName", originalDs.getDsName());
                item.put("originalDsCode", originalDs.getDsCode());
            }
            DataSourceConfig targetDs = dataSourceConfigService.getById(mapping.getTargetDsId());
            if (targetDs != null) {
                item.put("targetDsName", targetDs.getDsName());
                item.put("targetDsCode", targetDs.getDsCode());
            }
            result.add(item);
        }
        return Result.success(result);
    }

    @ApiOperation("获取当前租户的所有数据源映射")
    @GetMapping("/current")
    public Result<List<Map<String, Object>>> listByCurrentTenant() {
        Long tenantId = SecurityContextHolder.getTenantId();
        if (tenantId == null) {
            return Result.success(new ArrayList<>());
        }
        return listByTenant(tenantId);
    }

    @ApiOperation("解析指定数据源在当前租户下的实际数据源")
    @GetMapping("/resolve/{originalDsId}")
    public Result<Map<String, Object>> resolveDatasource(@PathVariable Long originalDsId) {
        DataSourceConfig dsConfig = tenantDatasourceMappingService.resolveDatasourceForCurrentUser(originalDsId);
        Map<String, Object> result = new HashMap<>();
        if (dsConfig != null) {
            result.put("id", dsConfig.getId());
            result.put("dsName", dsConfig.getDsName());
            result.put("dsCode", dsConfig.getDsCode());
            result.put("dsType", dsConfig.getDsType());
            result.put("status", dsConfig.getStatus());
        }
        return Result.success(result);
    }

    @ApiOperation("新增或更新租户数据源映射")
    @PostMapping
    @RequirePerms("system:tenant:edit")
    public Result<Void> saveMapping(@RequestBody Map<String, Long> body) {
        Long tenantId = body.get("tenantId");
        Long originalDsId = body.get("originalDsId");
        Long targetDsId = body.get("targetDsId");
        if (tenantId == null || originalDsId == null || targetDsId == null) {
            return Result.error("参数不完整");
        }
        Long userId = SecurityContextHolder.getUserId();
        tenantDatasourceMappingService.saveMapping(tenantId, originalDsId, targetDsId, userId);
        return Result.success();
    }

    @ApiOperation("更新映射的目标数据源")
    @PutMapping("/{id}")
    @RequirePerms("system:tenant:edit")
    public Result<Void> updateMapping(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long targetDsId = body.get("targetDsId");
        if (targetDsId == null) {
            return Result.error("目标数据源ID不能为空");
        }
        Long userId = SecurityContextHolder.getUserId();
        tenantDatasourceMappingService.updateMapping(id, targetDsId, userId);
        return Result.success();
    }

    @ApiOperation("删除映射")
    @DeleteMapping("/{id}")
    @RequirePerms("system:tenant:edit")
    public Result<Void> deleteMapping(@PathVariable Long id) {
        tenantDatasourceMappingService.deleteMapping(id);
        return Result.success();
    }

    @ApiOperation("批量配置租户数据源映射")
    @PostMapping("/batch")
    @RequirePerms("system:tenant:edit")
    public Result<Void> batchSaveMappings(@RequestBody Map<String, Object> body) {
        Long tenantId = Long.valueOf(body.get("tenantId").toString());
        Long userId = SecurityContextHolder.getUserId();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mappings = (List<Map<String, Object>>) body.get("mappings");
        if (mappings != null) {
            for (Map<String, Object> mapping : mappings) {
                Long originalDsId = Long.valueOf(mapping.get("originalDsId").toString());
                Long targetDsId = Long.valueOf(mapping.get("targetDsId").toString());
                tenantDatasourceMappingService.saveMapping(tenantId, originalDsId, targetDsId, userId);
            }
        }
        return Result.success();
    }

    @ApiOperation("测试租户数据源连接")
    @PostMapping("/test/{tenantId}/{originalDsId}")
    @RequirePerms("system:tenant:list")
    public Result<Map<String, Object>> testTenantDatasource(
            @PathVariable Long tenantId, @PathVariable Long originalDsId) {
        DataSourceConfig dsConfig = tenantDatasourceMappingService.resolveDatasource(tenantId, originalDsId);
        Map<String, Object> result = new HashMap<>();
        if (dsConfig == null) {
            result.put("success", false);
            result.put("message", "数据源配置不存在");
            return Result.success(result);
        }

        boolean connected = dataSourceConfigService.testConnection(dsConfig);
        result.put("success", connected);
        result.put("message", connected ? "连接成功" : "连接失败");
        result.put("dsName", dsConfig.getDsName());
        result.put("dsId", dsConfig.getId());
        return Result.success(result);
    }
}
