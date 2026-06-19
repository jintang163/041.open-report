package com.openreport.admin.config;

import com.alibaba.fastjson.JSON;
import com.openreport.admin.service.SysMenuService;
import com.openreport.admin.service.SysRoleService;
import com.openreport.admin.service.SysUserService;
import com.openreport.common.result.Result;
import com.openreport.common.result.ResultCode;
import com.openreport.common.utils.JwtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SysMenuService sysMenuService;

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysUserService sysUserService;

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

        io.jsonwebtoken.Claims claims = jwtUtils.parseToken(token);
        String tokenType = claims.get("type") != null ? claims.get("type").toString() : null;

        Long userId;
        String username;

        if ("service".equals(tokenType)) {
            Object serviceKey = claims.get("serviceKey");
            Object serviceSecret = claims.get("serviceSecret");
            String expectedServiceKey = "openreport-scheduler";
            String expectedServiceSecret = "openreport-scheduler-secret-2024";
            if (serviceKey == null || serviceSecret == null ||
                    !expectedServiceKey.equals(serviceKey.toString()) ||
                    !expectedServiceSecret.equals(serviceSecret.toString())) {
                writeErrorResponse(response, ResultCode.UNAUTHORIZED);
                return false;
            }
            Object id = claims.get("userId");
            Object uname = claims.get("username");
            userId = id != null ? Long.valueOf(id.toString()) : null;
            username = uname != null ? uname.toString() : "scheduler_service";
        } else if ("embed".equals(tokenType)) {
            Object createBy = claims.get("createBy");
            Object createByUsername = claims.get("createByUsername");
            userId = createBy != null ? Long.valueOf(createBy.toString()) : null;
            username = createByUsername != null ? createByUsername.toString() : "embed_user";
        } else {
            userId = jwtUtils.getUserIdFromToken(token);
            username = jwtUtils.getUsernameFromToken(token);
        }

        if (userId == null) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        request.setAttribute("userId", userId);
        request.setAttribute("username", username);

        buildSecurityContext(userId, username);

        return true;
    }

    private void buildSecurityContext(Long userId, String username) {
        com.openreport.admin.entity.SysUser user = sysUserService.getById(userId);
        Long deptId = user != null ? user.getDeptId() : null;
        Long tenantId = user != null ? user.getTenantId() : null;
        List<com.openreport.admin.entity.SysRole> roles = sysRoleService.listByUserId(userId);
        List<Long> roleIds = roles.stream().map(com.openreport.admin.entity.SysRole::getId).collect(Collectors.toList());
        List<com.openreport.admin.entity.SysMenu> menus = sysMenuService.listByUserId(userId);
        Set<String> permissions = new HashSet<>();
        for (com.openreport.admin.entity.SysMenu menu : menus) {
            if (StringUtils.isNotBlank(menu.getPerms())) {
                permissions.add(menu.getPerms());
            }
        }
        boolean isSuperAdmin = roles.stream()
                .anyMatch(r -> "SUPER_ADMIN".equals(r.getRoleCode()));
        if (isSuperAdmin) {
            permissions.add("*");
        }
        SecurityContext securityContext = new SecurityContext(userId, username, deptId, tenantId, permissions, roleIds);
        SecurityContextHolder.set(securityContext);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityContextHolder.clear();
    }

    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(resultCode.getCode());
        Result<Object> result = Result.failure(resultCode);
        response.getWriter().write(JSON.toJSONString(result));
    }
}
