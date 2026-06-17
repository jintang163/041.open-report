package com.openreport.admin.controller;

import com.openreport.admin.dto.AiGenerateRequest;
import com.openreport.admin.dto.AiGenerateResult;
import com.openreport.admin.service.AiService;
import com.openreport.admin.service.ReportGeneratorService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "AI智能报表生成")
@RestController
@RequestMapping("/ai-report")
public class AiReportController {

    @Autowired
    private AiService aiService;

    @Autowired
    private ReportGeneratorService reportGeneratorService;

    @ApiOperation("检查AI服务状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("enabled", aiService.isEnabled());
        result.put("mode", aiService.isEnabled() ? "real" : "demo");
        return Result.success(result);
    }

    @ApiOperation("AI生成SQL和图表建议")
    @PostMapping("/generate")
    public Result<AiGenerateResult> generate(@RequestBody AiGenerateRequest request) {
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            return Result.error("请输入报表描述");
        }
        AiGenerateResult result = aiService.generateReport(request);
        return Result.success(result);
    }

    @ApiOperation("AI仅生成SQL")
    @PostMapping("/generate-sql")
    public Result<AiGenerateResult> generateSql(@RequestBody AiGenerateRequest request) {
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            return Result.error("请输入查询描述");
        }
        AiGenerateResult result = aiService.generateSqlOnly(request);
        return Result.success(result);
    }

    @ApiOperation("一键生成报表（从自然语言到报表模板）")
    @PostMapping("/create-report")
    public Result<ReportGeneratorService.GeneratedReportResult> createReport(
            @RequestBody AiGenerateRequest request,
            @RequestAttribute("userId") Long userId) {
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            return Result.error("请输入报表描述");
        }
        if (request.getDsId() == null) {
            return Result.error("请选择数据源");
        }
        ReportGeneratorService.GeneratedReportResult result = reportGeneratorService.generateReportFromPrompt(
                request.getPrompt(), request.getDsId(), userId);
        return Result.success(result);
    }

    @ApiOperation("根据AI结果生成报表")
    @PostMapping("/create-from-result")
    public Result<ReportGeneratorService.GeneratedReportResult> createFromResult(
            @RequestBody Map<String, Object> body,
            @RequestAttribute("userId") Long userId) {

        AiGenerateResult aiResult = com.alibaba.fastjson.JSON.parseObject(
                com.alibaba.fastjson.JSON.toJSONString(body.get("aiResult")),
                AiGenerateResult.class);
        Long dsId = body.get("dsId") != null ? Long.valueOf(body.get("dsId").toString()) : null;

        if (aiResult == null || aiResult.getSql() == null) {
            return Result.error("AI生成结果不能为空");
        }
        if (dsId == null) {
            return Result.error("请选择数据源");
        }

        ReportGeneratorService.GeneratedReportResult result = reportGeneratorService.generateReport(
                aiResult, dsId, userId);
        return Result.success(result);
    }
}
