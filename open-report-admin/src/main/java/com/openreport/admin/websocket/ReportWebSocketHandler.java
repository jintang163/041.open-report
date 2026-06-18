package com.openreport.admin.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openreport.common.utils.JwtUtils;
import com.openreport.common.websocket.WebSocketMessage;
import com.openreport.common.websocket.WebSocketMessageType;
import com.openreport.common.websocket.WebSocketTopic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class ReportWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Set<WebSocketSession>> topicSessions = new ConcurrentHashMap<>();

    private final Map<WebSocketSession, Set<String>> sessionTopics = new ConcurrentHashMap<>();

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        URI uri = session.getUri();

        if (!authenticateToken(uri)) {
            log.warn("WebSocket连接认证失败，关闭连接: sessionId={}", sessionId);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("无效的认证token"));
            return;
        }

        Long userId = extractUserIdFromToken(uri);
        if (userId != null) {
            session.getAttributes().put("userId", userId);
        }

        sessions.put(sessionId, session);
        sessionTopics.put(session, new CopyOnWriteArraySet<>());
        log.info("WebSocket连接建立成功: sessionId={}, userId={}", sessionId, userId);
    }

    private boolean authenticateToken(URI uri) {
        if (uri == null) {
            return false;
        }
        try {
            Map<String, String> queryParams = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();
            String token = queryParams.get("token");
            if (token == null || token.isEmpty()) {
                return false;
            }
            return jwtUtils.validateToken(token);
        } catch (Exception e) {
            log.error("WebSocket token认证异常", e);
            return false;
        }
    }

    private Long extractUserIdFromToken(URI uri) {
        if (uri == null) {
            return null;
        }
        try {
            Map<String, String> queryParams = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();
            String token = queryParams.get("token");
            if (token != null && !token.isEmpty()) {
                return jwtUtils.getUserIdFromToken(token);
            }
        } catch (Exception e) {
            log.warn("从token中提取userId失败", e);
        }
        return null;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            WebSocketMessage msg = objectMapper.readValue(payload, WebSocketMessage.class);

            switch (msg.getType()) {
                case WebSocketMessageType.HEARTBEAT:
                    sendMessage(session, WebSocketMessage.of(
                            WebSocketMessageType.HEARTBEAT_ACK, WebSocketTopic.ALL, "pong"
                    ));
                    break;
                case WebSocketMessageType.SUBSCRIBE:
                    handleSubscribe(session, msg);
                    break;
                case WebSocketMessageType.UNSUBSCRIBE:
                    handleUnsubscribe(session, msg);
                    break;
                default:
                    log.debug("收到未处理的消息类型: type={}, sessionId={}",
                            msg.getType(), session.getId());
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息异常: sessionId={}", session.getId(), e);
        }
    }

    private void handleSubscribe(WebSocketSession session, WebSocketMessage msg) {
        String topic = msg.getTopic() != null ? String.valueOf(msg.getTopic()) : WebSocketTopic.ALL;
        topicSessions.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionTopics.computeIfAbsent(session, k -> new CopyOnWriteArraySet<>()).add(topic);
        log.debug("订阅主题: sessionId={}, topic={}", session.getId(), topic);
    }

    private void handleUnsubscribe(WebSocketSession session, WebSocketMessage msg) {
        String topic = msg.getTopic() != null ? String.valueOf(msg.getTopic()) : WebSocketTopic.ALL;
        Set<WebSocketSession> sessionsOfTopic = topicSessions.get(topic);
        if (sessionsOfTopic != null) {
            sessionsOfTopic.remove(session);
        }
        Set<String> topicsOfSession = sessionTopics.get(session);
        if (topicsOfSession != null) {
            topicsOfSession.remove(topic);
        }
        log.debug("取消订阅: sessionId={}, topic={}", session.getId(), topic);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        Set<String> topicsOfSession = sessionTopics.remove(session);
        if (topicsOfSession != null) {
            for (String topic : topicsOfSession) {
                Set<WebSocketSession> sessionsOfTopic = topicSessions.get(topic);
                if (sessionsOfTopic != null) {
                    sessionsOfTopic.remove(session);
                }
            }
        }
        log.info("WebSocket连接关闭: sessionId={}, status={}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输异常: sessionId={}", session.getId(), exception);
    }

    public void sendMessage(WebSocketSession session, WebSocketMessage message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("发送WebSocket消息失败: sessionId={}", session.getId(), e);
        }
    }

    public void broadcastToTopic(String topic, WebSocketMessage message) {
        Set<WebSocketSession> sessionsOfTopic = topicSessions.get(topic);
        if (sessionsOfTopic != null && !sessionsOfTopic.isEmpty()) {
            for (WebSocketSession session : sessionsOfTopic) {
                sendMessage(session, message);
            }
        }
        Set<WebSocketSession> allSessions = topicSessions.get(WebSocketTopic.ALL);
        if (allSessions != null && !allSessions.isEmpty()) {
            for (WebSocketSession session : allSessions) {
                sendMessage(session, message);
            }
        }
        log.debug("广播消息到主题: topic={}, messageType={}, sessionsCount={}",
                topic, message.getType(),
                (sessionsOfTopic == null ? 0 : sessionsOfTopic.size())
                        + (allSessions == null ? 0 : allSessions.size()));
    }

    public void broadcastToAll(WebSocketMessage message) {
        for (WebSocketSession session : sessions.values()) {
            sendMessage(session, message);
        }
        log.debug("广播消息到所有连接: messageType={}, sessionsCount={}",
                message.getType(), sessions.size());
    }

    public int getOnlineCount() {
        return sessions.size();
    }
}
