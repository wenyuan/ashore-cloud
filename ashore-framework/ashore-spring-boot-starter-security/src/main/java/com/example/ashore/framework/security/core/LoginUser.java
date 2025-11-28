package com.example.ashore.framework.security.core;

import cn.hutool.core.map.MapUtil;
import com.example.ashore.framework.common.enums.UserTypeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录用户信息
 */
@Data
public class LoginUser {

    // ========== 常量定义 ==========
    public static final String INFO_KEY_NICKNAME = "nickname";
    public static final String INFO_KEY_DEPT_ID = "deptId";

    // ========== 基本信息 ==========

    /**
     * 用户编号
     */
    private String id;

    /**
     * 用户类型
     *
     * 关联 {@link UserTypeEnum}
     */
    private Integer userType;

    /**
     * 额外的用户信息
     * 存储昵称、部门ID等扩展信息
     */
    private Map<String, String> info;

    /**
     * 租户编号
     * 多租户场景下，标识用户所属租户
     */
    private String tenantId;

    /**
     * 授权范围
     * OAuth2 Scope，例如：["user.read", "user.write"]
     */
    private List<String> scopes;

    /**
     * Token 过期时间
     */
    private LocalDateTime expiresTime;

    // ========== 上下文字段 ==========

    /**
     * 上下文字段，不进行持久化
     * 用于基于 LoginUser 维度的临时缓存
     * 例如：存储本次请求的临时数据
     */
    @JsonIgnore
    private Map<String, Object> context;

    /**
     * 访问的租户编号
     * 跨租户访问时使用，与 tenantId 不同
     */
    private Long visitTenantId;

    // ========== 上下文操作方法 ==========

    /**
     * 设置上下文数据
     * @param key 键
     * @param value 值
     */
    public void setContext(String key, Object value) {
        if (context == null) {
            context = new HashMap<>();
        }
        context.put(key, value);
    }

    /**
     * 获取上下文数据
     * @param key 键
     * @param type 值的类型
     * @return 值
     */
    public <T> T getContext(String key, Class<T> type) {
        return MapUtil.get(context, key, type);
    }

}
