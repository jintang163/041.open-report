package com.openreport.admin.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openreport.admin.entity.DataSet;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.ReportTemplateService;
import com.openreport.common.enums.ReportStatusEnum;
import com.openreport.common.result.Result;
import com.openreport.common.result.ResultCode;
import com.openreport.common.utils.JwtUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Slf4j
@Api(tags = "嵌入式集成对外API")
@RestController
@RequestMapping("/api/embed")
public class EmbedReportController {

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private JwtUtils jwtUtils;

    private static final String CLAIM_KEY_REPORT_ID = "reportId";
    private static final String CLAIM_KEY_TYPE = "type";
    private static final String CLAIM_TYPE_EMBED = "embed";

    @ApiOperation("获取公开报表配置（免登录，token校验）")
    @GetMapping("/report/{id}")
    public Result<Map<String, Object>> getEmbedReportConfig(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @ApiParam(value = "访问token", required = true) @RequestParam(required = false) String token,
            HttpServletRequest request) {

        String validToken = resolveToken(token, request);
        if (StringUtils.isBlank(validToken)) {
            return Result.failure(ResultCode.TOKEN_INVALID);
        }
        if (!validateEmbedToken(validToken, id)) {
            return Result.failure(ResultCode.TOKEN_INVALID);
        }

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return Result.failure(ResultCode.REPORT_NOT_FOUND);
        }
        if (!ReportStatusEnum.PUBLISHED.getCode().equals(template.getStatus())) {
            return Result.failure(ResultCode.REPORT_NOT_FOUND, "报表未发布");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", template.getId());
        result.put("templateName", template.getTemplateName());
        result.put("templateCode", template.getTemplateCode());
        result.put("description", template.getDescription());
        result.put("templateJson", template.getTemplateJson());

        List<Map<String, Object>> params = new ArrayList<>();
        if (template.getParamConfig() != null) {
            try {
                params = JSON.parseObject(template.getParamConfig(), new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ignored) {
            }
        }
        result.put("params", params);

        return Result.success(result);
    }

    @ApiOperation("获取报表数据（带参数）")
    @GetMapping("/report/{id}/data")
    public Result<Map<String, Object>> getEmbedReportData(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @ApiParam(value = "访问token", required = true) @RequestParam(required = false) String token,
            @RequestParam Map<String, Object> allParams,
            HttpServletRequest request) {

        String validToken = resolveToken(token, request);
        if (StringUtils.isBlank(validToken)) {
            return Result.failure(ResultCode.TOKEN_INVALID);
        }
        if (!validateEmbedToken(validToken, id)) {
            return Result.failure(ResultCode.TOKEN_INVALID);
        }

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return Result.failure(ResultCode.REPORT_NOT_FOUND);
        }
        if (!ReportStatusEnum.PUBLISHED.getCode().equals(template.getStatus())) {
            return Result.failure(ResultCode.REPORT_NOT_FOUND, "报表未发布");
        }

        Map<String, Object> params = new HashMap<>();
        if (allParams != null) {
            for (Map.Entry<String, Object> entry : allParams.entrySet()) {
                if (!"token".equalsIgnoreCase(entry.getKey())) {
                    params.put(entry.getKey(), entry.getValue());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("templateId", id);
        result.put("templateName", template.getTemplateName());
        result.put("templateJson", template.getTemplateJson());

        Map<String, Object> dataSetData = new HashMap<>();
        if (template.getDataSetBind() != null) {
            try {
                List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> binding : bindings) {
                    Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
                    String bindName = binding.get("bindName") != null ? binding.get("bindName").toString() : "dataSet" + dataSetId;
                    Map<String, Object> previewResult = dataSetService.previewData(dataSetId, params, null);
                    dataSetData.put(bindName, previewResult);
                }
            } catch (Exception e) {
                log.error("解析数据集绑定失败", e);
                result.put("error", "解析数据集绑定失败: " + e.getMessage());
            }
        }
        result.put("dataSets", dataSetData);

        return Result.success(result);
    }

    @ApiOperation("导出报表（公开接口）")
    @PostMapping("/report/{id}/export")
    public Result<Map<String, Object>> exportEmbedReport(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @ApiParam(value = "访问token", required = true) @RequestParam(required = false) String token,
            @ApiParam(value = "导出类型: excel/pdf/html") @RequestParam(defaultValue = "excel") String exportType,
            @RequestBody(required = false) Map<String, Object> params,
            HttpServletRequest request) {

        String validToken = resolveToken(token, request);
        if (StringUtils.isBlank(validToken)) {
            return Result.failure(ResultCode.TOKEN_INVALID);
        }
        if (!validateEmbedToken(validToken, id)) {
            return Result.failure(ResultCode.TOKEN_INVALID);
        }

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return Result.failure(ResultCode.REPORT_NOT_FOUND);
        }
        if (!ReportStatusEnum.PUBLISHED.getCode().equals(template.getStatus())) {
            return Result.failure(ResultCode.REPORT_NOT_FOUND, "报表未发布");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("templateId", id);
        result.put("templateName", template.getTemplateName());
        result.put("exportType", exportType);

        Map<String, Object> exportData = new HashMap<>();
        if (template.getDataSetBind() != null) {
            try {
                List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> binding : bindings) {
                    Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
                    String bindName = binding.get("bindName") != null ? binding.get("bindName").toString() : "dataSet" + dataSetId;
                    Map<String, Object> previewResult = dataSetService.previewData(dataSetId, params, null);
                    exportData.put(bindName, previewResult);
                }
            } catch (Exception e) {
                log.error("解析数据集绑定失败", e);
                result.put("error", "解析数据集绑定失败: " + e.getMessage());
            }
        }
        result.put("exportData", exportData);
        result.put("fileName", template.getTemplateName() + "_" + System.currentTimeMillis());

        return Result.success(result);
    }

    @ApiOperation("生成临时访问token（需要鉴权）")
    @GetMapping("/report/{id}/token")
    public Result<Map<String, Object>> generateEmbedToken(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @ApiParam(value = "有效期（秒），默认3600") @RequestParam(defaultValue = "3600") Long expireSeconds,
            HttpServletRequest request) {

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return Result.failure(ResultCode.REPORT_NOT_FOUND);
        }

        Long userId = (Long) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_REPORT_ID, id);
        claims.put(CLAIM_KEY_TYPE, CLAIM_TYPE_EMBED);
        claims.put("createBy", userId);
        claims.put("createByUsername", username);
        claims.put("expireSeconds", expireSeconds);

        String token = jwtUtils.generateToken(claims);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("reportId", id);
        result.put("reportName", template.getTemplateName());
        result.put("expireSeconds", expireSeconds);
        result.put("createTime", System.currentTimeMillis());
        result.put("expireTime", System.currentTimeMillis() + expireSeconds * 1000);

        return Result.success(result);
    }

    private String resolveToken(String paramToken, HttpServletRequest request) {
        if (StringUtils.isNotBlank(paramToken)) {
            return paramToken;
        }
        String headerToken = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(headerToken) && headerToken.startsWith("Bearer ")) {
            return headerToken.substring(7);
        }
        return null;
    }

    private boolean validateEmbedToken(String token, Long reportId) {
        try {
            if (!jwtUtils.validateToken(token)) {
                return false;
            }
            io.jsonwebtoken.Claims claims = jwtUtils.parseToken(token);
            String type = claims.get(CLAIM_KEY_TYPE) != null ? claims.get(CLAIM_KEY_TYPE).toString() : null;
            if (!CLAIM_TYPE_EMBED.equals(type)) {
                return false;
            }
            Object tokenReportId = claims.get(CLAIM_KEY_REPORT_ID);
            if (tokenReportId == null) {
                return false;
            }
            Long tokenRptId = Long.valueOf(tokenReportId.toString());
            return tokenRptId.equals(reportId);
        } catch (Exception e) {
            log.error("validate embed token error", e);
            return false;
        }
    }
}
