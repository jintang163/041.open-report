package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.entity.DataSourceConfig;
import com.openreport.admin.service.DataSourceConfigService;
import com.openreport.common.result.Result;
import com.openreport.common.result.ResultCode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Api(tags = "数据源管理")
@RestController
@RequestMapping("/datasource")
public class DataSourceConfigController {

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @ApiOperation("分页查询数据源列表")
    @GetMapping("/page")
    public Result<Page<DataSourceConfig>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String dsName,
            @RequestParam(required = false) String dsType) {
        return Result.success(dataSourceConfigService.pageList(pageNum, pageSize, dsName, dsType));
    }

    @ApiOperation("获取所有数据源列表")
    @GetMapping("/list")
    public Result<List<DataSourceConfig>> list() {
        return Result.success(dataSourceConfigService.listAll());
    }

    @ApiOperation("获取数据源详情")
    @GetMapping("/{id}")
    public Result<DataSourceConfig> getById(@PathVariable Long id) {
        return Result.success(dataSourceConfigService.getById(id));
    }

    @ApiOperation("新增数据源")
    @PostMapping
    @RequirePerms("data:source:add")
    public Result<Void> add(@RequestBody DataSourceConfig config, @RequestAttribute("userId") Long userId) {
        config.setCreateBy(userId);
        config.setUpdateBy(userId);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        config.setDeleted(0);
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        dataSourceConfigService.save(config);
        return Result.success();
    }

    @ApiOperation("更新数据源")
    @PutMapping
    @RequirePerms("data:source:edit")
    public Result<Void> update(@RequestBody DataSourceConfig config, @RequestAttribute("userId") Long userId) {
        config.setUpdateBy(userId);
        config.setUpdateTime(LocalDateTime.now());
        dataSourceConfigService.updateById(config);
        return Result.success();
    }

    @ApiOperation("删除数据源")
    @DeleteMapping("/{id}")
    @RequirePerms("data:source:remove")
    public Result<Void> delete(@PathVariable Long id) {
        dataSourceConfigService.removeById(id);
        return Result.success();
    }

    @ApiOperation("测试数据源连接")
    @PostMapping("/test")
    public Result<Boolean> testConnection(@RequestBody DataSourceConfig config) {
        boolean success = dataSourceConfigService.testConnection(config);
        if (success) {
            return Result.success("连接成功", true);
        } else {
            return Result.failure(ResultCode.DATA_SOURCE_CONNECT_ERROR);
        }
    }

    @ApiOperation("根据ID测试连接")
    @PostMapping("/test/{id}")
    public Result<Boolean> testConnectionById(@PathVariable Long id) {
        DataSourceConfig config = dataSourceConfigService.getById(id);
        if (config == null) {
            return Result.failure(ResultCode.DATA_SOURCE_NOT_FOUND);
        }
        boolean success = dataSourceConfigService.testConnection(config);
        if (success) {
            return Result.success("连接成功", true);
        } else {
            return Result.failure(ResultCode.DATA_SOURCE_CONNECT_ERROR);
        }
    }

    @ApiOperation("获取数据源连接信息")
    @GetMapping("/connection/{id}")
    public Result<Map<String, Object>> getConnectionInfo(@PathVariable Long id) {
        return Result.success(dataSourceConfigService.getConnectionInfo(id));
    }

    @ApiOperation("获取数据源表列表")
    @GetMapping("/{id}/tables")
    public Result<List<Map<String, Object>>> getTables(@PathVariable Long id) {
        return Result.success(dataSourceConfigService.getTables(id));
    }

    @ApiOperation("获取表字段信息")
    @GetMapping("/{id}/columns/{tableName}")
    public Result<List<Map<String, Object>>> getTableColumns(
            @PathVariable Long id,
            @PathVariable String tableName) {
        return Result.success(dataSourceConfigService.getTableColumns(id, tableName));
    }

    @ApiOperation("生成数据源Schema信息（提供给大模型）")
    @GetMapping("/{id}/schema")
    public Result<String> generateSchemaInfo(@PathVariable Long id) {
        return Result.success(dataSourceConfigService.generateSchemaInfo(id));
    }

    @ApiOperation("校验SQL是否可执行")
    @PostMapping("/{id}/validate-sql")
    public Result<Map<String, Object>> validateSql(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        return Result.success(dataSourceConfigService.validateSql(id, sql));
    }
}
