package com.example.ashore.framework.common.enums;

/**
 * 定义各个 Filter 的优先级（bean.setOrder(order); -- 数字越小，优先级越高，越先执行）
 * 考虑到每个 starter 都需要用到该工具类，所以放到 common 模块下的 enum 包下
 *
 * 关键的执行顺序设计：
 *   1. CORS_FILTER = Integer.MIN_VALUE                      - 最先执行，确保跨域配置对所有请求生效
 *   2. REQUEST_BODY_CACHE_FILTER = Integer.MIN_VALUE + 500  - 很早执行，缓存请求体供后续使用（要在读取 body 的 Filter 如日志、XSS 之前执行）
 *   3. API_ACCESS_LOG_FILTER = -103                         - 记录 API 访问日志
 *   4. XSS_FILTER = -102                                    - XSS 防护过滤
 *   5. Spring Security Filter = -100                        - Spring Security 认证授权（注释中说明）
 *   6. DEMO_FILTER = Integer.MAX_VALUE                      - 最后执行，演示模式拦截（最后执行，在所有校验通过后才拦截写操作）
 *
 * 如果顺序错了，可能导致问题。比如如果 DEMO_FILTER 在 REQUEST_BODY_CACHE_FILTER 之前，那么 DEMO_FILTER 读取 body 后，后续就无法再读取了
 */
public interface WebFilterOrderEnum {

    int CORS_FILTER = Integer.MIN_VALUE;

    int TRACE_FILTER = CORS_FILTER + 1;

    int ENV_TAG_FILTER = TRACE_FILTER + 1;

    int REQUEST_BODY_CACHE_FILTER = Integer.MIN_VALUE + 500;

    int API_ENCRYPT_FILTER = REQUEST_BODY_CACHE_FILTER + 1;

    // OrderedRequestContextFilter 默认为 -105，用于国际化上下文等等

    int TENANT_CONTEXT_FILTER = - 104; // 需要保证在 ApiAccessLogFilter 前面

    int API_ACCESS_LOG_FILTER = -103; // 需要保证在 RequestBodyCacheFilter 后面

    int XSS_FILTER = -102;  // 需要保证在 RequestBodyCacheFilter 后面

    // Spring Security Filter 默认为 -100，可见 org.springframework.boot.autoconfigure.security.SecurityProperties 配置属性类

    int TENANT_SECURITY_FILTER = -99; // 需要保证在 Spring Security 过滤器后面

    int FLOWABLE_FILTER = -98; // 需要保证在 Spring Security 过滤后面

    int DEMO_FILTER = Integer.MAX_VALUE;

}
