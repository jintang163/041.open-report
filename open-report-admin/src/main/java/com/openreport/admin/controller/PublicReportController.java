package com.openreport.admin.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.ReportTemplateService;
import com.openreport.common.enums.ReportStatusEnum;
import com.openreport.common.result.Result;
import com.openreport.common.result.ResultCode;
import com.openreport.engine.export.ExcelExporter;
import com.openreport.engine.export.PdfExporter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Api(tags = "公开报表访问（匿名分享）")
@RestController
@RequestMapping("/report/public")
public class PublicReportController {

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ExcelExporter excelExporter;

    @Autowired
    private PdfExporter pdfExporter;

    private static final String CLAIM_KEY_TYPE = "type";
    private static final String CLAIM_TYPE_SHARE = "share";
    private static final String CLAIM_KEY_REPORT_ID = "reportId";
    private static final String CLAIM_KEY_TOKEN = "shareToken";

    @ApiOperation("获取公开报表信息（通过分享token）")
    @GetMapping("/{token}")
    public Result<Map<String, Object>> getPublicReportInfo(
            @ApiParam(value = "分享token", required = true) @PathVariable String token,
            @ApiParam(value = "访问密码") @RequestParam(required = false) String password) {

        ReportTemplate template = validateShareToken(token, password);
        if (template == null) {
            return Result.failure(ResultCode.TOKEN_INVALID, "分享链接无效或已过期");
        }

        incrementShareViewCount(template.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", template.getId());
        result.put("name", template.getTemplateName());
        result.put("description", template.getDescription());

        List<Map<String, Object>> params = new ArrayList<>();
        if (template.getParamConfig() != null) {
            try {
                params = JSON.parseObject(template.getParamConfig(),
                        new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ignored) {
            }
        }
        result.put("params", params);

        return Result.success(result);
    }

    @ApiOperation("执行公开报表（通过分享token）")
    @PostMapping("/execute/{token}")
    public Result<Map<String, Object>> executePublicReport(
            @ApiParam(value = "分享token", required = true) @PathVariable String token,
            @ApiParam(value = "访问密码") @RequestParam(required = false) String password,
            @RequestBody(required = false) Map<String, Object> params) {

        ReportTemplate template = validateShareToken(token, password);
        if (template == null) {
            return Result.failure(ResultCode.TOKEN_INVALID, "分享链接无效或已过期");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", template.getId());
        result.put("templateName", template.getTemplateName());
        result.put("templateJson", template.getTemplateJson());

        List<Map<String, Object>> tables = new ArrayList<>();
        Map<String, Object> dataSetData = new LinkedHashMap<>();

        if (template.getDataSetBind() != null) {
            try {
                List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                        new TypeReference<List<Map<String, Object>>>() {});

                for (Map<String, Object> binding : bindings) {
                    Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
                    String bindName = binding.get("bindName") != null
                            ? binding.get("bindName").toString()
                            : "dataSet" + dataSetId;

                    Map<String, Object> tableItem = new LinkedHashMap<>();
                    List<Map<String, Object>> tableColumns = new ArrayList<>();
                    List<Map<String, Object>> tableRows = new ArrayList<>();

                    Map<String, Object> previewResult = dataSetService.previewDataWithCount(
                            dataSetId, params, null);
                    dataSetData.put(bindName, previewResult);

                    if (previewResult != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> cols = (List<Map<String, Object>>) previewResult.get("columns");
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> rows = (List<Map<String, Object>>) previewResult.get("rows");
                        if (cols != null) {
                            for (Map<String, Object> c : cols) {
                                Map<String, Object> tc = new LinkedHashMap<>();
                                tc.put("title", c.get("title") != null ? c.get("title") : c.get("name"));
                                tc.put("dataIndex", c.get("dataIndex") != null ? c.get("dataIndex") : c.get("name"));
                                tc.put("key", c.get("key") != null ? c.get("key") : c.get("name"));
                                if (c.get("width") != null) tc.put("width", c.get("width"));
                                if (c.get("align") != null) tc.put("align", c.get("align"));
                                tableColumns.add(tc);
                            }
                        }
                        if (rows != null) tableRows.addAll(rows);
                    }

                    tableItem.put("bindName", bindName);
                    tableItem.put("dataSetId", dataSetId);
                    tableItem.put("columns", tableColumns);
                    tableItem.put("rows", tableRows);
                    tableItem.put("total", previewResult != null ? previewResult.get("total") : 0);
                    tables.add(tableItem);
                }
            } catch (Exception e) {
                result.put("error", "解析数据集绑定失败: " + e.getMessage());
            }
        }

        result.put("tables", tables);
        result.put("dataSets", dataSetData);
        result.put("title", template.getTemplateName());
        result.put("summary", template.getDescription());
        result.put("charts", new ArrayList<>());
        result.put("html", null);
        result.put("pageMode", false);

        if (!tables.isEmpty()) {
            Map<String, Object> first = tables.get(0);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tCols = (List<Map<String, Object>>) first.get("columns");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tRows = (List<Map<String, Object>>) first.get("rows");
            Map<String, Object> tableData = new LinkedHashMap<>();
            tableData.put("columns", tCols);
            tableData.put("dataSource", tRows);
            tableData.put("total", first.get("total"));
            result.put("table", tableData);
        }

        return Result.success(result);
    }

    @ApiOperation("获取公开报表参数配置")
    @GetMapping("/params/{token}")
    public Result<List<Map<String, Object>>> getPublicReportParams(
            @ApiParam(value = "分享token", required = true) @PathVariable String token,
            @ApiParam(value = "访问密码") @RequestParam(required = false) String password) {

        ReportTemplate template = validateShareToken(token, password);
        if (template == null) {
            return Result.failure(ResultCode.TOKEN_INVALID, "分享链接无效或已过期");
        }

        List<Map<String, Object>> params = new ArrayList<>();
        if (template.getParamConfig() != null) {
            try {
                params = JSON.parseObject(template.getParamConfig(),
                        new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ignored) {
            }
        }
        return Result.success(params);
    }

    @ApiOperation("生成分享链接（需要登录）")
    @PostMapping("/{id}/generate")
    public Result<Map<String, Object>> generateShareLink(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @ApiParam(value = "有效期（秒），默认7天") @RequestParam(defaultValue = "604800") Long expireSeconds,
            @ApiParam(value = "访问密码") @RequestParam(required = false) String password) {

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return Result.failure(ResultCode.DATA_NOT_FOUND, "报表不存在");
        }

        if (!ReportStatusEnum.PUBLISHED.getCode().equals(template.getStatus())) {
            return Result.failure(ResultCode.BAD_REQUEST, "报表未发布，无法分享");
        }

        String shareToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(expireSeconds);

        template.setShareEnabled(1);
        template.setShareToken(shareToken);
        template.setShareExpireTime(expireTime);
        if (StringUtils.isNotBlank(password)) {
            template.setSharePassword(password);
        }
        reportTemplateService.updateById(template);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", id);
        result.put("reportName", template.getTemplateName());
        result.put("shareToken", shareToken);
        result.put("shareUrl", "/h5/share/" + shareToken);
        result.put("expireTime", expireTime.toString());
        result.put("expireSeconds", expireSeconds);
        result.put("hasPassword", StringUtils.isNotBlank(password));

        return Result.success(result);
    }

    @ApiOperation("取消分享（需要登录）")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelShare(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id) {

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return Result.failure(ResultCode.DATA_NOT_FOUND, "报表不存在");
        }

        template.setShareEnabled(0);
        template.setShareToken(null);
        template.setShareExpireTime(null);
        template.setSharePassword(null);
        reportTemplateService.updateById(template);

        return Result.success();
    }

    @ApiOperation("获取分享状态（需要登录）")
    @GetMapping("/{id}/status")
    public Result<Map<String, Object>> getShareStatus(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id) {

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return Result.failure(ResultCode.DATA_NOT_FOUND, "报表不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", id);
        result.put("shareEnabled", template.getShareEnabled() != null && template.getShareEnabled() == 1);
        result.put("shareToken", template.getShareToken());
        result.put("shareUrl", template.getShareToken() != null ? "/h5/share/" + template.getShareToken() : null);
        result.put("shareExpireTime", template.getShareExpireTime() != null ? template.getShareExpireTime().toString() : null);
        result.put("hasPassword", StringUtils.isNotBlank(template.getSharePassword()));
        result.put("shareViewCount", template.getShareViewCount() != null ? template.getShareViewCount() : 0);

        return Result.success(result);
    }

    @ApiOperation("导出公开报表Excel（通过分享token）")
    @GetMapping("/export/{token}/excel")
    public ResponseEntity<byte[]> exportPublicReportExcel(
            @ApiParam(value = "分享token", required = true) @PathVariable String token,
            @ApiParam(value = "访问密码") @RequestParam(required = false) String password,
            @RequestParam(required = false) Map<String, Object> allParams) {

        ReportTemplate template = validateShareToken(token, password);
        if (template == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> params = extractParams(allParams);
        byte[] bytes = buildExcelBytes(template, params);

        String fileName = encodeFileName(template.getTemplateName() + ".xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @ApiOperation("导出公开报表PDF（通过分享token）")
    @GetMapping("/export/{token}/pdf")
    public ResponseEntity<byte[]> exportPublicReportPdf(
            @ApiParam(value = "分享token", required = true) @PathVariable String token,
            @ApiParam(value = "访问密码") @RequestParam(required = false) String password,
            @RequestParam(required = false) Map<String, Object> allParams) {

        ReportTemplate template = validateShareToken(token, password);
        if (template == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> params = extractParams(allParams);
        byte[] bytes = buildPdfBytes(template, params);

        String fileName = encodeFileName(template.getTemplateName() + ".pdf");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @ApiOperation("导出公开报表HTML（通过分享token）")
    @GetMapping("/export/{token}/html")
    public ResponseEntity<String> exportPublicReportHtml(
            @ApiParam(value = "分享token", required = true) @PathVariable String token,
            @ApiParam(value = "访问密码") @RequestParam(required = false) String password,
            @RequestParam(required = false) Map<String, Object> allParams) {

        ReportTemplate template = validateShareToken(token, password);
        if (template == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Map<String, Object> params = extractParams(allParams);
        Map<String, Object> dataSets = collectDataSetData(template, params);

        String html = buildHtmlDocument(template, dataSets);
        String fileName = encodeFileName(template.getTemplateName() + ".html");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private Map<String, Object> extractParams(Map<String, Object> allParams) {
        Map<String, Object> params = new HashMap<>();
        if (allParams != null) {
            for (Map.Entry<String, Object> entry : allParams.entrySet()) {
                String key = entry.getKey();
                if ("token".equalsIgnoreCase(key) || "password".equalsIgnoreCase(key)) {
                    continue;
                }
                params.put(key, entry.getValue());
            }
        }
        return params;
    }

    private Map<String, Object> collectDataSetData(ReportTemplate template, Map<String, Object> params) {
        Map<String, Object> dataSetData = new HashMap<>();
        if (template.getDataSetBind() != null) {
            try {
                List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> binding : bindings) {
                    Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
                    String bindName = binding.get("bindName") != null ? binding.get("bindName").toString() : "dataSet" + dataSetId;
                    Map<String, Object> previewResult = dataSetService.previewDataWithCount(dataSetId, params, null);
                    dataSetData.put(bindName, previewResult);
                }
            } catch (Exception e) {
                log.error("解析数据集绑定失败", e);
            }
        }
        return dataSetData;
    }

    private byte[] buildExcelBytes(ReportTemplate template, Map<String, Object> params) {
        Map<String, Object> dataSets = collectDataSetData(template, params);
        if (dataSets != null && !dataSets.isEmpty()) {
            Map.Entry<String, Object> firstEntry = dataSets.entrySet().iterator().next();
            Object dataObj = firstEntry.getValue();
            if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                Object rows = dataMap.get("rows");
                if (rows instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rowList = (List<Map<String, Object>>) rows;
                    return excelExporter.exportDataSet(template.getTemplateName(), rowList);
                }
            }
        }
        return excelExporter.exportDataSet(template.getTemplateName(), new ArrayList<>());
    }

    private byte[] buildPdfBytes(ReportTemplate template, Map<String, Object> params) {
        Map<String, Object> dataSets = collectDataSetData(template, params);
        if (dataSets != null && !dataSets.isEmpty()) {
            Map.Entry<String, Object> firstEntry = dataSets.entrySet().iterator().next();
            Object dataObj = firstEntry.getValue();
            if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                Object rows = dataMap.get("rows");
                if (rows instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rowList = (List<Map<String, Object>>) rows;
                    return pdfExporter.exportDataSet(template.getTemplateName(), rowList);
                }
            }
        }
        return pdfExporter.exportDataSet(template.getTemplateName(), new ArrayList<>());
    }

    private String buildHtmlDocument(ReportTemplate template, Map<String, Object> dataSets) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <title>").append(escapeHtml(template.getTemplateName())).append("</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 40px; }\n");
        html.append("        h1 { text-align: center; color: #333; }\n");
        html.append("        table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n");
        html.append("        th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }\n");
        html.append("        th { background-color: #f5f5f5; font-weight: bold; }\n");
        html.append("        tr:nth-child(even) { background-color: #fafafa; }\n");
        html.append("        .meta { color: #888; font-size: 12px; text-align: center; margin-top: 20px; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <h1>").append(escapeHtml(template.getTemplateName())).append("</h1>\n");

        if (template.getDescription() != null && !template.getDescription().isEmpty()) {
            html.append("    <p style=\"text-align:center;color:#666;\">").append(escapeHtml(template.getDescription())).append("</p>\n");
        }

        if (dataSets != null && !dataSets.isEmpty()) {
            for (Map.Entry<String, Object> entry : dataSets.entrySet()) {
                html.append("    <h3>").append(escapeHtml(entry.getKey())).append("</h3>\n");
                Object dataObj = entry.getValue();
                if (dataObj instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                    Object rows = dataMap.get("rows");
                    if (rows instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> rowList = (List<Map<String, Object>>) rows;
                        html.append(buildHtmlTable(rowList));
                    }
                }
            }
        }

        html.append("    <div class=\"meta\">导出时间: ").append(new Date()).append(" | 报表ID: ").append(template.getId()).append("</div>\n");
        html.append("</body>\n");
        html.append("</html>");
        return html.toString();
    }

    private String buildHtmlTable(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "    <p>暂无数据</p>\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("    <table>\n");
        sb.append("        <thead><tr>\n");
        Map<String, Object> firstRow = rows.get(0);
        for (String key : firstRow.keySet()) {
            sb.append("            <th>").append(escapeHtml(key)).append("</th>\n");
        }
        sb.append("        </tr></thead>\n");
        sb.append("        <tbody>\n");
        for (Map<String, Object> row : rows) {
            sb.append("        <tr>\n");
            for (Object value : row.values()) {
                sb.append("            <td>").append(value == null ? "" : escapeHtml(value.toString())).append("</td>\n");
            }
            sb.append("        </tr>\n");
        }
        sb.append("        </tbody>\n");
        sb.append("    </table>\n");
        return sb.toString();
    }

    private String encodeFileName(String fileName) {
        try {
            return URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception e) {
            return fileName;
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private ReportTemplate validateShareToken(String token, String password) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        ReportTemplate template = reportTemplateService.lambdaQuery()
                .eq(ReportTemplate::getShareToken, token)
                .eq(ReportTemplate::getShareEnabled, 1)
                .one();

        if (template == null) {
            return null;
        }

        if (!ReportStatusEnum.PUBLISHED.getCode().equals(template.getStatus())) {
            return null;
        }

        if (template.getShareExpireTime() != null && LocalDateTime.now().isAfter(template.getShareExpireTime())) {
            return null;
        }

        if (StringUtils.isNotBlank(template.getSharePassword())) {
            if (!template.getSharePassword().equals(password)) {
                return null;
            }
        }

        return template;
    }

    private void incrementShareViewCount(Long reportId) {
        try {
            ReportTemplate template = reportTemplateService.getById(reportId);
            if (template != null) {
                Long currentCount = template.getShareViewCount() != null ? template.getShareViewCount() : 0L;
                template.setShareViewCount(currentCount + 1);
                reportTemplateService.updateById(template);
            }
        } catch (Exception e) {
            log.warn("更新分享浏览次数失败", e);
        }
    }
}
