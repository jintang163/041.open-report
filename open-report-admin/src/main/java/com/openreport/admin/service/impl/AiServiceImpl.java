package com.openreport.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.openreport.admin.config.AiProperties;
import com.openreport.admin.dto.AiGenerateRequest;
import com.openreport.admin.dto.AiGenerateResult;
import com.openreport.admin.service.AiService;
import com.openreport.admin.service.DataSourceConfigService;
import com.openreport.admin.entity.DataSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    private RestTemplate restTemplate = new RestTemplate();

    @Override
    public AiGenerateResult generateReport(AiGenerateRequest request) {
        if (!aiProperties.getEnabled() || aiProperties.getApiKey() == null) {
            return generateMockResult(request);
        }
        try {
            return callAiApi(request, true);
        } catch (Exception e) {
            log.error("AI生成报表失败，使用Mock数据: {}", e.getMessage());
            return generateMockResult(request);
        }
    }

    @Override
    public AiGenerateResult generateSqlOnly(AiGenerateRequest request) {
        if (!aiProperties.getEnabled() || aiProperties.getApiKey() == null) {
            AiGenerateResult result = generateMockResult(request);
            result.setCharts(null);
            return result;
        }
        try {
            return callAiApi(request, false);
        } catch (Exception e) {
            log.error("AI生成SQL失败，使用Mock数据: {}", e.getMessage());
            AiGenerateResult result = generateMockResult(request);
            result.setCharts(null);
            return result;
        }
    }

    @Override
    public boolean isEnabled() {
        return aiProperties.getEnabled() && aiProperties.getApiKey() != null;
    }

    private AiGenerateResult callAiApi(AiGenerateRequest request, boolean includeCharts) {
        String systemPrompt = buildSystemPrompt(request, includeCharts);
        String userPrompt = buildUserPrompt(request);

        JSONObject body = new JSONObject();
        body.put("model", aiProperties.getModel());
        body.put("temperature", aiProperties.getTemperature());
        body.put("max_tokens", aiProperties.getMaxTokens());

        JSONArray messages = new JSONArray();
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                aiProperties.getApiUrl(),
                HttpMethod.POST,
                entity,
                String.class
        );

        JSONObject responseJson = JSON.parseObject(response.getBody());
        String content = responseJson.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        return parseAiResponse(content);
    }

    private String buildSystemPrompt(AiGenerateRequest request, boolean includeCharts) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的报表生成助手。根据用户的自然语言描述，生成对应的SQL查询和图表建议。\n\n");
        sb.append("要求：\n");
        sb.append("1. SQL必须是可执行的标准SQL\n");
        sb.append("2. 字段命名清晰，有业务含义\n");
        sb.append("3. 只返回JSON格式的数据，不要有其他解释文字\n");

        if (includeCharts) {
            sb.append("4. 提供2-3个最合适的图表建议，图表类型可选：bar(柱状图)、line(折线图)、pie(饼图)、area(面积图)、radar(雷达图)、scatter(散点图)\n");
        }

        sb.append("\n返回JSON格式：\n");
        sb.append("{\n");
        sb.append("  \"reportTitle\": \"报表标题\",\n");
        sb.append("  \"sql\": \"SELECT ...\",\n");
        sb.append("  \"description\": \"报表说明\",\n");
        sb.append("  \"fields\": [\n");
        sb.append("    { \"name\": \"字段名\", \"type\": \"STRING|NUMBER|DATE\", \"label\": \"中文标签\" }\n");
        sb.append("  ]");
        if (includeCharts) {
            sb.append(",\n  \"charts\": [\n");
            sb.append("    { \"chartType\": \"bar\", \"title\": \"图表标题\", \"xField\": \"分类字段\", \"yFields\": [\"数值字段\"], \"description\": \"图表说明\" }\n");
            sb.append("  ]");
        }
        sb.append("\n}\n");

        return sb.toString();
    }

    private String buildUserPrompt(AiGenerateRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户需求：").append(request.getPrompt()).append("\n\n");

        if (request.getDsId() != null) {
            DataSourceConfig ds = dataSourceConfigService.getById(request.getDsId());
            if (ds != null) {
                sb.append("数据源：").append(ds.getDsName()).append(" (").append(ds.getDsType()).append(")\n");
            }
        }

        if (request.getSchemaInfo() != null && !request.getSchemaInfo().isEmpty()) {
            sb.append("可用表结构：\n").append(request.getSchemaInfo()).append("\n");
        }

        if (request.getTableNames() != null && !request.getTableNames().isEmpty()) {
            sb.append("涉及的表：").append(request.getTableNames()).append("\n");
        }

        return sb.toString();
    }

    private AiGenerateResult parseAiResponse(String content) {
        try {
            String jsonStr = extractJson(content);
            return JSON.parseObject(jsonStr, AiGenerateResult.class);
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", e.getMessage());
            throw new RuntimeException("AI响应格式解析失败", e);
        }
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private AiGenerateResult generateMockResult(AiGenerateRequest request) {
        AiGenerateResult result = new AiGenerateResult();
        result.setReportTitle(request.getPrompt() != null ? request.getPrompt() : "AI生成报表");
        result.setDescription("AI智能生成的报表，包含核心业务指标分析。（当前为演示模式数据）");

        String prompt = request.getPrompt() != null ? request.getPrompt().toLowerCase() : "";

        if (prompt.contains("销售") || prompt.contains("sale")) {
            result.setSql("SELECT \n" +
                    "  product_name AS product,\n" +
                    "  SUM(sales_amount) AS total_sales,\n" +
                    "  COUNT(order_id) AS order_count,\n" +
                    "  DATE_FORMAT(order_date, '%Y-%m') AS month\n" +
                    "FROM sales_order\n" +
                    "WHERE order_date >= DATE_SUB(NOW(), INTERVAL 3 MONTH)\n" +
                    "GROUP BY product_name, DATE_FORMAT(order_date, '%Y-%m')\n" +
                    "ORDER BY total_sales DESC\n" +
                    "LIMIT 10");

            List<AiGenerateResult.FieldInfo> fields = new ArrayList<>();
            fields.add(createField("product", "STRING", "产品名称"));
            fields.add(createField("total_sales", "NUMBER", "销售总额"));
            fields.add(createField("order_count", "NUMBER", "订单数量"));
            fields.add(createField("month", "STRING", "月份"));
            result.setFields(fields);

            List<AiGenerateResult.ChartSuggestion> charts = new ArrayList<>();
            charts.add(createChart("bar", "产品销售额TOP10", "product", Arrays.asList("total_sales"), "展示各产品的销售额对比"));
            charts.add(createChart("line", "月度销售趋势", "month", Arrays.asList("total_sales", "order_count"), "近三个月的销售趋势变化"));
            charts.add(createChart("pie", "销售占比分布", "product", Arrays.asList("total_sales"), "各产品的销售额占比"));
            result.setCharts(charts);

        } else if (prompt.contains("用户") || prompt.contains("user") || prompt.contains("客户")) {
            result.setSql("SELECT \n" +
                    "  DATE_FORMAT(register_time, '%Y-%m-%d') AS date,\n" +
                    "  COUNT(user_id) AS new_users,\n" +
                    "  SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) AS active_users\n" +
                    "FROM user_info\n" +
                    "WHERE register_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)\n" +
                    "GROUP BY DATE_FORMAT(register_time, '%Y-%m-%d')\n" +
                    "ORDER BY date");

            List<AiGenerateResult.FieldInfo> fields = new ArrayList<>();
            fields.add(createField("date", "STRING", "日期"));
            fields.add(createField("new_users", "NUMBER", "新增用户"));
            fields.add(createField("active_users", "NUMBER", "活跃用户"));
            result.setFields(fields);

            List<AiGenerateResult.ChartSuggestion> charts = new ArrayList<>();
            charts.add(createChart("line", "用户增长趋势", "date", Arrays.asList("new_users", "active_users"), "近30天用户增长和活跃情况"));
            charts.add(createChart("area", "用户累计趋势", "date", Arrays.asList("new_users"), "新增用户的面积图展示"));
            result.setCharts(charts);

        } else if (prompt.contains("库存") || prompt.contains("stock") || prompt.contains("库存")) {
            result.setSql("SELECT \n" +
                    "  category_name AS category,\n" +
                    "  product_name AS product,\n" +
                    "  stock_quantity AS stock,\n" +
                    "  warning_quantity AS warning,\n" +
                    "  (stock_quantity - warning_quantity) AS available\n" +
                    "FROM product_stock\n" +
                    "WHERE status = 1\n" +
                    "ORDER BY stock_quantity ASC\n" +
                    "LIMIT 20");

            List<AiGenerateResult.FieldInfo> fields = new ArrayList<>();
            fields.add(createField("category", "STRING", "分类"));
            fields.add(createField("product", "STRING", "产品"));
            fields.add(createField("stock", "NUMBER", "库存量"));
            fields.add(createField("warning", "NUMBER", "预警值"));
            fields.add(createField("available", "NUMBER", "可用量"));
            result.setFields(fields);

            List<AiGenerateResult.ChartSuggestion> charts = new ArrayList<>();
            charts.add(createChart("bar", "库存预警TOP20", "product", Arrays.asList("stock", "warning"), "库存低于预警线的产品"));
            charts.add(createChart("pie", "分类库存占比", "category", Arrays.asList("stock"), "各分类的库存占比"));
            result.setCharts(charts);

        } else {
            result.setSql("SELECT \n" +
                    "  t1.dim_date AS stat_date,\n" +
                    "  t1.metric1 AS metric1,\n" +
                    "  t1.metric2 AS metric2,\n" +
                    "  t1.metric3 AS metric3\n" +
                    "FROM business_metrics t1\n" +
                    "WHERE t1.dim_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)\n" +
                    "ORDER BY t1.dim_date");

            List<AiGenerateResult.FieldInfo> fields = new ArrayList<>();
            fields.add(createField("stat_date", "STRING", "统计日期"));
            fields.add(createField("metric1", "NUMBER", "指标一"));
            fields.add(createField("metric2", "NUMBER", "指标二"));
            fields.add(createField("metric3", "NUMBER", "指标三"));
            result.setFields(fields);

            List<AiGenerateResult.ChartSuggestion> charts = new ArrayList<>();
            charts.add(createChart("line", "指标趋势分析", "stat_date", Arrays.asList("metric1", "metric2", "metric3"), "核心指标的趋势变化"));
            charts.add(createChart("bar", "指标对比", "stat_date", Arrays.asList("metric1", "metric2"), "多指标对比分析"));
            result.setCharts(charts);
        }

        return result;
    }

    private AiGenerateResult.FieldInfo createField(String name, String type, String label) {
        AiGenerateResult.FieldInfo field = new AiGenerateResult.FieldInfo();
        field.setName(name);
        field.setType(type);
        field.setLabel(label);
        return field;
    }

    private AiGenerateResult.ChartSuggestion createChart(String chartType, String title,
                                                          String xField, List<String> yFields, String description) {
        AiGenerateResult.ChartSuggestion chart = new AiGenerateResult.ChartSuggestion();
        chart.setChartType(chartType);
        chart.setTitle(title);
        chart.setxField(xField);
        chart.setYFields(yFields);
        chart.setDescription(description);
        return chart;
    }
}
