package com.example.ashore.framework.web.core.util;

import cn.hutool.core.util.NumberUtil;
import com.example.ashore.framework.common.enums.RpcConstants;
import com.example.ashore.framework.common.enums.TerminalEnum;
import com.example.ashore.framework.common.enums.UserTypeEnum;
import com.example.ashore.framework.common.pojo.ApiResponse;
import com.example.ashore.framework.web.config.WebProperties;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 专属于 web 包的工具类
 */
public class WebFrameworkUtils {

    /**
     * 请求属性 KEY：登录用户ID
     * 用于在请求处理过程中传递当前登录用户的ID
     */
    private static final String REQUEST_ATTRIBUTE_LOGIN_USER_ID = "login_user_id";
    /**
     * 请求属性 KEY：登录用户类型
     * 用于在请求处理过程中传递当前登录用户的类型(管理用户/普通用户)
     * @see com.example.ashore.framework.common.enums.UserTypeEnum
     */
    private static final String REQUEST_ATTRIBUTE_LOGIN_USER_TYPE = "login_user_type";

    /**
     * 请求属性 KEY: 通用返回结果
     * 用于在过滤器/拦截器中存储统一的返回结果对象
     */
    private static final String REQUEST_ATTRIBUTE_API_RESPONSE = "api_response";

    /**
     * HTTP 请求头：租户ID
     * 客户端通过此请求头传递当前操作的租户ID，实现多租户隔离
     */
    public static final String HEADER_TENANT_ID = "tenant-id";

    /**
     * HTTP 请求头：访问的租户ID
     * 用于跨租户访问场景，表示要访问的目标租户ID
     */
    public static final String HEADER_VISIT_TENANT_ID = "visit-tenant-id";

    /**
     * HTTP 请求头: 终端类型
     * 客户端通过此请求头标识访问来源(微信小程序/H5/App等)
     * @see com.example.ashore.framework.common.enums.TerminalEnum
     */
    public static final String HEADER_TERMINAL = "terminal";

    /**
     * Web 配置属性
     * 存储 API 前缀等配置信息，用于判断用户类型
     */
    private static WebProperties properties;

    /**
     * 构造函数，用于注入 WebProperties 配置
     * 这样其他静态方法就可以访问配置信息
     *
     * @param webProperties Web 配置属性对象
     */
    public WebFrameworkUtils(WebProperties webProperties) {
        WebFrameworkUtils.properties = webProperties;
    }

    /**
     * 从 HTTP 请求头中获取租户编号
     * 考虑到其它 framework 组件也会使用到租户编号，所以统一在此提供
     *
     * @param request HTTP 请求对象
     * @return        租户编号，如果请求头中没有就返回 null
     */
    public static String getTenantId(HttpServletRequest request) {
        return request.getHeader(HEADER_TENANT_ID);
    }

    /**
     * 从 HTTP 请求头中获取访问的租户编号
     * 用于跨租户访问场景，表示要访问的目标租户ID
     * 与 getTenantId() 的区别: getTenantId() 是当前用户所属租户，getVisitTenantId() 是要访问的租户
     *
     * @param request HTTP 请求对象
     * @return        访问的租户编号，如果请求头中没有就返回 null
     */
    public static String getVisitTenantId(HttpServletRequest request) {
        return request.getHeader(HEADER_VISIT_TENANT_ID);
    }

    /**
     * 设置登录用户ID到请求属性中
     *
     * 功能说明:
     * - 将当前登录用户的ID存储到请求对象的属性中
     * - 通常在认证过滤器/拦截器中调用,将解析出的用户ID存储起来
     * - 后续的业务代码可以通过 getLoginUserId() 获取
     *
     * 使用场景:
     * - TokenAuthenticationFilter 验证 token 后,设置用户ID
     * - 在请求处理链中传递用户身份信息
     *
     * @param request Servlet 请求对象
     * @param userId  登录用户的ID
     */
    public static void setLoginUserId(ServletRequest request, String userId) {
        request.setAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_ID, userId);
    }

    /**
     * 设置登录用户类型到请求属性中
     *
     * @param request  请求对象
     * @param userType 用户类型
     */
    public static void setLoginUserType(ServletRequest request, Integer userType) {
        request.setAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_TYPE, userType);
    }

    /**
     * 从请求属性中获取当前登录用户的ID
     *
     * 功能说明:
     * - 从请求对象的属性中读取之前存储的用户ID
     * - 这个ID通常由认证过滤器通过 setLoginUserId() 方法设置
     * - 注意：该方法仅限于 framework 框架内部使用！！！
     *
     * @param request HTTP 请求对象
     * @return        用户ID，如果未设置或request为null则返回null
     */
    public static String getLoginUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return (String) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_ID);
    }

    /**
     * 获取当前登录用户的类型
     *
     * 功能说明:
     * - 支持两种获取方式:优先从请求属性获取，其次通过URL前缀推断
     * - 注意：该方法仅限于 web 相关的 framework 组件使用！！！
     *
     * @param request HTTP 请求对象
     * @return        用户类型，无法确定时返回 null
     */
    public static Integer getLoginUserType(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // 1. 优先从 Attribute 中获取
        Integer userType = (Integer) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_TYPE);
        if (userType != null) {
            return userType;
        }
        // 2. 其次基于 URL 前缀的约定
        if (request.getServletPath().startsWith(properties.getAdminApi().getPrefix())) {
            return UserTypeEnum.ADMIN.getValue();
        }
        if (request.getServletPath().startsWith(properties.getAppApi().getPrefix())) {
            return UserTypeEnum.MEMBER.getValue();
        }
        return null;
    }

    /**
     * 获取当前登录用户的ID(无参版本)
     *
     * 功能说明:
     * - 这是 getLoginUserId(HttpServletRequest) 的便捷方法
     * - 自动从 Spring 上下文中获取当前请求对象
     * - 适用于业务代码中无法直接获取 request 对象的场景
     *
     * @return 用户ID，如果无法获取则返回 null
     */
    public static String getLoginUserId() {
        HttpServletRequest request = getRequest();
        return getLoginUserId(request);
    }

    /**
     * 获取当前登录用户的类型(无参版本)
     *
     * 功能说明:
     * - 这是 getLoginUserType(HttpServletRequest) 的便捷方法
     * - 自动从 Spring 上下文中获取当前请求对象
     * - 适用于业务代码中无法直接获取 request 对象的场景
     *
     * @return 用户类型，如果无法获取则返回 null
     */
    public static Integer getLoginUserType() {
        HttpServletRequest request = getRequest();
        return getLoginUserType(request);
    }

    /**
     * 获取当前请求的终端类型
     *
     * 功能说明:
     *   - 从 HTTP 请求头 "terminal" 中获取终端类型
     *
     * 实现逻辑:
     * 1. 获取当前请求对象
     * 2. 如果请求为 null，返回未知终端类型(UNKNOWN)
     * 3. 从请求头中读取 terminal 值并解析为整数
     * 4. 如果解析失败，也返回未知终端类型
     *
     * @return 终端类型值，参考 {@link TerminalEnum}
     */
    public static Integer getTerminal() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return TerminalEnum.UNKNOWN.getTerminal();
        }
        String terminalValue = request.getHeader(HEADER_TERMINAL);
        return NumberUtil.parseInt(terminalValue, TerminalEnum.UNKNOWN.getTerminal());
    }

    /**
     * 设置通用返回结果到请求属性中
     *
     * 功能说明:
     *   - 将统一的返回结果对象存储到请求中
     *   - 通常在过滤器或拦截器中使用
     *   - 用于在请求处理链的不同阶段传递结果对象
     *
     * 使用场景:
     *   - 在异常处理器中设置错误响应
     *   - 在日志过滤器中记录返回结果
     *   - 在响应加密过滤器中获取原始结果
     *
     * @param request     Servlet 请求对象
     * @param apiResponse 通用返回结果对象
     */
    public static void setApiResponse(ServletRequest request, ApiResponse<?> apiResponse) {
        request.setAttribute(REQUEST_ATTRIBUTE_API_RESPONSE, apiResponse);
    }

    /**
     * 从请求属性中获取通用返回结果
     *
     * 功能说明:
     *   - 获取之前通过 setApiResponse() 存储的返回结果
     *   - 用于在请求处理链中访问已生成的响应结果
     *
     * @param request Servlet 请求对象
     * @return        通用返回结果对象，如果未设置则返回 null
     */
    public static ApiResponse<?> getApiResponse(ServletRequest request) {
        return (ApiResponse<?>) request.getAttribute(REQUEST_ATTRIBUTE_API_RESPONSE);
    }

    /**
     * 获取当前线程关联的 HTTP 请求对象
     *
     * 功能说明:
     *   - 从 Spring 的 RequestContextHolder 中获取当前请求
     *   - 这是一个基础工具方法，被其他无参方法调用
     *   - 利用 Spring MVC 的 ThreadLocal 机制实现请求对象的跨层传递
     *
     * 实现逻辑:
     *   1. 通过 RequestContextHolder 获取请求属性对象
     *   2. 判断是否为 ServletRequestAttributes 类型
     *   3. 如果是，则转换并提取 HttpServletRequest
     *   4. 如果不是(如异步请求或非Web环境)，返回 null
     *
     * 为什么这样实现:
     *   - RequestContextHolder 使用 ThreadLocal 存储请求对象
     *   - 每个 HTTP 请求在独立的线程中处理，不会冲突
     *   - 这样可以在任何层级的代码中获取当前请求，无需手动传递参数
     *
     * @return 当前 HTTP 请求对象，如果不在 Web 环境或非 Servlet 请求则返回 null
     */
    @SuppressWarnings("PatternVariableCanBeUsed")
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        return servletRequestAttributes.getRequest();
    }

    /**
     * 判断是否为 RPC 请求(基于URL)
     *
     * 功能说明:
     *   - 通过检查请求的 URI 前缀判断是否为 RPC 调用
     *   - 用于区分普通的 HTTP 请求和微服务之间的 RPC 调用
     *
     * 实现逻辑:
     *   - 判断请求 URI 是否以 RPC_API_PREFIX 开头
     *   - RPC_API_PREFIX 通常定义为 "/rpc" 等特殊前缀
     *
     * 使用场景:
     *   - 在拦截器中对 RPC 请求和普通请求做差异化处理
     *   - RPC 请求可能不需要某些认证或日志记录
     *
     * @param request HTTP 请求对象
     * @return        true 表示是 RPC 请求，false 表示普通 HTTP 请求
     */
    public static boolean isRpcRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith(RpcConstants.RPC_API_PREFIX);
    }

    /**
     * 判断是否为 RPC 接口类(基于类名约定)
     *
     * 功能说明:
     *   - 通过类名判断是否为 RPC 接口
     *   - 采用"约定优于配置"的设计原则
     *
     * 实现逻辑:
     *   - 约定: 所有以 "Api" 结尾的类名都认为是 RPC 接口
     *   - 例如: UserApi、OrderApi、ProductApi 等
     *
     * 使用场景:
     *   - 在 AOP 切面中识别 RPC 接口，应用特定的处理逻辑
     *   - 在代码生成工具中识别 RPC 接口，生成相应的代理代码
     *
     * 为什么这样实现:
     *   - 通过命名约定简化配置，无需额外的注解或配置文件
     *   - 保持代码简洁，提高开发效率
     *   - 符合团队的编码规范
     *
     * @param className 类名(不包含包名)，如 "UserApi"
     * @return          true 表示是 RPC 接口类，false 表示普通类
     */
    public static boolean isRpcRequest(String className) {
        return className.endsWith("Api");
    }

}
