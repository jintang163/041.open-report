package com.openreport.scheduler.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.openreport.scheduler.entity.ReportLog;
import com.openreport.scheduler.service.ReportLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ReportExecuteListener {

    private static Logger logger = LoggerFactory.getLogger(ReportExecuteListener.class);

    @Value("${report.output.path:./output}")
    private String outputPath;

    @Autowired
    private ReportLogService reportLogService;

    @KafkaListener(topics = "report-execute-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String message) {
        logger.info("收到报表执行消息：{}", message);
        Long logId = null;
        long startTime = System.currentTimeMillis();
        try {
            JSONObject messageObj = JSON.parseObject(message);
            Long reportId = messageObj.getLong("reportId");
            String params = messageObj.getString("params");
            String executeType = messageObj.getString("executeType");
            String outputType = messageObj.getString("outputType");
            String emailList = messageObj.getString("emailList");

            ReportLog log = reportLogService.createLog(reportId, executeType, params);
            logId = log.getId();

            String outputFilePath = generateOutputFilePath(reportId, outputType);
            logger.info("开始执行报表，reportId: {}, outputType: {}, outputPath: {}", reportId, outputType, outputFilePath);

            Thread.sleep(1000);

            long costTime = System.currentTimeMillis() - startTime;
            reportLogService.updateLogSuccess(logId, costTime, outputFilePath);
            logger.info("报表执行成功，reportId: {}, costTime: {}ms", reportId, costTime);

            if ("EMAIL".equals(outputType) && emailList != null && !emailList.isEmpty()) {
                sendEmail(emailList, outputFilePath);
            }
        } catch (Exception e) {
            logger.error("报表执行失败", e);
            long costTime = System.currentTimeMillis() - startTime;
            if (logId != null) {
                reportLogService.updateLogFail(logId, costTime, e.getMessage());
            }
        }
    }

    private String generateOutputFilePath(Long reportId, String outputType) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String fileExt = "EXCEL".equals(outputType) ? ".xlsx" : ("PDF".equals(outputType) ? ".pdf" : ".html");
        String dirPath = outputPath + File.separator + dateStr;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dirPath + File.separator + "report_" + reportId + "_" + timeStr + fileExt;
    }

    private void sendEmail(String emailList, String filePath) {
        logger.info("发送邮件，收件人：{}，附件：{}", emailList, filePath);
    }
}
