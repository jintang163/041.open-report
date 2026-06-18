package com.openreport.admin.config;

import com.alibaba.fastjson.JSON;
import com.openreport.common.result.Result;
import com.openreport.common.result.ResultCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(com.openreport.admin.config.RequirePerms)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        SecurityContext context = SecurityContextHolder.get();
        if (context == null) {
            writeForbiddenResponse();
            return null;
        }
        if (context.isSuperAdmin()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePerms requirePerms = method.getAnnotation(RequirePerms.class);
        String[] requiredPerms = requirePerms.value();
        Logical logical = requirePerms.logical();

        boolean hasPermission;
        if (logical == Logical.AND) {
            hasPermission = true;
            for (String perm : requiredPerms) {
                if (!context.hasPermission(perm)) {
                    hasPermission = false;
                    break;
                }
            }
        } else {
            hasPermission = false;
            for (String perm : requiredPerms) {
                if (context.hasPermission(perm)) {
                    hasPermission = true;
                    break;
                }
            }
        }

        if (!hasPermission) {
            writeForbiddenResponse();
            return null;
        }

        return joinPoint.proceed();
    }

    private void writeForbiddenResponse() throws Exception {
        HttpServletResponse response = getHttpResponse();
        if (response != null) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            Result<Object> result = Result.failure(ResultCode.FORBIDDEN);
            response.getWriter().write(JSON.toJSONString(result));
        }
    }

    private HttpServletResponse getHttpResponse() {
        org.springframework.web.context.request.ServletRequestAttributes attrs =
                (org.springframework.web.context.request.ServletRequestAttributes)
                        org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getResponse() : null;
    }
}
