package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.config.SecurityContextHolder;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.entity.ReportTemplateMarket;
import com.openreport.admin.service.ReportTemplateMarketService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "模板市场")
@RestController
@RequestMapping("/template-market")
public class TemplateMarketController {

    @Autowired
    private ReportTemplateMarketService marketService;

    @ApiOperation("分页查询公开模板列表")
    @GetMapping("/page")
    @RequirePerms("report:designer:list")
    public Result<Page<ReportTemplateMarket>> pagePublic(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer templateType,
            @RequestParam(defaultValue = "newest") String sortBy) {
        return Result.success(marketService.pagePublic(pageNum, pageSize, keyword,
                category, templateType, sortBy));
    }

    @ApiOperation("分页查询我上传的模板")
    @GetMapping("/my-uploads")
    @RequirePerms("report:designer:list")
    public Result<Page<ReportTemplateMarket>> pageMyUploads(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer visibility) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(marketService.pageMyUploads(userId, pageNum, pageSize, keyword, visibility));
    }

    @ApiOperation("获取模板市场详情")
    @GetMapping("/{id}")
    @RequirePerms("report:designer:list")
    public Result<ReportTemplateMarket> getDetail(@PathVariable Long id) {
        return Result.success(marketService.getDetail(id));
    }

    @ApiOperation("发布模板到市场")
    @PostMapping("/publish/{templateId}")
    @RequirePerms("report:designer:edit")
    public Result<ReportTemplateMarket> publish(
            @PathVariable Long templateId,
            @RequestBody Map<String, Object> params) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        Integer visibility = params.get("visibility") != null
                ? Integer.valueOf(params.get("visibility").toString())
                : 0;
        String category = params.get("category") != null ? params.get("category").toString() : null;
        String tags = params.get("tags") != null ? params.get("tags").toString() : null;
        String description = params.get("description") != null ? params.get("description").toString() : null;
        String coverImage = params.get("coverImage") != null ? params.get("coverImage").toString() : null;
        ReportTemplateMarket result = marketService.publishTemplate(
                templateId, userId, userName, visibility, category, tags, description, coverImage);
        return Result.success(result);
    }

    @ApiOperation("一键安装模板")
    @PostMapping("/install/{marketId}")
    @RequirePerms("report:designer:add")
    public Result<ReportTemplate> install(@PathVariable Long marketId) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        ReportTemplate result = marketService.installTemplate(marketId, userId, userName);
        return Result.success(result);
    }

    @ApiOperation("下架我的模板")
    @PostMapping("/take-down/{id}")
    @RequirePerms("report:designer:edit")
    public Result<Void> takeDown(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        marketService.takeDown(id, userId);
        return Result.success();
    }

    @ApiOperation("点赞模板")
    @PostMapping("/like/{id}")
    @RequirePerms("report:designer:list")
    public Result<Void> like(@PathVariable Long id) {
        marketService.incrementLikeCount(id);
        return Result.success();
    }
}
