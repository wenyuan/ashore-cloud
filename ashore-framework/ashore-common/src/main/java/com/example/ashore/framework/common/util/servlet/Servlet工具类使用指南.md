# Servlet 工具类使用指南

## 一、包概览

`servlet` 包提供了 Servlet 请求响应处理的工具类，主要包含：

- **ServletUtils**: Servlet 请求响应处理工具类

该类封装了常用的 Servlet 操作，简化了 HTTP 请求响应的处理。

---

## 二、ServletUtils 类详解

### 2.1 类的整体介绍

`ServletUtils` 是 Servlet 工具类，主要解决以下问题：

1. **JSON 响应**: 简化 JSON 响应的返回
2. **请求信息获取**: 获取 User-Agent、客户端 IP 等信息
3. **请求对象获取**: 在非 Controller 层获取当前请求对象
4. **请求体处理**: 获取请求体内容（JSON）
5. **参数获取**: 获取请求参数和请求头

**为什么要二次封装？**
- 统一 JSON 响应格式
- 简化 Servlet API 的使用
- 提供便捷的工具方法

**底层依赖**: Hutool (`JakartaServletUtil`)、Spring Web

### 2.2 核心方法详解

#### `writeJSON(HttpServletResponse response, Object object)` - 返回 JSON 响应
**作用**: 将对象序列化为 JSON 并写入响应

**参数**:
- `response`: HTTP 响应对象
- `object`: 待序列化的对象

**返回值**: 无

**使用示例**:
```java
// 示例1：返回成功响应
@GetMapping("/test")
public void test(HttpServletResponse response) {
    CommonResult<String> result = CommonResult.success("操作成功");
    ServletUtils.writeJSON(response, result);
}

// 示例2：返回错误响应
public void handleError(HttpServletResponse response, String message) {
    CommonResult<?> result = CommonResult.error(500, message);
    ServletUtils.writeJSON(response, result);
}

// 示例3：返回对象
public void returnUser(HttpServletResponse response, Long userId) {
    User user = userService.getUser(userId);
    ServletUtils.writeJSON(response, user);
}
```

**项目实际使用场景**:
- **认证失败响应**: 在 `AuthenticationEntryPoint` 中返回认证失败信息
  ```java
  // ashore-framework/ashore-spring-boot-starter-security/src/main/java/com/example/ashore/framework/security/core/handler/AuthenticationEntryPointImpl.java
  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
                       AuthenticationException e) {
      log.warn("用户未认证，url: {}", request.getRequestURI(), e);

      // 返回 401 错误
      CommonResult<?> result = CommonResult.error(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(),
          GlobalErrorCodeConstants.UNAUTHORIZED.getMsg());

      ServletUtils.writeJSON(response, result);
  }
  ```

- **权限拒绝响应**: 在 `AccessDeniedHandler` 中返回权限不足信息
  ```java
  // ashore-framework/ashore-spring-boot-starter-security/src/main/java/com/example/ashore/framework/security/core/handler/AccessDeniedHandlerImpl.java
  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
                     AccessDeniedException e) {
      log.warn("用户无权限访问，url: {}", request.getRequestURI(), e);

      // 返回 403 错误
      CommonResult<?> result = CommonResult.error(GlobalErrorCodeConstants.FORBIDDEN.getCode(),
          GlobalErrorCodeConstants.FORBIDDEN.getMsg());

      ServletUtils.writeJSON(response, result);
  }
  ```

**响应格式**:
```json
// Content-Type: application/json;charset=UTF-8
{
    "code": 200,
    "data": "操作成功",
    "msg": null
}
```

**注意事项**:
1. **Content-Type**: 自动设置为 `application/json;charset=UTF-8`
2. **编码**: 使用 UTF-8 编码，避免中文乱码
3. **异常处理**: 序列化失败会抛出异常

---

#### `getUserAgent(HttpServletRequest request)` - 获取 User-Agent
**作用**: 获取请求的 User-Agent（用户代理字符串）

**参数**:
- `request`: HTTP 请求对象

**返回值**: User-Agent 字符串，如果不存在返回空字符串

**使用示例**:
```java
// 示例1：获取 User-Agent
String userAgent = ServletUtils.getUserAgent(request);
// 结果："Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36..."

// 示例2：记录日志
String userAgent = ServletUtils.getUserAgent(request);
log.info("用户访问，User-Agent: {}", userAgent);

// 示例3：判断设备类型
String userAgent = ServletUtils.getUserAgent(request);
if (userAgent.contains("Mobile")) {
    // 移动设备
    return "mobile.html";
} else {
    // PC 设备
    return "pc.html";
}
```

**项目实际使用场景**:
- **登录日志**: 记录用户登录时的 User-Agent
  ```java
  // ashore-module-system/ashore-module-system-biz/src/main/java/com/example/ashore/module/system/service/auth/AuthServiceImpl.java
  @Override
  public LoginUser login(LoginReqVO reqVO) {
      // 执行登录逻辑
      LoginUser loginUser = doLogin(reqVO);

      // 记录登录日志
      LoginLogCreateReqDTO loginLog = new LoginLogCreateReqDTO();
      loginLog.setUsername(reqVO.getUsername());
      loginLog.setUserAgent(ServletUtils.getUserAgent());  // 记录 User-Agent
      loginLog.setClientIp(ServletUtils.getClientIP());
      loginLogService.createLoginLog(loginLog);

      return loginUser;
  }
  ```

- **API 访问日志**: 记录 API 访问的 User-Agent
  ```java
  // ashore-framework/ashore-spring-boot-starter-web/src/main/java/com/example/ashore/framework/apilog/core/interceptor/ApiAccessLogInterceptor.java
  @Override
  public void afterCompletion(HttpServletRequest request, ...) {
      ApiAccessLogCreateReqDTO accessLog = new ApiAccessLogCreateReqDTO();
      accessLog.setUserAgent(ServletUtils.getUserAgent(request));
      accessLog.setUrl(request.getRequestURI());
      accessLog.setMethod(request.getMethod());

      apiAccessLogService.createApiAccessLog(accessLog);
  }
  ```

**User-Agent 示例**:
```
// Chrome 浏览器
Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36

// Safari 浏览器
Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15

// 移动端 Chrome
Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36

// Postman
PostmanRuntime/7.33.0
```

---

#### `getUserAgent()` - 获取当前请求的 User-Agent
**作用**: 获取当前线程请求的 User-Agent（无需传递 request 参数）

**参数**: 无

**返回值**: User-Agent 字符串，如果不存在返回 null

**使用示例**:
```java
// 示例：在 Service 层获取 User-Agent
@Service
public class UserServiceImpl implements UserService {

    @Override
    public void doSomething() {
        // 无需传递 request，直接获取当前请求的 User-Agent
        String userAgent = ServletUtils.getUserAgent();
        log.info("当前请求的 User-Agent: {}", userAgent);
    }
}
```

**实现原理**: 通过 Spring 的 `RequestContextHolder` 获取当前线程绑定的请求对象

**注意事项**:
1. **仅在 Web 请求线程中可用**: 定时任务、异步线程中无法获取
2. **返回 null**: 如果不在 Web 请求上下文中，返回 null

---

#### `getRequest()` - 获取当前请求对象
**作用**: 获取当前线程的 HTTP 请求对象

**参数**: 无

**返回值**: HttpServletRequest 对象，如果不存在返回 null

**使用示例**:
```java
// 示例1：在 Service 层获取请求对象
@Service
public class UserServiceImpl implements UserService {

    @Override
    public void createUser(UserCreateReqVO reqVO) {
        // 获取当前请求
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null) {
            String clientIp = ServletUtils.getClientIP(request);
            log.info("用户创建请求来自 IP: {}", clientIp);
        }

        // 业务逻辑
        userMapper.insert(user);
    }
}

// 示例2：在工具类中获取请求
public class SecurityUtils {
    public static Long getCurrentUserId() {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return null;
        }

        String token = request.getHeader("Authorization");
        return parseUserIdFromToken(token);
    }
}
```

**项目实际使用场景**:
- **操作日志**: 在 Service 层记录操作日志时获取请求信息
  ```java
  // ashore-framework/ashore-spring-boot-starter-web/src/main/java/com/example/ashore/framework/operatelog/core/aop/OperateLogAspect.java
  @Around("@annotation(operateLog)")
  public Object around(ProceedingJoinPoint joinPoint, OperateLog operateLog) throws Throwable {
      // 获取当前请求
      HttpServletRequest request = ServletUtils.getRequest();

      // 记录操作日志
      OperateLogCreateReqDTO log = new OperateLogCreateReqDTO();
      if (request != null) {
          log.setRequestUrl(request.getRequestURI());
          log.setRequestMethod(request.getMethod());
          log.setUserAgent(ServletUtils.getUserAgent(request));
          log.setClientIp(ServletUtils.getClientIP(request));
      }

      operateLogService.createOperateLog(log);

      return joinPoint.proceed();
  }
  ```

**注意事项**:
1. **线程绑定**: 基于 `ThreadLocal`，仅在 Web 请求线程中可用
2. **异步线程**: 异步线程中无法获取（需要手动传递）
3. **返回 null**: 非 Web 请求上下文返回 null

---

#### `getClientIP()` - 获取客户端 IP（无参数）
**作用**: 获取当前请求的客户端 IP 地址

**参数**: 无

**返回值**: 客户端 IP 地址，如果不存在返回 null

**使用示例**:
```java
// 示例：在 Service 层获取客户端 IP
@Service
public class LoginServiceImpl implements LoginService {

    @Override
    public LoginUser login(LoginReqVO reqVO) {
        // 获取客户端 IP
        String clientIp = ServletUtils.getClientIP();
        log.info("用户登录，IP: {}, username: {}", clientIp, reqVO.getUsername());

        // 登录逻辑
        LoginUser loginUser = authenticate(reqVO);

        // 记录登录日志
        loginLogService.createLoginLog(reqVO.getUsername(), clientIp);

        return loginUser;
    }
}
```

---

#### `getClientIP(HttpServletRequest request)` - 获取客户端 IP（带参数）
**作用**: 获取指定请求的客户端 IP 地址

**参数**:
- `request`: HTTP 请求对象

**返回值**: 客户端 IP 地址

**使用示例**:
```java
// 示例：在拦截器中获取客户端 IP
@Component
public class IpWhitelistInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        // 获取客户端 IP
        String clientIp = ServletUtils.getClientIP(request);

        // 判断是否在白名单中
        if (!isInWhitelist(clientIp)) {
            log.warn("IP不在白名单中，拒绝访问: {}", clientIp);
            return false;
        }

        return true;
    }
}
```

**项目实际使用场景**:
- **登录日志**: 记录登录 IP
- **API 访问日志**: 记录访问 IP
- **IP 黑白名单**: 校验 IP 是否允许访问
- **风控**: 检测异常 IP 访问

**IP 获取优先级**:
1. `X-Forwarded-For` 请求头（代理服务器会设置）
2. `X-Real-IP` 请求头（Nginx 反向代理会设置）
3. `remote_addr`（直连 IP）

**注意事项**:
1. **代理场景**: 经过代理服务器时，需要配置代理正确设置 X-Forwarded-For
2. **伪造风险**: X-Forwarded-For 可以被伪造，需要配置可信代理
3. **IPv6**: 支持 IPv6 地址

---

#### `isJsonRequest(ServletRequest request)` - 判断是否为 JSON 请求
**作用**: 判断请求的 Content-Type 是否为 `application/json`

**参数**:
- `request`: Servlet 请求对象

**返回值**: true-是 JSON 请求，false-不是 JSON 请求

**使用示例**:
```java
// 示例1：判断请求类型
if (ServletUtils.isJsonRequest(request)) {
    // JSON 请求，读取请求体
    String body = ServletUtils.getBody(request);
    User user = JsonUtils.parseObject(body, User.class);
} else {
    // 表单请求，读取参数
    String name = request.getParameter("name");
    String age = request.getParameter("age");
}

// 示例2：在过滤器中判断
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        if (ServletUtils.isJsonRequest(request)) {
            log.info("JSON 请求，Body: {}", ServletUtils.getBody(request));
        } else {
            log.info("表单请求，参数: {}", ServletUtils.getParamMap(request));
        }

        filterChain.doFilter(request, response);
    }
}
```

**判断规则**:
```java
// Content-Type 以 "application/json" 开头（忽略大小写）
request.getContentType().startsWith("application/json")
```

**常见 Content-Type**:
- `application/json`: JSON 格式
- `application/x-www-form-urlencoded`: 表单格式
- `multipart/form-data`: 文件上传格式
- `text/plain`: 纯文本格式

---

#### `getBody(HttpServletRequest request)` - 获取请求体（字符串）
**作用**: 获取请求体内容（UTF-8 字符串）

**参数**:
- `request`: HTTP 请求对象

**返回值**: 请求体字符串，如果不是 JSON 请求则返回 null

**使用示例**:
```java
// 示例1：获取 JSON 请求体
if (ServletUtils.isJsonRequest(request)) {
    String body = ServletUtils.getBody(request);
    log.info("请求体: {}", body);

    // 解析 JSON
    User user = JsonUtils.parseObject(body, User.class);
}

// 示例2：在过滤器中记录请求体
@Component
public class ApiAccessLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        // 记录请求体
        if (ServletUtils.isJsonRequest(request)) {
            String requestBody = ServletUtils.getBody(request);
            log.info("API 请求，url: {}, body: {}",
                request.getRequestURI(), requestBody);
        }

        filterChain.doFilter(request, response);
    }
}
```

**注意事项**:
1. **仅支持 JSON 请求**: 只有 `isJsonRequest` 返回 true 时才能读取
2. **需要缓存支持**: 必须使用 `CacheRequestBodyFilter` 才能重复读取
3. **返回 null**: 非 JSON 请求返回 null

**原理**:
- Servlet 的 InputStream 只能读取一次
- 需要使用 `CacheRequestBodyFilter` 缓存请求体
- 缓存后才能多次读取

---

#### `getBodyBytes(HttpServletRequest request)` - 获取请求体（字节数组）
**作用**: 获取请求体内容（字节数组）

**参数**:
- `request`: HTTP 请求对象

**返回值**: 请求体字节数组，如果不是 JSON 请求则返回 null

**使用示例**:
```java
// 示例：获取请求体字节数组
if (ServletUtils.isJsonRequest(request)) {
    byte[] bodyBytes = ServletUtils.getBodyBytes(request);
    log.info("请求体大小: {} bytes", bodyBytes.length);

    // 计算请求体的 MD5
    String md5 = DigestUtil.md5Hex(bodyBytes);
    log.info("请求体 MD5: {}", md5);
}
```

---

#### `getParamMap(HttpServletRequest request)` - 获取请求参数 Map
**作用**: 获取所有请求参数（包括 URL 参数和表单参数）

**参数**:
- `request`: HTTP 请求对象

**返回值**: Map<String, String>，key 为参数名，value 为参数值

**使用示例**:
```java
// 示例1：获取所有参数
Map<String, String> paramMap = ServletUtils.getParamMap(request);
log.info("请求参数: {}", JsonUtils.toJsonString(paramMap));

// 结果：{"name":"张三","age":"18","city":"北京"}

// 示例2：在拦截器中记录参数
@Override
public void afterCompletion(HttpServletRequest request, ...) {
    Map<String, String> params = ServletUtils.getParamMap(request);
    log.info("请求完成，url: {}, params: {}",
        request.getRequestURI(), JsonUtils.toJsonString(params));
}

// 示例3：签名校验
Map<String, String> params = ServletUtils.getParamMap(request);
String sign = params.remove("sign");  // 移除签名参数

// 生成签名
String expectedSign = SignUtils.generateSign(params);

// 校验签名
if (!expectedSign.equals(sign)) {
    throw new BusinessException("签名错误");
}
```

**注意事项**:
1. **多值参数**: 如果参数有多个值，只返回第一个
2. **不包含请求体**: 不会读取请求体中的参数
3. **编码**: 自动进行 URL 解码

---

#### `getHeaderMap(HttpServletRequest request)` - 获取请求头 Map
**作用**: 获取所有请求头

**参数**:
- `request`: HTTP 请求对象

**返回值**: Map<String, String>，key 为请求头名，value 为请求头值

**使用示例**:
```java
// 示例1：获取所有请求头
Map<String, String> headerMap = ServletUtils.getHeaderMap(request);
log.info("请求头: {}", JsonUtils.toJsonString(headerMap));

// 结果：{"Content-Type":"application/json","Authorization":"Bearer xxx","User-Agent":"..."}

// 示例2：在拦截器中记录请求头
@Override
public void afterCompletion(HttpServletRequest request, ...) {
    Map<String, String> headers = ServletUtils.getHeaderMap(request);
    log.info("请求头: {}", JsonUtils.toJsonString(headers));
}

// 示例3：获取自定义请求头
Map<String, String> headers = ServletUtils.getHeaderMap(request);
String appVersion = headers.get("X-App-Version");
String deviceId = headers.get("X-Device-Id");

log.info("应用版本: {}, 设备ID: {}", appVersion, deviceId);
```

**注意事项**:
1. **大小写**: 请求头名不区分大小写
2. **多值请求头**: 如果请求头有多个值，只返回第一个

---

## 三、实战场景总结

### 场景1：认证失败返回 JSON 响应
```java
// Spring Security 认证入口点
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException e) {
        // 记录日志
        log.warn("用户未认证，url: {}, ip: {}",
            request.getRequestURI(),
            ServletUtils.getClientIP(request), e);

        // 返回 401 错误
        CommonResult<?> result = CommonResult.error(401, "用户未认证，请先登录");
        ServletUtils.writeJSON(response, result);
    }
}
```

### 场景2：登录日志记录
```java
// 登录服务
@Service
public class AuthServiceImpl implements AuthService {

    @Override
    public LoginUser login(LoginReqVO reqVO) {
        // 执行登录逻辑
        LoginUser loginUser = authenticate(reqVO);

        // 记录登录日志
        LoginLogCreateReqDTO loginLog = new LoginLogCreateReqDTO();
        loginLog.setUsername(reqVO.getUsername());
        loginLog.setUserAgent(ServletUtils.getUserAgent());
        loginLog.setClientIp(ServletUtils.getClientIP());
        loginLog.setTraceId(TracerUtils.getTraceId());
        loginLog.setResult(LoginLogResultEnum.SUCCESS.getCode());

        loginLogService.createLoginLog(loginLog);

        return loginUser;
    }
}
```

### 场景3：API 访问日志
```java
// API 访问日志拦截器
@Component
public class ApiAccessLogInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 记录 API 访问日志
        ApiAccessLogCreateReqDTO accessLog = new ApiAccessLogCreateReqDTO();
        accessLog.setUrl(request.getRequestURI());
        accessLog.setMethod(request.getMethod());
        accessLog.setUserAgent(ServletUtils.getUserAgent(request));
        accessLog.setClientIp(ServletUtils.getClientIP(request));
        accessLog.setTraceId(TracerUtils.getTraceId());

        // 请求参数
        if (ServletUtils.isJsonRequest(request)) {
            accessLog.setRequestBody(ServletUtils.getBody(request));
        } else {
            accessLog.setRequestParams(JsonUtils.toJsonString(ServletUtils.getParamMap(request)));
        }

        // 响应状态
        accessLog.setResponseStatus(response.getStatus());

        apiAccessLogService.createApiAccessLog(accessLog);
    }
}
```

### 场景4：IP 白名单校验
```java
// IP 白名单拦截器
@Component
public class IpWhitelistInterceptor implements HandlerInterceptor {

    @Value("${security.ip.whitelist}")
    private List<String> ipWhitelist;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // 获取客户端 IP
        String clientIp = ServletUtils.getClientIP(request);

        // 判断是否在白名单中
        if (!ipWhitelist.contains(clientIp)) {
            log.warn("IP 不在白名单中，拒绝访问，IP: {}, url: {}",
                clientIp, request.getRequestURI());

            // 返回错误响应
            CommonResult<?> result = CommonResult.error(403, "IP 不在白名单中，拒绝访问");
            ServletUtils.writeJSON(response, result);

            return false;
        }

        return true;
    }
}
```

### 场景5：请求签名校验
```java
// 签名校验过滤器
@Component
public class SignatureFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 获取所有参数
        Map<String, String> params = ServletUtils.getParamMap(request);

        // 提取签名
        String sign = params.remove("sign");
        if (StrUtil.isEmpty(sign)) {
            CommonResult<?> result = CommonResult.error(400, "缺少签名参数");
            ServletUtils.writeJSON(response, result);
            return;
        }

        // 生成签名
        String expectedSign = SignUtils.generateSign(params);

        // 校验签名
        if (!expectedSign.equals(sign)) {
            log.warn("签名错误，clientIp: {}, params: {}",
                ServletUtils.getClientIP(request),
                JsonUtils.toJsonString(params));

            CommonResult<?> result = CommonResult.error(400, "签名错误");
            ServletUtils.writeJSON(response, result);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

### 场景6：在 Service 层获取请求信息
```java
// 用户服务
@Service
public class UserServiceImpl implements UserService {

    @Override
    public void createUser(UserCreateReqVO reqVO) {
        // 创建用户
        UserDO user = BeanUtils.toBean(reqVO, UserDO.class);
        userMapper.insert(user);

        // 记录操作日志（在 Service 层获取请求信息）
        OperateLogCreateReqDTO operateLog = new OperateLogCreateReqDTO();
        operateLog.setModule("用户管理");
        operateLog.setType("创建");
        operateLog.setContent("创建用户: " + user.getName());

        // 获取请求信息（无需传递 request 参数）
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null) {
            operateLog.setRequestUrl(request.getRequestURI());
            operateLog.setRequestMethod(request.getMethod());
            operateLog.setUserAgent(ServletUtils.getUserAgent());
            operateLog.setClientIp(ServletUtils.getClientIP());
        }

        operateLogService.createOperateLog(operateLog);
    }
}
```

---

## 四、注意事项

### 4.1 writeJSON 方法
1. **Content-Type**: 自动设置为 `application/json;charset=UTF-8`
2. **编码**: 使用 UTF-8 编码，避免中文乱码
3. **异常处理**: 序列化失败会抛出异常
4. **已废弃警告**: 使用了已废弃的 `APPLICATION_JSON_UTF8_VALUE`，但为了避免乱码必须使用

### 4.2 getUserAgent/getClientIP
1. **空字符串 vs null**: `getUserAgent(request)` 返回空字符串，`getUserAgent()` 返回 null
2. **代理场景**: 通过代理服务器时，IP 获取优先级：X-Forwarded-For > X-Real-IP > remote_addr
3. **伪造风险**: X-Forwarded-For 可以被伪造，需要配置可信代理

### 4.3 getRequest 方法
1. **线程绑定**: 基于 `ThreadLocal`，仅在 Web 请求线程中可用
2. **异步线程**: 异步线程中无法获取（返回 null）
3. **定时任务**: 定时任务中无法获取（返回 null）

### 4.4 getBody/getBodyBytes
1. **仅支持 JSON**: 只有 `isJsonRequest` 返回 true 时才能读取
2. **需要缓存**: 必须使用 `CacheRequestBodyFilter` 才能重复读取
3. **一次性读取**: Servlet InputStream 只能读取一次

### 4.5 getParamMap/getHeaderMap
1. **多值参数**: 只返回第一个值
2. **大小写**: Header 名不区分大小写
3. **编码**: 自动进行 URL 解码

---

## 五、常见问题

### Q1: writeJSON 为什么要使用已废弃的 APPLICATION_JSON_UTF8_VALUE？
A: 因为不使用的话会导致中文乱码。虽然 Spring 已废弃，但为了兼容性和避免乱码，仍然使用。

### Q2: getRequest() 在异步线程中为什么返回 null？
A: 因为 `RequestContextHolder` 使用 `ThreadLocal` 存储请求对象，异步线程是新的线程，无法获取父线程的 ThreadLocal 值。

### Q3: getBody() 为什么只能读取 JSON 请求？
A: 因为只有 JSON 请求才通过 `CacheRequestBodyFilter` 缓存了请求体，表单请求没有缓存，无法重复读取。

### Q4: getClientIP() 如何处理代理场景？
A: 优先从 X-Forwarded-For 和 X-Real-IP 请求头获取，最后才使用 remote_addr。

### Q5: 如何在异步线程中获取请求信息？
A: 需要在主线程中先获取并传递给异步线程：
```java
// 主线程
String userAgent = ServletUtils.getUserAgent();
String clientIp = ServletUtils.getClientIP();

// 异步线程
CompletableFuture.runAsync(() -> {
    log.info("异步任务，User-Agent: {}, IP: {}", userAgent, clientIp);
});
```

### Q6: getParamMap 会获取请求体中的参数吗？
A: 不会。只获取 URL 参数和表单参数，不会读取 JSON 请求体中的参数。

---

## 六、最佳实践

### 实践1：统一使用 writeJSON 返回错误
```java
// 在过滤器、拦截器中统一使用 writeJSON 返回错误
if (error) {
    CommonResult<?> result = CommonResult.error(code, message);
    ServletUtils.writeJSON(response, result);
    return;
}
```

### 实践2：在 Service 层使用无参方法
```java
// Service 层使用无参的 getUserAgent() 和 getClientIP()
@Service
public class UserServiceImpl {
    public void doSomething() {
        String userAgent = ServletUtils.getUserAgent();
        String clientIp = ServletUtils.getClientIP();
        // 业务逻辑
    }
}
```

### 实践3：判断请求类型后再读取请求体
```java
// 先判断是否为 JSON 请求，再读取请求体
if (ServletUtils.isJsonRequest(request)) {
    String body = ServletUtils.getBody(request);
    // 处理 JSON 请求体
}
```

### 实践4：记录日志时获取完整的请求信息
```java
// 记录日志时获取完整信息
log.info("API 请求，url: {}, method: {}, userAgent: {}, clientIp: {}, params: {}",
    request.getRequestURI(),
    request.getMethod(),
    ServletUtils.getUserAgent(request),
    ServletUtils.getClientIP(request),
    JsonUtils.toJsonString(ServletUtils.getParamMap(request)));
```

---

## 七、运行机制总结

- **类型**: 静态工具类
- **触发方式**: 开发人员主动调用
- **调用位置**: Controller、Filter、Interceptor、Handler、Service 等需要处理 HTTP 请求响应的地方
- **依赖库**:
  - Hutool (`cn.hutool.extra.servlet.JakartaServletUtil`)
  - Spring Web (`org.springframework.web.context.request`)
  - Jakarta Servlet (`jakarta.servlet`)
  - JsonUtils（项目内部）
- **设计模式**: 门面模式（封装复杂的 Servlet 操作，提供简单接口）
- **线程安全**: 基于 `RequestContextHolder`（ThreadLocal），线程安全

**文档版本**: v1.0  
**最后更新**: 2025-10-19  
**维护者**: Ashore 团队  
