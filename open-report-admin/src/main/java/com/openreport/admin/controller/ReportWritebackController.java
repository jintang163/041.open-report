package com.openreport.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.openreport.admin.dto.writeback.DataSubmitRequest;
import com.openreport.admin.dto.writeback.DataSubmitResult;
import com.openreport.admin.entity.ReportWritebackDetail;
import com.openreport.admin.entity.ReportWritebackHistory;
import com.openreport.admin.service.ReportWritebackHistoryService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "报表数据回写")
@RestController
@RequestMapping("/report-writeback")
public class ReportWritebackController {

    @Autowired
    private ReportWritebackHistoryService historyService;

    @ApiOperation("提交报表数据")
    @PostMapping("/submit")
    public Result<DataSubmitResult> submitData(@RequestBody DataSubmitRequest request,
                                                @RequestAttribute("userId") Long userId) {
        return Result.success(historyService.submitData(request, userId));
    }

    @ApiOperation("分页查询提交历史")
    @GetMapping("/history/page")
    public Result<IPage<ReportWritebackHistory>> getHistoryPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long reportId) {
        return Result.success(historyService.getHistoryPage(pageNum, pageSize, reportId));
    }

    @ApiOperation("获取提交历史详情")
    @GetMapping("/history/{id}")
    public Result<ReportWritebackHistory> getHistoryDetail(@PathVariable Long id) {
        return Result.success(historyService.getHistoryDetail(id));
    }

    @ApiOperation("获取提交明细")
    @GetMapping("/history/{historyId}/details")
    public Result<List<ReportWritebackDetail>> getHistoryDetails(@PathVariable Long historyId) {
        return Result.success(historyService.getHistoryDetails(historyId));
    }
}
