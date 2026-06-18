package com.openreport.admin.config;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class SecurityContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private Long deptId;
    private Set<String> permissions;
    private List<Long> roleIds;

    public SecurityContext() {
    }

    public SecurityContext(Long userId, String username, Long deptId, Set<String> permissions, List<Long> roleIds) {
        this.userId = userId;
        this.username = username;
        this.deptId = deptId;
        this.permissions = permissions;
        this.roleIds = roleIds;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public boolean hasPermission(String permission) {
        if (permissions == null) {
            return false;
        }
        return permissions.contains(permission) || permissions.contains("*");
    }

    public boolean isSuperAdmin() {
        if (permissions == null) {
            return false;
        }
        return permissions.contains("*");
    }
}
