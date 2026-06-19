package com.openreport.admin.controller;

import com.openreport.admin.config.SecurityContextHolder;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.entity.ReportComment;
import com.openreport.admin.service.ReportCommentService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "报表评论管理")
@RestController
@RequestMapping("/report-comment")
public class ReportCommentController {

    @Autowired
    private ReportCommentService commentService;

    @ApiOperation("获取报表评论列表")
    @GetMapping("/template/{templateId}")
    @RequirePerms("report:comment:list")
    public Result<List<ReportComment>> listByTemplateId(
            @PathVariable Long templateId,
            @RequestParam(required = false) Integer snapshotVersion) {
        Long userId = SecurityContextHolder.getUserId();
        if (snapshotVersion != null) {
            return Result.success(commentService.listByTemplateIdAndVersion(templateId, snapshotVersion, userId));
        }
        return Result.success(commentService.listByTemplateId(templateId, userId));
    }

    @ApiOperation("获取单元格评论")
    @GetMapping("/template/{templateId}/cell/{cellRef}")
    @RequirePerms("report:comment:list")
    public Result<List<ReportComment>> listByCellRef(
            @PathVariable Long templateId,
            @PathVariable String cellRef) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(commentService.listByCellRef(templateId, cellRef, userId));
    }

    @ApiOperation("获取图表评论")
    @GetMapping("/template/{templateId}/chart/{chartId}")
    @RequirePerms("report:comment:list")
    public Result<List<ReportComment>> listByChartId(
            @PathVariable Long templateId,
            @PathVariable String chartId) {
        Long userId = SecurityContextHolder.getUserId();
        return Result.success(commentService.listByChartId(templateId, chartId, userId));
    }

    @ApiOperation("获取有评论的单元格引用列表")
    @GetMapping("/template/{templateId}/cell-refs")
    @RequirePerms("report:comment:list")
    public Result<List<String>> getCellRefsWithComments(@PathVariable Long templateId) {
        return Result.success(commentService.getCellRefsWithComments(templateId));
    }

    @ApiOperation("获取有评论的图表ID列表")
    @GetMapping("/template/{templateId}/chart-ids")
    @RequirePerms("report:comment:list")
    public Result<List<String>> getChartIdsWithComments(@PathVariable Long templateId) {
        return Result.success(commentService.getChartIdsWithComments(templateId));
    }

    @ApiOperation("获取评论数量")
    @GetMapping("/template/{templateId}/count")
    @RequirePerms("report:comment:list")
    public Result<Integer> countByTemplateId(@PathVariable Long templateId) {
        return Result.success(commentService.countByTemplateId(templateId));
    }

    @ApiOperation("添加评论")
    @PostMapping
    @RequirePerms("report:comment:add")
    public Result<ReportComment> addComment(@RequestBody ReportComment comment) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        return Result.success(commentService.addComment(comment, userId, userName));
    }

    @ApiOperation("回复评论")
    @PostMapping("/{parentId}/reply")
    @RequirePerms("report:comment:add")
    public Result<ReportComment> addReply(
            @PathVariable Long parentId,
            @RequestBody ReportComment reply) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        return Result.success(commentService.addReply(parentId, reply, userId, userName));
    }

    @ApiOperation("删除评论")
    @DeleteMapping("/{commentId}")
    @RequirePerms("report:comment:delete")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = SecurityContextHolder.getUserId();
        commentService.deleteComment(commentId, userId);
        return Result.success(null);
    }

    @ApiOperation("点赞/取消点赞")
    @PostMapping("/{commentId}/like")
    @RequirePerms("report:comment:like")
    public Result<Boolean> toggleLike(@PathVariable Long commentId) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        boolean liked = commentService.toggleLike(commentId, userId, userName);
        return Result.success(liked);
    }
}
