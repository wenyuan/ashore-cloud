package com.example.ashore.framework.common.util.servlet;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.example.ashore.framework.common.util.json.JsonUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * 客户端工具类
 * 封装了常用的 HTTP 请求和响应操作
 */
public class ServletUtils {

    /**
     * 将 Java 对象转为 JSON 格式并写入 HTTP 响应
     *
     * 使用时机：
     * - 在 Filter 或拦截器中直接返回 JSON 响应
     * - 处理认证失败、权限不足等异常时返回错误信息
     * - 不经过 Controller 直接返回数据时
     *
     * @param response 响应对象
     * @param object   对象，会序列化成 JSON 字符串
     */
    @SuppressWarnings("deprecation") // 必须使用 APPLICATION_JSON_UTF8_VALUE，否则会乱码
    public static void writeJSON(HttpServletResponse response, Object object) {
        String content = JsonUtils.toJsonString(object);
        JakartaServletUtil.write(response, content, MediaType.APPLICATION_JSON_UTF8_VALUE);
    }

    /**
     * 在任何地方获取当前线程的 HttpServletRequest 对象
     *
     * 使用时机:
     * - 在 Service 层需要获取请求信息时
     * - 在工具类中需要访问当前请求时
     * - 前提: 必须在 Spring MVC 的请求处理线程中调用
     *
     * @return HttpServletRequest 对象，如果不在请求上下文中则返回 null
     */
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    /**
     * 从请求头获取用户代理信息(浏览器类型、版本、操作系统等)
     *
     * 使用时机:
     * - 记录用户访问日志
     * - 根据不同设备返回不同内容
     * - 安全审计
     *
     * @param request 请求对象
     * @return        User-Agent 字符串，如果请求头中不存在则返回空字符串
     */
    public static String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "";
    }

    /**
     * 无参版本，自动获取当前请求的 User-Agent
     * 与上面的方法相同，但更方便，不需要传递 request 参数
     *
     * @return User-Agent 字符串，如果不在请求上下文中或请求头不存在则返回 null
     */
    public static String getUserAgent() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return getUserAgent(request);
    }

    /**
     * 获取客户端真实 IP 地址
     * 会处理代理场景，从 X-Forwarded-For 等头部提取真实 IP
     *
     * 使用时机:
     * - 记录访问日志
     * - IP 限流、黑白名单控制
     * - 安全审计
     *
     * @param request 请求对象
     * @return        客户端 IP 地址
     */
    public static String getClientIP(HttpServletRequest request) {
        return JakartaServletUtil.getClientIP(request);
    }

    /**
     * 无参版本，自动获取当前请求的 客户端 IP 地址
     * 与上面的方法相同，但更方便，不需要传递 request 参数
     *
     * @return 客户端 IP 地址
     */
    public static String getClientIP() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return JakartaServletUtil.getClientIP(request);
    }

    /**
     * 判断请求的 Content-Type 是否为 JSON
     * 使用时机:
     *   - 在 Filter 中判断请求类型
     *   - 决定如何解析请求体
     *
     * @param request 请求对象
     * @return        true 表示是 JSON 请求，false 表示不是
     */
    public static boolean isJsonRequest(ServletRequest request) {
        return StrUtil.startWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * 获取 JSON 请求的请求体(字符串格式)
     * 注意：只能读取 JSON 请求，因为框架会通过 CacheRequestBodyFilter 缓存请求体，允许重复读取
     * 使用时机:
     *   - 在日志中记录请求参数
     *   - 自定义参数校验
     *
     * @param request 请求对象
     * @return        请求体字符串，非 JSON 请求返回 null
     * @see com.example.ashore.framework.web.core.filter.CacheRequestBodyFilter
     */
    public static String getBody(HttpServletRequest request) {
        // 只有在 json 请求在读取，因为只有 CacheRequestBodyFilter 才会进行缓存，支持重复读取
        if (isJsonRequest(request)) {
            return JakartaServletUtil.getBody(request);
        }
        return null;
    }

    /**
     * 获取 JSON 请求的请求体(字节数组格式)
     * 注意：只能读取 JSON 请求，因为框架会通过 CacheRequestBodyFilter 缓存请求体，允许重复读取
     * 使用时机：与上面的方法类似，但需要字节数组时使用(如加密、签名校验)
     *
     * @param request 请求对象
     * @return        请求体字节数组，非 JSON 请求返回 null
     * @see com.example.ashore.framework.web.core.filter.CacheRequestBodyFilter
     */
    public static byte[] getBodyBytes(HttpServletRequest request) {
        // 只有在 json 请求在读取，因为只有 CacheRequestBodyFilter 才会进行缓存，支持重复读取
        if (isJsonRequest(request)) {
            return JakartaServletUtil.getBodyBytes(request);
        }
        return null;
    }

    /**
     * 获取请求的所有 URL 参数(query string)
     * 使用时机:
     *   - 日志记录
     *   - 参数签名校验
     *
     * 示例：
     * // 请求: /api/user?id=1&name=张三
     * Map<String, String> params = ServletUtils.getParamMap(request);
     * // params = {id: "1", name: "张三"}
     *
     * @param request 请求对象
     * @return        参数映射表，key 为参数名，value 为参数值
     */
    public static Map<String, String> getParamMap(HttpServletRequest request) {
        return JakartaServletUtil.getParamMap(request);
    }

    /**
     * 获取请求的所有 HTTP 头部信息
     * 使用时机:
     *   - 日志记录完整请求信息
     *   - 获取自定义头部(如 token)
     *
     * @param request 请求对象
     * @return        头部映射表，key 为头部名称，value 为头部值
     */
    public static Map<String, String> getHeaderMap(HttpServletRequest request) {
        return JakartaServletUtil.getHeaderMap(request);
    }

}
