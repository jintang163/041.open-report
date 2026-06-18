package com.openreport.admin.controller;

import com.openreport.admin.entity.SysMenu;
import com.openreport.admin.entity.SysRole;
import com.openreport.admin.entity.SysUser;
import com.openreport.admin.service.SysMenuService;
import com.openreport.admin.service.SysRoleService;
import com.openreport.admin.service.SysUserService;
import com.openreport.common.result.Result;
import com.openreport.common.result.ResultCode;
import com.openreport.common.utils.JwtUtils;
import com.openreport.common.utils.PasswordUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Api(tags = "登录认证")
@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysMenuService sysMenuService;

    @Autowired
    private JwtUtils jwtUtils;

    @ApiOperation("登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        if (username == null || password == null) {
            return Result.failure(ResultCode.BAD_REQUEST.getCode(), "用户名或密码不能为空");
        }
        SysUser user = sysUserService.getByUsername(username);
        if (user == null) {
            return Result.failure(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.failure(ResultCode.USER_DISABLED);
        }
        if (!PasswordUtils.matches(password, user.getPassword())) {
            return Result.failure(ResultCode.LOGIN_ERROR);
        }
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("deptId", user.getDeptId());

        List<SysRole> roles = sysRoleService.listByUserId(user.getId());
        List<Map<String, Object>> roleList = new ArrayList<>();
        Set<String> permissions = new HashSet<>();
        for (SysRole role : roles) {
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("id", role.getId());
            roleMap.put("roleName", role.getRoleName());
            roleMap.put("roleCode", role.getRoleCode());
            roleList.add(roleMap);
        }
        data.put("roles", roleList);

        boolean isSuperAdmin = roles.stream()
                .anyMatch(r -> "SUPER_ADMIN".equals(r.getRoleCode()));

        List<SysMenu> menus = sysMenuService.listByUserId(user.getId());
        List<Map<String, Object>> menuList = buildMenuTree(menus);
        data.put("menus", menuList);

        for (SysMenu menu : menus) {
            if (StringUtils.isNotBlank(menu.getPerms())) {
                permissions.add(menu.getPerms());
            }
        }
        if (isSuperAdmin) {
            permissions.add("*");
        }
        data.put("permissions", new ArrayList<>(permissions));

        return Result.success(data);
    }

    private List<Map<String, Object>> buildMenuTree(List<SysMenu> menus) {
        Map<Long, Map<String, Object>> menuMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", menu.getId());
            node.put("parentId", menu.getParentId());
            node.put("name", menu.getMenuName());
            node.put("path", menu.getPath());
            node.put("component", menu.getComponent());
            node.put("perms", menu.getPerms());
            node.put("icon", menu.getIcon());
            node.put("menuType", menu.getMenuType());
            node.put("sortOrder", menu.getSortOrder());
            node.put("children", new ArrayList<Map<String, Object>>());
            menuMap.put(menu.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> node : menuMap.values()) {
            Long parentId = (Long) node.get("parentId");
            if (parentId == null || parentId == 0) {
                roots.add(node);
            } else {
                Map<String, Object> parent = menuMap.get(parentId);
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    @ApiOperation("登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    @ApiOperation("获取当前用户信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> info(@RequestAttribute("userId") Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return Result.failure(ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("avatar", user.getAvatar());
        data.put("status", user.getStatus());
        data.put("deptId", user.getDeptId());

        List<SysRole> roles = sysRoleService.listByUserId(userId);
        List<Map<String, Object>> roleList = new ArrayList<>();
        Set<String> permissions = new HashSet<>();
        for (SysRole role : roles) {
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("id", role.getId());
            roleMap.put("roleName", role.getRoleName());
            roleMap.put("roleCode", role.getRoleCode());
            roleList.add(roleMap);
        }
        data.put("roles", roleList);

        boolean isSuperAdmin = roles.stream()
                .anyMatch(r -> "SUPER_ADMIN".equals(r.getRoleCode()));

        List<SysMenu> menus = sysMenuService.listByUserId(userId);
        List<Map<String, Object>> menuList = buildMenuTree(menus);
        data.put("menus", menuList);

        for (SysMenu menu : menus) {
            if (StringUtils.isNotBlank(menu.getPerms())) {
                permissions.add(menu.getPerms());
            }
        }
        if (isSuperAdmin) {
            permissions.add("*");
        }
        data.put("permissions", new ArrayList<>(permissions));

        return Result.success(data);
    }
}
