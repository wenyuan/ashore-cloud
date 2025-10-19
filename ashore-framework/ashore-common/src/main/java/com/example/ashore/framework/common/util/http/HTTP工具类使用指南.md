# HTTP 工具类使用指南

## 一、包概览

`http` 包提供了 HTTP 请求和 URL 处理的工具类，主要包含：

- **HttpUtils**: HTTP 请求发送和 URL 处理工具类

该类封装了常用的 HTTP 操作，简化了 URL 参数编码、URL 拼接、Basic 认证解析等功能。

---

## 二、HttpUtils 类详解

### 2.1 类的整体介绍

`HttpUtils` 是 HTTP 工具类，主要解决以下问题：

1. **URL 编码问题**: 统一使用 UTF-8 编码 URL 参数
2. **URL 参数处理**: 替换、移除 URL 查询参数
3. **URL 拼接**: OAuth2 等场景下复杂的 URL 拼接
4. **Basic 认证解析**: 解析 HTTP Basic 认证头
5. **HTTP 请求增强**: 支持自定义 Headers 的 POST/GET 请求

**为什么要二次封装？**
- Hutool 的 `HttpUtil` 不支持传递自定义 Headers
- Spring OAuth2 的 URL 拼接逻辑复杂，需要封装简化
- 统一 URL 编码格式，避免乱码问题

### 2.2 核心方法详解

#### `encodeUtf8(String value)` - URL 参数编码
**作用**: 使用 UTF-8 编码 URL 参数

**参数**:
- `value`: 待编码的字符串

**返回值**: 编码后的字符串

**使用示例**:
```java
// 示例1：编码中文参数
String encoded = HttpUtils.encodeUtf8("测试编码");
// 结果："%E6%B5%8B%E8%AF%95%E7%BC%96%E7%A0%81"

// 示例2：编码特殊字符
String encoded2 = HttpUtils.encodeUtf8("hello world");
// 结果："hello+world"

// 示例3：构建 URL 参数
String url = "https://api.example.com/search?keyword=" + HttpUtils.encodeUtf8("Java 开发");
// 结果："https://api.example.com/search?keyword=Java+%E5%BC%80%E5%8F%91"
```

**项目实际使用场景**:
- **阿里云短信**: 在 `AliyunSmsClient` 中编码短信参数
  ```java
  // ashore-module-system/ashore-module-system-biz/src/main/java/com/example/ashore/module/system/framework/sms/core/client/impl/AliyunSmsClient.java
  // 构建请求参数
  String phoneNumbers = HttpUtils.encodeUtf8(mobile);
  String signName = HttpUtils.encodeUtf8(apiSignName);
  String templateParam = HttpUtils.encodeUtf8(JsonUtils.toJsonString(templateParams));
  ```

---

#### `replaceUrlQuery(String url, String key, String value)` - 替换 URL 查询参数
**作用**: 替换 URL 中的查询参数，如果参数不存在则添加

**参数**:
- `url`: 原始 URL
- `key`: 参数名
- `value`: 参数值

**返回值**: 替换后的 URL

**使用示例**:
```java
// 示例1：替换已存在的参数
String url = "https://example.com/page?id=1&name=old";
String newUrl = HttpUtils.replaceUrlQuery(url, "name", "new");
// 结果："https://example.com/page?id=1&name=new"

// 示例2：添加不存在的参数
String url = "https://example.com/page?id=1";
String newUrl = HttpUtils.replaceUrlQuery(url, "type", "product");
// 结果："https://example.com/page?id=1&type=product"

// 示例3：替换中文参数
String url = "https://example.com/search?keyword=旧关键词";
String newUrl = HttpUtils.replaceUrlQuery(url, "keyword", "新关键词");
// 结果："https://example.com/search?keyword=新关键词"
```

**项目实际使用场景**:
- **社交登录**: 在 `SocialAuthRequestFactory` 中替换回调 URL 的参数
  ```java
  // ashore-module-system/ashore-module-system-biz/src/main/java/com/example/ashore/module/system/framework/social/core/SocialAuthRequestFactory.java
  // 替换 redirect_uri 参数
  String redirectUri = config.getRedirectUri();
  redirectUri = HttpUtils.replaceUrlQuery(redirectUri, "type", type);
  redirectUri = HttpUtils.replaceUrlQuery(redirectUri, "state", state);
  ```

---

#### `removeUrlQuery(String url)` - 移除 URL 查询参数
**作用**: 移除 URL 中的所有查询参数和 fragment（#后面的部分）

**参数**:
- `url`: 原始 URL

**返回值**: 移除参数后的 URL

**使用示例**:
```java
// 示例1：移除查询参数
String url = "https://example.com/page?id=1&name=test";
String cleanUrl = HttpUtils.removeUrlQuery(url);
// 结果："https://example.com/page"

// 示例2：移除查询参数和 fragment
String url = "https://example.com/page?id=1#section";
String cleanUrl = HttpUtils.removeUrlQuery(url);
// 结果："https://example.com/page"

// 示例3：没有查询参数的 URL
String url = "https://example.com/page";
String cleanUrl = HttpUtils.removeUrlQuery(url);
// 结果："https://example.com/page"（不变）
```

**项目实际使用场景**:
- **文件上传**: 在 `S3FileClient` 中移除文件 URL 的签名参数
  ```java
  // ashore-framework/ashore-spring-boot-starter-file/src/main/java/com/example/ashore/framework/file/core/client/s3/S3FileClient.java
  // 移除签名参数，获取纯净的文件路径
  String filePath = HttpUtils.removeUrlQuery(fileUrl);
  ```

---

#### `append(String base, Map<String, ?> query, Map<String, String> keys, boolean fragment)` - 拼接 URL
**作用**: 拼接 URL，支持参数映射和 fragment 模式

**参数**:
- `base`: 基础 URL
- `query`: 查询参数 Map
- `keys`: 参数名映射（可选），用于将 query 中的 key 映射到 URL 中的另一个 key
- `fragment`: 是否拼接到 fragment（#后面）

**返回值**: 拼接后的 URL

**使用示例**:
```java
// 示例1：普通参数拼接
String baseUrl = "https://example.com/callback";
Map<String, Object> params = new HashMap<>();
params.put("code", "abc123");
params.put("state", "xyz");

String url = HttpUtils.append(baseUrl, params, null, false);
// 结果："https://example.com/callback?code=abc123&state=xyz"

// 示例2：参数名映射
String baseUrl = "https://example.com/oauth";
Map<String, Object> params = new HashMap<>();
params.put("clientId", "12345");
params.put("scope", "read");

Map<String, String> keyMapping = new HashMap<>();
keyMapping.put("clientId", "client_id");  // clientId -> client_id

String url = HttpUtils.append(baseUrl, params, keyMapping, false);
// 结果："https://example.com/oauth?client_id=12345&scope=read"

// 示例3：拼接到 fragment
String baseUrl = "https://example.com/page";
Map<String, Object> params = new HashMap<>();
params.put("token", "abc123");

String url = HttpUtils.append(baseUrl, params, null, true);
// 结果："https://example.com/page#token=abc123"
```

**项目实际使用场景**:
- **OAuth2 授权回调**: 在 `OAuth2OpenAuthServiceImpl` 中构建授权回调 URL
  ```java
  // ashore-module-system/ashore-module-system-biz/src/main/java/com/example/ashore/module/system/service/oauth2/OAuth2OpenAuthServiceImpl.java
  // 构建授权成功的回调 URL
  Map<String, Object> params = new HashMap<>();
  params.put("code", authorizationCode);
  params.put("state", state);

  String redirectUri = HttpUtils.append(redirectUri, params, null, false);
  ```

---

#### `obtainBasicAuthorization(HttpServletRequest request)` - 解析 Basic 认证
**作用**: 解析 HTTP Basic 认证信息，支持从 Header 或 URL 参数获取

**参数**:
- `request`: HTTP 请求对象

**返回值**: String[2]，[0]为 clientId，[1]为 clientSecret；如果解析失败返回 null

**使用示例**:
```java
// 示例1：从 Header 解析（标准方式）
// 请求头：Authorization: Basic YWRtaW46MTIzNDU2
String[] auth = HttpUtils.obtainBasicAuthorization(request);
if (auth != null) {
    String clientId = auth[0];      // "admin"
    String clientSecret = auth[1];  // "123456"
}

// 示例2：从 URL 参数解析（兼容方式）
// URL: /oauth/token?client_id=admin&client_secret=123456
String[] auth = HttpUtils.obtainBasicAuthorization(request);
if (auth != null) {
    String clientId = auth[0];      // "admin"
    String clientSecret = auth[1];  // "123456"
}

// 示例3：验证客户端身份
String[] auth = HttpUtils.obtainBasicAuthorization(request);
if (auth == null) {
    throw new BusinessException("缺少客户端认证信息");
}

OAuth2ClientDO client = clientService.validOAuthClientFromCache(auth[0], auth[1]);
```

**项目实际使用场景**:
- **OAuth2 令牌端点**: 在 `OAuth2TokenController` 中解析客户端认证信息
  ```java
  // ashore-module-system/ashore-module-system-biz/src/main/java/com/example/ashore/module/system/controller/oauth2/OAuth2TokenController.java
  @PostMapping("/token")
  public OAuth2AccessTokenRespVO postAccessToken(HttpServletRequest request) {
      // 解析客户端认证
      String[] clientAuth = HttpUtils.obtainBasicAuthorization(request);
      if (clientAuth == null) {
          throw new BusinessException("缺少客户端认证");
      }

      // 验证客户端
      OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(
          clientAuth[0], clientAuth[1],
          reqVO.getGrantType(), reqVO.getScopes(), reqVO.getRedirectUri()
      );

      // 生成令牌
      return oauth2TokenService.createAccessToken(client, reqVO);
  }
  ```

---

#### `post(String url, Map<String, String> headers, String requestBody)` - 发送 POST 请求
**作用**: 发送 POST 请求，支持自定义 Headers

**参数**:
- `url`: 请求 URL
- `headers`: 请求头
- `requestBody`: 请求体（通常是 JSON 字符串）

**返回值**: 响应内容（String）

**使用示例**:
```java
// 示例1：发送 JSON 请求
String url = "https://api.example.com/users";
Map<String, String> headers = new HashMap<>();
headers.put("Content-Type", "application/json");
headers.put("Authorization", "Bearer token123");

String requestBody = JsonUtils.toJsonString(userDTO);
String response = HttpUtils.post(url, headers, requestBody);

// 示例2：调用第三方 API
String url = "https://sms.example.com/send";
Map<String, String> headers = new HashMap<>();
headers.put("Content-Type", "application/json");
headers.put("X-API-Key", "your-api-key");

Map<String, Object> params = new HashMap<>();
params.put("mobile", "13800138000");
params.put("content", "验证码：123456");

String response = HttpUtils.post(url, headers, JsonUtils.toJsonString(params));
```

**项目实际使用场景**:
- **腾讯云短信**: 在 `TencentSmsClient` 中发送短信
  ```java
  // ashore-module-system/ashore-module-system-biz/src/main/java/com/example/ashore/module/system/framework/sms/core/client/impl/TencentSmsClient.java
  @Override
  protected SmsSendRespDTO doSendSms(Long sendLogId, String mobile,
                                     String apiTemplateId, List<KeyValue<String, Object>> templateParams) {
      // 构建请求头
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("Authorization", buildAuthorization());

      // 构建请求体
      String requestBody = buildRequestBody(mobile, apiTemplateId, templateParams);

      // 发送请求
      String response = HttpUtils.post(SMS_URL, headers, requestBody);

      // 解析响应
      return parseResponse(response);
  }
  ```

- **七牛云短信**: 在 `QiNiuSmsClient` 中发送短信
  ```java
  // ashore-module-system/ashore-module-system-biz/src/main/java/com/example/ashore/module/system/framework/sms/core/client/impl/QiNiuSmsClient.java
  String response = HttpUtils.post(url, headers, requestBody);
  ```

---

#### `get(String url, Map<String, String> headers)` - 发送 GET 请求
**作用**: 发送 GET 请求，支持自定义 Headers

**参数**:
- `url`: 请求 URL
- `headers`: 请求头

**返回值**: 响应内容（String）

**使用示例**:
```java
// 示例1：发送带认证的 GET 请求
String url = "https://api.example.com/users/123";
Map<String, String> headers = new HashMap<>();
headers.put("Authorization", "Bearer token123");

String response = HttpUtils.get(url, headers);
User user = JsonUtils.parseObject(response, User.class);

// 示例2：调用第三方 API 查询
String url = "https://api.weather.com/current?city=Beijing";
Map<String, String> headers = new HashMap<>();
headers.put("X-API-Key", "your-api-key");

String response = HttpUtils.get(url, headers);
WeatherInfo weather = JsonUtils.parseObject(response, WeatherInfo.class);
```

**项目实际使用场景**:
- **短信渠道查询**: 查询短信发送状态
- **第三方 API 调用**: 调用外部 API 获取数据

---

## 三、实战场景总结

### 场景1：阿里云短信发送
```java
// 构建请求参数（需要 URL 编码）
Map<String, String> params = new HashMap<>();
params.put("PhoneNumbers", HttpUtils.encodeUtf8(mobile));
params.put("SignName", HttpUtils.encodeUtf8(signName));
params.put("TemplateCode", apiTemplateId);
params.put("TemplateParam", HttpUtils.encodeUtf8(JsonUtils.toJsonString(templateParams)));

// 构建签名
String signature = buildSignature(params);
params.put("Signature", signature);

// 发送请求
String url = buildUrl(params);
String response = HttpUtils.get(url, new HashMap<>());
```

### 场景2：OAuth2 授权回调
```java
// 构建授权成功的回调 URL
Map<String, Object> params = new HashMap<>();
params.put("code", authorizationCode);
params.put("state", state);

// 参数名映射（如果需要）
Map<String, String> keyMapping = new HashMap<>();
keyMapping.put("code", "authorization_code");

String redirectUri = HttpUtils.append(
    originalRedirectUri,
    params,
    keyMapping,
    false  // 拼接到 query，不是 fragment
);

// 重定向到回调地址
response.sendRedirect(redirectUri);
```

### 场景3：社交登录回调 URL 动态调整
```java
// 根据不同的社交平台，动态调整回调 URL
String redirectUri = socialConfig.getRedirectUri();

// 添加社交类型参数
redirectUri = HttpUtils.replaceUrlQuery(redirectUri, "type", socialType);

// 添加状态参数（防 CSRF）
redirectUri = HttpUtils.replaceUrlQuery(redirectUri, "state", UUID.randomUUID().toString());

// 使用新的回调 URL
return redirectUri;
```

### 场景4：文件 URL 清理
```java
// 从 S3 获取的文件 URL 包含签名参数
String fileUrl = s3Client.getObjectUrl(bucketName, objectKey);
// 例如：https://s3.amazonaws.com/bucket/file.jpg?AWSAccessKeyId=xxx&Signature=xxx&Expires=xxx

// 移除签名参数，存储纯净的文件路径
String cleanPath = HttpUtils.removeUrlQuery(fileUrl);
// 结果：https://s3.amazonaws.com/bucket/file.jpg

// 保存到数据库
fileDO.setUrl(cleanPath);
```

### 场景5：OAuth2 客户端认证
```java
// OAuth2 令牌端点，客户端认证支持两种方式

// 方式1：HTTP Basic 认证（推荐）
// 请求头：Authorization: Basic YWRtaW46MTIzNDU2

// 方式2：URL 参数认证（兼容）
// URL: /oauth/token?client_id=admin&client_secret=123456

// 统一处理
String[] clientAuth = HttpUtils.obtainBasicAuthorization(request);
if (clientAuth == null) {
    throw new BusinessException("缺少客户端认证信息");
}

// 验证客户端
OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(
    clientAuth[0],  // clientId
    clientAuth[1],  // clientSecret
    grantType,
    scopes,
    redirectUri
);
```

### 场景6：第三方短信平台调用
```java
// 腾讯云短信发送
public SmsSendRespDTO sendSms(String mobile, String templateId, Map<String, Object> params) {
    // 1. 构建请求头
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Authorization", buildAuthorization());
    headers.put("X-TC-Action", "SendSms");
    headers.put("X-TC-Version", "2021-01-11");
    headers.put("X-TC-Timestamp", String.valueOf(System.currentTimeMillis() / 1000));

    // 2. 构建请求体
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("PhoneNumberSet", Collections.singletonList(mobile));
    requestBody.put("TemplateId", templateId);
    requestBody.put("TemplateParamSet", buildTemplateParams(params));
    requestBody.put("SmsSdkAppId", appId);
    requestBody.put("SignName", signName);

    String requestBodyJson = JsonUtils.toJsonString(requestBody);

    // 3. 发送请求
    String response = HttpUtils.post(SMS_URL, headers, requestBodyJson);

    // 4. 解析响应
    return parseResponse(response);
}
```

---

## 四、注意事项

### 4.1 URL 编码
1. **统一使用 UTF-8**: `encodeUtf8()` 强制使用 UTF-8，避免乱码
2. **中文参数必须编码**: 包含中文的参数传递前必须编码
3. **特殊字符处理**: 空格、&、=、? 等特殊字符会被转义

### 4.2 URL 参数处理
1. **replaceUrlQuery 会先移除再添加**: 避免重复参数
2. **removeUrlQuery 会移除 fragment**: 不仅移除 query，还会移除 #后面的内容
3. **参数顺序不保证**: 处理后的参数顺序可能改变

### 4.3 HTTP 请求
1. **响应会自动关闭**: `post()` 和 `get()` 方法内部使用 try-with-resources，自动关闭连接
2. **异常处理**: 网络异常会抛出 `IORuntimeException`，需要捕获处理
3. **超时设置**: 使用默认超时时间，长时间请求需要自定义

### 4.4 Basic 认证解析
1. **支持两种方式**: Header 和 URL 参数，优先从 Header 获取
2. **返回 null 表示失败**: 需要判空处理
3. **Base64 解码**: Authorization 头会自动进行 Base64 解码

### 4.5 append() 方法
1. **来自 Spring Security OAuth2**: 这是从 Spring Security OAuth2 项目中复制的方法
2. **支持参数映射**: 可以将内部参数名映射为外部参数名
3. **支持 fragment 模式**: OAuth2 的 Implicit 模式需要拼接到 fragment

---

## 五、常见问题

### Q1: 为什么 Hutool 有 HttpUtil，还要封装 HttpUtils？
A: Hutool 的 `HttpUtil.post()` 和 `get()` 方法不支持自定义 Headers，而很多第三方 API（如短信平台）需要传递自定义的认证 Header。

### Q2: replaceUrlQuery() 和 append() 有什么区别？
A:
- `replaceUrlQuery()`: 替换单个参数，简单场景使用
- `append()`: 批量添加参数，支持参数映射和 fragment 模式，复杂场景使用

### Q3: obtainBasicAuthorization() 为什么要支持 URL 参数方式？
A: 虽然 OAuth2 规范推荐使用 HTTP Basic 认证，但部分客户端不支持 Header，需要通过 URL 参数传递。为了兼容性，两种方式都支持。

### Q4: 为什么 post() 和 get() 方法的 Headers 参数是 Map 而不是 List？
A: 因为 HTTP Headers 通常是 key-value 形式，Map 更符合使用习惯。如果需要传递多个相同名称的 Header，可以用逗号分隔。

### Q5: removeUrlQuery() 会移除路径参数（Path Variable）吗？
A: 不会，它只移除查询参数（?后面的部分）和 fragment（#后面的部分），不会影响路径部分。
例如：`https://example.com/user/123?id=456` → `https://example.com/user/123`

---

## 六、运行机制总结

- **类型**: 静态工具类
- **触发方式**: 开发人员主动调用
- **调用位置**: Service、Client 等需要 HTTP 操作的地方
- **依赖库**:
  - Hutool (`cn.hutool.http`)
  - Spring Web (`org.springframework.web.util`)
  - Jakarta Servlet (`jakarta.servlet`)
- **设计模式**: 门面模式（封装复杂的 HTTP 操作，提供简单接口）
- **线程安全**: 所有方法都是无状态的，线程安全

**文档版本**: v1.0  
**最后更新**: 2025-10-19  
**维护者**: Ashore 团队
