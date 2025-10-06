# RestTemplate 配置与使用指南

## 一、背景说明

### 1.1 什么是 RestTemplate？

`RestTemplate` 是 Spring 提供的 **HTTP 客户端工具类**，用于发送 HTTP 请求（GET、POST、PUT、DELETE 等）。

**常见使用场景**：
- 调用第三方 API（如微信 API、支付宝 API）
- 微服务之间的 HTTP 通信
- 调用 HTTP 协议的数据库（如 ClickHouse、Elasticsearch）

### 1.2 为什么需要两个 RestTemplate？

| RestTemplate 类型 | 是否带负载均衡 | 适用场景 | 示例 |
|-------------------|---------------|---------|------|
| **loadBalancedRestTemplate** | ✅ 是（@LoadBalanced） | 微服务间调用 | `http://user-service/api/user/1` |
| **databaseRestTemplate** | ❌ 否 | HTTP 数据库、第三方 API | `http://clickhouse:8123/?query=...` |

**核心区别**：
- 带 `@LoadBalanced` 的会将 URL 中的主机名当作**服务名**去注册中心查找
- 不带 `@LoadBalanced` 的直接使用**实际 URL** 发起请求

---

## 二、在模块中添加数据库专用 RestTemplate

本框架中自带了微服务间调用版的 RestTemplate，

位于 `ashore-framework/ashore-spring-boot-starter-web/src/main/java/com/example/ashore/framework/web/config/AshoreWebAutoConfiguration.java#loadBalancedRestTemplate`

如果在某个模块中，需要一个能够请求第三方 API 的 RestTemplate 实例，按如下步骤在配置类中定义 Bean 即可。

### 2.1 创建配置类

假设你在 `ashore-module-system` 模块中需要调用 ClickHouse 等数据库。

**文件路径**：`ashore-module-system/src/main/java/com/example/ashore/module/system/config/DatabaseRestTemplateConfig.java`

```java
package com.example.ashore.module.system.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 数据库专用 RestTemplate 配置类
 *
 * 作用：创建一个不带负载均衡的 RestTemplate，用于调用 HTTP 协议的数据库（如 ClickHouse、Elasticsearch）
 */
@Configuration  // 标识这是一个配置类，Spring 会自动加载
public class DatabaseRestTemplateConfig {

    /**
     * 创建数据库专用的 RestTemplate Bean
     *
     * 功能特性：
     * 1. 使用 Apache HttpClient5 作为底层 HTTP 客户端（性能更好）
     * 2. 配置连接池，支持高并发场景
     * 3. 设置超时时间，避免请求无限等待
     * 4. 添加统一请求头
     *
     * @param builder RestTemplate 构建器，由 Spring Boot 自动提供
     * @return 配置完成的 RestTemplate 实例
     */
    @Bean  // 将方法返回值注册为 Spring Bean，Bean 名称默认为方法名 "databaseRestTemplate"
    public RestTemplate databaseRestTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(this::createRequestFactory)  // 设置自定义的请求工厂（包含连接池配置）
                .setConnectTimeout(Duration.ofSeconds(5))    // 连接超时：5秒（建立 TCP 连接的最大等待时间）
                .setReadTimeout(Duration.ofSeconds(30))      // 读取超时：30秒（等待响应数据的最大时间，数据库查询可能较慢）
                .additionalInterceptors((request, body, execution) -> {          // 添加拦截器，统一处理请求
                    // 设置统一的请求头
                    request.getHeaders().add("User-Agent", "Ashore-System/1.0");  // 标识客户端身份
                    request.getHeaders().add("Accept", "application/json");       // 期望返回 JSON 格式

                    // 继续执行请求
                    return execution.execute(request, body);
                })
                .build();  // 构建 RestTemplate 实例
    }

    /**
     * 创建自定义的 HTTP 请求工厂（配置连接池）
     *
     * 为什么需要连接池？
     * - 复用 TCP 连接，避免频繁创建和销毁连接
     * - 提升性能，降低延迟
     * - 控制并发连接数，避免资源耗尽
     *
     * @return 配置完成的请求工厂
     */
    private ClientHttpRequestFactory createRequestFactory() {
        // ========== 1. 配置连接池 ==========
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        // 设置连接池的最大连接数（所有路由的总连接数上限）
        // 场景：假设同时调用 ClickHouse、Elasticsearch 等多个服务
        connectionManager.setMaxTotal(200);  // 最多同时保持 200 个连接

        // 设置每个路由的最大连接数（单个主机的连接数上限）
        // 场景：对同一个 ClickHouse 实例，最多同时发起 50 个请求
        connectionManager.setDefaultMaxPerRoute(50);


        // ========== 2. 配置请求参数 ==========
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(3))  // 从连接池获取连接的超时时间（3秒）
                .setResponseTimeout(Timeout.ofSeconds(30))          // 等待响应数据的超时时间（30秒）
                .build();


        // ========== 3. 创建 HttpClient ==========
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)  // 使用自定义的连接池
                .setDefaultRequestConfig(requestConfig)   // 使用自定义的请求配置
                .build();


        // ========== 4. 创建请求工厂 ==========
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        // 设置连接超时（建立 TCP 连接的最大等待时间）
        factory.setConnectTimeout(5000);  // 5秒，单位：毫秒

        return factory;
    }
}
```

### 2.2 添加 Maven 依赖

由于使用了 Apache HttpClient5，需要在模块的 `pom.xml` 中添加依赖。

**文件路径**：`ashore-module-system/pom.xml`

```xml
<dependencies>
    <!-- 其他依赖... -->

    <!-- Apache HttpClient5：高性能 HTTP 客户端，支持连接池 -->
    <dependency>
        <groupId>org.apache.httpcomponents.client5</groupId>
        <artifactId>httpclient5</artifactId>
        <!-- 版本由 Spring Boot 统一管理，无需指定 -->
    </dependency>
</dependencies>
```

---

## 三、验证 Bean 是否创建成功

### 3.1 执行顺序

- Spring 先加载模块的配置类（`databaseRestTemplate`）
- 再加载框架的自动配置类（`AshoreWebAutoConfiguration`）
- 因为设置了 `@ConditionalOnMissingBean` 框架会检测容器中有没有相同的 Bean
  - 如果是 `@ConditionalOnMissingBean`，会检测到容器中已存在 `RestTemplate` 类型的 Bean，框架的 restTemplate 不会创建
  - 所以设置的是 `@ConditionalOnMissingBean(name = "loadBalancedRestTemplate")`，检测容器中是否存在同名 Bean

### 3.2 检查容器中的 Bean

创建一个测试类，打印容器中所有 `RestTemplate` 类型的 Bean。

```java
package com.example.ashore.module.system;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 启动时检查 RestTemplate Bean
 *
 * 作用：在应用启动后，打印容器中所有 RestTemplate 类型的 Bean 名称
 */
@Component  // 标识为 Spring 组件，会被自动扫描和加载
public class RestTemplateBeanChecker implements CommandLineRunner {

    private final ApplicationContext applicationContext;  // Spring 应用上下文（容器）

    // 构造器注入：Spring 自动传入 ApplicationContext
    public RestTemplateBeanChecker(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 应用启动后执行
     */
    @Override
    public void run(String... args) {
        // 获取容器中所有 RestTemplate 类型的 Bean
        Map<String, RestTemplate> beans = applicationContext.getBeansOfType(RestTemplate.class);

        System.out.println("========== 容器中的 RestTemplate Bean ==========");
        beans.forEach((name, bean) -> {
            System.out.println("Bean 名称: " + name);
            System.out.println("Bean 类型: " + bean.getClass().getName());
            System.out.println("-----------------------------------------------");
        });
    }
}
```

**预期输出**：
```
========== 容器中的 RestTemplate Bean ==========
Bean 名称: loadBalancedRestTemplate
Bean 类型: org.springframework.web.client.RestTemplate
-----------------------------------------------
Bean 名称: databaseRestTemplate
Bean 类型: org.springframework.web.client.RestTemplate
-----------------------------------------------
```

---

## 四、在业务代码中使用 RestTemplate

### 4.1 场景示例：同时调用微服务和 ClickHouse

假设你在 `SystemUserService` 中需要：
1. 调用 `member-service` 微服务获取会员信息
2. 调用 ClickHouse 数据库查询统计数据

**文件路径**：`ashore-module-system/src/main/java/com/example/ashore/module/system/service/user/SystemUserServiceImpl.java`

```java
package com.example.ashore.module.system.service.user;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 系统用户 Service 实现类
 */
@Service  // 标识为 Service 层组件
@Slf4j    // Lombok 注解，自动生成日志对象 log
public class SystemUserServiceImpl implements SystemUserService {

    // ========== 注入两个不同的 RestTemplate ==========

    /**
     * 带负载均衡的 RestTemplate，用于微服务间调用
     *
     * 注解说明：
     * - @Resource：Java 标准注解，用于依赖注入
     * - 匹配规则：先按变量名 "loadBalancedRestTemplate" 查找 Bean，找不到再按类型查找
     */
    @Resource
    private RestTemplate loadBalancedRestTemplate;

    /**
     * 数据库专用 RestTemplate，用于调用 HTTP 数据库
     *
     * 注解说明：
     * - @Resource：Java 标准注解，用于依赖注入
     * - 匹配规则：先按变量名 "databaseRestTemplate" 查找 Bean（即我们自定义的）
     */
    @Resource
    private RestTemplate databaseRestTemplate;


    // ========== 业务方法 ==========

    /**
     * 根据用户 ID 获取用户的完整信息（包含会员信息和统计数据）
     *
     * @param userId 用户 ID
     * @return 用户完整信息
     */
    @Override
    public UserFullInfo getUserFullInfo(Long userId) {
        // 1. 调用微服务获取会员信息
        MemberInfo memberInfo = callMemberService(userId);

        // 2. 调用 ClickHouse 获取统计数据
        UserStatistics statistics = queryUserStatistics(userId);

        // 3. 组装返回结果
        UserFullInfo fullInfo = new UserFullInfo();
        fullInfo.setUserId(userId);
        fullInfo.setMemberInfo(memberInfo);
        fullInfo.setStatistics(statistics);

        return fullInfo;
    }


    // ========== 私有方法：调用微服务 ==========

    /**
     * 调用会员微服务获取会员信息
     *
     * @param userId 用户 ID
     * @return 会员信息
     */
    private MemberInfo callMemberService(Long userId) {
        try {
            // 使用带负载均衡的 RestTemplate
            // URL 格式：http://{服务名}/{路径}
            // 服务名会被自动解析为实际的 IP:Port，并进行负载均衡
            String url = "http://member-service/admin-api/member/user/get?id=" + userId;

            log.info("[callMemberService] 调用会员服务，userId: {}, url: {}", userId, url);

            // 发起 GET 请求，返回值自动反序列化为 ApiResponse 对象
            ApiResponse<MemberInfo> result = loadBalancedRestTemplate.getForObject(
                    url,
                    // 泛型类型，需要使用 ParameterizedTypeReference 来保留泛型信息
                    new ParameterizedTypeReference<ApiResponse<MemberInfo>>() {}
            );

            // 检查响应结果
            if (result == null || !result.isSuccess()) {
                log.error("[callMemberService] 调用失败，result: {}", result);
                throw new RuntimeException("调用会员服务失败");
            }

            log.info("[callMemberService] 调用成功，memberInfo: {}", result.getData());
            return result.getData();

        } catch (Exception e) {
            log.error("[callMemberService] 调用异常，userId: {}", userId, e);
            throw new RuntimeException("调用会员服务异常", e);
        }
    }


    // ========== 私有方法：调用 ClickHouse 数据库 ==========

    /**
     * 从 ClickHouse 查询用户统计数据
     *
     * @param userId 用户 ID
     * @return 用户统计数据
     */
    private UserStatistics queryUserStatistics(Long userId) {
        try {
            // 使用数据库专用 RestTemplate（不带负载均衡）
            // URL 格式：直接使用实际的主机名或 IP
            String host = "http://clickhouse.internal:8123";  // ClickHouse HTTP 接口地址

            // 构建 SQL 查询
            String sql = String.format(
                    "SELECT " +
                    "  user_id, " +
                    "  count(*) as login_count, " +
                    "  max(login_time) as last_login_time " +
                    "FROM user_login_log " +
                    "WHERE user_id = %d " +
                    "GROUP BY user_id " +
                    "FORMAT JSON",  // ClickHouse 返回 JSON 格式
                    userId
            );

            // 完整 URL（ClickHouse 使用 GET 请求，SQL 放在 query 参数中）
            String url = host + "/?query=" + urlEncode(sql);

            log.info("[queryUserStatistics] 查询 ClickHouse，userId: {}, sql: {}", userId, sql);

            // 发起 GET 请求
            ClickHouseResponse response = databaseRestTemplate.getForObject(
                    url,
                    ClickHouseResponse.class  // ClickHouse 返回的 JSON 结构
            );

            // 解析返回结果
            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.warn("[queryUserStatistics] 无数据，userId: {}", userId);
                return new UserStatistics();  // 返回空对象
            }

            // 提取第一行数据
            UserStatistics statistics = response.getData().get(0);
            log.info("[queryUserStatistics] 查询成功，statistics: {}", statistics);

            return statistics;

        } catch (Exception e) {
            log.error("[queryUserStatistics] 查询异常，userId: {}", userId, e);
            throw new RuntimeException("查询 ClickHouse 异常", e);
        }
    }

    /**
     * URL 编码工具方法
     *
     * @param value 原始字符串
     * @return 编码后的字符串
     */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("URL 编码失败", e);
        }
    }
}
```

### 4.2 示例 2：使用 POST 请求调用第三方 API

```java
/**
 * 调用微信 API 发送模板消息
 *
 * @param openId 用户 openId
 * @param message 消息内容
 */
public void sendWechatMessage(String openId, String message) {
    try {
        // 微信 API 地址
        String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + getAccessToken();

        // 构建请求体
        WechatMessageRequest request = new WechatMessageRequest();
        request.setTouser(openId);
        request.setTemplateId("your_template_id");
        request.setData(message);

        log.info("[sendWechatMessage] 发送微信消息，openId: {}, request: {}", openId, request);

        // 发起 POST 请求（使用数据库专用 RestTemplate，因为是外部 API）
        WechatMessageResponse response = databaseRestTemplate.postForObject(
                url,
                request,  // 请求体会自动序列化为 JSON
                WechatMessageResponse.class  // 响应体自动反序列化
        );

        // 检查响应
        if (response == null || response.getErrcode() != 0) {
            log.error("[sendWechatMessage] 发送失败，response: {}", response);
            throw new RuntimeException("发送微信消息失败");
        }

        log.info("[sendWechatMessage] 发送成功");

    } catch (Exception e) {
        log.error("[sendWechatMessage] 发送异常，openId: {}", openId, e);
        throw new RuntimeException("发送微信消息异常", e);
    }
}
```

---

## 五、配置参数说明

### 5.1 超时时间配置

| 参数 | 说明 | 推荐值 | 场景 |
|------|------|--------|------|
| **connectTimeout** | 建立 TCP 连接的超时时间 | 3-5秒 | 网络较好：3秒<br>网络较差：5秒 |
| **readTimeout** | 等待响应数据的超时时间 | 10-30秒 | 快速接口：10秒<br>慢查询：30秒 |
| **connectionRequestTimeout** | 从连接池获取连接的超时时间 | 3秒 | 高并发场景 |

**示例场景**：
```
调用流程：
1. 从连接池获取连接 ← connectionRequestTimeout（3秒）
2. 建立 TCP 连接      ← connectTimeout（5秒）
3. 发送请求并等待响应 ← readTimeout（30秒）

总超时 = 3 + 5 + 30 = 38秒
```

### 5.2 连接池配置

| 参数 | 说明 | 推荐值 | 计算方式 |
|------|------|--------|---------|
| **maxTotal** | 连接池最大连接数（总数） | 100-200 | 根据并发量调整 |
| **defaultMaxPerRoute** | 每个路由的最大连接数 | 20-50 | maxTotal / 预计的主机数 |

**示例场景**：
```
假设你的系统需要调用：
- ClickHouse: http://clickhouse:8123
- Elasticsearch: http://elasticsearch:9200
- 微信 API: https://api.weixin.qq.com

共 3 个不同的主机（路由）

配置建议：
- maxTotal = 150（总共最多 150 个连接）
- defaultMaxPerRoute = 50（每个主机最多 50 个连接）

实际使用：
- ClickHouse: 最多 50 个并发连接
- Elasticsearch: 最多 50 个并发连接
- 微信 API: 最多 50 个并发连接
```

---

## 六、常见问题

### 6.1 如何区分使用哪个 RestTemplate？

| 场景 | 使用哪个 | URL 示例 |
|------|---------|---------|
| 调用微服务 | `loadBalancedRestTemplate` | `http://user-service/api/user/1` |
| 调用 HTTP 数据库 | `databaseRestTemplate` | `http://clickhouse:8123/?query=...` |
| 调用第三方 API | `databaseRestTemplate` | `https://api.weixin.qq.com/...` |

**判断标准**：URL 中是否使用**服务名**？
- 是 → 用 `loadBalancedRestTemplate`
- 否 → 用 `databaseRestTemplate`

### 6.2 如何调试 HTTP 请求？

启用日志输出：

```yaml
# application.yml
logging:
  level:
    org.apache.hc.client5: DEBUG  # Apache HttpClient 日志
    org.springframework.web.client.RestTemplate: DEBUG  # RestTemplate 日志
```

### 6.3 如何处理 HTTP 错误？

```java
try {
    String response = databaseRestTemplate.getForObject(url, String.class);
} catch (HttpClientErrorException e) {
    // 4xx 错误（客户端错误）
    log.error("客户端错误，状态码: {}, 响应体: {}", e.getStatusCode(), e.getResponseBodyAsString());
} catch (HttpServerErrorException e) {
    // 5xx 错误（服务端错误）
    log.error("服务端错误，状态码: {}, 响应体: {}", e.getStatusCode(), e.getResponseBodyAsString());
} catch (ResourceAccessException e) {
    // 网络异常（如超时、连接失败）
    log.error("网络异常", e);
}
```

---

## 七、完整示例代码汇总

### 7.1 配置类（DatabaseRestTemplateConfig.java）

见 [第二章节](#21-创建配置类)

### 7.2 业务代码（SystemUserServiceImpl.java）

见 [第四章节](#41-场景示例同时调用微服务和-clickhouse)

### 7.3 数据传输对象（DTO）

```java
// 会员信息
@Data
public class MemberInfo {
    private Long userId;
    private String nickname;
    private String mobile;
    private Integer level;
}

// 用户统计数据
@Data
public class UserStatistics {
    private Long userId;
    private Long loginCount;      // 登录次数
    private LocalDateTime lastLoginTime;  // 最后登录时间
}

// 用户完整信息
@Data
public class UserFullInfo {
    private Long userId;
    private MemberInfo memberInfo;
    private UserStatistics statistics;
}

// ClickHouse 响应结构
@Data
public class ClickHouseResponse {
    private List<UserStatistics> data;  // 查询结果
}

// 通用响应结构
@Data
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;

    public boolean isSuccess() {
        return code != null && code == 0;
    }
}
```

---

## 八、总结

### 8.1 关键要点

1. **两个 RestTemplate 共存**：带负载均衡的用于微服务，不带的用于外部调用
2. **使用连接池**：生产环境必须配置连接池，提升性能
3. **设置超时时间**：避免请求无限等待
4. **变量名匹配 Bean 名**：使用 `@Resource` 时，变量名与 Bean 名一致，无需额外配置

### 8.2 最佳实践

| 实践 | 说明 |
|------|------|
| ✅ 使用 `RestTemplateBuilder` | 自动应用 Spring Boot 配置 |
| ✅ 配置连接池 | 复用连接，提升性能 |
| ✅ 设置超时时间 | 避免雪崩效应 |
| ✅ 统一异常处理 | 使用 try-catch 捕获并记录日志 |
| ✅ 记录请求日志 | 便于排查问题 |

---

**文档版本**：v1.0  
**最后更新**：2025-10-05  
**适用框架**：Spring Boot 3.x + Spring Cloud  
**维护者**: Ashore 团队  
