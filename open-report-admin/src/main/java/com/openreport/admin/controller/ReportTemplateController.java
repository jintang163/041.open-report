package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.service.ReportTemplateService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Api(tags = "报表模板管理")
@RestController
@RequestMapping("/report-template")
public class ReportTemplateController {

    @Autowired
    private ReportTemplateService reportTemplateService;

    @ApiOperation("分页查询报表模板列表")
    @GetMapping("/page")
    public Result<Page<ReportTemplate>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) Integer templateType) {
        return Result.success(reportTemplateService.pageList(pageNum, pageSize, templateName, templateType));
    }

    @ApiOperation("获取所有报表模板列表")
    @GetMapping("/list")
    public Result<List<ReportTemplate>> list() {
        return Result.success(reportTemplateService.listAll());
    }

    @ApiOperation("获取报表模板详情")
    @GetMapping("/{id}")
    public Result<ReportTemplate> getById(@PathVariable Long id) {
        return Result.success(reportTemplateService.getById(id));
    }

    @ApiOperation("新增报表模板")
    @PostMapping
    @RequirePerms("report:designer:add")
    public Result<Void> add(@RequestBody ReportTemplate template, @RequestAttribute("userId") Long userId) {
        template.setCreateBy(userId);
        template.setUpdateBy(userId);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setDeleted(0);
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        reportTemplateService.save(template);
        return Result.success();
    }

    @ApiOperation("更新报表模板")
    @PutMapping
    @RequirePerms("report:designer:edit")
    public Result<Void> update(@RequestBody ReportTemplate template, @RequestAttribute("userId") Long userId) {
        template.setUpdateBy(userId);
        template.setUpdateTime(LocalDateTime.now());
        reportTemplateService.updateById(template);
        return Result.success();
    }

    @ApiOperation("删除报表模板")
    @DeleteMapping("/{id}")
    @RequirePerms("report:designer:remove")
    public Result<Void> delete(@PathVariable Long id) {
        reportTemplateService.removeById(id);
        return Result.success();
    }
}
