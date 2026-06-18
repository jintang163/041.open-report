package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openreport.admin.dto.TemplateVersionDiffDTO;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.entity.ReportTemplateSnapshot;
import com.openreport.admin.mapper.ReportTemplateMapper;
import com.openreport.admin.mapper.ReportTemplateSnapshotMapper;
import com.openreport.admin.service.ReportTemplateSnapshotService;
import com.openreport.common.enums.ReportStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ReportTemplateSnapshotServiceImpl extends ServiceImpl<ReportTemplateSnapshotMapper, ReportTemplateSnapshot>
        implements ReportTemplateSnapshotService {

    @Autowired
    private ReportTemplateMapper reportTemplateMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ReportTemplateSnapshot> listByTemplateId(Long templateId) {
        return baseMapper.listByTemplateId(templateId);
    }

    @Override
    public ReportTemplateSnapshot getByVersion(Long templateId, Integer version) {
        return baseMapper.getByVersion(templateId, version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplateSnapshot createSnapshot(Long templateId, Long userId, String userName, String changeLog) {
        ReportTemplate template = reportTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        Integer currentMaxVersion = baseMapper.getMaxVersion(templateId);
        int newVersion = currentMaxVersion == null ? 1 : currentMaxVersion + 1;

        ReportTemplateSnapshot snapshot = new ReportTemplateSnapshot();
        snapshot.setTemplateId(templateId);
        snapshot.setVersion(newVersion);
        snapshot.setTemplateName(template.getTemplateName());
        snapshot.setTemplateJson(template.getTemplateJson());
        snapshot.setDataSetBind(template.getDataSetBind());
        snapshot.setParamConfig(template.getParamConfig());
        snapshot.setDescription(template.getDescription());
        snapshot.setChangeLog(changeLog);
        snapshot.setStatus(template.getStatus());
        snapshot.setCreateBy(userId);
        snapshot.setCreateByName(userName);
        snapshot.setCreateTime(LocalDateTime.now());

        baseMapper.insert(snapshot);
        return snapshot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplateSnapshot rollbackToVersion(Long templateId, Integer version, Long userId, String userName) {
        ReportTemplateSnapshot targetSnapshot = baseMapper.getByVersion(templateId, version);
        if (targetSnapshot == null) {
            throw new RuntimeException("目标版本不存在");
        }

        ReportTemplate template = reportTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        template.setTemplateName(targetSnapshot.getTemplateName());
        template.setTemplateJson(targetSnapshot.getTemplateJson());
        template.setDataSetBind(targetSnapshot.getDataSetBind());
        template.setParamConfig(targetSnapshot.getParamConfig());
        template.setDescription(targetSnapshot.getDescription());
        template.setStatus(ReportStatusEnum.DRAFT.getCode());
        template.setUpdateBy(userId);
        template.setUpdateTime(LocalDateTime.now());
        reportTemplateMapper.updateById(template);

        Integer currentMaxVersion = baseMapper.getMaxVersion(templateId);
        int newVersion = currentMaxVersion == null ? 1 : currentMaxVersion + 1;

        ReportTemplateSnapshot rollbackSnapshot = new ReportTemplateSnapshot();
        rollbackSnapshot.setTemplateId(templateId);
        rollbackSnapshot.setVersion(newVersion);
        rollbackSnapshot.setTemplateName(targetSnapshot.getTemplateName());
        rollbackSnapshot.setTemplateJson(targetSnapshot.getTemplateJson());
        rollbackSnapshot.setDataSetBind(targetSnapshot.getDataSetBind());
        rollbackSnapshot.setParamConfig(targetSnapshot.getParamConfig());
        rollbackSnapshot.setDescription(targetSnapshot.getDescription());
        rollbackSnapshot.setChangeLog("回滚到版本 v" + version);
        rollbackSnapshot.setStatus(ReportStatusEnum.DRAFT.getCode());
        rollbackSnapshot.setCreateBy(userId);
        rollbackSnapshot.setCreateByName(userName);
        rollbackSnapshot.setCreateTime(LocalDateTime.now());

        baseMapper.insert(rollbackSnapshot);
        return rollbackSnapshot;
    }

    @Override
    public Integer getMaxVersion(Long templateId) {
        return baseMapper.getMaxVersion(templateId);
    }

    @Override
    public TemplateVersionDiffDTO compareVersions(Long templateId, Integer baseVersion, Integer targetVersion) {
        ReportTemplateSnapshot baseSnapshot = getByVersion(templateId, baseVersion);
        ReportTemplateSnapshot targetSnapshot = getByVersion(templateId, targetVersion);

        if (baseSnapshot == null || targetSnapshot == null) {
            throw new RuntimeException("版本不存在");
        }

        TemplateVersionDiffDTO diffDTO = new TemplateVersionDiffDTO();
        diffDTO.setTemplateId(templateId);
        diffDTO.setBaseVersion(baseVersion);
        diffDTO.setTargetVersion(targetVersion);
        diffDTO.setBaseVersionName(baseSnapshot.getTemplateName());
        diffDTO.setTargetVersionName(targetSnapshot.getTemplateName());
        diffDTO.setBaseCreateByName(baseSnapshot.getCreateByName());
        diffDTO.setTargetCreateByName(targetSnapshot.getCreateByName());
        diffDTO.setBaseCreateTime(baseSnapshot.getCreateTime());
        diffDTO.setTargetCreateTime(targetSnapshot.getCreateTime());

        List<TemplateVersionDiffDTO.DiffItem> diffItems = new ArrayList<>();

        compareField(diffItems, "templateName", "模板名称", "基本信息",
                baseSnapshot.getTemplateName(), targetSnapshot.getTemplateName());
        compareField(diffItems, "description", "描述", "基本信息",
                baseSnapshot.getDescription(), targetSnapshot.getDescription());
        compareField(diffItems, "dataSetBind", "数据集绑定", "数据配置",
                baseSnapshot.getDataSetBind(), targetSnapshot.getDataSetBind());
        compareField(diffItems, "paramConfig", "参数配置", "数据配置",
                baseSnapshot.getParamConfig(), targetSnapshot.getParamConfig());

        compareTemplateJsonDiff(diffItems, baseSnapshot.getTemplateJson(), targetSnapshot.getTemplateJson());

        diffDTO.setDiffItems(diffItems);
        return diffDTO;
    }

    @Override
    public ReportTemplateSnapshot getLatestPublishedVersion(Long templateId) {
        LambdaQueryWrapper<ReportTemplateSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportTemplateSnapshot::getTemplateId, templateId)
                .eq(ReportTemplateSnapshot::getStatus, ReportStatusEnum.PUBLISHED.getCode())
                .orderByDesc(ReportTemplateSnapshot::getVersion)
                .last("LIMIT 1");
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public ReportTemplateSnapshot previewPublish(Long templateId) {
        ReportTemplate template = reportTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        ReportTemplateSnapshot previewSnapshot = new ReportTemplateSnapshot();
        previewSnapshot.setTemplateId(templateId);
        previewSnapshot.setVersion(0);
        previewSnapshot.setTemplateName(template.getTemplateName());
        previewSnapshot.setTemplateJson(template.getTemplateJson());
        previewSnapshot.setDataSetBind(template.getDataSetBind());
        previewSnapshot.setParamConfig(template.getParamConfig());
        previewSnapshot.setDescription(template.getDescription());
        previewSnapshot.setChangeLog("发布预览");
        previewSnapshot.setStatus(template.getStatus());
        previewSnapshot.setCreateTime(LocalDateTime.now());

        return previewSnapshot;
    }

    private void compareField(List<TemplateVersionDiffDTO.DiffItem> diffItems,
                              String fieldName, String fieldLabel, String path,
                              String baseValue, String targetValue) {
        String baseStr = baseValue != null ? baseValue : "";
        String targetStr = targetValue != null ? targetValue : "";

        if (!Objects.equals(baseStr, targetStr)) {
            String diffType;
            if (baseStr.isEmpty() && !targetStr.isEmpty()) {
                diffType = "ADD";
            } else if (!baseStr.isEmpty() && targetStr.isEmpty()) {
                diffType = "DELETE";
            } else {
                diffType = "MODIFY";
            }
            diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                    fieldName, fieldLabel, baseValue, targetValue, diffType, path
            ));
        }
    }

    private void compareTemplateJsonDiff(List<TemplateVersionDiffDTO.DiffItem> diffItems,
                                         String baseJson, String targetJson) {
        String baseStr = baseJson != null ? baseJson : "";
        String targetStr = targetJson != null ? targetJson : "";

        if (baseStr.isEmpty() && targetStr.isEmpty()) {
            return;
        }

        if (baseStr.isEmpty()) {
            diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                    "templateJson", "模板内容", null, targetJson, "ADD", "模板内容"
            ));
            return;
        }

        if (targetStr.isEmpty()) {
            diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                    "templateJson", "模板内容", baseJson, null, "DELETE", "模板内容"
            ));
            return;
        }

        try {
            JsonNode baseNode = objectMapper.readTree(baseStr);
            JsonNode targetNode = objectMapper.readTree(targetJson);

            compareSheetsDiff(diffItems, baseNode, targetNode);
            compareArrayItems(diffItems, "conditionalFormats", "条件格式", "模板内容.条件格式",
                    baseNode.get("conditionalFormats"), targetNode.get("conditionalFormats"));
            compareArrayItems(diffItems, "charts", "图表配置", "模板内容.图表配置",
                    baseNode.get("charts"), targetNode.get("charts"));
            compareArrayItems(diffItems, "parameters", "参数定义", "模板内容.参数定义",
                    baseNode.get("parameters"), targetNode.get("parameters"));
            compareArrayItems(diffItems, "datasets", "数据集定义", "模板内容.数据集定义",
                    baseNode.get("datasets"), targetNode.get("datasets"));

        } catch (JsonProcessingException e) {
            if (!baseStr.equals(targetStr)) {
                diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                        "templateJson", "模板内容", baseJson, targetJson, "MODIFY", "模板内容"
                ));
            }
        }
    }

    private void compareSheetsDiff(List<TemplateVersionDiffDTO.DiffItem> diffItems,
                                   JsonNode baseNode, JsonNode targetNode) {
        JsonNode baseSheets = baseNode.get("sheets");
        JsonNode targetSheets = targetNode.get("sheets");

        if (baseSheets == null && targetSheets == null) return;

        int baseLen = baseSheets != null ? baseSheets.size() : 0;
        int targetLen = targetSheets != null ? targetSheets.size() : 0;

        if (baseLen != targetLen) {
            diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                    "sheets", "工作表数量",
                    String.valueOf(baseLen), String.valueOf(targetLen), "MODIFY", "模板内容.工作表"
            ));
        }

        int minLen = Math.min(baseLen, targetLen);
        for (int i = 0; i < minLen; i++) {
            JsonNode baseSheet = baseSheets != null ? baseSheets.get(i) : null;
            JsonNode targetSheet = targetSheets != null ? targetSheets.get(i) : null;
            String sheetName = (baseSheet != null ? baseSheet.path("name").asText() :
                    (targetSheet != null ? targetSheet.path("name").asText() : "Sheet" + (i + 1)));
            String pathPrefix = "模板内容.工作表." + sheetName;

            if (baseSheet == null || targetSheet == null) continue;

            compareJsonNodeField(diffItems, "name", "工作表名称", pathPrefix,
                    baseSheet.get("name"), targetSheet.get("name"));
            compareJsonNodeField(diffItems, "frozen", "冻结配置", pathPrefix,
                    baseSheet.get("frozen"), targetSheet.get("frozen"));

            compareCellsDiff(diffItems, pathPrefix, baseSheet, targetSheet);
            compareColumnRowConfig(diffItems, pathPrefix, baseSheet, targetSheet);
        }
    }

    private void compareCellsDiff(List<TemplateVersionDiffDTO.DiffItem> diffItems,
                                  String pathPrefix, JsonNode baseSheet, JsonNode targetSheet) {
        JsonNode baseCells = baseSheet.get("cells");
        JsonNode targetCells = targetSheet.get("cells");

        if (baseCells == null && targetCells == null) return;

        java.util.Map<String, JsonNode> baseCellMap = buildCellMap(baseCells);
        java.util.Map<String, JsonNode> targetCellMap = buildCellMap(targetCells);

        java.util.Set<String> allKeys = new java.util.LinkedHashSet<>();
        allKeys.addAll(baseCellMap.keySet());
        allKeys.addAll(targetCellMap.keySet());

        for (String key : allKeys) {
            JsonNode baseCell = baseCellMap.get(key);
            JsonNode targetCell = targetCellMap.get(key);
            String cellPath = pathPrefix + ".单元格[" + key + "]";

            if (baseCell == null && targetCell != null) {
                diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                        "cell", "单元格 " + key, null, truncateJson(targetCell), "ADD", cellPath
                ));
            } else if (baseCell != null && targetCell == null) {
                diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                        "cell", "单元格 " + key, truncateJson(baseCell), null, "DELETE", cellPath
                ));
            } else if (baseCell != null && targetCell != null && !baseCell.equals(targetCell)) {
                List<String> subDiffs = findCellSubDiff(baseCell, targetCell);
                for (String subDiff : subDiffs) {
                    diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                            "cell", "单元格 " + key + " " + subDiff,
                            truncateJson(baseCell), truncateJson(targetCell), "MODIFY", cellPath
                    ));
                }
                if (subDiffs.isEmpty()) {
                    diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                            "cell", "单元格 " + key,
                            truncateJson(baseCell), truncateJson(targetCell), "MODIFY", cellPath
                    ));
                }
            }
        }
    }

    private java.util.Map<String, JsonNode> buildCellMap(JsonNode cells) {
        java.util.Map<String, JsonNode> map = new java.util.LinkedHashMap<>();
        if (cells == null || !cells.isArray()) return map;
        for (JsonNode cell : cells) {
            String key = cell.path("row").asText() + "," + cell.path("col").asText();
            map.put(key, cell);
        }
        return map;
    }

    private List<String> findCellSubDiff(JsonNode baseCell, JsonNode targetCell) {
        List<String> diffs = new ArrayList<>();
        String[] cellFields = {"value", "formula", "expression"};
        for (String field : cellFields) {
            JsonNode baseVal = baseCell.get(field);
            JsonNode targetVal = targetCell.get(field);
            if (!Objects.equals(baseVal, targetVal)) {
                diffs.add(field + ": " + nullableStr(baseVal) + " → " + nullableStr(targetVal));
            }
        }
        JsonNode baseStyle = baseCell.get("style");
        JsonNode targetStyle = targetCell.get("style");
        if (!Objects.equals(baseStyle, targetStyle)) {
            diffs.add("样式变更");
        }
        JsonNode baseBinding = baseCell.get("dataBinding");
        JsonNode targetBinding = targetCell.get("dataBinding");
        if (!Objects.equals(baseBinding, targetBinding)) {
            diffs.add("数据绑定变更");
        }
        return diffs;
    }

    private String nullableStr(JsonNode node) {
        return node == null || node.isNull() ? "空" : node.asText();
    }

    private String truncateJson(JsonNode node) {
        if (node == null) return null;
        String str = node.toString();
        return str.length() > 300 ? str.substring(0, 300) + "..." : str;
    }

    private void compareColumnRowConfig(List<TemplateVersionDiffDTO.DiffItem> diffItems,
                                        String pathPrefix, JsonNode baseSheet, JsonNode targetSheet) {
        JsonNode baseConfig = baseSheet.get("columnWidths");
        JsonNode targetConfig = targetSheet.get("columnWidths");
        if (!Objects.equals(baseConfig, targetConfig)) {
            diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                    "columnWidths", "列宽配置",
                    truncateJson(baseConfig), truncateJson(targetConfig), "MODIFY", pathPrefix
            ));
        }

        baseConfig = baseSheet.get("rowHeights");
        targetConfig = targetSheet.get("rowHeights");
        if (!Objects.equals(baseConfig, targetConfig)) {
            diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                    "rowHeights", "行高配置",
                    truncateJson(baseConfig), truncateJson(targetConfig), "MODIFY", pathPrefix
            ));
        }

        baseConfig = baseSheet.get("mergedCells");
        targetConfig = targetSheet.get("mergedCells");
        if (!Objects.equals(baseConfig, targetConfig)) {
            diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                    "mergedCells", "合并单元格",
                    truncateJson(baseConfig), truncateJson(targetConfig), "MODIFY", pathPrefix
            ));
        }
    }

    private void compareJsonNodeField(List<TemplateVersionDiffDTO.DiffItem> diffItems,
                                      String fieldName, String fieldLabel, String path,
                                      JsonNode baseVal, JsonNode targetVal) {
        if (!Objects.equals(baseVal, targetVal)) {
            String diffType = (baseVal == null || baseVal.isNull()) ? "ADD" :
                    (targetVal == null || targetVal.isNull()) ? "DELETE" : "MODIFY";
            diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                    fieldName, fieldLabel,
                    baseVal == null ? null : baseVal.asText(),
                    targetVal == null ? null : targetVal.asText(),
                    diffType, path
            ));
        }
    }

    private void compareArrayItems(List<TemplateVersionDiffDTO.DiffItem> diffItems,
                                   String fieldName, String fieldLabel, String path,
                                   JsonNode baseArr, JsonNode targetArr) {
        if (Objects.equals(baseArr, targetArr)) return;

        int baseLen = baseArr != null && baseArr.isArray() ? baseArr.size() : 0;
        int targetLen = targetArr != null && targetArr.isArray() ? targetArr.size() : 0;

        if (baseLen != targetLen) {
            diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                    fieldName, fieldLabel + "数量",
                    String.valueOf(baseLen), String.valueOf(targetLen), "MODIFY", path
            ));
        }

        int minLen = Math.min(baseLen, targetLen);
        for (int i = 0; i < minLen; i++) {
            JsonNode baseItem = baseArr.get(i);
            JsonNode targetItem = targetArr.get(i);
            if (!baseItem.equals(targetItem)) {
                String itemName = baseItem.path("name").asText(
                        baseItem.path("id").asText(fieldLabel + "[" + i + "]"));
                diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                        fieldName, fieldLabel + " - " + itemName,
                        truncateJson(baseItem), truncateJson(targetItem), "MODIFY", path + "." + itemName
                ));
            }
        }

        for (int i = minLen; i < targetLen; i++) {
            JsonNode item = targetArr.get(i);
            String itemName = item.path("name").asText(fieldLabel + "[" + i + "]");
            diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                    fieldName, fieldLabel + " - " + itemName,
                    null, truncateJson(item), "ADD", path + "." + itemName
            ));
        }

        for (int i = minLen; i < baseLen; i++) {
            JsonNode item = baseArr.get(i);
            String itemName = item.path("name").asText(fieldLabel + "[" + i + "]");
            diffItems.add(new TemplateVersionDiffDTO.DiffItem(
                    fieldName, fieldLabel + " - " + itemName,
                    truncateJson(item), null, "DELETE", path + "." + itemName
            ));
        }
    }
}
