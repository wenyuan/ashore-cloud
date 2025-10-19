# StrUtils 工具类使用指南

## 一、概述

`StrUtils` 是基于 Hutool 工具库进行二次封装的字符串工具类。该类针对项目实际业务场景进行了优化，提供了更便捷、更符合项目开发习惯的字符串处理方法。

---

## 二、方法列表

### 1.1 maxLength - 字符串最大长度截取

**方法签名**
```java
public static String maxLength(CharSequence str, int maxLength)
```

**功能说明**

截取字符串到指定最大长度，超出部分使用 `...` 代替。

**二次封装原因**
- Hutool 的 `maxLength` 方法会在截取后自动补充 `"..."`，占用 3 个字符
- 调用者传入的 `maxLength` 是期望的最终长度，本方法自动减去 3 避免超长
- 简化调用方逻辑，无需手动计算省略号长度

**参数说明**
- `str`：待截取字符串
- `maxLength`：最大长度（包含省略号）

**返回值**

截取后的字符串

**使用示例**

```java
// 示例 1: 日志参数截取
String requestParams = "很长的请求参数内容...";
String truncated = StrUtils.maxLength(requestParams, 2000); // 最终长度不超过 2000 字符
```

**项目中的实际使用**

```java
// ashore-module-infra-server/ApiAccessLogServiceImpl.java
apiAccessLog.setRequestParams(StrUtils.maxLength(apiAccessLog.getRequestParams(), REQUEST_PARAMS_MAX_LENGTH));
apiAccessLog.setResultMsg(StrUtils.maxLength(apiAccessLog.getResultMsg(), RESULT_MSG_MAX_LENGTH));
```

```java
// ashore-module-infra-server/ApiErrorLogServiceImpl.java
apiErrorLog.setRequestParams(StrUtils.maxLength(apiErrorLog.getRequestParams(), REQUEST_PARAMS_MAX_LENGTH));
```

---

### 1.2 startWithAny - 判断字符串是否以任一前缀开头

**方法签名**
```java
public static boolean startWithAny(String str, Collection<String> prefixes)
```

**功能说明**

判断给定字符串是否以集合中任意一个字符串开头。

**二次封装原因**
- Hutool 的 `startWithAny` 方法只支持数组参数，不支持 `Collection`
- 项目中很多场景使用 `List`/`Set` 等集合类型，直接传入更便捷
- 避免调用方频繁进行集合与数组的转换

**参数说明**
- `str`：待检测字符串
- `prefixes`：前缀集合

**返回值**

如果以任一前缀开头返回 `true`，否则返回 `false`。字符串或集合为空时返回 `false`。

**使用示例**

```java
// 示例 1: OAuth2 回调地址验证
List<String> allowedUris = Arrays.asList("http://localhost:8080", "https://example.com");
String redirectUri = "http://localhost:8080/callback";
boolean isValid = StrUtils.startWithAny(redirectUri, allowedUris); // true
```

**项目中的实际使用**

```java
// ashore-module-system-server/OAuth2ClientServiceImpl.java
if (StrUtil.isNotEmpty(redirectUri) && !StrUtils.startWithAny(redirectUri, client.getRedirectUris())) {
    throw exception(OAUTH2_CLIENT_REDIRECT_URI_NOT_MATCH, redirectUri);
}
```

---

### 1.3 splitToLong - 分割字符串转 Long 列表

**方法签名**
```java
public static List<Long> splitToLong(String value, CharSequence separator)
```

**功能说明**

将字符串按指定分隔符分割，转换为 `List<Long>` 集合。

**二次封装原因**
- Hutool 的 `splitToLong` 返回基本类型数组 `long[]`，不便于后续流式操作
- 项目中更常用 `List<Long>` 集合类型，支持 `null` 值且 API 更丰富
- 统一返回类型风格，与其他业务方法保持一致

**参数说明**
- `value`：待分割字符串
- `separator`：分隔符

**返回值**

`List<Long>` 集合

**使用示例**

```java
// 示例 1: 解析用户 ID 列表
String userIds = "1,2,3,4,5";
List<Long> idList = StrUtils.splitToLong(userIds, ",");
// 结果: [1L, 2L, 3L, 4L, 5L]
```

**项目中的实际使用**

```java
// ashore-module-bpm-server/BpmTaskCandidateUserStrategy.java
public LinkedHashSet<Long> calculateUsers(String param) {
    return new LinkedHashSet<>(StrUtils.splitToLong(param, StrPool.COMMA));
}
```

```java
// ashore-framework/mybatis/LongListTypeHandler.java:55
private List<Long> getResult(String value) {
    if (value == null) {
        return null;
    }
    return StrUtils.splitToLong(value, COMMA);
}
```

---

### 1.4 splitToLongSet - 分割字符串转 Long 集合（去重）

**方法签名**
```java
public static Set<Long> splitToLongSet(String value)
public static Set<Long> splitToLongSet(String value, CharSequence separator)
```

**功能说明**

将字符串按分隔符分割，转换为 `Set<Long>` 集合（自动去重）。默认使用逗号分隔。

**二次封装原因**
- Hutool 的 `splitToLong` 返回基本类型数组 `long[]`
- 使用 `Set` 自动去重，避免重复 ID
- 包装类型更适合集合操作和业务传递
- 提供默认分隔符（逗号），简化常见场景的调用

**参数说明**
- `value`：待分割字符串
- `separator`：分隔符（可选，默认逗号）

**返回值**

`Set<Long>` 集合（去重后）

**使用示例**

```java
// 示例 1: 解析去重的用户 ID 集合
String userIds = "1,2,3,2,1,4";
Set<Long> uniqueIds = StrUtils.splitToLongSet(userIds);
// 结果: [1L, 2L, 3L, 4L]

// 示例 2: 指定分隔符
String roleIds = "10|20|30|20";
Set<Long> roleSet = StrUtils.splitToLongSet(roleIds, "|");
// 结果: [10L, 20L, 30L]
```

**项目中的实际使用**

```java
// ashore-module-bpm-server/BpmTaskCandidateUserStrategy.java
public void validateParam(String param) {
    adminUserApi.validateUserList(StrUtils.splitToLongSet(param)).checkError();
}
```

---

### 1.5 splitToInteger - 分割字符串转 Integer 列表

**方法签名**
```java
public static List<Integer> splitToInteger(String value, CharSequence separator)
```

**功能说明**

将字符串按指定分隔符分割，转换为 `List<Integer>` 集合。

**二次封装原因**
- Hutool 的 `splitToInt` 返回基本类型数组 `int[]`
- `List<Integer>` 更符合项目开发习惯，便于传参和后续处理
- 与 `splitToLong` 方法保持 API 风格统一

**参数说明**
- `value`：待分割字符串
- `separator`：分隔符

**返回值**

`List<Integer>` 集合

**使用示例**

```java
// 示例 1: 解析商品数量列表
String quantities = "10,20,30,5";
List<Integer> quantityList = StrUtils.splitToInteger(quantities, ",");
// 结果: [10, 20, 30, 5]
```

---

### 1.6 removeLineContains - 移除包含指定字符串的行

**方法签名**
```java
public static String removeLineContains(String content, String sequence)
```

**功能说明**

移除多行字符串中包含指定字符串的行。

**二次封装原因**
- Hutool 未提供按行过滤的工具方法
- 项目中有日志处理、代码生成等场景需要移除特定行
- 封装统一实现，避免各模块重复编写相同逻辑

**参数说明**
- `content`：多行字符串
- `sequence`：要匹配的字符串（包含该字符串的行将被移除）

**返回值**

移除后的字符串

**使用示例**

```java
// 示例 1: 移除日志中的敏感信息
String log = """
    [INFO] User login success
    [DEBUG] Password: 123456
    [INFO] Access granted
    """;
String cleaned = StrUtils.removeLineContains(log, "Password");
// 结果:
// [INFO] User login success
// [INFO] Access granted
```

**项目中的实际使用**

```java
// ashore-module-infra-server/CodegenEngine.java
// Vue 界面：去除多余的 dateFormatter，只有一个的情况下，说明没使用到
if (StrUtil.count(content, "dateFormatter") == 1) {
    content = StrUtils.removeLineContains(content, "dateFormatter");
}
```

```java
// ashore-module-infra-server/CodegenEngine.java
// 如果没有使用字典类型，移除相关导入
if (StrUtil.count(content, "DICT_TYPE.") == 0) {
    content = StrUtils.removeLineContains(content, "DICT_TYPE");
}
```

---

### 1.7 joinMethodArgs - 拼接 AOP 方法参数

**方法签名**
```java
public static String joinMethodArgs(JoinPoint joinPoint)
```

**功能说明**

拼接 AOP 切面方法的参数，自动过滤掉无法序列化的 Servlet 对象。

**二次封装原因**
- AOP 日志场景中，直接序列化参数可能导致循环引用或序列化异常
- `ServletRequest`/`ServletResponse` 等对象体积大且无法序列化，需要过滤
- 封装统一的参数拼接逻辑，避免各 AOP 切面重复实现
- 支持项目从 `javax` 到 `jakarta` 的平滑迁移

**参数说明**
- `joinPoint`：AOP 连接点

**返回值**

拼接后的参数字符串（逗号分隔），已过滤 Servlet 相关对象

**使用示例**

```java
// 示例 1: AOP 日志记录
@Around("@annotation(rateLimiter)")
public Object around(ProceedingJoinPoint joinPoint, RateLimiter rateLimiter) {
    String methodName = joinPoint.getSignature().toString();
    String argsStr = StrUtils.joinMethodArgs(joinPoint); // 自动过滤 HttpServletRequest 等对象
    log.info("Method: {}, Args: {}", methodName, argsStr);
    return joinPoint.proceed();
}
```

**项目中的实际使用**

```java
// ashore-framework/ratelimiter/DefaultRateLimiterKeyResolver.java
public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
    String methodName = joinPoint.getSignature().toString();
    String argsStr = StrUtils.joinMethodArgs(joinPoint);
    return SecureUtil.md5(methodName + argsStr);
}
```

**自动过滤的对象类型**
- `javax.servlet.*`（如 `HttpServletRequest`、`HttpServletResponse`）
- `jakarta.servlet.*`（Jakarta EE 版本）
- `org.springframework.web.*`（如 `MultipartFile`）

---

## 常见使用场景总结

| 场景 | 推荐方法 | 说明 |
|------|---------|------|
| 日志参数截断 | `maxLength` | 避免超长日志影响数据库存储 |
| OAuth2 回调地址验证 | `startWithAny` | 检查 URL 是否在白名单中 |
| 用户 ID 列表解析 | `splitToLong` / `splitToLongSet` | 前者保留顺序，后者自动去重 |
| MyBatis TypeHandler | `splitToLong` | 数据库 varchar 与 List 互转 |
| 代码生成清理 | `removeLineContains` | 移除未使用的导入或声明 |
| AOP 限流键生成 | `joinMethodArgs` | 生成方法签名+参数的唯一标识 |

---

## 注意事项

1. **`maxLength` 的长度计算**：传入的 `maxLength` 是最终期望长度（包含省略号），方法内部会自动 `-3`
2. **`splitToLongSet` 的去重特性**：适用于 ID 列表等需要唯一性的场景，如果需要保留顺序和重复值，请使用 `splitToLong`
3. **`joinMethodArgs` 的过滤逻辑**：基于类名前缀匹配，如果有其他需要过滤的类型，需要修改源码
4. **字符串为空的处理**：大部分方法对空字符串有容错处理，不会抛出异常

---


**文档版本**: v1.0  
**最后更新**: 2025-10-05  
**维护者**: Ashore 团队  
**依赖**：Hutool 5.x、AspectJ（`joinMethodArgs` 方法）
