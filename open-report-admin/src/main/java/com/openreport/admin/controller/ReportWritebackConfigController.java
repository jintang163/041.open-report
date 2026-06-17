package com.openreport.admin.controller;

import com.openreport.admin.dto.writeback.WritebackConfigDTO;
import com.openreport.admin.entity.ReportWritebackConfig;
import com.openreport.admin.service.ReportWritebackConfigService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "报表回写配置管理")
@RestController
@RequestMapping("/report-writeback/config")
public class ReportWritebackConfigController {

    @Autowired
    private ReportWritebackConfigService configService;

    @ApiOperation("获取报表的回写配置列表")
    @GetMapping("/list/{reportId}")
    public Result<List<ReportWritebackConfig>> getByReportId(@PathVariable Long reportId) {
        return Result.success(configService.getByReportId(reportId));
    }

    @ApiOperation("获取回写配置详情")
    @GetMapping("/{id}")
    public Result<ReportWritebackConfig> getDetailById(@PathVariable Long id) {
        return Result.success(configService.getDetailById(id));
    }

    @ApiOperation("新增回写配置")
    @PostMapping
    public Result<Void> saveConfig(@RequestBody WritebackConfigDTO dto, @RequestAttribute("userId") Long userId) {
        dto.setId(null);
        configService.saveConfig(dto);
        return Result.success();
    }

    @ApiOperation("更新回写配置")
    @PutMapping
    public Result<Void> updateConfig(@RequestBody WritebackConfigDTO dto, @RequestAttribute("userId") Long userId) {
        configService.updateConfig(dto);
        return Result.success();
    }

    @ApiOperation("删除回写配置")
    @DeleteMapping("/{id}")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        configService.deleteConfig(id);
        return Result.success();
    }
}
