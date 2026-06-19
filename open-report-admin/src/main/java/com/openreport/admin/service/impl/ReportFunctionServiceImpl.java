package com.openreport.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.ReportFunction;
import com.openreport.admin.entity.ReportFunctionVersion;
import com.openreport.admin.mapper.ReportFunctionMapper;
import com.openreport.admin.mapper.ReportFunctionVersionMapper;
import com.openreport.admin.service.ReportFunctionService;
import com.openreport.common.exception.BusinessException;
import com.openreport.engine.function.FunctionInitializer;
import com.openreport.engine.function.FunctionMeta;
import com.openreport.engine.function.FunctionRegistry;
import com.openreport.engine.function.GroovyFunctionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportFunctionServiceImpl extends ServiceImpl<ReportFunctionMapper, ReportFunction> implements ReportFunctionService {

    @Autowired
    private ReportFunctionVersionMapper versionMapper;

    @Autowired
    private FunctionRegistry functionRegistry;

    @Autowired
    private FunctionInitializer functionInitializer;

    @PostConstruct
    public void init() {
        try {
            reloadCustomFunctions();
        } catch (Exception e) {
            log.warn("Initial load custom functions failed, will retry later", e);
        }
    }

    @Override
    public Page<ReportFunction> pageList(Integer pageNum, Integer pageSize, String funcName, String category, Integer status) {
        Page<ReportFunction> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReportFunction> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(funcName)) {
            wrapper.and(w -> w.like(ReportFunction::getFuncName, funcName)
                    .or().like(ReportFunction::getFuncLabel, funcName));
        }
        if (StringUtils.isNotBlank(category)) {
            wrapper.eq(ReportFunction::getFuncCategory, category);
        }
        if (status != null) {
            wrapper.eq(ReportFunction::getStatus, status);
        }
        wrapper.orderByDesc(ReportFunction::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<ReportFunction> listEnabled() {
        LambdaQueryWrapper<ReportFunction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportFunction::getStatus, 1);
        wrapper.orderByAsc(ReportFunction::getFuncCategory, ReportFunction::getFuncName);
        return list(wrapper);
    }

    @Override
    public ReportFunction getDetail(Long id) {
        ReportFunction func = getById(id);
        if (func == null) {
            throw new BusinessException("函数不存在");
        }
        return func;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWithVersion(ReportFunction function, String scriptContent, String changeLog, Long userId) {
        if (StringUtils.isBlank(function.getFuncName())) {
            throw new BusinessException("函数名称不能为空");
        }
        if (StringUtils.isBlank(function.getFuncLabel())) {
            throw new BusinessException("函数显示名称不能为空");
        }

        LambdaQueryWrapper<ReportFunction> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(ReportFunction::getFuncName, function.getFuncName());
        if (count(checkWrapper) > 0) {
            throw new BusinessException("函数名称已存在: " + function.getFuncName());
        }

        if ("CUSTOM".equals(function.getFuncCategory())) {
            if (StringUtils.isBlank(scriptContent)) {
                throw new BusinessException("自定义函数的脚本内容不能为空");
            }
            GroovyFunctionExecutor.validateScript(scriptContent);
        }

        LocalDateTime now = LocalDateTime.now();
        function.setCurrentVersion(1);
        function.setCreateBy(userId);
        function.setCreateTime(now);
        function.setUpdateBy(userId);
        function.setUpdateTime(now);
        if (function.getStatus() == null) {
            function.setStatus(1);
        }
        if (function.getFuncCategory() == null) {
            function.setFuncCategory("CUSTOM");
        }
        boolean saved = save(function);
        if (saved) {
            ReportFunctionVersion version = new ReportFunctionVersion();
            version.setFuncId(function.getId());
            version.setVersion(1);
            version.setScriptContent(scriptContent);
            version.setScriptType("CUSTOM".equals(function.getFuncCategory()) ? "GROOVY" : "SYSTEM");
            version.setChangeLog(StringUtils.isBlank(changeLog) ? "初始版本" : changeLog);
            version.setCreateBy(userId);
            version.setCreateTime(now);
            versionMapper.insert(version);

            if (function.getStatus() == 1) {
                reloadCustomFunctions();
            }
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithVersion(ReportFunction function, String scriptContent, String changeLog, Long userId) {
        ReportFunction existing = getById(function.getId());
        if (existing == null) {
            throw new BusinessException("函数不存在");
        }

        boolean scriptChanged = StringUtils.isNotBlank(scriptContent)
                && !StringUtils.equals(scriptContent, getCurrentScriptContent(function.getId()));

        if (scriptChanged && "CUSTOM".equals(existing.getFuncCategory())) {
            GroovyFunctionExecutor.validateScript(scriptContent);
        }

        LocalDateTime now = LocalDateTime.now();
        function.setUpdateBy(userId);
        function.setUpdateTime(now);
        boolean updated = updateById(function);

        if (updated && scriptChanged) {
            Integer nextVersion = existing.getCurrentVersion() + 1;
            ReportFunctionVersion version = new ReportFunctionVersion();
            version.setFuncId(function.getId());
            version.setVersion(nextVersion);
            version.setScriptContent(scriptContent);
            version.setScriptType("CUSTOM".equals(existing.getFuncCategory()) ? "GROOVY" : "SYSTEM");
            version.setChangeLog(StringUtils.isBlank(changeLog) ? "更新脚本" : changeLog);
            version.setCreateBy(userId);
            version.setCreateTime(now);
            versionMapper.insert(version);

            function.setCurrentVersion(nextVersion);
            updateById(function);

            if (function.getStatus() == 1) {
                reloadCustomFunctions();
            }
        } else if (updated) {
            reloadCustomFunctions();
        }
        return updated;
    }

    private String getCurrentScriptContent(Long funcId) {
        ReportFunction func = getById(funcId);
        if (func == null) return null;
        LambdaQueryWrapper<ReportFunctionVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportFunctionVersion::getFuncId, funcId);
        wrapper.eq(ReportFunctionVersion::getVersion, func.getCurrentVersion());
        ReportFunctionVersion version = versionMapper.selectOne(wrapper);
        return version != null ? version.getScriptContent() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFunction(Long id) {
        ReportFunction existing = getById(id);
        if (existing == null) {
            throw new BusinessException("函数不存在");
        }
        if (!"CUSTOM".equals(existing.getFuncCategory())) {
            throw new BusinessException("系统内置函数不允许删除");
        }
        boolean removed = removeById(id);
        if (removed) {
            LambdaQueryWrapper<ReportFunctionVersion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReportFunctionVersion::getFuncId, id);
            versionMapper.delete(wrapper);
            reloadCustomFunctions();
        }
        return removed;
    }

    @Override
    public List<ReportFunctionVersion> listVersions(Long funcId) {
        LambdaQueryWrapper<ReportFunctionVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportFunctionVersion::getFuncId, funcId);
        wrapper.orderByDesc(ReportFunctionVersion::getVersion);
        return versionMapper.selectList(wrapper);
    }

    @Override
    public ReportFunctionVersion getVersion(Long versionId) {
        return versionMapper.selectById(versionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean switchVersion(Long funcId, Integer version, Long userId) {
        ReportFunction func = getById(funcId);
        if (func == null) {
            throw new BusinessException("函数不存在");
        }
        LambdaQueryWrapper<ReportFunctionVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportFunctionVersion::getFuncId, funcId);
        wrapper.eq(ReportFunctionVersion::getVersion, version);
        ReportFunctionVersion ver = versionMapper.selectOne(wrapper);
        if (ver == null) {
            throw new BusinessException("版本不存在: v" + version);
        }
        func.setCurrentVersion(version);
        func.setUpdateBy(userId);
        func.setUpdateTime(LocalDateTime.now());
        boolean updated = updateById(func);
        if (updated) {
            reloadCustomFunctions();
        }
        return updated;
    }

    @Override
    public Object testExecute(Long funcId, Map<String, Object> params) {
        ReportFunction func = getById(funcId);
        if (func == null) {
            throw new BusinessException("函数不存在");
        }
        LambdaQueryWrapper<ReportFunctionVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportFunctionVersion::getFuncId, funcId);
        wrapper.eq(ReportFunctionVersion::getVersion, func.getCurrentVersion());
        ReportFunctionVersion version = versionMapper.selectOne(wrapper);
        if (version == null || StringUtils.isBlank(version.getScriptContent())) {
            throw new BusinessException("函数脚本不存在");
        }
        return testExecuteScript(version.getScriptContent(), params);
    }

    @Override
    public Object testExecuteScript(String scriptContent, Map<String, Object> testData) {
        if (StringUtils.isBlank(scriptContent)) {
            throw new BusinessException("脚本内容不能为空");
        }
        GroovyFunctionExecutor.validateScript(scriptContent);
        GroovyFunctionExecutor executor = new GroovyFunctionExecutor(scriptContent);
        @SuppressWarnings("unchecked")
        List<Object> args = (List<Object>) (testData != null ? testData.get("args") : null);
        if (args == null) args = Collections.emptyList();
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> dataSets =
                (Map<String, List<Map<String, Object>>>) (testData != null ? testData.get("dataSets") : null);
        if (dataSets == null) dataSets = new HashMap<>();
        int currentRow = testData != null && testData.get("currentRow") != null
                ? ((Number) testData.get("currentRow")).intValue() : 0;
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters =
                (Map<String, Object>) (testData != null ? testData.get("parameters") : null);
        if (parameters == null) parameters = new HashMap<>();
        return executor.execute(args, dataSets, currentRow, parameters);
    }

    @Override
    public void validateScript(String scriptContent) {
        GroovyFunctionExecutor.validateScript(scriptContent);
    }

    @Override
    public void reloadCustomFunctions() {
        try {
            LambdaQueryWrapper<ReportFunction> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReportFunction::getStatus, 1);
            wrapper.eq(ReportFunction::getFuncCategory, "CUSTOM");
            List<ReportFunction> functions = list(wrapper);
            if (functions.isEmpty()) {
                functionRegistry.clearCustomFunctions();
                return;
            }

            List<Long> funcIds = functions.stream().map(ReportFunction::getId).collect(Collectors.toList());
            LambdaQueryWrapper<ReportFunctionVersion> versionWrapper = new LambdaQueryWrapper<>();
            versionWrapper.in(ReportFunctionVersion::getFuncId, funcIds);
            List<ReportFunctionVersion> allVersions = versionMapper.selectList(versionWrapper);
            Map<Long, Map<Integer, ReportFunctionVersion>> versionMap = allVersions.stream()
                    .collect(Collectors.groupingBy(ReportFunctionVersion::getFuncId,
                            Collectors.toMap(ReportFunctionVersion::getVersion, v -> v)));

            List<Map<String, Object>> functionData = new ArrayList<>();
            for (ReportFunction func : functions) {
                Map<Integer, ReportFunctionVersion> versions = versionMap.get(func.getId());
                ReportFunctionVersion currentVersion = versions != null ? versions.get(func.getCurrentVersion()) : null;
                if (currentVersion == null || StringUtils.isBlank(currentVersion.getScriptContent())) {
                    log.warn("Custom function {} has no valid script, skipped", func.getFuncName());
                    continue;
                }
                Map<String, Object> data = new HashMap<>();
                data.put("funcName", func.getFuncName());
                data.put("funcLabel", func.getFuncLabel());
                data.put("funcCategory", func.getFuncCategory());
                data.put("description", func.getDescription());
                data.put("paramConfig", func.getParamConfig());
                data.put("returnType", func.getReturnType());
                data.put("example", func.getExample());
                data.put("scriptContent", currentVersion.getScriptContent());
                functionData.add(data);
            }
            functionInitializer.loadCustomFunctions(functionData);
            log.info("Reloaded {} custom functions", functionData.size());
        } catch (Exception e) {
            log.error("Failed to reload custom functions", e);
            throw new BusinessException("重新加载自定义函数失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getFunctionDocs() {
        Map<String, FunctionMeta> builtinMeta = functionRegistry.getAllMeta();
        List<ReportFunction> dbFunctions = listEnabled();
        Map<String, ReportFunction> dbFunctionMap = dbFunctions.stream()
                .collect(Collectors.toMap(ReportFunction::getFuncName, f -> f, (a, b) -> a));

        Set<String> allNames = new LinkedHashSet<>(builtinMeta.keySet());
        for (ReportFunction f : dbFunctions) {
            allNames.add(f.getFuncName().toLowerCase());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : allNames) {
            Map<String, Object> doc = new LinkedHashMap<>();
            FunctionMeta meta = builtinMeta.get(name);
            ReportFunction dbFunc = dbFunctionMap.get(name);
            if (dbFunc != null) {
                doc.put("id", dbFunc.getId());
                doc.put("name", dbFunc.getFuncName());
                doc.put("label", dbFunc.getFuncLabel());
                doc.put("category", dbFunc.getFuncCategory());
                doc.put("description", dbFunc.getDescription());
                doc.put("returnType", dbFunc.getReturnType());
                doc.put("example", dbFunc.getExample());
                doc.put("status", dbFunc.getStatus());
                if (StringUtils.isNotBlank(dbFunc.getParamConfig())) {
                    try {
                        doc.put("params", JSON.parseArray(dbFunc.getParamConfig()));
                    } catch (Exception e) {
                        doc.put("params", Collections.emptyList());
                    }
                } else {
                    doc.put("params", Collections.emptyList());
                }
            } else if (meta != null) {
                doc.put("name", meta.getName());
                doc.put("label", meta.getLabel());
                doc.put("category", meta.getCategory());
                doc.put("description", meta.getDescription());
                doc.put("returnType", meta.getReturnType());
                doc.put("example", meta.getExample());
                doc.put("params", meta.getParams() != null ? meta.getParams() : Collections.emptyList());
                doc.put("status", 1);
            }
            if (!doc.isEmpty()) {
                result.add(doc);
            }
        }
        return result;
    }
}
