package com.example.ashore.framework.web.core.filter;

import cn.hutool.core.util.StrUtil;
import com.example.ashore.framework.web.config.WebProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * API 请求过滤器基类
 * 依赖 WebProperties 配置类获取 API 前缀配置
 * 只有以 admin-api 或 app-api 前缀开头的请求才会被过滤，其他路径的请求（静态资源、其他路径的请求）会被跳过
 */
@RequiredArgsConstructor
public abstract class ApiRequestFilter extends OncePerRequestFilter {

    protected final WebProperties webProperties;

    /**
     * 判断当前请求是否需要被过滤
     *
     * @param request HTTP 请求对象
     * @return        true 表示不过滤该请求，false 表示需要过滤
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只过滤 API 请求的地址
        // 获取去除 contextPath (server.servlet.context-path) 后的请求 URI
        String apiUri = request.getRequestURI().substring(request.getContextPath().length());
        return !StrUtil.startWithAny(apiUri, webProperties.getAdminApi().getPrefix(), webProperties.getAppApi().getPrefix());
    }

}
