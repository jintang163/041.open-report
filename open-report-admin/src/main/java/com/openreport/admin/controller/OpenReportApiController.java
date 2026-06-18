package com.openreport.admin.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.entity.SysApiKey;
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
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = "开放报表API（外部系统调用）")
@RestController
@RequestMapping("/api/open")
public class OpenReportApiController {

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private JwtUtils jwtUtils;

    @ApiOperation("获取报表JSON数据")
    @PostMapping("/report/{id}/data")
    public Result<Map<String, Object>> getReportData(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> params,
            HttpServletRequest request) {

        SysApiKey apiKey = (SysApiKey) request.getAttribute("apiKeyObj");
        if (apiKey == null) {
            return Result.failure(ResultCode.API_KEY_REQUIRED);
        }

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return Result.failure(ResultCode.REPORT_NOT_FOUND);
        }
        if (!ReportStatusEnum.PUBLISHED.getCode().equals(template.getStatus())) {
            return Result.failure(ResultCode.REPORT_NOT_PUBLISHED);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", id);
        result.put("reportName", template.getTemplateName());
        result.put("reportCode", template.getTemplateCode());
        result.put("description", template.getDescription());

        Map<String, Object> dataSetData = new LinkedHashMap<>();
        if (template.getDataSetBind() != null) {
            try {
                List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> binding : bindings) {
                    Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
                    String bindName = binding.get("bindName") != null
                            ? binding.get("bindName").toString() : "dataSet" + dataSetId;
                    Map<String, Object> previewResult = dataSetService.previewData(dataSetId, params, null);
                    dataSetData.put(bindName, previewResult);
                }
            } catch (Exception e) {
                log.error("开放API获取报表数据失败, reportId={}", id, e);
                result.put("error", "数据获取失败: " + e.getMessage());
            }
        }
        result.put("data", dataSetData);
        result.put("queryTime", System.currentTimeMillis());

        return Result.success(result);
    }

    @ApiOperation("获取报表HTML片段")
    @PostMapping("/report/{id}/html")
    public void getReportHtml(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> params,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        SysApiKey apiKey = (SysApiKey) request.getAttribute("apiKeyObj");

        response.setContentType("text/html;charset=UTF-8");

        if (apiKey == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(buildHtmlError("API Key无效", "请通过X-API-Key请求头提供有效的API Key"));
            return;
        }

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(buildHtmlError("报表不存在", "报表ID: " + id + " 未找到"));
            return;
        }
        if (!ReportStatusEnum.PUBLISHED.getCode().equals(template.getStatus())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(buildHtmlError("报表未发布", "该报表尚未发布，无法通过API访问"));
            return;
        }

        Map<String, Object> dataSetData = new LinkedHashMap<>();
        if (template.getDataSetBind() != null) {
            try {
                List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> binding : bindings) {
                    Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
                    String bindName = binding.get("bindName") != null
                            ? binding.get("bindName").toString() : "dataSet" + dataSetId;
                    Map<String, Object> previewResult = dataSetService.previewData(dataSetId, params, null);
                    dataSetData.put(bindName, previewResult);
                }
            } catch (Exception e) {
                log.error("开放API获取报表HTML失败, reportId={}", id, e);
            }
        }

        String html = buildReportHtml(template, dataSetData, params);
        response.getWriter().write(html);
    }

    @ApiOperation("获取图表Base64图片")
    @PostMapping("/report/{id}/chart")
    public Result<Map<String, Object>> getReportChart(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> params,
            @ApiParam(value = "图表宽度", required = false) @RequestParam(defaultValue = "800") Integer width,
            @ApiParam(value = "图表高度", required = false) @RequestParam(defaultValue = "600") Integer height,
            HttpServletRequest request) {

        SysApiKey apiKey = (SysApiKey) request.getAttribute("apiKeyObj");
        if (apiKey == null) {
            return Result.failure(ResultCode.API_KEY_REQUIRED);
        }

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return Result.failure(ResultCode.REPORT_NOT_FOUND);
        }
        if (!ReportStatusEnum.PUBLISHED.getCode().equals(template.getStatus())) {
            return Result.failure(ResultCode.REPORT_NOT_PUBLISHED);
        }

        Map<String, Object> dataSetData = new LinkedHashMap<>();
        if (template.getDataSetBind() != null) {
            try {
                List<Map<String, Object>> bindings = JSON.parseObject(template.getDataSetBind(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> binding : bindings) {
                    Long dataSetId = Long.valueOf(binding.get("dataSetId").toString());
                    String bindName = binding.get("bindName") != null
                            ? binding.get("bindName").toString() : "dataSet" + dataSetId;
                    Map<String, Object> previewResult = dataSetService.previewData(dataSetId, params, null);
                    dataSetData.put(bindName, previewResult);
                }
            } catch (Exception e) {
                log.error("开放API获取图表数据失败, reportId={}", id, e);
            }
        }

        String chartBase64 = generateChartBase64(template, dataSetData, width, height);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", id);
        result.put("reportName", template.getTemplateName());
        result.put("chartImage", chartBase64);
        result.put("chartFormat", "png");
        result.put("width", width);
        result.put("height", height);
        result.put("data", dataSetData);
        result.put("queryTime", System.currentTimeMillis());

        return Result.success(result);
    }

    @ApiOperation("获取报表参数配置（公开接口）")
    @GetMapping("/report/{id}/params")
    public Result<Map<String, Object>> getReportParams(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {

        SysApiKey apiKey = (SysApiKey) request.getAttribute("apiKeyObj");
        if (apiKey == null) {
            return Result.failure(ResultCode.API_KEY_REQUIRED);
        }

        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            return Result.failure(ResultCode.REPORT_NOT_FOUND);
        }
        if (!ReportStatusEnum.PUBLISHED.getCode().equals(template.getStatus())) {
            return Result.failure(ResultCode.REPORT_NOT_PUBLISHED);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", id);
        result.put("reportName", template.getTemplateName());
        result.put("reportCode", template.getTemplateCode());
        result.put("description", template.getDescription());

        List<Map<String, Object>> paramList = new ArrayList<>();
        if (template.getParamConfig() != null) {
            try {
                paramList = JSON.parseObject(template.getParamConfig(),
                        new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ignored) {
            }
        }
        result.put("params", paramList);

        return Result.success(result);
    }

    @ApiOperation("批量获取报表列表（公开接口）")
    @GetMapping("/reports")
    public Result<List<Map<String, Object>>> listReports(
            @ApiParam(value = "分类") @RequestParam(required = false) Integer templateType,
            HttpServletRequest request) {

        SysApiKey apiKey = (SysApiKey) request.getAttribute("apiKeyObj");
        if (apiKey == null) {
            return Result.failure(ResultCode.API_KEY_REQUIRED);
        }

        List<ReportTemplate> templates = reportTemplateService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReportTemplate>()
                        .eq(ReportTemplate::getStatus, ReportStatusEnum.PUBLISHED.getCode())
                        .eq(templateType != null, ReportTemplate::getTemplateType, templateType)
                        .orderByDesc(ReportTemplate::getUpdateTime)
                        .select(ReportTemplate::getId, ReportTemplate::getTemplateName,
                                ReportTemplate::getTemplateCode, ReportTemplate::getTemplateType,
                                ReportTemplate::getDescription, ReportTemplate::getUpdateTime));

        List<Map<String, Object>> result = templates.stream().map(t -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", t.getId());
            item.put("name", t.getTemplateName());
            item.put("code", t.getTemplateCode());
            item.put("type", t.getTemplateType());
            item.put("description", t.getDescription());
            item.put("updateTime", t.getUpdateTime());
            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    private String buildReportHtml(ReportTemplate template, Map<String, Object> dataSetData,
                                    Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"openreport-container\" data-report-id=\"").append(template.getId()).append("\">\n");

        sb.append("  <div class=\"openreport-header\">\n");
        sb.append("    <h2 class=\"openreport-title\">").append(escapeHtml(template.getTemplateName())).append("</h2>\n");
        if (StringUtils.isNotBlank(template.getDescription())) {
            sb.append("    <p class=\"openreport-desc\">").append(escapeHtml(template.getDescription())).append("</p>\n");
        }
        sb.append("  </div>\n");

        sb.append("  <div class=\"openreport-body\" id=\"reportBody\">\n");

        String templateJson = template.getTemplateJson();
        if (StringUtils.isNotBlank(templateJson)) {
            sb.append("    <div class=\"openreport-designer\" data-config='")
              .append(escapeAttr(templateJson)).append("'");
            if (!dataSetData.isEmpty()) {
                sb.append(" data-datasets='").append(escapeAttr(JSON.toJSONString(dataSetData))).append("'");
            }
            if (params != null && !params.isEmpty()) {
                sb.append(" data-params='").append(escapeAttr(JSON.toJSONString(params))).append("'");
            }
            sb.append("></div>\n");
        }

        if (!dataSetData.isEmpty()) {
            sb.append("    <div class=\"openreport-tables\">\n");
            for (Map.Entry<String, Object> entry : dataSetData.entrySet()) {
                sb.append("      <div class=\"openreport-dataset\" data-name=\"").append(entry.getKey()).append("\">\n");
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dsMap = (Map<String, Object>) entry.getValue();
                    Object rows = dsMap.get("rows");
                    Object columns = dsMap.get("columns");
                    if (rows instanceof List && columns instanceof List) {
                        sb.append(buildTableHtml(entry.getKey(), (List<?>) columns, (List<?>) rows));
                    }
                }
                sb.append("      </div>\n");
            }
            sb.append("    </div>\n");
        }

        sb.append("  </div>\n");
        sb.append("  <div class=\"openreport-footer\">\n");
        sb.append("    <span>Powered by OpenReport</span>\n");
        sb.append("    <span>").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("</span>\n");
        sb.append("  </div>\n");
        sb.append("</div>");

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String buildTableHtml(String name, List<?> columns, List<?> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("        <table class=\"openreport-table\" border=\"1\" cellpadding=\"6\" cellspacing=\"0\" style=\"border-collapse:collapse;width:100%;\">\n");
        sb.append("          <thead><tr>\n");
        for (Object col : columns) {
            Map<String, Object> c = (Map<String, Object>) col;
            String title = c.get("title") != null ? c.get("title").toString() : (c.get("name") != null ? c.get("name").toString() : "");
            sb.append("            <th>").append(escapeHtml(title)).append("</th>\n");
        }
        sb.append("          </tr></thead>\n");
        sb.append("          <tbody>\n");
        for (Object row : rows) {
            Map<String, Object> r = (Map<String, Object>) row;
            sb.append("            <tr>\n");
            for (Object col : columns) {
                Map<String, Object> c = (Map<String, Object>) col;
                String key = c.get("dataIndex") != null ? c.get("dataIndex").toString() : (c.get("name") != null ? c.get("name").toString() : "");
                Object val = r.get(key);
                sb.append("              <td>").append(val != null ? escapeHtml(val.toString()) : "").append("</td>\n");
            }
            sb.append("            </tr>\n");
        }
        sb.append("          </tbody>\n");
        sb.append("        </table>\n");
        return sb.toString();
    }

    private String generateChartBase64(ReportTemplate template, Map<String, Object> dataSetData,
                                        int width, int height) {
        String templateJson = template.getTemplateJson();
        if (StringUtils.isBlank(templateJson) || dataSetData.isEmpty()) {
            return generatePlaceholderImage(width, height, template.getTemplateName());
        }

        String svgChart = generateSvgChart(template, dataSetData, width, height);
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svgChart.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String generateSvgChart(ReportTemplate template, Map<String, Object> dataSetData,
                                     int width, int height) {
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
           .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ").append(width).append(" ").append(height).append("\">\n");

        svg.append("  <rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");

        svg.append("  <text x=\"").append(width / 2).append("\" y=\"40\" text-anchor=\"middle\" ")
           .append("font-size=\"20\" font-weight=\"bold\" fill=\"#303133\">")
           .append(escapeHtml(template.getTemplateName())).append("</text>\n");

        int yOffset = 70;
        int maxBars = 10;
        int barHeight = 30;
        int barGap = 8;
        int leftMargin = 120;
        int rightMargin = 40;
        int chartWidth = width - leftMargin - rightMargin;

        for (Map.Entry<String, Object> entry : dataSetData.entrySet()) {
            if (yOffset + maxBars * (barHeight + barGap) > height - 60) break;

            svg.append("  <text x=\"10\" y=\"").append(yOffset)
               .append("\" font-size=\"14\" font-weight=\"600\" fill=\"#606266\">")
               .append(escapeHtml(entry.getKey())).append("</text>\n");
            yOffset += 25;

            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dsMap = (Map<String, Object>) entry.getValue();
                Object columnsObj = dsMap.get("columns");
                Object rowsObj = dsMap.get("rows");
                if (rowsObj instanceof List && columnsObj instanceof List) {
                    List<?> rows = (List<?>) rowsObj;
                    List<?> columns = (List<?>) columnsObj;
                    if (!rows.isEmpty() && !columns.isEmpty()) {
                        String labelKey = ((Map<String, Object>) columns.get(0)).get("name") != null
                                ? ((Map<String, Object>) columns.get(0)).get("name").toString() : "name";
                        String valueKey = columns.size() > 1 && ((Map<String, Object>) columns.get(1)).get("name") != null
                                ? ((Map<String, Object>) columns.get(1)).get("name").toString() : "value";

                        double maxValue = 0;
                        List<Map<String, Object>> chartRows = new ArrayList<>();
                        int count = 0;
                        for (Object row : rows) {
                            if (count >= maxBars) break;
                            @SuppressWarnings("unchecked")
                            Map<String, Object> r = (Map<String, Object>) row;
                            chartRows.add(r);
                            Object val = r.get(valueKey);
                            if (val instanceof Number) {
                                maxValue = Math.max(maxValue, Math.abs(((Number) val).doubleValue()));
                            }
                            count++;
                        }
                        if (maxValue == 0) maxValue = 1;

                        String[] colors = {"#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#909399", "#5DADE2", "#58D68D", "#F4D03F", "#EC7063", "#AAB7B8"};
                        int barIdx = 0;
                        for (Map<String, Object> r : chartRows) {
                            if (yOffset + barHeight > height - 60) break;
                            String label = r.get(labelKey) != null ? r.get(labelKey).toString() : "";
                            Object valObj = r.get(valueKey);
                            double val = valObj instanceof Number ? ((Number) valObj).doubleValue() : 0;
                            int barWidth = (int) ((Math.abs(val) / maxValue) * chartWidth);
                            String color = colors[barIdx % colors.length];

                            svg.append("  <text x=\"").append(leftMargin - 5).append("\" y=\"").append(yOffset + barHeight / 2 + 5)
                               .append("\" text-anchor=\"end\" font-size=\"12\" fill=\"#606266\">")
                               .append(escapeHtml(label)).append("</text>\n");
                            svg.append("  <rect x=\"").append(leftMargin).append("\" y=\"").append(yOffset)
                               .append("\" width=\"").append(Math.max(barWidth, 2)).append("\" height=\"").append(barHeight)
                               .append("\" fill=\"").append(color).append("\" rx=\"3\"/>\n");
                            svg.append("  <text x=\"").append(leftMargin + barWidth + 8).append("\" y=\"").append(yOffset + barHeight / 2 + 5)
                               .append("\" font-size=\"12\" fill=\"#909399\">")
                               .append(valObj != null ? valObj.toString() : "0").append("</text>\n");

                            yOffset += barHeight + barGap;
                            barIdx++;
                        }
                    }
                }
            }
            yOffset += 15;
        }

        svg.append("  <text x=\"").append(width / 2).append("\" y=\"").append(height - 15)
           .append("\" text-anchor=\"middle\" font-size=\"11\" fill=\"#c0c4cc\">")
           .append("OpenReport API Chart").append("</text>\n");
        svg.append("</svg>");
        return svg.toString();
    }

    private String generatePlaceholderImage(int width, int height, String reportName) {
        String svg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "\" height=\"" + height + "\">" +
                "<rect width=\"100%\" height=\"100%\" fill=\"#f5f7fa\"/>" +
                "<text x=\"50%\" y=\"45%\" text-anchor=\"middle\" font-size=\"18\" fill=\"#909399\">" + escapeHtml(reportName) + "</text>" +
                "<text x=\"50%\" y=\"55%\" text-anchor=\"middle\" font-size=\"13\" fill=\"#c0c4cc\">暂无图表数据</text>" +
                "</svg>";
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String buildHtmlError(String title, String detail) {
        return "<div style=\"font-family:sans-serif;padding:40px;text-align:center;color:#f56c6c;\">" +
                "<h3>" + title + "</h3>" +
                "<p style=\"color:#909399;\">" + detail + "</p></div>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String escapeAttr(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
