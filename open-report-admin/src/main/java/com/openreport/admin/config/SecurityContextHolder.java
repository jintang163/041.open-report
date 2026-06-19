package com.openreport.admin.config;

public class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> CONTEXT = new ThreadLocal<>();

    public static void set(SecurityContext context) {
        CONTEXT.set(context);
    }

    public static SecurityContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static Long getUserId() {
        SecurityContext ctx = get();
        return ctx != null ? ctx.getUserId() : null;
    }

    public static String getUsername() {
        SecurityContext ctx = get();
        return ctx != null ? ctx.getUsername() : null;
    }

    public static Long getDeptId() {
        SecurityContext ctx = get();
        return ctx != null ? ctx.getDeptId() : null;
    }

    public static Long getTenantId() {
        SecurityContext ctx = get();
        return ctx != null ? ctx.getTenantId() : null;
    }

    public static boolean hasPermission(String permission) {
        SecurityContext ctx = get();
        return ctx != null && ctx.hasPermission(permission);
    }

    public static boolean isSuperAdmin() {
        SecurityContext ctx = get();
        return ctx != null && ctx.isSuperAdmin();
    }
}
