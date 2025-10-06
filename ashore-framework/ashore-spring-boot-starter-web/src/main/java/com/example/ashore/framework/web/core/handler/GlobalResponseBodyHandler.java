package com.example.ashore.framework.web.core.handler;

import com.example.ashore.framework.common.pojo.ApiResponse;
import com.example.ashore.framework.web.core.util.WebFrameworkUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 全局响应结果（ResponseBody）处理器
 *
 * 作用：
 * 这个类是一个全局的响应体拦截器，主要用于在 Controller 方法返回响应体之前，对响应数据进行统一的处理。
 * 本项目中，它的主要职责是记录 Controller 的返回结果，以便后续的访问日志记录组件（ApiAccessLogFilter）能够获取到响应数据。
 * see {@link com.example.ashore.framework.apilog.core.filter.ApiAccessLogFilter}
 *
 * 设计理念说明：
 * - 在 Controller 返回时，会自己主动包上
 * - 实现 ResponseBodyAdvice 接口，对所有 Controller 的响应进行统一处理
 * - 这个类本质上是 AOP，它不应该改变 Controller 返回的数据结构
 */
@ControllerAdvice
public class GlobalResponseBodyHandler implements ResponseBodyAdvice {

    /**
     * 判断是否需要执行 beforeBodyWrite 方法
     *
     * @param returnType    返回值的类型信息，包含了方法的元数据（如方法对象、返回类型等）
     * @param converterType 即将使用的 HTTP 消息转换器类型（如 MappingJackson2HttpMessageConverter）
     * @return              true-需要处理该响应；false-跳过处理
     */
    @Override
    @SuppressWarnings("NullableProblems") // 避免 IDEA 警告
    public boolean supports(MethodParameter returnType, Class converterType) {
        // 防御性检查
        if (returnType.getMethod() == null) {
            return false;
        }
        // 核心判断：只拦截返回结果为 ApiResponse 类型的方法
        // 使用 == 而不是 instanceof，是为了精确匹配 ApiResponse 类型，不包括其子类
        return returnType.getMethod().getReturnType() == ApiResponse.class;
    }

    /**
     * 在响应体写入到 HTTP 响应之前，对响应体进行处理
     * 触发时机：
     * 该方法会在 Controller 方法执行完成后，响应数据被转换成 JSON（或其他格式）并写入到 HTTP 响应流之前调用。
     * 核心职责：
     * 将 Controller 返回的 ApiResponse 对象保存到当前请求的属性中，以便后续的过滤器（ApiAccessLogFilter）可以从请求中获取到响应结果，记录到访问日志。
     *
     * @param body                  Controller 方法返回的响应体对象（已经经过处理，但还未序列化）
     * @param returnType            返回值的类型信息，与 supports 方法中的参数相同
     * @param selectedContentType   选定的响应内容类型（如 application/json）
     * @param selectedConverterType 选定的 HTTP 消息转换器类型
     * @param request               当前的 HTTP 请求对象（Spring 封装的 ServerHttpRequest）
     * @param response              当前的 HTTP 响应对象（Spring 封装的 ServerHttpResponse）
     * @return                      处理后的响应体对象。可以返回原对象，也可以返回修改后的对象。
     *                              本方法返回原对象 body，因为我们不需要修改响应内容，只需要记录
     */
    @Override
    @SuppressWarnings("NullableProblems") // 避免 IDEA 警告
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 关键步骤：记录 Controller 的返回结果到 Request 属性中
        // 1. ((ServletServerHttpRequest) request).getServletRequest()：获取底层的 HttpServletRequest 对象
        // 2. (ApiResponse<?>) body：将响应体转换为 ApiResponse 类型
        // 3. setApiResponse()：调用工具类方法，将结果存储到 request.setAttribute("api_response", result)
        WebFrameworkUtils.setApiResponse(((ServletServerHttpRequest) request).getServletRequest(), (ApiResponse<?>) body);

        // 返回原始的响应体对象，不做任何修改
        // 这样可以保证响应数据的完整性，不会影响客户端接收到的数据
        return body;

        /*
         * 【过程详解】
         * 1. 将 ServerHttpRequest 强制转换为 ServletServerHttpRequest：
         *    - Spring 在处理 Servlet 请求时，会将 HttpServletRequest 包装为 ServerHttpRequest
         *    - 这里需要获取底层的 ServletRequest 对象来设置属性，所以需要强转
         *    - 调用 getServletRequest() 获取原始的 HttpServletRequest 对象
         *
         * 2. 将 body 强制转换为 ApiResponse<?> 类型：
         *    - 因为在 supports 方法中已经确保只有 ApiResponse 类型才会进入此方法
         *    - 所以这里的强制转换是安全的
         *
         * 3. 调用 WebFrameworkUtils.setApiResponse() 方法：
         *    - 这个方法会将 ApiResponse 对象存储到 request.setAttribute() 中
         *    - 键名为："api_response"
         *    - 这样后续的过滤器就可以通过 request.getAttribute("api_response") 获取到响应结果
         *
         * 4. 为什么要通过 Request 属性传递数据：
         *    - Request 对象在整个请求处理链中是共享的（Filter -> Interceptor -> Controller -> ResponseBodyAdvice -> Filter）
         *    - 这是一种常见的在不同组件之间传递数据的方式
         *    - ApiAccessLogFilter 在请求返回时需要记录响应结果，通过 Request 属性可以方便地获取
         *
         * 5. 返回原始 body 对象：
         *    - 我们的目的只是"记录"响应结果，而不是"修改"响应结果
         *    - 所以直接返回原对象，让后续的序列化流程继续进行
         */
    }

}
