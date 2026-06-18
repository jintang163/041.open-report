package com.openreport.admin.websocket;

import com.openreport.admin.entity.ReportApproval;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.entity.ReportTemplateSnapshot;
import com.openreport.common.websocket.WebSocketMessage;
import com.openreport.common.websocket.WebSocketMessageType;
import com.openreport.common.websocket.WebSocketTopic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class WebSocketPushService {

    @Autowired
    private ReportWebSocketHandler webSocketHandler;

    public void pushTemplateChange(ReportTemplate template, String changeType) {
        if (template == null || template.getId() == null) {
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", template.getId());
            payload.put("templateName", template.getTemplateName());
            payload.put("templateCode", template.getTemplateCode());
            payload.put("status", template.getStatus());
            payload.put("changeType", changeType);
            payload.put("data", template);

            WebSocketMessage msg = WebSocketMessage.of(
                    WebSocketMessageType.REPORT_TEMPLATE_CHANGED,
                    WebSocketTopic.report(template.getId()),
                    payload
            );
            webSocketHandler.broadcastToTopic(WebSocketTopic.report(template.getId()), msg);
            webSocketHandler.broadcastToTopic(WebSocketTopic.REPORT_LIST, msg);

            log.info("推送模板变更: templateId={}, changeType={}", template.getId(), changeType);
        } catch (Exception e) {
            log.error("推送模板变更失败: templateId={}", template == null ? null : template.getId(), e);
        }
    }

    public void pushDataChange(Long templateId) {
        if (templateId == null) {
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("templateId", templateId);
            payload.put("action", "REFRESH");

            WebSocketMessage msg = WebSocketMessage.of(
                    WebSocketMessageType.REPORT_DATA_CHANGED,
                    WebSocketTopic.report(templateId),
                    payload
            );
            webSocketHandler.broadcastToTopic(WebSocketTopic.report(templateId), msg);

            log.info("推送数据变更: templateId={}", templateId);
        } catch (Exception e) {
            log.error("推送数据变更失败: templateId={}", templateId, e);
        }
    }

    public void pushApprovalChange(ReportApproval approval, ReportTemplate template) {
        if (approval == null) {
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("approvalId", approval.getId());
            payload.put("templateId", approval.getTemplateId());
            payload.put("status", approval.getApprovalStatus());
            payload.put("approvalType", approval.getApprovalType());
            payload.put("version", approval.getVersion());
            payload.put("data", approval);
            if (template != null) {
                payload.put("template", template);
            }

            WebSocketMessage msg = WebSocketMessage.of(
                    WebSocketMessageType.REPORT_APPROVAL_CHANGED,
                    approval.getTemplateId() != null ? WebSocketTopic.approval(approval.getTemplateId()) : WebSocketTopic.APPROVAL_LIST,
                    payload
            );
            if (approval.getTemplateId() != null) {
                webSocketHandler.broadcastToTopic(WebSocketTopic.approval(approval.getTemplateId()), msg);
            }
            webSocketHandler.broadcastToTopic(WebSocketTopic.APPROVAL_LIST, msg);
            webSocketHandler.broadcastToTopic(WebSocketTopic.REPORT_LIST, msg);

            log.info("推送审批变更: approvalId={}, templateId={}", approval.getId(), approval.getTemplateId());
        } catch (Exception e) {
            log.error("推送审批变更失败: approvalId={}", approval == null ? null : approval.getId(), e);
        }
    }

    public void pushVersionChange(Long templateId, ReportTemplateSnapshot snapshot, String changeType) {
        if (templateId == null) {
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("templateId", templateId);
            payload.put("version", snapshot != null ? snapshot.getVersion() : null);
            payload.put("changeType", changeType);
            payload.put("snapshot", snapshot);

            WebSocketMessage msg = WebSocketMessage.of(
                    WebSocketMessageType.REPORT_VERSION_CHANGED,
                    WebSocketTopic.report(templateId),
                    payload
            );
            webSocketHandler.broadcastToTopic(WebSocketTopic.report(templateId), msg);

            log.info("推送版本变更: templateId={}, changeType={}", templateId, changeType);
        } catch (Exception e) {
            log.error("推送版本变更失败: templateId={}", templateId, e);
        }
    }

    public void pushScheduleTaskFinished(Long taskId, Long templateId, Object result) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("taskId", taskId);
            payload.put("templateId", templateId);
            payload.put("result", result);

            WebSocketMessage msg = WebSocketMessage.of(
                    WebSocketMessageType.SCHEDULE_TASK_FINISHED,
                    templateId != null ? WebSocketTopic.report(templateId) : WebSocketTopic.ALL,
                    payload
            );
            if (templateId != null) {
                webSocketHandler.broadcastToTopic(WebSocketTopic.report(templateId), msg);
            }
            webSocketHandler.broadcastToAll(msg);

            log.info("推送定时任务完成: taskId={}, templateId={}", taskId, templateId);
        } catch (Exception e) {
            log.error("推送定时任务完成失败: taskId={}", taskId, e);
        }
    }

    public void pushDataChangeToAll(String action) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", action);
            payload.put("timestamp", System.currentTimeMillis());

            WebSocketMessage msg = WebSocketMessage.of(
                    WebSocketMessageType.REPORT_DATA_CHANGED,
                    WebSocketTopic.REPORT_LIST,
                    payload
            );
            webSocketHandler.broadcastToTopic(WebSocketTopic.REPORT_LIST, msg);

            log.info("推送全量数据变更: action={}", action);
        } catch (Exception e) {
            log.error("推送全量数据变更失败: action={}", action, e);
        }
    }

    public int getOnlineCount() {
        return webSocketHandler.getOnlineCount();
    }
}
