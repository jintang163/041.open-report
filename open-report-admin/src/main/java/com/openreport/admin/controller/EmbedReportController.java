package com.openreport.admin.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.service.DataSetService;
import com.openreport.admin.service.EmbedSsoService;
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

    @Autowired
    private EmbedSsoService embedSsoService;

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

    @ApiOperation("iframe嵌入页面（返回完整HTML，自动携带SSO Token）")
    @GetMapping("/{id}/iframe")
    public void getEmbedIframePage(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @ApiParam(value = "访问token") @RequestParam(required = false) String token,
            @ApiParam(value = "主题") @RequestParam(required = false, defaultValue = "default") String theme,
            @ApiParam(value = "是否隐藏头部") @RequestParam(required = false, defaultValue = "false") Boolean hideHeader,
            @ApiParam(value = "是否隐藏工具栏") @RequestParam(required = false, defaultValue = "false") Boolean hideToolbar,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String validToken = resolveToken(token, request);
        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null) {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(generateErrorHtml("报表不存在", theme));
            return;
        }
        if (!ReportStatusEnum.PUBLISHED.getCode().equals(template.getStatus())) {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(generateErrorHtml("报表未发布", theme));
            return;
        }

        String ssoToken = validToken;
        if (StringUtils.isBlank(ssoToken)) {
            try {
                Long userId = (Long) request.getAttribute("userId");
                String username = (String) request.getAttribute("username");
                if (userId != null) {
                    Map<String, Object> claims = new HashMap<>();
                    claims.put(CLAIM_KEY_REPORT_ID, id);
                    claims.put(CLAIM_KEY_TYPE, CLAIM_TYPE_EMBED);
                    claims.put("createBy", userId);
                    claims.put("createByUsername", username != null ? username : "embed_user");
                    ssoToken = jwtUtils.generateToken(claims);
                }
            } catch (Exception ignored) {
            }
        }

        String html = generateIframeHtml(template, ssoToken, theme, hideHeader, hideToolbar);
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.getWriter().write(html);
    }

    @ApiOperation("生成SSO嵌入链接（需要鉴权）")
    @GetMapping("/{id}/sso-link")
    public Result<Map<String, Object>> generateSsoLink(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @ApiParam(value = "有效期（秒），默认3600") @RequestParam(defaultValue = "3600") Long expireSeconds,
            @ApiParam(value = "主题") @RequestParam(required = false, defaultValue = "default") String theme,
            @ApiParam(value = "是否隐藏头部") @RequestParam(required = false, defaultValue = "false") Boolean hideHeader,
            @ApiParam(value = "是否隐藏工具栏") @RequestParam(required = false, defaultValue = "false") Boolean hideToolbar,
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
        claims.put("createByUsername", username != null ? username : "embed_user");
        claims.put("expireSeconds", expireSeconds);

        String ssoToken = jwtUtils.generateToken(claims);

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("/api/embed/").append(id).append("/iframe");
        urlBuilder.append("?token=").append(ssoToken);
        urlBuilder.append("&theme=").append(theme);
        urlBuilder.append("&hideHeader=").append(hideHeader);
        urlBuilder.append("&hideToolbar=").append(hideToolbar);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", id);
        result.put("reportName", template.getTemplateName());
        result.put("token", ssoToken);
        result.put("embedUrl", urlBuilder.toString());
        result.put("expireSeconds", expireSeconds);
        result.put("expireTime", System.currentTimeMillis() + expireSeconds * 1000);
        result.put("iframeCode", "<iframe src=\"" + urlBuilder.toString()
                + "\" width=\"100%\" height=\"600\" frameborder=\"0\" "
                + "allowfullscreen=\"true\" style=\"border:0;\"></iframe>");

        return Result.success(result);
    }

    private String generateIframeHtml(ReportTemplate template, String ssoToken, String theme,
                                       Boolean hideHeader, Boolean hideToolbar) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">\n");
        html.append("    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n");
        html.append("    <title>").append(template.getTemplateName()).append(" - OpenReport</title>\n");
        html.append("    <style>\n");
        html.append("        * { margin:0; padding:0; box-sizing:border-box; }\n");
        html.append("        html, body { width:100%; height:100%; overflow:hidden; font-family:-apple-system,BlinkMacSystemFont,");
        html.append("'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif; }\n");
        html.append("        .report-container { width:100%; height:100vh; display:flex; flex-direction:column; background:#f5f7fa; }\n");
        html.append("        .report-header { padding:12px 20px; background:#fff; border-bottom:1px solid #ebeef5;");
        html.append(" display:flex; align-items:center; justify-content:space-between; }\n");
        html.append("        .report-title { font-size:16px; font-weight:600; color:#303133; }\n");
        html.append("        .report-toolbar { display:flex; gap:8px; }\n");
        html.append("        .report-body { flex:1; overflow:auto; padding:16px; position:relative; }\n");
        html.append("        .report-loading { position:absolute; top:50%; left:50%; transform:translate(-50%,-50%);");
        html.append(" color:#909399; font-size:14px; }\n");
        html.append("        .report-footer { padding:8px 20px; background:#fff; border-top:1px solid #ebeef5;");
        html.append(" color:#c0c4cc; font-size:12px; text-align:right; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"report-container\" data-theme=\"").append(theme).append("\">\n");

        if (!Boolean.TRUE.equals(hideHeader)) {
            html.append("        <div class=\"report-header\">\n");
            html.append("            <div class=\"report-title\">").append(template.getTemplateName()).append("</div>\n");
            if (!Boolean.TRUE.equals(hideToolbar)) {
                html.append("            <div class=\"report-toolbar\">\n");
                html.append("                <button onclick=\"window.parent.postMessage({type:'refresh'},'*')\" ");
                html.append("style=\"padding:6px 12px;border:1px solid #dcdfe6;background:#fff;border-radius:4px;cursor:pointer;\">刷新</button>\n");
                html.append("                <button onclick=\"window.parent.postMessage({type:'export'},'*')\" ");
                html.append("style=\"padding:6px 12px;border:1px solid #409eff;background:#409eff;color:#fff;border-radius:4px;cursor:pointer;\">导出</button>\n");
                html.append("            </div>\n");
            }
            html.append("        </div>\n");
        }

        html.append("        <div class=\"report-body\" id=\"reportBody\">\n");
        html.append("            <div class=\"report-loading\">报表加载中...</div>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"report-footer\">Powered by OpenReport | ").append(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date())).append("</div>\n");
        html.append("    </div>\n");
        html.append("    <script>\n");
        html.append("        (function() {\n");
        html.append("            window.__OPENREPORT__ = {\n");
        html.append("                templateId: ").append(template.getId()).append(",\n");
        html.append("                templateName: '").append(template.getTemplateName().replace("'", "\\'")).append("',\n");
        html.append("                ssoToken: '").append(StringUtils.defaultString(ssoToken)).append("',\n");
        html.append("                theme: '").append(theme).append("',\n");
        html.append("                templateJson: ").append(StringUtils.defaultIfBlank(template.getTemplateJson(), "{}")).append("\n");
        html.append("            };\n");
        html.append("            window.addEventListener('message', function(e) {\n");
        html.append("                if (e.data && e.data.type === 'getToken') {\n");
        html.append("                    e.source.postMessage({type:'token', token: window.__OPENREPORT__.ssoToken}, '*');\n");
        html.append("                }\n");
        html.append("            });\n");
        html.append("            document.getElementById('reportBody').innerHTML = '<div style=\\\"padding:20px;text-align:center;color:#606266;\\\">报表加载完成，等待设计器渲染...</div>';\n");
        html.append("        })();\n");
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>");
        return html.toString();
    }

    private String generateErrorHtml(String message, String theme) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>错误</title>" +
                "<style>body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;" +
                "height:100vh;margin:0;background:#f5f7fa;color:#f56c6c;}" +
                ".box{padding:40px;background:#fff;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,.1);}" +
                "</style></head><body><div class='box'><h3>" + message + "</h3></div></body></html>";
    }

    @ApiOperation("SSO Token心跳续期")
    @PostMapping("/sso/heartbeat")
    public Result<Map<String, Object>> ssoHeartbeat(
            @ApiParam(value = "访问token") @RequestParam(required = false) String token,
            @ApiParam(value = "续期秒数") @RequestParam(defaultValue = "3600") Long expireSeconds,
            HttpServletRequest request) {

        String validToken = resolveToken(token, request);
        if (StringUtils.isBlank(validToken)) {
            return Result.failure(ResultCode.TOKEN_INVALID);
        }

        boolean renewed = embedSsoService.renewSsoToken(validToken, expireSeconds);
        if (!renewed) {
            return Result.failure(ResultCode.EMBED_TOKEN_EXPIRED, "Token已过期或无效，请重新获取");
        }

        Map<String, Object> tokenInfo = embedSsoService.getSsoTokenInfo(validToken);
        Map<String, Object> result = new HashMap<>();
        result.put("renewed", true);
        result.put("tokenInfo", tokenInfo);
        return Result.success(result);
    }

    @ApiOperation("获取SSO Token信息")
    @GetMapping("/sso/info")
    public Result<Map<String, Object>> getSsoInfo(
            @ApiParam(value = "访问token") @RequestParam(required = false) String token,
            HttpServletRequest request) {

        String validToken = resolveToken(token, request);
        if (StringUtils.isBlank(validToken)) {
            return Result.failure(ResultCode.TOKEN_INVALID);
        }

        Map<String, Object> tokenInfo = embedSsoService.getSsoTokenInfo(validToken);
        if (tokenInfo == null) {
            return Result.failure(ResultCode.EMBED_TOKEN_EXPIRED);
        }
        return Result.success(tokenInfo);
    }

    @ApiOperation("撤销SSO Token")
    @PostMapping("/sso/revoke")
    public Result<Void> revokeSso(
            @ApiParam(value = "访问token") @RequestParam(required = false) String token,
            HttpServletRequest request) {

        String validToken = resolveToken(token, request);
        if (StringUtils.isBlank(validToken)) {
            return Result.failure(ResultCode.TOKEN_INVALID);
        }

        embedSsoService.revokeSsoToken(validToken);
        return Result.success();
    }

    @ApiOperation("获取报表图表Base64图片（嵌入式）")
    @PostMapping("/report/{id}/chart")
    public Result<Map<String, Object>> getEmbedChartImage(
            @ApiParam(value = "报表ID", required = true) @PathVariable Long id,
            @ApiParam(value = "访问token") @RequestParam(required = false) String token,
            @ApiParam(value = "图表宽度") @RequestParam(defaultValue = "800") Integer width,
            @ApiParam(value = "图表高度") @RequestParam(defaultValue = "600") Integer height,
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
            return Result.failure(ResultCode.REPORT_NOT_PUBLISHED);
        }

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
                log.error("嵌入式获取图表数据失败", e);
            }
        }

        String svgChart = generateSvgChartImage(template, dataSetData, width, height);
        String chartBase64 = "data:image/svg+xml;base64," +
                java.util.Base64.getEncoder().encodeToString(svgChart.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", id);
        result.put("reportName", template.getTemplateName());
        result.put("chartImage", chartBase64);
        result.put("chartFormat", "svg+base64");
        result.put("width", width);
        result.put("height", height);
        result.put("data", dataSetData);

        return Result.success(result);
    }

    private String generateSvgChartImage(ReportTemplate template, Map<String, Object> dataSetData,
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
        int barHeight = 30;
        int barGap = 8;
        int leftMargin = 120;
        int chartWidth = width - leftMargin - 40;
        String[] colors = {"#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#909399"};

        for (Map.Entry<String, Object> entry : dataSetData.entrySet()) {
            if (yOffset > height - 60) break;
            svg.append("  <text x=\"10\" y=\"").append(yOffset)
               .append("\" font-size=\"14\" font-weight=\"600\" fill=\"#606266\">")
               .append(escapeHtml(entry.getKey())).append("</text>\n");
            yOffset += 25;

            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dsMap = (Map<String, Object>) entry.getValue();
                Object rowsObj = dsMap.get("rows");
                Object columnsObj = dsMap.get("columns");
                if (rowsObj instanceof List && columnsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) rowsObj;
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> columns = (List<Map<String, Object>>) columnsObj;
                    if (!rows.isEmpty() && columns.size() >= 2) {
                        String labelKey = columns.get(0).get("name") != null ? columns.get(0).get("name").toString() : "name";
                        String valueKey = columns.get(1).get("name") != null ? columns.get(1).get("name").toString() : "value";
                        double maxVal = rows.stream().mapToDouble(r -> {
                            Object v = r.get(valueKey);
                            return v instanceof Number ? Math.abs(((Number) v).doubleValue()) : 0;
                        }).max().orElse(1);
                        if (maxVal == 0) maxVal = 1;

                        int idx = 0;
                        for (Map<String, Object> r : rows) {
                            if (yOffset + barHeight > height - 60 || idx >= 10) break;
                            String label = r.get(labelKey) != null ? r.get(labelKey).toString() : "";
                            Object valObj = r.get(valueKey);
                            double val = valObj instanceof Number ? ((Number) valObj).doubleValue() : 0;
                            int barW = (int) ((Math.abs(val) / maxVal) * chartWidth);
                            String color = colors[idx % colors.length];
                            svg.append("  <text x=\"").append(leftMargin - 5).append("\" y=\"").append(yOffset + barHeight / 2 + 5)
                               .append("\" text-anchor=\"end\" font-size=\"12\" fill=\"#606266\">")
                               .append(escapeHtml(label)).append("</text>\n");
                            svg.append("  <rect x=\"").append(leftMargin).append("\" y=\"").append(yOffset)
                               .append("\" width=\"").append(Math.max(barW, 2)).append("\" height=\"").append(barHeight)
                               .append("\" fill=\"").append(color).append("\" rx=\"3\"/>\n");
                            svg.append("  <text x=\"").append(leftMargin + barW + 8).append("\" y=\"").append(yOffset + barHeight / 2 + 5)
                               .append("\" font-size=\"12\" fill=\"#909399\">")
                               .append(valObj != null ? valObj.toString() : "0").append("</text>\n");
                            yOffset += barHeight + barGap;
                            idx++;
                        }
                    }
                }
            }
            yOffset += 15;
        }

        svg.append("  <text x=\"").append(width / 2).append("\" y=\"").append(height - 15)
           .append("\" text-anchor=\"middle\" font-size=\"11\" fill=\"#c0c4cc\">OpenReport Embed</text>\n");
        svg.append("</svg>");
        return svg.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
