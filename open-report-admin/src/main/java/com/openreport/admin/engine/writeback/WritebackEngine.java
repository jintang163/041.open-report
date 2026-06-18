package com.openreport.admin.engine.writeback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openreport.admin.dto.writeback.CellDataChange;
import com.openreport.admin.dto.writeback.DataSubmitRequest;
import com.openreport.admin.dto.writeback.DataSubmitResult;
import com.openreport.admin.dto.writeback.ValidationResult;
import com.openreport.admin.entity.*;
import com.openreport.admin.enums.RowStatusEnum;
import com.openreport.admin.enums.SubmitStatusEnum;
import com.openreport.admin.mapper.*;
import com.openreport.common.exception.BusinessException;
import com.openreport.engine.datasource.DynamicDataSourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WritebackEngine {

    @Autowired
    private DataValidator dataValidator;

    @Autowired
    private SqlGenerator sqlGenerator;

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    @Autowired
    private DataSourceConfigMapper dataSourceConfigMapper;

    @Autowired
    private ReportWritebackConfigMapper writebackConfigMapper;

    @Autowired
    private ReportWritebackFieldMapper writebackFieldMapper;

    @Autowired
    private ReportWritebackHistoryMapper historyMapper;

    @Autowired
    private ReportWritebackDetailMapper detailMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.openreport.admin.websocket.WebSocketPushService pushService;

    private static final DateTimeFormatter BATCH_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public DataSubmitResult executeSubmit(DataSubmitRequest request, Long userId) {
        if (request.getReportId() == null) {
            throw new BusinessException("报表ID不能为空");
        }
        if (CollectionUtils.isEmpty(request.getChanges())) {
            throw new BusinessException("没有需要提交的数据");
        }

        Long configId = request.getConfigId();
        ReportWritebackConfig config;
        if (configId != null) {
            config = writebackConfigMapper.selectById(configId);
            if (config == null) {
                throw new BusinessException("回写配置不存在");
            }
        } else {
            List<ReportWritebackConfig> configs = writebackConfigMapper.selectByReportId(request.getReportId());
            if (CollectionUtils.isEmpty(configs)) {
                throw new BusinessException("报表未配置回写规则");
            }
            config = configs.get(0);
            configId = config.getId();
        }

        List<ReportWritebackField> fields = writebackFieldMapper.selectByConfigId(configId);
        if (CollectionUtils.isEmpty(fields)) {
            throw new BusinessException("回写配置未设置字段映射");
        }

        Map<String, ReportWritebackField> fieldMap = fields.stream()
                .collect(Collectors.toMap(ReportWritebackField::getFieldName, f -> f, (a, b) -> a));

        String batchNo = generateBatchNo();
        long startTime = System.currentTimeMillis();

        ReportWritebackHistory history = createHistory(request, config, batchNo, userId);

        List<ValidationResult> validationResults = new ArrayList<>();
        for (CellDataChange change : request.getChanges()) {
            Map<String, Object> rowData = buildRowData(change, fieldMap);
            ValidationResult result = dataValidator.validateRow(
                    config, fields, rowData, change.getRowStatus(), change.getRowIndex());
            validationResults.add(result);
        }

        List<ValidationResult> invalidResults = validationResults.stream()
                .filter(r -> !r.isValid())
                .collect(Collectors.toList());

        if (!invalidResults.isEmpty()) {
            String errorMsg = invalidResults.stream()
                    .flatMap(r -> r.getErrors().stream())
                    .collect(Collectors.joining("; "));

            history.setStatus(SubmitStatusEnum.FAIL.getCode());
            history.setErrorMsg(errorMsg);
            history.setTotalCount(request.getChanges().size());
            history.setFailCount(request.getChanges().size());
            history.setExecuteTime(System.currentTimeMillis() - startTime);
            historyMapper.updateById(history);

            throw new BusinessException("数据校验失败：" + errorMsg);
        }

        boolean transactionEnable = config.getTransactionEnable() != null && config.getTransactionEnable() == 1;
        TransactionStatus txStatus = null;
        if (transactionEnable) {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setName("WritebackTx_" + batchNo);
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            txStatus = transactionManager.getTransaction(def);
        }

        DataSubmitResult result = new DataSubmitResult();
        result.setBatchNo(batchNo);
        result.setTotalCount(request.getChanges().size());

        List<DataSubmitResult.DetailResult> detailResults = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();

        try {
            DataSource dataSource = getTargetDataSource(config);

            try (Connection conn = dataSource.getConnection()) {
                if (transactionEnable) {
                    conn.setAutoCommit(false);
                }

                for (int i = 0; i < request.getChanges().size(); i++) {
                    CellDataChange change = request.getChanges().get(i);
                    DataSubmitResult.DetailResult detailResult = new DataSubmitResult.DetailResult();
                    detailResult.setRowIndex(change.getRowIndex());
                    detailResult.setRowStatus(change.getRowStatus());

                    ReportWritebackDetail detail = new ReportWritebackDetail();
                    detail.setHistoryId(history.getId());
                    detail.setRowIndex(change.getRowIndex());
                    detail.setRowStatus(change.getRowStatus());

                    try {
                        Map<String, Object> rowData = buildRowData(change, fieldMap);
                        Map<String, Object> oldData = change.getOldData();

                        String pkValue = rowData.get(config.getPrimaryKeyField()) != null
                                ? rowData.get(config.getPrimaryKeyField()).toString() : null;
                        detail.setPrimaryKeyValue(pkValue);
                        detail.setOldData(toJson(oldData));
                        detail.setNewData(toJson(rowData));

                        String sql = sqlGenerator.generateSql(config, fields, rowData, change.getRowStatus());
                        List<Object> params = sqlGenerator.getParameters(config, fields, rowData, change.getRowStatus());

                        detailResult.setExecuteSql(formatSqlWithParams(sql, params));
                        detail.setExecuteSql(detailResult.getExecuteSql());

                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            sqlGenerator.setParameters(ps, params);
                            int affectedRows = ps.executeUpdate();

                            if (affectedRows == 0 && RowStatusEnum.UPDATE.getCode().equals(change.getRowStatus())) {
                                throw new BusinessException("数据已被修改或不存在，请刷新后重试");
                            }
                        }

                        detail.setStatus(SubmitStatusEnum.SUCCESS.getCode());
                        detailResult.setStatus(SubmitStatusEnum.SUCCESS.getCode());
                        successCount++;

                    } catch (Exception e) {
                        log.error("Execute row writeback failed, rowIndex: {}", change.getRowIndex(), e);
                        detail.setStatus(SubmitStatusEnum.FAIL.getCode());
                        detail.setErrorMsg(e.getMessage());
                        detailResult.setStatus(SubmitStatusEnum.FAIL.getCode());
                        detailResult.setErrorMsg(e.getMessage());
                        failCount++;

                        if (errorMsg.length() > 0) {
                            errorMsg.append("; ");
                        }
                        errorMsg.append("第").append(change.getRowIndex()).append("行：").append(e.getMessage());

                        if (transactionEnable) {
                            throw e;
                        }
                    }

                    detailMapper.insert(detail);
                    detailResults.add(detailResult);
                }

                if (transactionEnable) {
                    conn.commit();
                }
            } catch (SQLException e) {
                log.error("Database operation failed", e);
                if (transactionEnable) {
                    throw e;
                }
            }

            if (transactionEnable && txStatus != null) {
                transactionManager.commit(txStatus);
            }

            history.setSuccessCount(successCount);
            history.setFailCount(failCount);
            history.setExecuteTime(System.currentTimeMillis() - startTime);

            if (failCount == 0) {
                history.setStatus(SubmitStatusEnum.SUCCESS.getCode());
                result.setStatus(SubmitStatusEnum.SUCCESS.getCode());
            } else if (successCount == 0) {
                history.setStatus(SubmitStatusEnum.FAIL.getCode());
                history.setErrorMsg(errorMsg.toString());
                result.setStatus(SubmitStatusEnum.FAIL.getCode());
                result.setErrorMsg(errorMsg.toString());
            } else {
                history.setStatus(SubmitStatusEnum.PARTIAL.getCode());
                history.setErrorMsg(errorMsg.toString());
                result.setStatus(SubmitStatusEnum.PARTIAL.getCode());
                result.setErrorMsg(errorMsg.toString());
            }

            historyMapper.updateById(history);

        } catch (Exception e) {
            log.error("Writeback transaction failed", e);
            if (transactionEnable && txStatus != null) {
                transactionManager.rollback(txStatus);
            }

            history.setStatus(SubmitStatusEnum.FAIL.getCode());
            history.setErrorMsg(e.getMessage());
            history.setTotalCount(request.getChanges().size());
            history.setFailCount(request.getChanges().size());
            history.setExecuteTime(System.currentTimeMillis() - startTime);
            historyMapper.updateById(history);

            result.setStatus(SubmitStatusEnum.FAIL.getCode());
            result.setErrorMsg(e.getMessage());
            result.setFailCount(request.getChanges().size());
            result.setSuccessCount(0);
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setExecuteTime(System.currentTimeMillis() - startTime);
        result.setDetails(detailResults);

        if (successCount > 0 && request.getReportId() != null) {
            try {
                pushService.pushDataChange(request.getReportId());
            } catch (Exception ex) {
                log.warn("回写提交后推送数据变更失败: reportId={}", request.getReportId(), ex);
            }
        }

        return result;
    }

    private Map<String, Object> buildRowData(CellDataChange change, Map<String, ReportWritebackField> fieldMap) {
        Map<String, Object> rowData = new HashMap<>();

        if (change.getNewData() != null && !change.getNewData().isEmpty()) {
            rowData.putAll(change.getNewData());
        }

        if (change.getCellValues() != null && !change.getCellValues().isEmpty()) {
            for (Map.Entry<String, String> entry : change.getCellValues().entrySet()) {
                String cellPosition = entry.getKey();
                String value = entry.getValue();
                for (ReportWritebackField field : fieldMap.values()) {
                    if (cellPosition.equalsIgnoreCase(field.getCellPosition())) {
                        rowData.put(field.getFieldName(), value);
                        break;
                    }
                }
            }
        }

        return rowData;
    }

    private DataSource getTargetDataSource(ReportWritebackConfig config) {
        Long dataSourceId = config.getDataSourceId();
        if (dataSourceId == null) {
            throw new BusinessException("回写配置未设置数据源");
        }

        String dsIdStr = dataSourceId.toString();
        if (dynamicDataSourceManager.exists(dsIdStr)) {
            return dynamicDataSourceManager.getDataSource(dsIdStr);
        }

        DataSourceConfig dsConfig = dataSourceConfigMapper.selectById(dataSourceId);
        if (dsConfig == null) {
            throw new BusinessException("数据源不存在: " + dataSourceId);
        }

        return dynamicDataSourceManager.createDataSource(
                dsIdStr,
                dsConfig.getDsName(),
                dsConfig.getDriverClass(),
                dsConfig.getJdbcUrl(),
                dsConfig.getUsername(),
                dsConfig.getPassword()
        );
    }

    private ReportWritebackHistory createHistory(DataSubmitRequest request,
                                                  ReportWritebackConfig config,
                                                  String batchNo,
                                                  Long userId) {
        ReportWritebackHistory history = new ReportWritebackHistory();
        history.setReportId(request.getReportId());
        history.setConfigId(config.getId());
        history.setBatchNo(batchNo);
        history.setTotalCount(request.getChanges().size());
        history.setSuccessCount(0);
        history.setFailCount(0);
        history.setStatus(SubmitStatusEnum.PROCESSING.getCode());
        history.setParams(toJson(request.getParams()));
        history.setCreateTime(LocalDateTime.now());
        history.setCreateBy(userId);
        history.setDeleted(0);
        historyMapper.insert(history);
        return history;
    }

    private String generateBatchNo() {
        return "WB" + LocalDateTime.now().format(BATCH_NO_FORMATTER)
                + String.format("%04d", new Random().nextInt(10000));
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to json", e);
            return obj.toString();
        }
    }

    private String formatSqlWithParams(String sql, List<Object> params) {
        if (CollectionUtils.isEmpty(params)) {
            return sql;
        }
        String formatted = sql;
        for (Object param : params) {
            String paramStr = param == null ? "NULL" :
                    (param instanceof String || param instanceof java.time.temporal.TemporalAccessor)
                            ? "'" + param.toString().replace("'", "''") + "'"
                            : param.toString();
            formatted = formatted.replaceFirst("\\?", paramStr);
        }
        return formatted;
    }
}
