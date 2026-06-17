package com.openreport.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.entity.ChartDashboard;
import com.openreport.admin.entity.ChartDashboardItem;
import com.openreport.admin.entity.DataSet;
import com.openreport.admin.service.ChartDashboardItemService;
import com.openreport.admin.service.ChartDashboardService;
import com.openreport.admin.service.DataSetService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "可视化大屏管理")
@RestController
@RequestMapping("/dashboard")
public class ChartDashboardController {

    @Autowired
    private ChartDashboardService dashboardService;

    @Autowired
    private ChartDashboardItemService dashboardItemService;

    @Autowired
    private DataSetService dataSetService;

    @ApiOperation("分页查询大屏列表")
    @GetMapping("/page")
    public Result<Page<ChartDashboard>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name) {
        Page<ChartDashboard> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ChartDashboard> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(ChartDashboard::getName, name);
        }
        wrapper.orderByDesc(ChartDashboard::getCreateTime);
        return Result.success(dashboardService.page(page, wrapper));
    }

    @ApiOperation("获取大屏列表(不分页)")
    @GetMapping("/list")
    public Result<List<ChartDashboard>> list() {
        return Result.success(dashboardService.list(new LambdaQueryWrapper<ChartDashboard>()
                .eq(ChartDashboard::getStatus, 1)
                .orderByDesc(ChartDashboard::getCreateTime)));
    }

    @ApiOperation("获取大屏详情(含组件)")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        ChartDashboard dashboard = dashboardService.getById(id);
        if (dashboard == null) {
            return Result.failure("大屏不存在");
        }
        List<ChartDashboardItem> items = dashboardItemService.listByDashboardId(id);

        Map<String, Object> result = new HashMap<>();
        result.put("dashboard", dashboard);
        result.put("items", items);
        return Result.success(result);
    }

    @ApiOperation("新增大屏")
    @PostMapping
    public Result<ChartDashboard> add(@RequestBody ChartDashboard dashboard, @RequestAttribute("userId") Long userId) {
        dashboard.setCreateBy(userId);
        dashboard.setUpdateBy(userId);
        dashboard.setCreateTime(LocalDateTime.now());
        dashboard.setUpdateTime(LocalDateTime.now());
        dashboard.setDeleted(0);
        if (dashboard.getCanvasWidth() == null) dashboard.setCanvasWidth(1920);
        if (dashboard.getCanvasHeight() == null) dashboard.setCanvasHeight(1080);
        if (dashboard.getBackgroundColor() == null) dashboard.setBackgroundColor("#0d1b2a");
        if (dashboard.getRefreshInterval() == null) dashboard.setRefreshInterval(0);
        if (dashboard.getStatus() == null) dashboard.setStatus(1);
        dashboardService.save(dashboard);
        return Result.success(dashboard);
    }

    @ApiOperation("更新大屏")
    @PutMapping
    public Result<Void> update(@RequestBody ChartDashboard dashboard, @RequestAttribute("userId") Long userId) {
        dashboard.setUpdateBy(userId);
        dashboard.setUpdateTime(LocalDateTime.now());
        dashboardService.updateById(dashboard);
        return Result.success();
    }

    @ApiOperation("删除大屏")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dashboardItemService.remove(new LambdaQueryWrapper<ChartDashboardItem>()
                .eq(ChartDashboardItem::getDashboardId, id));
        dashboardService.removeById(id);
        return Result.success();
    }

    @ApiOperation("保存大屏组件布局")
    @PostMapping("/{id}/items")
    public Result<Void> saveItems(@PathVariable Long id, @RequestBody List<ChartDashboardItem> items) {
        dashboardItemService.saveBatchItems(id, items);
        return Result.success();
    }

    @ApiOperation("获取大屏组件列表")
    @GetMapping("/{id}/items")
    public Result<List<ChartDashboardItem>> getItems(@PathVariable Long id) {
        return Result.success(dashboardItemService.listByDashboardId(id));
    }

    @ApiOperation("获取图表数据(数据集预览)")
    @PostMapping("/chart-data/{datasetId}")
    public Result<List<Map<String, Object>>> getChartData(
            @PathVariable Long datasetId,
            @RequestBody(required = false) Map<String, Object> params) {
        return Result.success(dataSetService.previewDataList(datasetId, params));
    }
}
