package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.entity.DataSet;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.DataLineageService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Api(tags = "数据集管理")
@RestController
@RequestMapping("/dataset")
public class DataSetController {

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private com.openreport.admin.websocket.WebSocketPushService pushService;

    @Autowired
    private DataLineageService dataLineageService;

    @ApiOperation("分页查询数据集列表")
    @GetMapping("/page")
    @RequirePerms("data:set:list")
    public Result<Page<DataSet>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String setName,
            @RequestParam(required = false) Long dsId) {
        return Result.success(dataSetService.pageList(pageNum, pageSize, setName, dsId));
    }

    @ApiOperation("根据数据源ID获取数据集列表")
    @GetMapping("/list/{dsId}")
    @RequirePerms("data:set:list")
    public Result<List<DataSet>> listByDsId(@PathVariable Long dsId) {
        return Result.success(dataSetService.listByDsId(dsId));
    }

    @ApiOperation("获取数据集详情")
    @GetMapping("/{id}")
    @RequirePerms("data:set:list")
    public Result<DataSet> getById(@PathVariable Long id) {
        return Result.success(dataSetService.getById(id));
    }

    @ApiOperation("新增数据集")
    @PostMapping
    @RequirePerms("data:set:add")
    public Result<Void> add(@RequestBody DataSet dataSet, @RequestAttribute("userId") Long userId) {
        dataSet.setCreateBy(userId);
        dataSet.setUpdateBy(userId);
        dataSet.setCreateTime(LocalDateTime.now());
        dataSet.setUpdateTime(LocalDateTime.now());
        dataSet.setDeleted(0);
        if (dataSet.getStatus() == null) {
            dataSet.setStatus(1);
        }
        dataSetService.save(dataSet);
        pushService.pushDataChangeToAll("DATASET_ADD");

        try {
            dataLineageService.refreshLineageForDataSet(dataSet.getId());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(DataSetController.class)
                    .warn("自动刷新血缘关系失败（不影响保存）: dataSetId={}", dataSet.getId(), e);
        }

        return Result.success();
    }

    @ApiOperation("更新数据集")
    @PutMapping
    @RequirePerms("data:set:edit")
    public Result<Void> update(@RequestBody DataSet dataSet, @RequestAttribute("userId") Long userId) {
        dataSet.setUpdateBy(userId);
        dataSet.setUpdateTime(LocalDateTime.now());
        dataSetService.updateById(dataSet);
        pushService.pushDataChangeToAll("DATASET_UPDATE");

        try {
            dataLineageService.refreshLineageForDataSet(dataSet.getId());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(DataSetController.class)
                    .warn("自动刷新血缘关系失败（不影响更新）: dataSetId={}", dataSet.getId(), e);
        }

        return Result.success();
    }

    @ApiOperation("删除数据集")
    @DeleteMapping("/{id}")
    @RequirePerms("data:set:remove")
    public Result<Void> delete(@PathVariable Long id) {
        dataSetService.removeById(id);
        pushService.pushDataChangeToAll("DATASET_DELETE");
        return Result.success();
    }

    @ApiOperation("预览数据集数据")
    @PostMapping("/preview/{id}")
    @RequirePerms("data:set:list")
    public Result<Map<String, Object>> previewData(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> params,
            @RequestParam(defaultValue = "100") Integer limit) {
        return Result.success(dataSetService.previewData(id, params, limit));
    }

    @ApiOperation("解析SQL获取参数")
    @PostMapping("/parse-sql")
    @RequirePerms("data:set:list")
    public Result<Map<String, Object>> parseSql(@RequestBody Map<String, String> body) {
        String sqlText = body.get("sqlText");
        return Result.success(dataSetService.parseSql(sqlText));
    }
}
