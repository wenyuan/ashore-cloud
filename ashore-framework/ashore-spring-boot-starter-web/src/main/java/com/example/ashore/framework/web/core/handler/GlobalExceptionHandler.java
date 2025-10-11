package com.example.ashore.framework.web.core.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.example.ashore.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import com.example.ashore.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import com.example.ashore.framework.common.exception.BusinessException;
import com.example.ashore.framework.common.exception.util.BusinessExceptionUtils;
import com.example.ashore.framework.common.pojo.ApiResponse;
import com.example.ashore.framework.common.util.collection.SetUtils;
import com.example.ashore.framework.common.util.json.JsonUtils;
import com.example.ashore.framework.common.util.monitor.TracerUtils;
import com.example.ashore.framework.common.util.servlet.ServletUtils;
import com.example.ashore.framework.web.core.util.WebFrameworkUtils;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.common.util.concurrent.UncheckedExecutionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.ashore.framework.common.exception.enums.GlobalErrorCodeConstants.*;

/**
 * 全局异常处理器(整个项目的异常处理中枢)
 *
 * 功能说明：
 * - 统一捕获应用中抛出的各种异常
 * - 将异常转换为统一的 ApiResponse 格式返回给前端
 * - 记录异常日志到数据库，方便后续排查问题
 * - 提供友好的错误提示信息给用户
 *
 * 运行机制：
 * - Spring 容器启动时，扫描到 @RestControllerAdvice 注解，将本类注册为全局异常处理器
 * - 当 Controller 层方法执行过程中抛出异常时，
 *   Spring MVC 的 DispatcherServlet 在 doDispatch() 方法的 catch 块中捕获该异常
 * - DispatcherServlet 遍历 HandlerExceptionResolver 链，
 *   由 ExceptionHandlerExceptionResolver 查找匹配的 @ExceptionHandler 方法（按异常类型精确匹配）
 * - 执行对应的异常处理方法，将异常转换为 ApiResponse 对象
 * - 将 ApiResponse 序列化为 JSON 返回给前端（@RestControllerAdvice 自动处理）
 *
 * Spring 通过 AOP 机制自动识别带有 @RestControllerAdvice 注解的类，
 * 并在 Controller 层抛出异常时，自动调用本类中标注了 @ExceptionHandler 的方法。
 */
@RestControllerAdvice
@AllArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 忽略的 ServiceException 错误提示集合
     * 某些业务异常（如"无效的刷新令牌"）会频繁发生，为避免日志刷屏，将这些异常的错误信息添加到此集合中，处理时不打印详细日志。
     * static：本就是单例，只为标注这是一个与实例无关的常量配置
     */
    public static final Set<String> IGNORE_ERROR_MESSAGES = SetUtils.asSet("无效的刷新令牌");

    /**
     * 应用名称
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final String applicationName;

    /**
     * API 异常日志记录接口
     * 通过 Feign 调用远程服务（infra-server）的接口，将异常信息持久化到数据库。
     * 采用异步方式调用，避免记录日志影响主流程性能。
     */
    private final ApiErrorLogCommonApi apiErrorLogApi;

    /**
     * 处理所有类型的异常（兜底方法）
     *
     * 作用：提供给 Filter 层使用的统一异常处理入口
     * 原因：Filter 在 Spring MVC 的 DispatcherServlet 之前执行，不受 @ExceptionHandler 管控。
     *      但我们希望 Filter 中的异常也能被统一处理，所以提供此方法供 Filter 手动调用，保持异常处理逻辑的一致性。
     * 实现逻辑：通过 instanceof 判断异常类型，委托给对应的专用处理方法。这样可以复用已有的异常处理逻辑。
     *
     * @param request HTTP 请求对象，包含请求 URL、参数、IP 等信息
     * @param ex      捕获的异常对象（使用 Throwable 作为参数类型以兼容所有异常）
     * @return        统一格式的响应结果，包含错误码和错误信息
     */
    public ApiResponse<?> allExceptionHandler(HttpServletRequest request, Throwable ex) {
        // 请求参数缺失异常
        if (ex instanceof MissingServletRequestParameterException) {
            return missingServletRequestParameterExceptionHandler((MissingServletRequestParameterException) ex);
        }
        // 请求参数类型不匹配异常
        if (ex instanceof MethodArgumentTypeMismatchException) {
            return methodArgumentTypeMismatchExceptionHandler((MethodArgumentTypeMismatchException) ex);
        }
        // @Valid 校验失败异常（用于 @RequestBody）
        if (ex instanceof MethodArgumentNotValidException) {
            return methodArgumentNotValidExceptionExceptionHandler((MethodArgumentNotValidException) ex);
        }
        // 数据绑定异常（用于表单提交）
        if (ex instanceof BindException) {
            return bindExceptionHandler((BindException) ex);
        }
        // JSR-303 约束违反异常（用于方法参数校验）
        if (ex instanceof ConstraintViolationException) {
            return constraintViolationExceptionHandler((ConstraintViolationException) ex);
        }
        // 通用校验异常
        if (ex instanceof ValidationException) {
            return validationException((ValidationException) ex);
        }
        // 文件上传大小超限异常
        if (ex instanceof MaxUploadSizeExceededException) {
            return maxUploadSizeExceededExceptionHandler((MaxUploadSizeExceededException) ex);
        }
        // 请求路径不存在异常（旧版 Spring）
        if (ex instanceof NoHandlerFoundException) {
            return noHandlerFoundExceptionHandler((NoHandlerFoundException) ex);
        }
        // 请求资源不存在异常（新版 Spring）
        if (ex instanceof NoResourceFoundException) {
            return noResourceFoundExceptionHandler(request, (NoResourceFoundException) ex);
        }
        // HTTP 请求方法不支持异常（如接口要求 POST，但发送了 GET）
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            return httpRequestMethodNotSupportedExceptionHandler((HttpRequestMethodNotSupportedException) ex);
        }
        // HTTP 媒体类型不支持异常（如 Content-Type 不匹配）
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return httpMediaTypeNotSupportedExceptionHandler((HttpMediaTypeNotSupportedException) ex);
        }
        // 业务逻辑异常
        if (ex instanceof BusinessException) {
            return businessExceptionHandler((BusinessException) ex);
        }
        // Spring Security 权限不足异常
        if (ex instanceof AccessDeniedException) {
            return accessDeniedExceptionHandler(request, (AccessDeniedException) ex);
        }
        // 其他未知异常，交给兜底处理器
        return defaultExceptionHandler(request, ex);
    }

    /**
     * 处理 SpringMVC 请求参数缺失异常
     *
     * 触发场景：
     * 接口方法定义了 @RequestParam("name") 参数且未设置 required=false，但前端请求时未传递该参数，就会抛出此异常。
     *
     * @param ex 异常对象，包含缺失的参数名称
     * @return   错误响应，提示用户缺少哪个参数
     */
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public ApiResponse<?> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException ex) {
        log.warn("[missingServletRequestParameterExceptionHandler]", ex);
        return ApiResponse.error(BAD_REQUEST.getCode(), String.format("请求参数缺失:%s", ex.getParameterName()));
    }

    /**
     * 处理 SpringMVC 请求参数类型错误异常
     *
     * 触发场景：
     * 接口参数定义为 Integer 类型，但前端传递了无法转换为 Integer 的字符串。
     * 例如接口定义 @RequestParam("age") Integer age，前端请求 /test?age=abc
     *
     * @param ex 异常对象，包含类型不匹配的详细信息
     * @return   错误响应，提示参数类型错误
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<?> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException ex) {
        log.warn("[methodArgumentTypeMismatchExceptionHandler]", ex);
        return ApiResponse.error(BAD_REQUEST.getCode(), String.format("请求参数类型错误:%s", ex.getMessage()));
    }

    /**
     * 处理 SpringMVC 参数校验失败异常（@Valid 注解触发）
     *
     * 触发场景：
     * 使用 @Valid 或 @Validated 注解校验 @RequestBody 参数时，参数对象的字段未通过 JSR-303 校验规则（如 @NotNull、@Size 等）。
     *
     * 实现细节：
     * - 优先获取字段级别的错误信息（fieldError.getDefaultMessage()）
     * - 如果没有字段错误，则尝试获取对象级别的错误（用于组合校验）
     * - 将第一个错误信息返回给前端
     *
     * @param ex 异常对象，包含所有校验失败的字段信息
     * @return   错误响应，包含第一个校验失败的错误提示
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> methodArgumentNotValidExceptionExceptionHandler(MethodArgumentNotValidException ex) {
        log.warn("[methodArgumentNotValidExceptionExceptionHandler]", ex);
        // 获取校验失败的错误信息
        String errorMessage = null;
        FieldError fieldError = ex.getBindingResult().getFieldError();

        if (fieldError == null) {
            // 没有字段错误，可能是组合校验（类级别的校验注解，如 @ScriptAssert）
            List<ObjectError> allErrors = ex.getBindingResult().getAllErrors();
            if (CollUtil.isNotEmpty(allErrors)) {
                errorMessage = allErrors.get(0).getDefaultMessage();  // 获取第一个对象级别错误
            }
        } else {
            errorMessage = fieldError.getDefaultMessage();  // 获取字段错误提示
        }
        // 构造返回结果
        if (StrUtil.isEmpty(errorMessage)) {
            return ApiResponse.error(BAD_REQUEST);  // 无具体错误信息，返回默认提示
        }
        return ApiResponse.error(BAD_REQUEST.getCode(), String.format("请求参数不正确:%s", errorMessage));
    }

    /**
     * 处理 SpringMVC 参数绑定失败异常
     *
     * 触发场景：
     * 使用 @ModelAttribute 或表单提交时，Spring 将请求参数绑定到对象失败，或绑定后的对象未通过 @Valid 校验。
     * 示例：
     * // Controller 方法定义
     * public void update(@Valid @ModelAttribute UserDTO dto) { }
     * // 前端发送表单（username 为空） → 抛出 BindException
     *
     * 与 {@link MethodArgumentNotValidException} 的区别：
     * - MethodArgumentNotValidException：用于 @RequestBody（JSON 格式）
     * - BindException：用于 @ModelAttribute（表单格式或 URL 参数）
     *
     * @param ex 异常对象，包含绑定失败的字段信息
     * @return   错误响应，包含第一个绑定失败的错误提示
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<?> bindExceptionHandler(BindException ex) {
        log.warn("[handleBindException]", ex);
        FieldError fieldError = ex.getFieldError();
        assert fieldError != null; // 断言字段错误不为空，避免 IDEA 的 NullPointerException 警告
        return ApiResponse.error(BAD_REQUEST.getCode(), String.format("请求参数不正确:%s", fieldError.getDefaultMessage()));
    }

    /**
     * 处理 SpringMVC 请求体类型错误异常
     *
     * 触发场景：
     * @RequestBody 接收 JSON 数据时，字段类型无法反序列化。
     * 示例：
     * // DTO 定义
     * public class UserDTO {
     *     private Integer age;  // 定义为 Integer
     * }
     * // 前端发送 {"age": "abc"} → Jackson 反序列化失败 → 抛出 HttpMessageNotReadableException
     *
     * 实现逻辑：
     * 检查是否为类型转换异常（InvalidFormatException），提取错误值
     * 检查是否为 request body 缺失（前端未传递 JSON 数据）
     * 其他情况交给默认异常处理器
     *
     * @param ex 异常对象，可能包含多种原因导致的 HTTP 消息不可读
     * @return   错误响应，提示具体的类型错误或缺失信息
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @SuppressWarnings("PatternVariableCanBeUsed")  // 抑制 IDEA 的模式匹配建议（为了兼容旧版 Java）
    public ApiResponse<?> methodArgumentTypeInvalidFormatExceptionHandler(HttpMessageNotReadableException ex) {
        log.warn("[methodArgumentTypeInvalidFormatExceptionHandler]", ex);

        // 情况一：类型转换错误（如字符串转数字失败）
        if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException invalidFormatException = (InvalidFormatException) ex.getCause();
            return ApiResponse.error(BAD_REQUEST.getCode(), String.format("请求参数类型错误:%s", invalidFormatException.getValue()));
        }

        // 情况二：request body 缺失（前端未传递 JSON）
        if (StrUtil.startWith(ex.getMessage(), "Required request body is missing")) {
            return ApiResponse.error(BAD_REQUEST.getCode(), "请求参数类型错误: request body 缺失");
        }

        // 其他未知情况，交给默认异常处理器
        return defaultExceptionHandler(ServletUtils.getRequest(), ex);
    }

    /**
     * 处理 JSR-303 约束违反异常
     *
     * 触发场景：
     * 在 Controller 类上添加 @Validated 注解后，直接对方法参数进行校验（如 @NotNull、@Min、@Max 等注解标注在参数上），校验失败时抛出。
     * 示例：
     * @Validated  // 类级别启用校验
     * @RestController
     * public class UserController {
     *     public void delete(@NotNull(message = "用户ID不能为空") Long userId) { }
     * }
     * // 前端请求 /delete?userId=null → 抛出 ConstraintViolationException
     *
     * @param ex 异常对象，包含所有违反的约束信息
     * @return   错误响应，包含第一个约束违反的错误提示
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    public ApiResponse<?> constraintViolationExceptionHandler(ConstraintViolationException ex) {
        log.warn("[constraintViolationExceptionHandler]", ex);

        // 获取第一个约束违反信息
        ConstraintViolation<?> constraintViolation = ex.getConstraintViolations().iterator().next();
        return ApiResponse.error(BAD_REQUEST.getCode(), String.format("请求参数不正确:%s", constraintViolation.getMessage()));
    }

    /**
     * 处理 Dubbo Consumer 本地参数校验异常
     *
     * 触发场景：
     * 使用 Dubbo 进行 RPC 调用时，Consumer 端配置了本地参数校验（validation=true），调用远程服务前参数校验失败会抛出此异常。
     *
     * 为什么错误信息不明确：
     * Dubbo Consumer 端抛出的 ValidationException 只包含简单的字符串信息，不像 Spring 的校验异常那样包含详细的字段和错误信息，因此只能返回通用错误码。
     *
     * @param ex 异常对象，包含简单的错误描述
     * @return   错误响应，返回通用的请求参数不正确提示
     */
    @ExceptionHandler(value = ValidationException.class)
    public ApiResponse<?> validationException(ValidationException ex) {
        log.warn("[constraintViolationExceptionHandler]", ex);
        // 无法拼接明细的错误信息，因为 Dubbo Consumer 抛出 ValidationException 异常时，是直接的字符串信息，且人类不可读
        return ApiResponse.error(BAD_REQUEST);
    }

    /**
     * 处理上传文件过大异常
     *
     * 触发场景：
     * 使用 @RequestParam("file") MultipartFile 接收文件上传时，文件大小超过配置的限制（spring.servlet.multipart.max-file-size）。
     * 示例配置：
     * spring:
     *   servlet:
     *     multipart:
     *       max-file-size: 10MB  # 单个文件最大 10MB
     *       max-request-size: 100MB  # 整个请求最大 100MB
     *
     * @param ex 异常对象，包含超限的文件信息
     * @return   错误响应，提示文件过大
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<?> maxUploadSizeExceededExceptionHandler(MaxUploadSizeExceededException ex) {
        return ApiResponse.error(BAD_REQUEST.getCode(), "上传文件过大，请调整后重试");
    }

    /**
     * 处理 SpringMVC 请求地址不存在
     *
     * 注意，它需要设置如下两个配置项：
     * 1. spring.mvc.throw-exception-if-no-handler-found 为 true
     * 2. spring.mvc.static-path-pattern 为 /statics/**
     */
    /**
     * 处理 SpringMVC 请求地址不存在异常（旧版机制）
     *
     * 触发场景：
     * 请求的 URL 路径在所有 Controller 中都没有匹配的 @RequestMapping，
     * 且配置了以下两项才会抛出此异常（否则返回 404 页面）：
     * spring:
     *   mvc:
     *     throw-exception-if-no-handler-found: true  # 找不到处理器时抛异常
     *     static-path-pattern: /statics/**  # 静态资源路径（避免与动态路由冲突）
     *
     * @param ex 异常对象，包含请求的 URL
     * @return   错误响应，提示请求地址不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResponse<?> noHandlerFoundExceptionHandler(NoHandlerFoundException ex) {
        log.warn("[noHandlerFoundExceptionHandler]", ex);
        return ApiResponse.error(NOT_FOUND.getCode(), String.format("请求地址不存在:%s", ex.getRequestURL()));
    }

    /**
     * 处理 SpringMVC 请求资源不存在异常（新版机制）
     *
     * 触发场景：
     * Spring 6.0+ 版本引入的新异常类型，用于替代 NoHandlerFoundException。
     * 当请求的静态资源或动态路径不存在时抛出。
     * 默认抛出，不需要额外配置。
     *
     * @param req HTTP 请求对象
     * @param ex  异常对象，包含请求的资源路径
     * @return    错误响应，提示请求地址不存在
     */
    @ExceptionHandler(NoResourceFoundException.class)
    private ApiResponse<?> noResourceFoundExceptionHandler(HttpServletRequest req, NoResourceFoundException ex) {
        log.warn("[noResourceFoundExceptionHandler: {}]", req.getRequestURI(), ex);
        return ApiResponse.error(NOT_FOUND.getCode(), String.format("请求地址不存在:%s", ex.getResourcePath()));
    }

    /**
     * 处理 HTTP 请求方法不匹配异常
     *
     * @param ex 异常对象，包含支持的请求方法列表
     * @return   错误响应，提示请求方法不正确
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<?> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException ex) {
        log.warn("[httpRequestMethodNotSupportedExceptionHandler]", ex);
        return ApiResponse.error(METHOD_NOT_ALLOWED.getCode(), String.format("请求方法不正确:%s", ex.getMessage()));
    }

    /**
     * 处理 HTTP 媒体类型（Content-Type）不支持异常
     *
     * @param ex 异常对象，包含支持的媒体类型列表
     * @return   错误响应，提示请求类型不正确
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ApiResponse<?> httpMediaTypeNotSupportedExceptionHandler(HttpMediaTypeNotSupportedException ex) {
        log.warn("[httpMediaTypeNotSupportedExceptionHandler]", ex);
        return ApiResponse.error(BAD_REQUEST.getCode(), String.format("请求类型不正确:%s", ex.getMessage()));
    }

    /**
     * 处理 Spring Security 权限不足异常
     *
     * 触发场景：
     * 在 Controller 方法上使用 @PreAuthorize 注解进行权限控制，
     * 当前登录用户不满足权限表达式时，Spring Security 的 AOP 拦截器会抛出此异常。
     *
     * 实现细节：
     * 记录日志时包含用户 ID 和请求 URL，便于排查权限问题。
     *
     * @param req HTTP 请求对象，用于获取用户 ID 和请求 URL
     * @param ex  异常对象，包含权限不足的详细信息
     * @return    错误响应，返回 403 禁止访问
     */
    @ExceptionHandler(value = AccessDeniedException.class)
    public ApiResponse<?> accessDeniedExceptionHandler(HttpServletRequest req, AccessDeniedException ex) {
        log.warn("[accessDeniedExceptionHandler][userId({}) 无法访问 url({})]", WebFrameworkUtils.getLoginUserId(req),
                req.getRequestURL(), ex);
        return ApiResponse.error(FORBIDDEN);
    }

    /**
     * 处理 Guava 缓存执行异常
     *
     * 触发场景：
     * 使用 Guava Cache 的 LoadingCache 时，CacheLoader 加载数据抛出未检查异常，
     * Guava 会将其包装为 UncheckedExecutionException 抛出。
     * 示例：
     * LoadingCache<Long, User> cache = CacheBuilder.newBuilder()
     *     .build(new CacheLoader<Long, User>() {
     *         public User load(Long id) {
     *             throw new ServiceException(500, "加载用户失败");  // 抛出异常
     *         }
     *     });
     * cache.get(1L);  // 抛出 UncheckedExecutionException，cause 为 ServiceException
     *
     * 实现逻辑：
     * 提取真实的异常原因（ex.getCause()），重新委托给 allExceptionHandler 处理，
     * 这样就能按照真实的异常类型返回对应的错误信息。
     *
     * @param req HTTP 请求对象
     * @param ex  异常对象，包装了真实的执行异常
     * @return    错误响应，根据真实异常类型返回对应错误
     */
    @ExceptionHandler(value = UncheckedExecutionException.class)
    public ApiResponse<?> uncheckedExecutionExceptionHandler(HttpServletRequest req, UncheckedExecutionException ex) {
        // 提取真实异常，重新进入异常处理流程
        return allExceptionHandler(req, ex.getCause());
    }

    /**
     * 处理业务逻辑异常（BusinessException）
     *
     * 实现逻辑：
     * - 检查异常信息是否在忽略列表中（如"无效的刷新令牌"），避免频繁打印日志
     * - 如果不在忽略列表，只打印第一层堆栈信息（非 BusinessExceptionUtil 类的调用位置）
     * - 使用 warn 级别日志，避免误以为是系统错误
     * - 返回异常中的错误码和错误信息
     *
     * 为什么只打印第一层堆栈：
     * 业务异常通常通过 BusinessExceptionUtil.exception() 工具方法抛出，
     * 完整堆栈会包含很多工具类的调用信息，没有实际意义。只打印业务代码的调用位置即可定位问题。
     *
     * @param ex 业务异常对象，包含错误码和错误信息
     * @return   错误响应，包含业务错误码和错误信息
     */
    @ExceptionHandler(value = BusinessException.class)
    public ApiResponse<?> businessExceptionHandler(BusinessException ex) {
        // 检查是否在忽略列表中
        if (!IGNORE_ERROR_MESSAGES.contains(ex.getMessage())) {
            // 只打印第一层堆栈信息，避免日志过多
            try {
                StackTraceElement[] stackTraces = ex.getStackTrace();
                for (StackTraceElement stackTrace : stackTraces) {
                    // 跳过 ServiceExceptionUtil 类的堆栈，找到真实的业务代码调用位置
                    if (ObjUtil.notEqual(stackTrace.getClassName(), BusinessExceptionUtils.class.getName())) {
                        log.warn("[businessExceptionHandler]\n\t{}", stackTrace);
                        break;
                    }
                }
            } catch (Exception ignored) {
                // 忽略日志记录异常，避免影响主流程
            }
        }
        // 返回业务异常信息
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理系统异常（兜底处理器）
     *
     * 作用：
     * 处理所有未被上述特定处理器捕获的异常，是最后的防线（防止系统返回 500 错误页面或堆栈信息泄露），
     * 确保任何异常都能被转换为统一的 ApiResponse 格式返回，避免暴露系统内部错误信息。
     *
     * 实现逻辑：
     * - 特殊处理：检查 cause 是否为 BusinessException，是则按业务异常处理（某些框架会包装异常）
     * - 检查是否为数据库表不存在异常，返回对应模块的开启提示
     * - 记录 error 级别日志（包含完整堆栈）
     * - 调用 createExceptionLog() 将异常信息持久化到数据库
     * - 返回通用的系统异常错误
     *
     * 为什么要检查 cause：
     * 某些框架（如 MyBatis、事务管理器）会将原始异常包装后再抛出，
     * 导致外层异常类型不是 BusinessException，但实际上是业务异常。
     * 通过检查 cause 可以正确识别这种情况。
     *
     * @param req HTTP 请求对象，用于记录请求信息到异常日志
     * @param ex  异常对象，可能是任何类型的异常
     * @return    错误响应，返回通用系统异常（或具体的业务错误）
     */
    @ExceptionHandler(value = Exception.class)  // 捕获所有 Exception 类型的异常
    public ApiResponse<?> defaultExceptionHandler(HttpServletRequest req, Throwable ex) {
        // 特殊：如果是 BusinessException 的异常，则直接返回
        // 原因：BusinessException 被多层包装后无法被全局 ExceptionHandler 捕获（被包装为 UncheckedExecutionException / ExecutionException / CompletionException 等）
        // 比如：调用 FeignClient 接口时，服务提供方的报错信息无法正确传递到调用方
        if (ex.getCause() != null && ex.getCause() instanceof BusinessException) {
            return businessExceptionHandler((BusinessException) ex.getCause());
        }

        // 情况一：处理数据库表不存在异常（可能是模块未启用）
        ApiResponse<?> tableNotExistsResult = handleTableNotExists(ex);
        if (tableNotExistsResult != null) {
            return tableNotExistsResult;
        }

        // 情况二：真正的系统异常
        log.error("[defaultExceptionHandler]", ex);
        createExceptionLog(req, ex);  // 记录 error 级别日志，包含完整堆栈
        return ApiResponse.error(INTERNAL_SERVER_ERROR.getCode(), INTERNAL_SERVER_ERROR.getMsg());  // 返回通用错误
    }

    /**
     * 创建异常日志并异步保存到数据库
     *
     * @param req HTTP 请求对象，用于提取请求信息
     * @param e   异常对象，用于提取异常信息
     */
    private void createExceptionLog(HttpServletRequest req, Throwable e) {
        ApiErrorLogCreateReqDTO errorLog = new ApiErrorLogCreateReqDTO();
        try {
            // 构建异常日志对象
            buildExceptionLog(errorLog, req, e);
            // 异步保存到数据库（通过 Feign 调用 infra-server 的接口）
            apiErrorLogApi.createApiErrorLogAsync(errorLog);
        } catch (Throwable th) {
            // 记录日志失败不影响主流程，只打印 error 日志
            log.error("[createExceptionLog][url({}) log({}) 发生异常]", req.getRequestURI(),  JsonUtils.toJsonString(errorLog), th);
        }
    }

    /**
     * 构建异常日志对象
     * 作用：
     * 从 HTTP 请求和异常对象中提取详细信息，填充到 ApiErrorLogCreateReqDTO 中。
     * 记录的信息包括：
     * - 用户信息：userId（用户 ID）、userType（用户类型）
     * - 异常信息：异常类名、异常消息、根异常消息、异常堆栈
     * - 异常位置：异常发生的类名、文件名、方法名、行号
     * - 请求信息：traceId（链路追踪ID）、applicationName（应用名）、requestUrl（请求URL）
     * - 请求参数：query（URL 参数）、body（请求体）
     * - 客户端信息：requestMethod（请求方法）、userAgent（浏览器信息）、userIp（客户端IP）
     * - 时间信息：exceptionTime（异常发生时间）
     *
     * @param errorLog 待填充的异常日志对象
     * @param request  HTTP 请求对象
     * @param e        异常对象
     */
    private void buildExceptionLog(ApiErrorLogCreateReqDTO errorLog, HttpServletRequest request, Throwable e) {
        // 1. 处理用户信息
        errorLog.setUserId(WebFrameworkUtils.getLoginUserId(request));      // 从 SecurityContext 或 Token 中获取用户 ID
        errorLog.setUserType(WebFrameworkUtils.getLoginUserType(request));  // 获取用户类型

        // 2. 设置异常字段
        errorLog.setExceptionName(e.getClass().getName());                            // 异常类的全限定名
        errorLog.setExceptionMessage(ExceptionUtil.getMessage(e));                    // 异常的 message
        errorLog.setExceptionRootCauseMessage(ExceptionUtil.getRootCauseMessage(e));  // 根异常的 message（处理异常嵌套）
        errorLog.setExceptionStackTrace(ExceptionUtil.stacktraceToString(e));         // 完整的异常堆栈字符串

        // 3. 设置异常发生位置（第一层堆栈信息）
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        Assert.notEmpty(stackTraceElements, "异常 stackTraceElements 不能为空");  // 断言堆栈不为空
        StackTraceElement stackTraceElement = stackTraceElements[0];                    // 获取第一个堆栈元素（异常抛出位置）
        errorLog.setExceptionClassName(stackTraceElement.getClassName());               // 异常发生的类名
        errorLog.setExceptionFileName(stackTraceElement.getFileName());                 // 异常发生的文件名
        errorLog.setExceptionMethodName(stackTraceElement.getMethodName());             // 异常发生的方法名
        errorLog.setExceptionLineNumber(stackTraceElement.getLineNumber());             // 异常发生的行号

        // 4. 设置其它字段
        errorLog.setTraceId(TracerUtils.getTraceId());    // 链路追踪 ID（用于关联日志）
        errorLog.setApplicationName(applicationName);     // 应用名称
        errorLog.setRequestUrl(request.getRequestURI());  // 请求 URL

        // 5. 设置请求参数
        Map<String, Object> requestParams = MapUtil.<String, Object>builder()
                .put("query", ServletUtils.getParamMap(request))        // URL 参数（?key=value）
                .put("body", ServletUtils.getBody(request)).build();    // 请求体（POST JSON）
        errorLog.setRequestParams(JsonUtils.toJsonString(requestParams));  // 序列化为 JSON 字符串

        // 6. 设置客户端信息
        errorLog.setRequestMethod(request.getMethod());             // 请求方法
        errorLog.setUserAgent(ServletUtils.getUserAgent(request));  // User-Agent
        errorLog.setUserIp(ServletUtils.getClientIP(request));      // 客户端 IP

        // 7. 设置时间
        errorLog.setExceptionTime(LocalDateTime.now());
    }

    /**
     * 处理数据库表不存在异常
     *
     * 触发场景：
     * 当访问的功能模块对应的数据库表未创建（或模块未启用）时，MyBatis 执行 SQL 会抛出 "Table doesn't exist" 异常。
     *
     * 实现逻辑：
     * - 提取根异常的错误信息（getRootCauseMessage）
     * - 检查是否包含 "doesn't exist" 关键字
     * - 根据表名前缀（如 report_、bpm_、mp_ 等）判断是哪个模块
     * - 返回对应模块的提示信息
     *
     * 支持的模块：TODO...后期根据模块的添加不断补充
     * - report_：报表模块
     * - bpm_：工作流模块
     *
     * @param ex 异常对象
     * @return   如果是表不存在异常，返回对应模块的提示信息；否则返回 null
     */
    private ApiResponse<?> handleTableNotExists(Throwable ex) {
        String message = ExceptionUtil.getRootCauseMessage(ex);  // 获取根异常消息

        // 检查是否为表不存在异常
        if (!message.contains("doesn't exist")) {
            return null;  // 不是表不存在异常，返回 null 由调用方继续处理
        }

        // 根据表名前缀判断模块，返回对应提示
        // 1. 数据报表
        if (message.contains("report_")) {
            log.error("[报表模块 ashore-module-report - 表结构未导入]");
            return ApiResponse.error(NOT_IMPLEMENTED.getCode(),
                    "[报表模块 ashore-module-report - 表结构未导入]");
        }
        // 2. 工作流
        if (message.contains("bpm_")) {
            log.error("[工作流模块 ashore-module-bpm - 表结构未导入]");
            return ApiResponse.error(NOT_IMPLEMENTED.getCode(),
                    "[工作流模块 ashore-module-bpm - 表结构未导入]");
        }

        return null;  // 未匹配到已知模块，返回 null
    }

}
