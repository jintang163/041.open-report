package com.openreport.scheduler.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import cn.hutool.core.util.StrUtil;
import com.openreport.engine.service.ReportEngineService;
import com.openreport.scheduler.entity.ReportLog;
import com.openreport.scheduler.entity.ReportSchedule;
import com.openreport.scheduler.entity.ReportTemplateInfo;
import com.openreport.scheduler.mapper.ReportTemplateInfoMapper;
import com.openreport.scheduler.service.EmailService;
import com.openreport.scheduler.service.ReportLogService;
import com.openreport.scheduler.service.ReportScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ReportExecuteListener {

    private static final Logger logger = LoggerFactory.getLogger(ReportExecuteListener.class);

    private static final String OUTPUT_TYPE_EXCEL = "EXCEL";
    private static final String OUTPUT_TYPE_PDF = "PDF";
    private static final String OUTPUT_TYPE_EMAIL = "EMAIL";

    @Value("${report.output.path:./output}")
    private String outputPath;

    @Value("${report.email.senderName:Open Report}")
    private String senderName;

    @Autowired
    private ReportLogService reportLogService;

    @Autowired
    private ReportScheduleService reportScheduleService;

    @Autowired
    private ReportTemplateInfoMapper reportTemplateInfoMapper;

    @Autowired
    private ReportEngineService reportEngineService;

    @Autowired
    private EmailService emailService;

    @KafkaListener(topics = "report-execute-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String message) {
        logger.info("收到报表执行消息：{}", message);
        Long logId = null;
        long startTime = System.currentTimeMillis();
        try {
            JSONObject messageObj = JSON.parseObject(message);
            Long scheduleId = messageObj.getLong("scheduleId");
            Long reportId = messageObj.getLong("reportId");
            String params = messageObj.getString("params");
            String executeType = messageObj.getString("executeType");
            String outputType = messageObj.getString("outputType");
            String emailList = messageObj.getString("emailList");
            String emailCcList = messageObj.getString("emailCcList");
            String emailSubject = messageObj.getString("emailSubject");
            String emailContent = messageObj.getString("emailContent");
            Integer retryCount = messageObj.getInteger("retryCount");

            if (retryCount == null) {
                retryCount = 0;
            }

            ReportLog log = reportLogService.createLog(reportId, scheduleId, executeType, params, retryCount);
            logId = log.getId();

            Map<String, Object> paramMap = parseParams(params);

            ReportTemplateInfo template = reportTemplateInfoMapper.selectById(reportId);
            if (template == null) {
                throw new RuntimeException("报表模板不存在，reportId: " + reportId);
            }

            String actualOutputType = determineOutputType(outputType);
            String outputFilePath = generateOutputFile(template, paramMap, actualOutputType);
            logger.info("报表生成成功，reportId: {}, outputType: {}, outputPath: {}", reportId, actualOutputType, outputFilePath);

            long costTime = System.currentTimeMillis() - startTime;
            reportLogService.updateLogSuccess(logId, costTime, outputFilePath);

            if (needsEmail(outputType, emailList)) {
                sendReportEmail(template, emailList, emailCcList, emailSubject, emailContent, outputFilePath, actualOutputType);
            }

            if (scheduleId != null) {
                resetRetryCount(scheduleId);
            }

            logger.info("报表执行完成，reportId: {}, costTime: {}ms", reportId, costTime);
        } catch (Exception e) {
            logger.error("报表执行失败", e);
            long costTime = System.currentTimeMillis() - startTime;
            if (logId != null) {
                reportLogService.updateLogFail(logId, costTime, e.getMessage());
            }

            JSONObject messageObj = JSON.parseObject(message);
            Long scheduleId = messageObj.getLong("scheduleId");
            if (scheduleId != null) {
                scheduleRetry(scheduleId, messageObj, e.getMessage());
            }
        }
    }

    private Map<String, Object> parseParams(String params) {
        if (StrUtil.isBlank(params)) {
            return new HashMap<>();
        }
        try {
            return JSON.parseObject(params, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            logger.warn("解析参数失败，params: {}", params, e);
            return new HashMap<>();
        }
    }

    private String determineOutputType(String outputType) {
        if (OUTPUT_TYPE_EMAIL.equalsIgnoreCase(outputType)) {
            return OUTPUT_TYPE_EXCEL;
        }
        return outputType != null ? outputType.toUpperCase() : OUTPUT_TYPE_EXCEL;
    }

    private boolean needsEmail(String outputType, String emailList) {
        return (OUTPUT_TYPE_EMAIL.equalsIgnoreCase(outputType) || StrUtil.isNotBlank(emailList))
                && StrUtil.isNotBlank(emailList);
    }

    private String generateOutputFile(ReportTemplateInfo template, Map<String, Object> paramMap, String outputType) {
        byte[] fileData;
        String fileExt;

        if (OUTPUT_TYPE_PDF.equalsIgnoreCase(outputType)) {
            fileData = reportEngineService.exportPdf(template.getTemplateJson(), paramMap);
            fileExt = ".pdf";
        } else {
            fileData = reportEngineService.exportExcel(template.getTemplateJson(), paramMap);
            fileExt = ".xlsx";
        }

        String outputFilePath = generateOutputFilePath(template.getId(), outputType, fileExt);
        File outputFile = new File(outputFilePath);
        File parentDir = outputFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(fileData);
        } catch (Exception e) {
            throw new RuntimeException("写入输出文件失败: " + outputFilePath, e);
        }

        return outputFilePath;
    }

    private String generateOutputFilePath(Long reportId, String outputType, String fileExt) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String dirPath = outputPath + File.separator + dateStr;
        return dirPath + File.separator + "report_" + reportId + "_" + timeStr + fileExt;
    }

    private void sendReportEmail(ReportTemplateInfo template, String emailList, String emailCcList,
                                  String emailSubject, String emailContent, String attachmentPath, String outputType) {
        List<String> toList = parseEmails(emailList);
        List<String> ccList = parseEmails(emailCcList);

        if (toList.isEmpty()) {
            logger.warn("收件人列表为空，跳过邮件发送");
            return;
        }

        String subject = StrUtil.isNotBlank(emailSubject)
                ? emailSubject
                : "【" + senderName + "】" + template.getName() + " - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String content = buildEmailContent(template, emailContent, outputType);

        File attachment = new File(attachmentPath);
        if (!attachment.exists()) {
            logger.warn("附件文件不存在，跳过邮件发送: {}", attachmentPath);
            return;
        }

        boolean success = emailService.sendEmailWithAttachment(toList, ccList, subject, content, attachment);
        if (!success) {
            logger.error("报表邮件发送失败，reportId: {}, to: {}", template.getId(), toList);
            throw new RuntimeException("邮件发送失败");
        }
        logger.info("报表邮件发送成功，reportId: {}, to: {}", template.getId(), toList);
    }

    private String buildEmailContent(ReportTemplateInfo template, String customContent, String outputType) {
        String typeName = OUTPUT_TYPE_PDF.equalsIgnoreCase(outputType) ? "PDF" : "Excel";
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family: Arial, sans-serif; padding: 20px;'>");
        sb.append("<h2 style='color: #1890ff;'>").append(template.getName()).append("</h2>");
        sb.append("<p>您好！</p>");
        if (StrUtil.isNotBlank(customContent)) {
            sb.append("<p>").append(customContent.replace("\n", "<br/>")).append("</p>");
        } else {
            sb.append("<p>附件是系统自动生成的报表文件，请查收。</p>");
        }
        sb.append("<p style='color: #666; font-size: 12px; margin-top: 30px;'>");
        sb.append("文件格式：").append(typeName).append("<br/>");
        sb.append("生成时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("<br/>");
        sb.append("此邮件由系统自动发送，请勿直接回复。");
        sb.append("</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private List<String> parseEmails(String emails) {
        if (StrUtil.isBlank(emails)) {
            return new ArrayList<>();
        }
        return Arrays.stream(emails.split("[,;，；]"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    private void resetRetryCount(Long scheduleId) {
        try {
            ReportSchedule schedule = reportScheduleService.getById(scheduleId);
            if (schedule != null) {
                schedule.setRetryCount(0);
                reportScheduleService.updateById(schedule);
            }
        } catch (Exception e) {
            logger.warn("重置重试次数失败，scheduleId: {}", scheduleId, e);
        }
    }

    private void scheduleRetry(Long scheduleId, JSONObject messageObj, String errorMsg) {
        try {
            ReportSchedule schedule = reportScheduleService.getById(scheduleId);
            if (schedule == null) {
                return;
            }

            int maxRetry = schedule.getMaxRetryCount() != null ? schedule.getMaxRetryCount() : 3;
            int currentRetry = schedule.getRetryCount() != null ? schedule.getRetryCount() : 0;

            if (currentRetry >= maxRetry) {
                logger.warn("已达到最大重试次数，不再重试，scheduleId: {}, retryCount: {}", scheduleId, currentRetry);
                return;
            }

            int nextRetry = currentRetry + 1;
            schedule.setRetryCount(nextRetry);
            reportScheduleService.updateById(schedule);

            long delayMs = computeRetryDelay(nextRetry);
            messageObj.put("retryCount", nextRetry);
            messageObj.put("executeType", "RETRY");

            logger.info("调度重试任务，scheduleId: {}, 第{}次重试，延迟{}ms", scheduleId, nextRetry, delayMs);

            new Thread(() -> {
                try {
                    Thread.sleep(delayMs);
                    org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate =
                            ApplicationContextProvider.getBean(org.springframework.kafka.core.KafkaTemplate.class);
                    if (kafkaTemplate != null) {
                        kafkaTemplate.send("report-execute-topic", messageObj.toJSONString());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (Exception e) {
            logger.error("调度重试失败，scheduleId: {}", scheduleId, e);
        }
    }

    private long computeRetryDelay(int retryCount) {
        int baseSeconds = 5;
        int delaySeconds = baseSeconds * (int) Math.pow(2, retryCount - 1);
        int maxDelay = 3600;
        return Math.min(delaySeconds, maxDelay) * 1000L;
    }
}
