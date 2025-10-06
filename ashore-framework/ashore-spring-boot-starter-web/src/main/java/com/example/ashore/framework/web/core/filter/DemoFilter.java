package com.example.ashore.framework.web.core.filter;

import cn.hutool.core.util.StrUtil;
import com.example.ashore.framework.common.pojo.ApiResponse;
import com.example.ashore.framework.common.util.servlet.ServletUtils;
import com.example.ashore.framework.web.core.util.WebFrameworkUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static com.example.ashore.framework.common.exception.enums.GlobalErrorCodeConstants.DEMO_DENY;

/**
 * 演示 Filter，禁止用户发起写操作，避免影响测试数据
 *
 * 工作流程：
 * - GET 请求 → shouldNotFilter 返回 true -> 正常放行
 * - 未登录 + POST -> shouldNotFilter 返回 true -> 正常放行（反正也会被权限拦截）
 * - 已登录 + POST -> shouldNotFilter 返回 false -> doFilterInternal 返回错误 -> 请求被阻止
 */
public class DemoFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        // 如果请求方法不是 POST/PUT/DELETE，返回 true（跳过拦截）
        // 用户未登录时，也返回 true（跳过拦截），因为未登录用户本身就无法进行写操作
        return !StrUtil.equalsAnyIgnoreCase(method, "POST", "PUT", "DELETE")
                || WebFrameworkUtils.getLoginUserId(request) == null;
    }

    /**
     * 执行过滤逻辑
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param chain    过滤器链(本方法中不会调用，请求在此被拦截)
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        // 返回错误响应（DEMO_DENY 错误码）
        // 没有调用 chain.doFilter()，意味着请求链被中断，不会继续往下执行
        ServletUtils.writeJSON(response, ApiResponse.error(DEMO_DENY));
    }

}
