package com.openreport.admin.config;

import com.alibaba.fastjson.JSON;
import com.openreport.common.result.Result;
import com.openreport.common.result.ResultCode;
import com.openreport.common.utils.JwtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }
        if (StringUtils.isNotBlank(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (StringUtils.isBlank(token)) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            return false;
        }
        if (jwtUtils.isTokenExpired(token)) {
            writeErrorResponse(response, ResultCode.TOKEN_EXPIRED);
            return false;
        }
        if (!jwtUtils.validateToken(token)) {
            writeErrorResponse(response, ResultCode.TOKEN_INVALID);
            return false;
        }
        Long userId = jwtUtils.getUserIdFromToken(token);
        String username = jwtUtils.getUsernameFromToken(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        return true;
    }

    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(resultCode.getCode());
        Result<Object> result = Result.failure(resultCode);
        response.getWriter().write(JSON.toJSONString(result));
    }
}
