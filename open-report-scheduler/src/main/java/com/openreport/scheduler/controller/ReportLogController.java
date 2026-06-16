package com.openreport.scheduler.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.common.result.Result;
import com.openreport.scheduler.entity.ReportLog;
import com.openreport.scheduler.service.ReportLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "报表执行日志")
@RestController
@RequestMapping("/report-log")
public class ReportLogController {

    @Autowired
    private ReportLogService reportLogService;

    @ApiOperation("分页查询报表执行日志")
    @GetMapping("/page")
    public Result<Page<ReportLog>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long reportId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String executeType) {
        return Result.success(reportLogService.pageList(pageNum, pageSize, reportId, status, executeType));
    }

    @ApiOperation("获取日志详情")
    @GetMapping("/{id}")
    public Result<ReportLog> getById(@PathVariable Long id) {
        return Result.success(reportLogService.getById(id));
    }
}
