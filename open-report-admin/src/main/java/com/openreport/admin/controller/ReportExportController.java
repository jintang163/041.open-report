package com.openreport.admin.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openreport.admin.config.RequirePerms;
import com.openreport.admin.entity.DataSet;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.ReportTemplateService;
import com.openreport.common.enums.ReportStatusEnum;
import com.openreport.common.result.Result;
import com.openreport.common.result.ResultCode;
import com.openreport.common.utils.JwtUtils;
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

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Api(tags = "报表导出打印")
@RestController
@RequestMapping("/api/export")
public class ReportExportController {

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ExcelExporter excelExporter;

    @Autowired
    private PdfExporter pdfExporter;

    @Autowired
    private JwtUtils jwtUtils;

    @ApiOperation("导出Excel")
    @GetMapping("/report/{id}/excel")
    @RequirePerms("report:manage:export")
    public ResponseEntity<byte[]> exportExcel(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @ApiParam(value = "访问token") @RequestParam(required = false) String token,
            HttpServletRequest request) {

        AuthResult authResult = checkAuth(token, request, id);
        if (!authResult.isSuccess()) {
            return ResponseEntity.status(401).build();
        }

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> exportData = collectExportData(template, extractParams(request));
        byte[] bytes = buildExcelBytes(template, exportData, extractParams(request));

        String fileName = encodeFileName(template.getTemplateName() + ".xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @ApiOperation("导出PDF")
    @GetMapping("/report/{id}/pdf")
    @RequirePerms("report:manage:export")
    public ResponseEntity<byte[]> exportPdf(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @ApiParam(value = "访问token") @RequestParam(required = false) String token,
            HttpServletRequest request) {

        AuthResult authResult = checkAuth(token, request, id);
        if (!authResult.isSuccess()) {
            return ResponseEntity.status(401).build();
        }

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> exportData = collectExportData(template, extractParams(request));
        byte[] bytes = buildPdfBytes(template, exportData, extractParams(request));

        String fileName = encodeFileName(template.getTemplateName() + ".pdf");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @ApiOperation("导出HTML")
    @GetMapping("/report/{id}/html")
    @RequirePerms("report:manage:export")
    public ResponseEntity<String> exportHtml(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @ApiParam(value = "访问token") @RequestParam(required = false) String token,
            HttpServletRequest request) {

        AuthResult authResult = checkAuth(token, request, id);
        if (!authResult.isSuccess()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> params = extractParams(request);
        Map<String, Object> dataSets = collectDataSetData(template, params);

        String html = buildHtmlDocument(template, dataSets);
        String fileName = encodeFileName(template.getTemplateName() + ".html");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @ApiOperation("批量导出")
    @PostMapping("/report/batch")
    @RequirePerms("report:manage:export")
    public Result<List<Map<String, Object>>> batchExport(
            @ApiParam(value = "批量导出参数", required = true) @RequestBody BatchExportRequest batchRequest,
            HttpServletRequest request) {

        if (batchRequest == null || batchRequest.getReportIds() == null || batchRequest.getReportIds().isEmpty()) {
            return Result.failure(ResultCode.BAD_REQUEST, "报表ID列表不能为空");
        }

        String exportType = StringUtils.isBlank(batchRequest.getExportType()) ? "excel" : batchRequest.getExportType();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Long reportId : batchRequest.getReportIds()) {
            Map<String, Object> item = new HashMap<>();
            item.put("reportId", reportId);
            try {
                ReportTemplate template = reportTemplateService.getById(reportId);
                if (template == null) {
                    item.put("success", false);
                    item.put("message", "报表不存在");
                    results.add(item);
                    continue;
                }
                if (!ReportStatusEnum.PUBLISHED.getCode().equals(template.getStatus())) {
                    item.put("success", false);
                    item.put("message", "报表未发布");
                    results.add(item);
                    continue;
                }

                Map<String, Object> exportData = collectExportData(template, batchRequest.getParams());
                String downloadUrl = "/api/export/report/" + reportId + "/" + exportType;
                String fileName = template.getTemplateName() + "." + getFileExtension(exportType);

                item.put("success", true);
                item.put("reportName", template.getTemplateName());
                item.put("exportType", exportType);
                item.put("fileName", fileName);
                item.put("downloadUrl", downloadUrl);
                item.put("exportData", exportData);

                try {
                    if ("excel".equalsIgnoreCase(exportType)) {
                        byte[] bytes = buildExcelBytes(template, exportData, batchRequest.getParams());
                        item.put("fileSize", bytes.length);
                    } else if ("pdf".equalsIgnoreCase(exportType)) {
                        byte[] bytes = buildPdfBytes(template, exportData, batchRequest.getParams());
                        item.put("fileSize", bytes.length);
                    }
                } catch (Exception e) {
                    log.warn("Batch export pre-build failed, fallback to link only, reportId: {}", reportId, e);
                }
            } catch (Exception e) {
                log.error("批量导出报表失败, reportId: {}", reportId, e);
                item.put("success", false);
                item.put("message", e.getMessage());
            }
            results.add(item);
        }

        return Result.success(results);
    }

    private static final long BIG_DATA_EXPORT_THRESHOLD = 100_000L;
    private static final int EXPORT_BATCH_SIZE = 1000;

    private AuthResult checkAuth(String paramToken, HttpServletRequest request, Long reportId) {
        String token = resolveToken(paramToken, request);
        if (StringUtils.isBlank(token)) {
            return AuthResult.fail();
        }

        if (jwtUtils.validateToken(token)) {
            try {
                io.jsonwebtoken.Claims claims = jwtUtils.parseToken(token);
                String type = claims.get("type") != null ? claims.get("type").toString() : null;
                if ("embed".equals(type)) {
                    Object tokenReportId = claims.get("reportId");
                    if (tokenReportId != null && reportId.equals(Long.valueOf(tokenReportId.toString()))) {
                        return AuthResult.ok();
                    }
                } else {
                    return AuthResult.ok();
                }
            } catch (Exception ignored) {
            }
        }

        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            return AuthResult.ok();
        }

        return AuthResult.fail();
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

    private Map<String, Object> extractParams(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            if ("token".equalsIgnoreCase(key)) {
                continue;
            }
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                params.put(key, values.length == 1 ? values[0] : values);
            }
        }
        return params;
    }

    private Map<String, Object> collectExportData(ReportTemplate template, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("templateName", template.getTemplateName());
        result.put("templateId", template.getId());
        result.put("dataSets", collectDataSetData(template, params));
        return result;
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
                    long total = dataSetService.countData(dataSetId, params);
                    if (total >= 0 && total > BIG_DATA_EXPORT_THRESHOLD) {
                        Map<String, Object> sample = dataSetService.previewData(dataSetId, params, 1);
                        sample.put("total", total);
                        sample.put("isBigData", true);
                        dataSetData.put(bindName, sample);
                    } else {
                        Map<String, Object> previewResult = dataSetService.previewDataWithCount(dataSetId, params, null);
                        dataSetData.put(bindName, previewResult);
                    }
                }
            } catch (Exception e) {
                log.error("解析数据集绑定失败", e);
            }
        }
        return dataSetData;
    }

    private byte[] buildExcelBytes(ReportTemplate template, Map<String, Object> exportData,
                                   Map<String, Object> params) {
        Map<String, Object> dataSets = (Map<String, Object>) exportData.get("dataSets");
        if (dataSets != null && !dataSets.isEmpty()) {
            Map.Entry<String, Object> firstEntry = dataSets.entrySet().iterator().next();
            String bindName = firstEntry.getKey();
            Object dataObj = firstEntry.getValue();
            if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                Object rows = dataMap.get("rows");
                Object totalObj = dataMap.get("total");
                long total = -1;
                if (totalObj instanceof Number) {
                    total = ((Number) totalObj).longValue();
                }

                Long dataSetId = resolveDataSetId(template, bindName);
                if (dataSetId != null && (total < 0 || total > BIG_DATA_EXPORT_THRESHOLD)) {
                    if (total < 0) {
                        total = dataSetService.countData(dataSetId, params);
                    }
                    if (total > BIG_DATA_EXPORT_THRESHOLD) {
                        return buildExcelStreaming(template, dataSetId, params, total);
                    }
                }

                if (rows instanceof List) {
                    List<Map<String, Object>> rowList = (List<Map<String, Object>>) rows;
                    return excelExporter.exportDataSet(template.getTemplateName(), rowList);
                }
            }
        }
        return excelExporter.exportDataSet(template.getTemplateName(), new ArrayList<>());
    }

    private Long resolveDataSetId(ReportTemplate template, String bindName) {
        if (template.getDataSetBind() == null) return null;
        try {
            List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                    new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> b : bindings) {
                String name = b.get("bindName") != null ? b.get("bindName").toString()
                        : ("dataSet" + b.get("dataSetId"));
                if (name.equals(bindName)) {
                    return Long.valueOf(b.get("dataSetId").toString());
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private byte[] buildExcelStreaming(ReportTemplate template, Long dataSetId,
                                       Map<String, Object> params, long total) {
        log.info("Streaming Excel export for reportId={}, dataSetId={}, total={}",
                template.getId(), dataSetId, total);

        final java.util.concurrent.atomic.AtomicReference<List<String>> headersRef =
                new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicBoolean headerReady =
                new java.util.concurrent.atomic.AtomicBoolean(false);

        dataSetService.streamBatchData(dataSetId, params, EXPORT_BATCH_SIZE, batch -> {
            if (!headerReady.get() && !batch.isEmpty()) {
                headersRef.set(new ArrayList<>(batch.get(0).keySet()));
                headerReady.set(true);
            }
        });

        if (!headerReady.get()) {
            headersRef.set(new ArrayList<>());
        }

        return excelExporter.exportDataSetStreaming(template.getTemplateName(),
                (int) Math.min(total, Integer.MAX_VALUE), headersRef.get(),
                writer -> {
                    dataSetService.streamBatchData(dataSetId, params, EXPORT_BATCH_SIZE, writer::writeBatch);
                });
    }

    private byte[] buildPdfBytes(ReportTemplate template, Map<String, Object> exportData,
                                  Map<String, Object> params) {
        Map<String, Object> dataSets = (Map<String, Object>) exportData.get("dataSets");
        if (dataSets != null && !dataSets.isEmpty()) {
            Map.Entry<String, Object> firstEntry = dataSets.entrySet().iterator().next();
            String bindName = firstEntry.getKey();
            Object dataObj = firstEntry.getValue();
            if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                Object totalObj = dataMap.get("total");
                long total = -1;
                if (totalObj instanceof Number) {
                    total = ((Number) totalObj).longValue();
                }
                Long dataSetId = resolveDataSetId(template, bindName);
                if (dataSetId != null && total < 0) {
                    total = dataSetService.countData(dataSetId, params);
                }
                if (total > BIG_DATA_EXPORT_THRESHOLD) {
                    log.warn("PDF export for reportId={}, total={} exceeds threshold, using Excel streaming recommended",
                            template.getId(), total);
                    final List<Map<String, Object>> limitedRows = new ArrayList<>(Math.min((int) total, 10000));
                    final int[] count = {0};
                    dataSetService.streamBatchData(dataSetId, params, 1000, batch -> {
                        for (Map<String, Object> row : batch) {
                            if (count[0] < 10000) {
                                limitedRows.add(row);
                                count[0]++;
                            } else {
                                break;
                            }
                        }
                    });
                    return pdfExporter.exportDataSet(template.getTemplateName() + "(前10000行)", limitedRows);
                }

                Object rows = dataMap.get("rows");
                if (rows instanceof List) {
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

    private String getFileExtension(String exportType) {
        switch (exportType.toLowerCase()) {
            case "pdf":
                return "pdf";
            case "html":
                return "html";
            case "excel":
            default:
                return "xlsx";
        }
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

    public static class BatchExportRequest {
        private List<Long> reportIds;
        private String exportType;
        private Map<String, Object> params;

        public List<Long> getReportIds() {
            return reportIds;
        }

        public void setReportIds(List<Long> reportIds) {
            this.reportIds = reportIds;
        }

        public String getExportType() {
            return exportType;
        }

        public void setExportType(String exportType) {
            this.exportType = exportType;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public void setParams(Map<String, Object> params) {
            this.params = params;
        }
    }

    private static class AuthResult {
        private final boolean success;

        private AuthResult(boolean success) {
            this.success = success;
        }

        public static AuthResult ok() {
            return new AuthResult(true);
        }

        public static AuthResult fail() {
            return new AuthResult(false);
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
