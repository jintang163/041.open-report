package com.openreport.scheduler.function;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openreport.engine.function.CustomFunctionLoader;
import com.openreport.scheduler.entity.ReportFunction;
import com.openreport.scheduler.entity.ReportFunctionVersion;
import com.openreport.scheduler.mapper.ReportFunctionMapper;
import com.openreport.scheduler.mapper.ReportFunctionVersionMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SchedulerCustomFunctionLoader implements CustomFunctionLoader {

    @Autowired
    private ReportFunctionMapper reportFunctionMapper;

    @Autowired
    private ReportFunctionVersionMapper reportFunctionVersionMapper;

    @Override
    public List<Map<String, Object>> loadEnabledCustomFunctions() {
        try {
            LambdaQueryWrapper<ReportFunction> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReportFunction::getStatus, 1);
            wrapper.eq(ReportFunction::getFuncCategory, "CUSTOM");
            List<ReportFunction> functions = reportFunctionMapper.selectList(wrapper);
            if (functions == null || functions.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> funcIds = functions.stream()
                    .map(ReportFunction::getId)
                    .collect(Collectors.toList());

            LambdaQueryWrapper<ReportFunctionVersion> versionWrapper = new LambdaQueryWrapper<>();
            versionWrapper.in(ReportFunctionVersion::getFuncId, funcIds);
            List<ReportFunctionVersion> allVersions = reportFunctionVersionMapper.selectList(versionWrapper);

            Map<Long, Map<Integer, ReportFunctionVersion>> versionMap = allVersions.stream()
                    .collect(Collectors.groupingBy(
                            ReportFunctionVersion::getFuncId,
                            Collectors.toMap(ReportFunctionVersion::getVersion, v -> v)
                    ));

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
            return functionData;
        } catch (Exception e) {
            log.error("Failed to load custom functions in scheduler", e);
            return Collections.emptyList();
        }
    }
}
