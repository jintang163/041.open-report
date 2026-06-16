package com.openreport.common.utils;

import cn.hutool.json.JSONUtil;
import com.openreport.common.constants.CommonConstants;
import com.openreport.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Slf4j
public class ServletUtils {

    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    public static HttpServletResponse getResponse() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getResponse() : null;
    }

    public static String getHeader(String name) {
        HttpServletRequest request = getRequest();
        return request != null ? request.getHeader(name) : null;
    }

    public static String getParameter(String name) {
        HttpServletRequest request = getRequest();
        return request != null ? request.getParameter(name) : null;
    }

    public static String getRequestURI() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRequestURI() : null;
    }

    public static String getRequestURL() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRequestURL().toString() : null;
    }

    public static String getMethod() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getMethod() : null;
    }

    public static String getIpAddr() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    public static void writeJson(HttpServletResponse response, Result<?> result) {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter writer = response.getWriter()) {
            writer.write(JSONUtil.toJsonStr(result));
            writer.flush();
        } catch (IOException e) {
            log.error("写入响应失败", e);
        }
    }

    public static void writeJson(HttpServletResponse response, int status, Result<?> result) {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter writer = response.getWriter()) {
            writer.write(JSONUtil.toJsonStr(result));
            writer.flush();
        } catch (IOException e) {
            log.error("写入响应失败", e);
        }
    }

    public static void writeFailure(HttpServletResponse response, String message) {
        writeJson(response, Result.failure(message));
    }

    public static String getToken() {
        String token = getHeader(CommonConstants.AUTHORIZATION);
        if (token != null && token.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return token.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return token;
    }
}
