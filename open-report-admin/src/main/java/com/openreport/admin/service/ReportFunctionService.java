package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.ReportFunction;
import com.openreport.admin.entity.ReportFunctionVersion;

import java.util.List;
import java.util.Map;

public interface ReportFunctionService extends IService<ReportFunction> {

    Page<ReportFunction> pageList(Integer pageNum, Integer pageSize, String funcName, String category, Integer status);

    List<ReportFunction> listEnabled();

    ReportFunction getDetail(Long id);

    boolean saveWithVersion(ReportFunction function, String scriptContent, String changeLog, Long userId);

    boolean updateWithVersion(ReportFunction function, String scriptContent, String changeLog, Long userId);

    boolean deleteFunction(Long id);

    List<ReportFunctionVersion> listVersions(Long funcId);

    ReportFunctionVersion getVersion(Long versionId);

    boolean switchVersion(Long funcId, Integer version, Long userId);

    Object testExecute(Long funcId, Map<String, Object> params);

    Object testExecuteScript(String scriptContent, Map<String, Object> testData);

    void validateScript(String scriptContent);

    void reloadCustomFunctions();

    List<Map<String, Object>> getFunctionDocs();
}
