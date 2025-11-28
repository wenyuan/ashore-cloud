package com.example.ashore.framework.security.core.service;

import cn.hutool.core.collection.CollUtil;
import com.example.ashore.framework.common.biz.system.permission.PermissionCommonApi;
import com.example.ashore.framework.common.core.KeyValue;
import com.example.ashore.framework.security.core.LoginUser;
import com.example.ashore.framework.security.core.util.SecurityFrameworkUtils;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static com.example.ashore.framework.common.util.cache.CacheUtils.buildCache;
import static com.example.ashore.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.example.ashore.framework.security.core.util.SecurityFrameworkUtils.skipPermissionCheck;

/**
 * 默认的 {@link SecurityFrameworkService} 实现类
 */
@AllArgsConstructor
public class SecurityFrameworkServiceImpl implements SecurityFrameworkService {

    private final PermissionCommonApi permissionApi;

    /**
     * 针对 {@link #hasAnyRoles(String...)} 的缓存
     */
    private final LoadingCache<KeyValue<String, List<String>>, Boolean> hasAnyRolesCache = buildCache(
            Duration.ofMinutes(1L), // 过期时间 1 分钟
            new CacheLoader<>() {
                @Override
                public Boolean load(KeyValue<String, List<String>> key) {
                    return permissionApi.hasAnyRoles(key.getKey(), key.getValue().toArray(new String[0])).getDataOrThrow();
                }

            });

    /**
     * 针对 {@link #hasAnyPermissions(String...)} 的缓存
     */
    private final LoadingCache<KeyValue<String, List<String>>, Boolean> hasAnyPermissionsCache = buildCache(
            Duration.ofMinutes(1L), // 过期时间 1 分钟
            new CacheLoader<>() {
                @Override
                public Boolean load(KeyValue<String, List<String>> key) {
                    return permissionApi.hasAnyPermissions(key.getKey(), key.getValue().toArray(new String[0])).getDataOrThrow();
                }

            });

    @Override
    public boolean hasPermission(String permission) {
        return hasAnyPermissions(permission);
    }

    @Override
    @SneakyThrows
    public boolean hasAnyPermissions(String... permissions) {
        // 特殊：跨租户访问
        if (skipPermissionCheck()) {
            return true;
        }

        // 权限校验
        String userId = getLoginUserId();
        if (userId == null) {
            return false;
        }
        return hasAnyPermissionsCache.get(new KeyValue<>(userId, Arrays.asList(permissions)));
    }

    @Override
    public boolean hasRole(String role) {
        return hasAnyRoles(role);
    }

    @Override
    @SneakyThrows
    public boolean hasAnyRoles(String... roles) {
        // 特殊：跨租户访问
        if (skipPermissionCheck()) {
            return true;
        }

        // 权限校验
        String userId = getLoginUserId();
        if (userId == null) {
            return false;
        }
        return hasAnyRolesCache.get(new KeyValue<>(userId, Arrays.asList(roles)));
    }

    @Override
    public boolean hasScope(String scope) {
        return hasAnyScopes(scope);
    }

    @Override
    public boolean hasAnyScopes(String... scope) {
        // 特殊：跨租户访问
        if (skipPermissionCheck()) {
            return true;
        }

        // 权限校验
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        if (user == null) {
            return false;
        }
        return CollUtil.containsAny(user.getScopes(), Arrays.asList(scope));
    }

}
