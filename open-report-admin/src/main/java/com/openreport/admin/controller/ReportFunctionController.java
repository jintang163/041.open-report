package com.openreport.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openreport.admin.entity.ReportFunction;
import com.openreport.admin.entity.ReportFunctionVersion;
import com.openreport.admin.service.ReportFunctionService;
import com.openreport.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "函数仓库管理")
@RestController
@RequestMapping("/report-function")
public class ReportFunctionController {

    @Autowired
    private ReportFunctionService reportFunctionService;

    @ApiOperation("分页查询函数列表")
    @GetMapping("/page")
    public Result<Page<ReportFunction>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String funcName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        return Result.success(reportFunctionService.pageList(pageNum, pageSize, funcName, category, status));
    }

    @ApiOperation("获取所有启用的函数（用于设计器）")
    @GetMapping("/enabled")
    public Result<List<ReportFunction>> listEnabled() {
        return Result.success(reportFunctionService.listEnabled());
    }

    @ApiOperation("获取函数文档列表（包含内置和自定义）")
    @GetMapping("/docs")
    public Result<List<Map<String, Object>>> getFunctionDocs() {
        return Result.success(reportFunctionService.getFunctionDocs());
    }

    @ApiOperation("获取函数详情")
    @GetMapping("/{id}")
    public Result<ReportFunction> getById(@PathVariable Long id) {
        return Result.success(reportFunctionService.getDetail(id));
    }

    @ApiOperation("新增函数")
    @PostMapping
    public Result<Void> add(@RequestBody Map<String, Object> body, @RequestAttribute("userId") Long userId) {
        ReportFunction function = new ReportFunction();
        function.setFuncName((String) body.get("funcName"));
        function.setFuncLabel((String) body.get("funcLabel"));
        function.setFuncCategory((String) body.getOrDefault("funcCategory", "CUSTOM"));
        function.setDescription((String) body.get("description"));
        function.setParamConfig((String) body.get("paramConfig"));
        function.setReturnType((String) body.get("returnType"));
        function.setExample((String) body.get("example"));
        Object status = body.get("status");
        if (status != null) {
            function.setStatus(((Number) status).intValue());
        }
        String scriptContent = (String) body.get("scriptContent");
        String changeLog = (String) body.get("changeLog");
        reportFunctionService.saveWithVersion(function, scriptContent, changeLog, userId);
        return Result.success();
    }

    @ApiOperation("更新函数")
    @PutMapping
    public Result<Void> update(@RequestBody Map<String, Object> body, @RequestAttribute("userId") Long userId) {
        ReportFunction function = new ReportFunction();
        function.setId(((Number) body.get("id")).longValue());
        function.setFuncName((String) body.get("funcName"));
        function.setFuncLabel((String) body.get("funcLabel"));
        function.setFuncCategory((String) body.get("funcCategory"));
        function.setDescription((String) body.get("description"));
        function.setParamConfig((String) body.get("paramConfig"));
        function.setReturnType((String) body.get("returnType"));
        function.setExample((String) body.get("example"));
        Object status = body.get("status");
        if (status != null) {
            function.setStatus(((Number) status).intValue());
        }
        String scriptContent = (String) body.get("scriptContent");
        String changeLog = (String) body.get("changeLog");
        reportFunctionService.updateWithVersion(function, scriptContent, changeLog, userId);
        return Result.success();
    }

    @ApiOperation("删除函数")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportFunctionService.deleteFunction(id);
        return Result.success();
    }

    @ApiOperation("获取函数版本列表")
    @GetMapping("/{id}/versions")
    public Result<List<ReportFunctionVersion>> listVersions(@PathVariable Long id) {
        return Result.success(reportFunctionService.listVersions(id));
    }

    @ApiOperation("切换函数版本")
    @PostMapping("/{id}/switch-version")
    public Result<Void> switchVersion(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body,
            @RequestAttribute("userId") Long userId) {
        Integer version = body.get("version");
        reportFunctionService.switchVersion(id, version, userId);
        return Result.success();
    }

    @ApiOperation("测试执行函数")
    @PostMapping("/{id}/test")
    public Result<Object> testExecute(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> params) {
        return Result.success(reportFunctionService.testExecute(id, params));
    }

    @ApiOperation("测试执行脚本")
    @PostMapping("/test-script")
    public Result<Object> testExecuteScript(@RequestBody Map<String, Object> body) {
        String scriptContent = (String) body.get("scriptContent");
        @SuppressWarnings("unchecked")
        Map<String, Object> testData = (Map<String, Object>) body.get("testData");
        return Result.success(reportFunctionService.testExecuteScript(scriptContent, testData));
    }

    @ApiOperation("验证Groovy脚本语法")
    @PostMapping("/validate-script")
    public Result<Void> validateScript(@RequestBody Map<String, String> body) {
        String scriptContent = body.get("scriptContent");
        reportFunctionService.validateScript(scriptContent);
        return Result.success();
    }

    @ApiOperation("重新加载自定义函数到运行时")
    @PostMapping("/reload")
    public Result<Void> reload() {
        reportFunctionService.reloadCustomFunctions();
        return Result.success();
    }
}
