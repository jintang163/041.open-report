package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.config.SecurityContextHolder;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.entity.ReportApproval;
import com.openreport.admin.service.ReportApprovalService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "报表审批管理")
@RestController
@RequestMapping("/report-approval")
public class ReportApprovalController {

    @Autowired
    private ReportApprovalService approvalService;

    @ApiOperation("分页查询审批列表")
    @GetMapping("/page")
    @RequirePerms("report:approval:list")
    public Result<Page<ReportApproval>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.success(approvalService.pageByStatus(pageNum, pageSize, status));
    }

    @ApiOperation("获取模板审批历史")
    @GetMapping("/template/{templateId}")
    @RequirePerms("report:approval:list")
    public Result<List<ReportApproval>> getByTemplateId(@PathVariable Long templateId) {
        return Result.success(approvalService.listByTemplateId(templateId));
    }

    @ApiOperation("获取审批详情")
    @GetMapping("/{id}")
    @RequirePerms("report:approval:list")
    public Result<ReportApproval> getById(@PathVariable Long id) {
        return Result.success(approvalService.getById(id));
    }

    @ApiOperation("审批通过")
    @PostMapping("/{id}/approve")
    @RequirePerms("report:approval:audit")
    public Result<ReportApproval> approve(
            @PathVariable Long id,
            @RequestParam(required = false) String remark) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        ReportApproval result = approvalService.approve(id, userId, userName, remark, true);
        return Result.success(result);
    }

    @ApiOperation("审批驳回")
    @PostMapping("/{id}/reject")
    @RequirePerms("report:approval:audit")
    public Result<ReportApproval> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String remark) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        ReportApproval result = approvalService.approve(id, userId, userName, remark, false);
        return Result.success(result);
    }

    @ApiOperation("撤销审批")
    @PostMapping("/{id}/cancel")
    @RequirePerms("report:approval:edit")
    public Result<ReportApproval> cancel(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        ReportApproval result = approvalService.cancelApproval(id, userId);
        return Result.success(result);
    }
}
