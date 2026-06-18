package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.config.SecurityContextHolder;
import com.openreport.admin.entity.ReportComponent;
import com.openreport.admin.entity.ReportComponentInstall;
import com.openreport.admin.service.ReportComponentService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "组件商店")
@RestController
@RequestMapping("/component-store")
public class ComponentStoreController {

    @Autowired
    private ReportComponentService componentService;

    @ApiOperation("分页查询组件列表")
    @GetMapping("/page")
    @RequirePerms("report:designer:list")
    public Result<Page<ReportComponent>> pageComponents(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer componentType,
            @RequestParam(required = false) Integer source,
            @RequestParam(defaultValue = "newest") String sortBy) {
        return Result.success(componentService.pageComponents(pageNum, pageSize, keyword,
                category, componentType, source, sortBy));
    }

    @ApiOperation("获取组件详情")
    @GetMapping("/{id}")
    @RequirePerms("report:designer:list")
    public Result<ReportComponent> getDetail(@PathVariable Long id) {
        return Result.success(componentService.getDetail(id));
    }

    @ApiOperation("获取组件分类列表")
    @GetMapping("/categories")
    @RequirePerms("report:designer:list")
    public Result<List<String>> listCategories() {
        return Result.success(componentService.listCategories());
    }

    @ApiOperation("查询我已安装的组件")
    @GetMapping("/my-installs")
    @RequirePerms("report:designer:list")
    public Result<Map<String, Object>> listMyInstalls() {
        Long userId = SecurityContextHolder.getUserId();
        List<ReportComponentInstall> installs = componentService.listMyInstalls(userId);
        List<Long> componentIds = installs.stream()
                .map(ReportComponentInstall::getComponentId)
                .collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("installs", installs);
        result.put("componentIds", componentIds);
        return Result.success(result);
    }

    @ApiOperation("检查组件是否已安装")
    @GetMapping("/installed/{id}")
    @RequirePerms("report:designer:list")
    public Result<Boolean> isInstalled(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(componentService.isInstalled(id, userId));
    }

    @ApiOperation("安装组件")
    @PostMapping("/install/{id}")
    @RequirePerms("report:designer:add")
    public Result<ReportComponentInstall> install(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        ReportComponentInstall result = componentService.installComponent(id, userId, userName);
        return Result.success(result);
    }

    @ApiOperation("卸载组件")
    @PostMapping("/uninstall/{id}")
    @RequirePerms("report:designer:edit")
    public Result<Void> uninstall(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        componentService.uninstallComponent(id, userId);
        return Result.success();
    }

    @ApiOperation("发布组件（管理员/社区）")
    @PostMapping("/publish")
    @RequirePerms("report:designer:edit")
    public Result<ReportComponent> publish(@RequestBody ReportComponent component,
                                            @RequestParam(defaultValue = "2") Integer source) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        ReportComponent result = componentService.publishComponent(component, userId, userName, source);
        return Result.success(result);
    }

    @ApiOperation("下载组件（获取组件JSON）")
    @GetMapping("/download/{id}")
    @RequirePerms("report:designer:list")
    public Result<Map<String, Object>> download(@PathVariable Long id) {
        ReportComponent component = componentService.getDetail(id);
        if (component == null) {
            return Result.failure(com.openreport.common.result.ResultCode.DATA_NOT_FOUND);
        }
        componentService.incrementDownloadCount(id);
        Map<String, Object> result = new HashMap<>();
        result.put("componentId", component.getId());
        result.put("componentName", component.getComponentName());
        result.put("componentCode", component.getComponentCode());
        result.put("componentJson", component.getComponentJson());
        result.put("version", component.getVersion());
        return Result.success(result);
    }
}
