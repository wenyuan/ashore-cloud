package com.example.ashore.framework.web.core.filter;

import cn.hutool.core.util.StrUtil;
import com.example.ashore.framework.common.util.servlet.ServletUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Request Body 缓存 Filter，实现它的可重复读取
 * 在实际应用中，可能多个地方需要读取请求体(如日志记录、参数校验、业务处理)，这个过滤器让请求体可以重复读取
 *
 * 为什么继承 OncePerRequestFilter：确保在请求转发(forward)或包含(include)场景下，过滤器只执行一次，避免重复缓存请求体，浪费内存和性能
 */
public class CacheRequestBodyFilter extends OncePerRequestFilter {

    /**
     * 定义需要排除的 URI 路径，这些路径不需要缓存请求体
     * - admin 请求可能是长连接或流式传输，缓存请求体可能导致客户端连接中断异常
     * - actuator 监控端点不需要业务处理，缓存请求体没有意义且浪费资源
     */
    private static final String[] IGNORE_URIS = {"/admin/", "/actuator/"};

    /**
     * 执行过滤逻辑的核心方法
     *
     * @param request           HTTP 请求对象
     * @param response          HTTP 响应对象
     * @param filterChain       过滤器链，用于将请求传递给下一个过滤器或目标资源
     * @throws IOException      读取请求体时可能抛出 IO 异常
     * @throws ServletException Servlet 处理异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        // 用包装类替换原始请求，后续所有处理都使用包装类
        // 将包装后的请求传递给后续过滤器链和控制器
        // 后续所有组件通过 getInputStream() 或 getReader() 读取时，都会从缓存的字节数组中获取，实现可重复读取
        filterChain.doFilter(new CacheRequestBodyWrapper(request), response);
    }

    /**
     * 判断是否应该跳过过滤逻辑
     * OncePerRequestFilter 提供的钩子方法，用于控制是否执行过滤
     *
     * @param request HTTP 请求对象
     * @return        true-跳过过滤；false-执行过滤
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 1. 校验是否为排除的 URL
        String requestURI = request.getRequestURI();
        if (StrUtil.startWithAny(requestURI, IGNORE_URIS)) {
            return true;
        }

        // 2. 只处理 json 请求内容，其他类型（文件上传、表单等）可能数据量大，缓存到内存会导致内存溢出，且通常不需要重复读取
        return !ServletUtils.isJsonRequest(request);
    }
}
