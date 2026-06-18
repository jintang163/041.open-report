package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.config.SecurityContextHolder;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.dto.TemplateEditLockInfo;
import com.openreport.admin.dto.TemplateVersionDiffDTO;
import com.openreport.admin.entity.ReportApproval;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.entity.ReportTemplateSnapshot;
import com.openreport.admin.enums.ApprovalTypeEnum;
import com.openreport.admin.service.ReportApprovalService;
import com.openreport.admin.service.ReportTemplateService;
import com.openreport.admin.service.ReportTemplateSnapshotService;
import com.openreport.common.result.Result;
import com.openreport.common.result.ResultCode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "报表模板管理")
@RestController
@RequestMapping("/report-template")
public class ReportTemplateController {

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private ReportTemplateSnapshotService snapshotService;

    @Autowired
    private ReportApprovalService approvalService;

    @ApiOperation("分页查询报表模板列表")
    @GetMapping("/page")
    @RequirePerms("report:designer:list")
    public Result<Page<ReportTemplate>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) Integer templateType) {
        return Result.success(reportTemplateService.pageList(pageNum, pageSize, templateName, templateType));
    }

    @ApiOperation("分页查询报表模板列表V2")
    @GetMapping("/page-v2")
    @RequirePerms("report:designer:list")
    public Result<Page<ReportTemplate>> pageV2(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(reportTemplateService.pageListV2(pageNum, pageSize, keyword, status));
    }

    @ApiOperation("获取所有报表模板列表")
    @GetMapping("/list")
    @RequirePerms("report:designer:list")
    public Result<List<ReportTemplate>> list() {
        return Result.success(reportTemplateService.listAll());
    }

    @ApiOperation("获取报表模板详情")
    @GetMapping("/{id}")
    @RequirePerms("report:designer:list")
    public Result<ReportTemplate> getById(@PathVariable Long id) {
        return Result.success(reportTemplateService.getById(id));
    }

    @ApiOperation("新增报表模板")
    @PostMapping
    @RequirePerms("report:designer:add")
    public Result<Void> add(@RequestBody ReportTemplate template) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        reportTemplateService.saveDraft(template, userId, userName);
        return Result.success();
    }

    @ApiOperation("保存草稿")
    @PostMapping("/save-draft")
    @RequirePerms("report:designer:edit")
    public Result<ReportTemplate> saveDraft(
            @RequestBody ReportTemplate template,
            @RequestHeader(value = "X-Lock-Token", required = false) String lockToken) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        try {
            ReportTemplate result = reportTemplateService.saveDraftWithLock(template, userId, userName, lockToken);
            return Result.success(result);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains(":")) {
                String[] parts = msg.split(":", 2);
                try {
                    int code = Integer.parseInt(parts[0]);
                    for (ResultCode rc : ResultCode.values()) {
                        if (rc.getCode().equals(code)) {
                            return Result.failure(rc, parts[1]);
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return Result.failure(ResultCode.TEMPLATE_LOCK_NOT_OWNER, msg);
        }
    }

    @ApiOperation("更新报表模板")
    @PutMapping
    @RequirePerms("report:designer:edit")
    public Result<Void> update(
            @RequestBody ReportTemplate template,
            @RequestHeader(value = "X-Lock-Token", required = false) String lockToken) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        try {
            reportTemplateService.saveDraftWithLock(template, userId, userName, lockToken);
            return Result.success();
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains(":")) {
                String[] parts = msg.split(":", 2);
                try {
                    int code = Integer.parseInt(parts[0]);
                    for (ResultCode rc : ResultCode.values()) {
                        if (rc.getCode().equals(code)) {
                            return Result.failure(rc, parts[1]);
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return Result.failure(ResultCode.TEMPLATE_LOCK_NOT_OWNER, msg);
        }
    }

    @ApiOperation("删除报表模板")
    @DeleteMapping("/{id}")
    @RequirePerms("report:designer:remove")
    public Result<Void> delete(@PathVariable Long id) {
        reportTemplateService.removeById(id);
        return Result.success();
    }

    @ApiOperation("复制模板")
    @PostMapping("/copy/{id}")
    @RequirePerms("report:designer:add")
    public Result<ReportTemplate> copyTemplate(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        ReportTemplate result = reportTemplateService.copyTemplate(id, userId, userName);
        return Result.success(result);
    }

    @ApiOperation("提交审批")
    @PostMapping("/submit-approval/{id}")
    @RequirePerms("report:designer:edit")
    public Result<ReportApproval> submitApproval(
            @PathVariable Long id,
            @RequestParam(required = false) String remark) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        ReportApproval result = approvalService.submitApproval(
                id, ApprovalTypeEnum.PUBLISH.getCode(), userId, userName, remark);
        return Result.success(result);
    }

    @ApiOperation("获取模板版本列表")
    @GetMapping("/{id}/versions")
    @RequirePerms("report:designer:list")
    public Result<List<ReportTemplateSnapshot>> getVersionList(@PathVariable Long id) {
        return Result.success(snapshotService.listByTemplateId(id));
    }

    @ApiOperation("获取指定版本详情")
    @GetMapping("/{id}/versions/{version}")
    @RequirePerms("report:designer:list")
    public Result<ReportTemplateSnapshot> getVersionDetail(
            @PathVariable Long id,
            @PathVariable Integer version) {
        return Result.success(snapshotService.getByVersion(id, version));
    }

    @ApiOperation("版本对比")
    @GetMapping("/{id}/versions/compare")
    @RequirePerms("report:designer:list")
    public Result<TemplateVersionDiffDTO> compareVersions(
            @PathVariable Long id,
            @RequestParam Integer baseVersion,
            @RequestParam Integer targetVersion) {
        return Result.success(snapshotService.compareVersions(id, baseVersion, targetVersion));
    }

    @ApiOperation("回滚到指定版本")
    @PostMapping("/{id}/versions/rollback/{version}")
    @RequirePerms("report:designer:edit")
    public Result<ReportTemplateSnapshot> rollbackToVersion(
            @PathVariable Long id,
            @PathVariable Integer version) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        ReportTemplateSnapshot result = snapshotService.rollbackToVersion(id, version, userId, userName);
        return Result.success(result);
    }

    @ApiOperation("获取最新发布版本")
    @GetMapping("/{id}/versions/latest-published")
    @RequirePerms("report:designer:list")
    public Result<ReportTemplateSnapshot> getLatestPublishedVersion(@PathVariable Long id) {
        return Result.success(snapshotService.getLatestPublishedVersion(id));
    }

    @ApiOperation("发布预览")
    @GetMapping("/{id}/preview-publish")
    @RequirePerms("report:designer:list")
    public Result<ReportTemplateSnapshot> previewPublish(@PathVariable Long id) {
        return Result.success(snapshotService.previewPublish(id));
    }

    @ApiOperation("进入编辑模式（获取编辑锁）")
    @PostMapping("/{id}/enter-edit")
    @RequirePerms("report:designer:edit")
    public Result<Map<String, Object>> enterEdit(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
        TemplateEditLockInfo lockInfo = reportTemplateService.enterEdit(id, userId, userName);
        Map<String, Object> result = new HashMap<>();
        if (lockInfo != null && lockInfo.getUserId().equals(userId)) {
            result.put("locked", true);
            result.put("isOwner", true);
            result.put("lockInfo", lockInfo);
            return Result.success(result);
        } else {
            TemplateEditLockInfo currentLock = reportTemplateService.getLockStatus(id);
            result.put("locked", currentLock != null);
            result.put("isOwner", false);
            if (currentLock != null) {
                result.put("lockInfo", currentLock);
                result.put("message", currentLock.getUserName() + "正在编辑该模板");
            }
            return Result.failure(ResultCode.TEMPLATE_LOCKED, result);
        }
    }

    @ApiOperation("退出编辑模式（释放编辑锁）")
    @PostMapping("/{id}/leave-edit")
    @RequirePerms("report:designer:edit")
    public Result<Void> leaveEdit(
            @PathVariable Long id,
            @RequestHeader(value = "X-Lock-Token", required = false) String lockToken) {
        Long userId = SecurityContextHolder.getUserId();
        boolean released = reportTemplateService.leaveEdit(id, userId, lockToken);
        if (released) {
            return Result.success();
        } else {
            return Result.failure(ResultCode.TEMPLATE_LOCK_NOT_OWNER);
        }
    }

    @ApiOperation("编辑心跳续期")
    @PostMapping("/{id}/heartbeat")
    @RequirePerms("report:designer:edit")
    public Result<Map<String, Object>> heartbeat(
            @PathVariable Long id,
            @RequestHeader(value = "X-Lock-Token", required = false) String lockToken) {
        Long userId = SecurityContextHolder.getUserId();
        Map<String, Object> result = new HashMap<>();
        boolean renewed = reportTemplateService.heartbeat(id, userId, lockToken);
        result.put("renewed", renewed);
        if (renewed) {
            TemplateEditLockInfo lockInfo = reportTemplateService.getLockStatus(id);
            result.put("lockInfo", lockInfo);
            return Result.success(result);
        } else {
            TemplateEditLockInfo currentLock = reportTemplateService.getLockStatus(id);
            result.put("lockInfo", currentLock);
            if (currentLock != null && !currentLock.getUserId().equals(userId)) {
                result.put("message", currentLock.getUserName() + "正在编辑该模板");
            } else if (currentLock == null) {
                result.put("message", "锁已过期，请重新进入编辑");
            }
            return Result.failure(ResultCode.TEMPLATE_LOCK_NOT_OWNER, result);
        }
    }

    @ApiOperation("获取模板锁状态")
    @GetMapping("/{id}/lock-status")
    @RequirePerms("report:designer:list")
    public Result<Map<String, Object>> getLockStatus(@PathVariable Long id) {
        Long userId = SecurityContextHolder.getUserId();
        TemplateEditLockInfo lockInfo = reportTemplateService.getLockStatus(id);
        Map<String, Object> result = new HashMap<>();
        result.put("locked", lockInfo != null);
        result.put("lockInfo", lockInfo);
        if (lockInfo != null) {
            result.put("isOwner", lockInfo.getUserId().equals(userId));
            if (!lockInfo.getUserId().equals(userId)) {
                result.put("message", lockInfo.getUserName() + "正在编辑该模板");
            }
        } else {
            result.put("isOwner", false);
        }
        return Result.success(result);
    }
}
