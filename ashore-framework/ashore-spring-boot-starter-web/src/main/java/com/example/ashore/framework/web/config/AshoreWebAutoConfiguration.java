package com.example.ashore.framework.web.config;

import cn.hutool.core.util.StrUtil;
import com.example.ashore.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import com.example.ashore.framework.common.enums.WebFilterOrderEnum;
import com.example.ashore.framework.web.core.filter.CacheRequestBodyFilter;
import com.example.ashore.framework.web.core.filter.DemoFilter;
import com.example.ashore.framework.web.core.handler.GlobalExceptionHandler;
import com.example.ashore.framework.web.core.handler.GlobalResponseBodyHandler;
import com.example.ashore.framework.web.core.util.WebFrameworkUtils;
import com.google.common.collect.Maps;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.function.Predicate;

/**
 * 自动配置类，用于配置 Web 层基础设施
 * - API 路径前缀
 * - 全局异常处理
 * - 响应体处理
 * - 跨域配置
 * - 过滤器
 */
@AutoConfiguration
@EnableConfigurationProperties(WebProperties.class) // 注册配置类
public class AshoreWebAutoConfiguration {

    /**
     * 应用名
     */
    @Value("${spring.application.name}")
    private String applicationName;

    // =============== API 路径配置 ===============

    /**
     * 配置自定义的 WebMvc 注册器，用于设置 API 路径前缀
     * 作用：自定义 RequestMappingHandlerMapping，为不同类型的 Controller 设置不同的 API 路径前缀（如 /admin-api、/app-api）
     *
     * @param webProperties 配置类，包含了前缀和 Controller 包的配置信息
     * @return              一个匿名类，实现 WebMvcRegistrations 接口，用于自定义 Spring MVC 的组件注册(替换默认的 RequestMappingHandlerMapping)
     */
    @Bean
    public WebMvcRegistrations webMvcRegistrations(WebProperties webProperties) {
        return new WebMvcRegistrations() {

            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                var mapping = new RequestMappingHandlerMapping();
                // 实例化时就带上前缀
                mapping.setPathPrefixes(buildPathPrefixes(webProperties));
                return mapping;
            }

            /**
             * 构建路径前缀映射表
             * 作用：为符合条件的 @RestController 添加 API 前缀
             *
             * @param webProperties Web 配置类，包含了前缀和 Controller 包的配置信息
             * @return              Map<String, Predicate<Class<?>>> 前缀 → 匹配条件的映射
             */
            private Map<String, Predicate<Class<?>>> buildPathPrefixes(WebProperties webProperties) {
                // 路径匹配，使用 . 作为分隔符(非默认的 /)
                AntPathMatcher antPathMatcher = new AntPathMatcher(".");
                // key: String 类型(表示路径前缀，如 /admin)，value: 一个 Predicate<Class<?>>(即一个函数式接口,用于判断某个类是否符合条件)
                Map<String, Predicate<Class<?>>> pathPrefixes = Maps.newLinkedHashMapWithExpectedSize(2);
                // 为 admin 和 app API 添加前缀规则
                putPathPrefix(pathPrefixes, webProperties.getAdminApi(), antPathMatcher);
                putPathPrefix(pathPrefixes, webProperties.getAppApi(), antPathMatcher);
                return pathPrefixes;
            }

            /**
             * 设置 API 前缀及其匹配条件
             * 作用：为符合条件的 @RestController 添加 API 前缀
             *      仅匹配指定 controller 包下的 @RestController 类
             *
             * @param pathPrefixes 路径前缀映射表
             * @param api          API 配置信息（包含前缀和 controller 包路径）
             * @param matcher      Ant 路径匹配器
             */
            private void putPathPrefix(Map<String, Predicate<Class<?>>> pathPrefixes, WebProperties.Api api, AntPathMatcher matcher) {
                if (api == null || StrUtil.isEmpty(api.getPrefix())) {
                    return;
                }
                pathPrefixes.put(api.getPrefix(), // api 前缀
                        clazz -> clazz.isAnnotationPresent(RestController.class) // 只匹配带有 @RestController 注解的类
                                && matcher.match(api.getController(), clazz.getPackage().getName())); // 并且类的包名与配置中的 controller 路径匹配
            }

        };
    }

    // =============== API 路径配置 =============== 结束

    // =============== 注册全局处理器 ===============

    /**
     * 注册全局异常处理器
     * 作用：统一处理应用中的异常
     *
     * @param apiErrorLogApi          错误日志记录 API，用于记录异常信息
     * @return GlobalExceptionHandler 全局异常处理器实例
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler(ApiErrorLogCommonApi apiErrorLogApi) {
        return new GlobalExceptionHandler(applicationName, apiErrorLogApi);
    }

    /**
     * 注册全局响应体处理器
     * 作用：统一处理 Controller 返回结果
     *
     * @return GlobalResponseBodyHandler 全局响应体处理器实例
     */
    @Bean
    public GlobalResponseBodyHandler globalResponseBodyHandler() {
        return new GlobalResponseBodyHandler();
    }

    // =============== 注册全局处理器 =============== 结束

    // =============== 注册工具类 ===============

    /**
     * 注册 Web 框架工具类
     * 作用：将工具类注册为 Bean，以便使用配置属性
     *
     * @param webProperties      Web 配置属性
     * @return WebFrameworkUtils Web 框架工具类实例
     */
    @Bean
    @SuppressWarnings("InstantiationOfUtilityClass")
    public WebFrameworkUtils webFrameworkUtils(WebProperties webProperties) {
        // 由于 WebFrameworkUtils 需要使用到 webProperties 属性，所以注册为一个 Bean
        return new WebFrameworkUtils(webProperties);
    }

    // =============== 注册工具类 =============== 结束

    // =============== 注册 Servlet Filter（过滤器），它们会在请求到达 Controller 之前进行处理 ===============

    /**
     * 注册跨域过滤器
     * 作用：
     * - 解决前后端分离时的跨域问题（CORS）
     * - 允许浏览器从不同域名访问后端 API
     * - 当前配置成允许所有源、所有请求头、所有请求方法
     * 执行顺序：{@link WebFilterOrderEnum#CORS_FILTER}
     *
     * @return FilterRegistrationBean<CorsFilter> 跨域过滤器注册 Bean
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterBean() {
        // 创建 CorsConfiguration 对象
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*"); // 设置访问源地址
        config.addAllowedHeader("*"); // 设置访问源请求头
        config.addAllowedMethod("*"); // 设置访问源请求方法
        // 创建 UrlBasedCorsConfigurationSource 对象
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 对接口配置跨域设置
        return createFilterBean(new CorsFilter(source), WebFilterOrderEnum.CORS_FILTER);
    }

    /**
     * 注册请求体缓存过滤器
     * 作用：
     * - 缓存 HTTP 请求的 body 内容
     * - 因为请求流只能读取一次，缓存后可以在多个地方重复读取（如日志记录、参数校验）
     * 执行顺序：{@link WebFilterOrderEnum#REQUEST_BODY_CACHE_FILTER}
     *
     * @return FilterRegistrationBean<CacheRequestBodyFilter> 请求体缓存过滤器注册 Bean
     */
    @Bean
    public FilterRegistrationBean<CacheRequestBodyFilter> requestBodyCacheFilter() {
        return createFilterBean(new CacheRequestBodyFilter(), WebFilterOrderEnum.REQUEST_BODY_CACHE_FILTER);
    }

    /**
     * 注册演示模式过滤器（条件注册）
     * 作用：
     * - 仅当配置 ashore.demo=true 时生效
     * - 用于演示环境，通常会拦截写操作（增删改）防止数据被修改
     * 执行顺序：{@link WebFilterOrderEnum#DEMO_FILTER}
     *
     * @return FilterRegistrationBean<DemoFilter> 演示模式过滤器注册 Bean
     */
    @Bean
    @ConditionalOnProperty(value = "ashore.demo", havingValue = "true")
    public FilterRegistrationBean<DemoFilter> demoFilter() {
        return createFilterBean(new DemoFilter(), WebFilterOrderEnum.DEMO_FILTER);
    }

    /**
     * 创建过滤器注册 Bean 的通用方法
     * 作用：封装过滤器注册逻辑，设置过滤器及其执行顺序，用来简化 Filter 注册的代码
     *
     * Tips：
     * 在 Spring Boot 中注册 Filter 有两种方式：
     * 1. 直接返回 Filter               - Spring 会自动注册，但无法控制顺序
     * 2. 返回 FilterRegistrationBean  - 可以精确控制顺序、URL 匹配等
     * 这个方法就是将第 1 种方式转换为第 2 种，让代码更简洁
     *
     * @param filter                     过滤器实例
     * @param order                      过滤器执行顺序 {@link WebFilterOrderEnum}
     * @return FilterRegistrationBean<T> 过滤器注册 Bean
     *
     * @param <T> 确保传入的必须是 Filter 类型，并且返回值类型自动匹配，保证类型安全
     */
    public static <T extends Filter> FilterRegistrationBean<T> createFilterBean(T filter, Integer order) {
        FilterRegistrationBean<T> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(order);
        return bean;
    }

    // =============== 注册 Servlet Filter（过滤器） =============== 结束

    // =============== HTTP 客户端配置 ===============

    /**
     * 注册用于微服务间调用的 RestTemplate HTTP 客户端
     * 作用：
     * 创建的 RestTemplate 实例具备服务发现 + 负载均衡能力
     * 可以直接使用服务名调用，如 restTemplate.getForObject("http://user-service/api/user/1", User.class)
     * Spring Cloud LoadBalancer 会自动将服务名解析为实际的 IP:Port，并在多个实例间负载均衡
     *
     * 注意：
     * 因为带了 @LoadBalanced，所以只能用于微服务间 HTTP 通信！！！
     * 如果请求其他URL，会尝试将 URL 中的 host 当作服务名去服务注册中心查找，因为找不到服务而报错
     *
     * @param restTemplateBuilder Spring Boot 自动配置提供的 RestTemplate 构建器
     *                            已预配置超时时间、消息转换器等默认设置，省的自己设置超时时间等
     *                            {@link RestTemplateAutoConfiguration#restTemplateBuilder}
     * @return RestTemplate       带负载均衡功能的 RestTemplate 实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "loadBalancedRestTemplate") // 仅当容器中不存在同名 Bean 时才创建
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

}
