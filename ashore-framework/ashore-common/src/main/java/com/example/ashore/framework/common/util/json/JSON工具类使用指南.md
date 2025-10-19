# JSON 工具类使用指南

## 一、包概览

`json` 包提供了完整的 JSON 序列化/反序列化工具，主要包含：

**主包**:
- **JsonUtils**: JSON 序列化和反序列化的核心工具类

**databind 子包**（自定义序列化器/反序列化器）:
- **TimestampLocalDateTimeSerializer**: LocalDateTime → 时间戳序列化器
- **TimestampLocalDateTimeDeserializer**: 时间戳 → LocalDateTime 反序列化器
- **NumberSerializer**: 超长 Long 值转 String 序列化器

该包基于 Jackson，提供了统一的 JSON 处理接口，并解决了时间类型和长整型的序列化问题。

---

## 二、JsonUtils 类详解

### 2.1 类的整体介绍

`JsonUtils` 是 JSON 工具类，主要解决以下问题：

1. **统一 JSON 框架**: 项目统一使用 Jackson，避免混用多个 JSON 库
2. **简化 API**: 封装 Jackson 复杂的 API，提供简单易用的方法
3. **时间类型处理**: 自动处理 LocalDateTime 与时间戳的转换
4. **长整型处理**: 解决 JavaScript 最大安全整数问题（2^53-1）
5. **异常处理**: 统一处理 JSON 解析异常

**为什么要二次封装？**
- Jackson 的 API 较复杂，需要处理各种异常
- 统一配置 ObjectMapper（忽略未知字段、null 值等）
- 自定义时间和数字的序列化规则

### 2.2 核心方法详解

#### `toJsonString(Object object)` - 对象转 JSON 字符串
**作用**: 将对象序列化为 JSON 字符串

**参数**:
- `object`: 待序列化的对象

**返回值**: JSON 字符串

**使用示例**:
```java
// 示例1：普通对象转 JSON
User user = new User();
user.setId(1L);
user.setName("张三");
user.setAge(18);

String json = JsonUtils.toJsonString(user);
// 结果：{"id":1,"name":"张三","age":18}

// 示例2：List 转 JSON
List<User> users = Arrays.asList(user1, user2, user3);
String json = JsonUtils.toJsonString(users);
// 结果：[{"id":1,"name":"张三"},{"id":2,"name":"李四"},...]

// 示例3：Map 转 JSON
Map<String, Object> data = new HashMap<>();
data.put("code", 200);
data.put("message", "success");
data.put("data", user);

String json = JsonUtils.toJsonString(data);
// 结果：{"code":200,"message":"success","data":{"id":1,"name":"张三"}}
```

**项目实际使用场景**:
- **HTTP 响应**: Controller 返回 JSON 响应
- **日志记录**: 记录对象的 JSON 格式日志
- **消息队列**: 发送 JSON 格式的消息

---

#### `toJsonByte(Object object)` - 对象转 JSON 字节数组
**作用**: 将对象序列化为 JSON 字节数组（UTF-8 编码）

**参数**:
- `object`: 待序列化的对象

**返回值**: JSON 字节数组

**使用示例**:
```java
// 示例：生成 JSON 字节数组
User user = new User(1L, "张三");
byte[] jsonBytes = JsonUtils.toJsonByte(user);

// 写入文件
Files.write(Paths.get("user.json"), jsonBytes);

// 发送网络请求
httpClient.post(url, jsonBytes);
```

**项目实际使用场景**:
- **网关响应**: 在 `GatewayAuthFilter` 中返回 JSON 字节数组
  ```java
  // ashore-gateway/src/main/java/com/example/ashore/gateway/filter/security/GatewayAuthFilter.java
  private Mono<Void> writeErrorResponse(ServerHttpResponse response, CommonResult<?> result) {
      response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      byte[] bytes = JsonUtils.toJsonByte(result);
      DataBuffer buffer = response.bufferFactory().wrap(bytes);
      return response.writeWith(Mono.just(buffer));
  }
  ```

---

#### `toJsonPrettyString(Object object)` - 对象转格式化 JSON 字符串
**作用**: 将对象序列化为格式化的 JSON 字符串（带缩进和换行）

**参数**:
- `object`: 待序列化的对象

**返回值**: 格式化的 JSON 字符串

**使用示例**:
```java
// 示例：生成格式化的 JSON
User user = new User(1L, "张三", 18);
String prettyJson = JsonUtils.toJsonPrettyString(user);

// 结果：
// {
//   "id" : 1,
//   "name" : "张三",
//   "age" : 18
// }

// 适用于日志输出、调试
log.info("用户信息：\n{}", JsonUtils.toJsonPrettyString(user));
```

**项目实际使用场景**:
- **网关日志**: 在 `GatewayAccessLogFilter` 中格式化输出请求/响应日志
  ```java
  // ashore-gateway/src/main/java/com/example/ashore/gateway/filter/logging/GatewayAccessLogFilter.java
  log.info("请求日志：\n{}", JsonUtils.toJsonPrettyString(accessLog));
  ```

---

#### `parseObject(String text, Class<T> clazz)` - JSON 字符串转对象
**作用**: 将 JSON 字符串反序列化为对象

**参数**:
- `text`: JSON 字符串
- `clazz`: 目标类型

**返回值**: 反序列化后的对象

**使用示例**:
```java
// 示例1：JSON 转对象
String json = "{\"id\":1,\"name\":\"张三\",\"age\":18}";
User user = JsonUtils.parseObject(json, User.class);
// 结果：User(id=1, name="张三", age=18)

// 示例2：处理嵌套对象
String json = "{\"code\":200,\"data\":{\"id\":1,\"name\":\"张三\"}}";
Response response = JsonUtils.parseObject(json, Response.class);
User user = response.getData();

// 示例3：空字符串处理
String emptyJson = "";
User user = JsonUtils.parseObject(emptyJson, User.class);
// 结果：null（不会抛异常）
```

**项目实际使用场景**:
- **BPM 流程变量解析**: 在 `BpmProcessInstanceServiceImpl` 中解析流程变量
  ```java
  // ashore-module-bpm/ashore-module-bpm-biz/src/main/java/com/example/ashore/module/bpm/service/task/BpmProcessInstanceServiceImpl.java
  String variablesJson = processInstance.getVariables();
  Map<String, Object> variables = JsonUtils.parseObject(variablesJson, Map.class);
  ```

---

#### `parseObject(String text, TypeReference<T> typeReference)` - JSON 字符串转泛型对象
**作用**: 将 JSON 字符串反序列化为泛型对象（如 `List<User>`, `Map<String, User>`）

**参数**:
- `text`: JSON 字符串
- `typeReference`: 类型引用

**返回值**: 反序列化后的泛型对象

**使用示例**:
```java
// 示例1：JSON 转 List
String json = "[{\"id\":1,\"name\":\"张三\"},{\"id\":2,\"name\":\"李四\"}]";
List<User> users = JsonUtils.parseObject(json, new TypeReference<List<User>>() {});
// 结果：[User(id=1, name="张三"), User(id=2, name="李四")]

// 示例2：JSON 转 Map
String json = "{\"user1\":{\"id\":1,\"name\":\"张三\"},\"user2\":{\"id\":2,\"name\":\"李四\"}}";
Map<String, User> userMap = JsonUtils.parseObject(json, new TypeReference<Map<String, User>>() {});
// 结果：{"user1": User(id=1, name="张三"), "user2": User(id=2, name="李四")}

// 示例3：JSON 转复杂嵌套结构
String json = "{\"data\":[{\"id\":1,\"items\":[{\"name\":\"商品1\"}]}]}";
Response<List<Order>> response = JsonUtils.parseObject(json,
    new TypeReference<Response<List<Order>>>() {});
```

**项目实际使用场景**:
- **网关令牌校验**: 解析令牌校验响应
  ```java
  // ashore-gateway/src/main/java/com/example/ashore/gateway/filter/security/GatewayAuthFilter.java
  String responseBody = getResponseBody();
  CommonResult<LoginUser> result = JsonUtils.parseObject(responseBody,
      new TypeReference<CommonResult<LoginUser>>() {});
  ```

---

#### `parseArray(String text, Class<T> clazz)` - JSON 数组字符串转 List
**作用**: 将 JSON 数组字符串反序列化为 List

**参数**:
- `text`: JSON 数组字符串
- `clazz`: 元素类型

**返回值**: List 对象

**使用示例**:
```java
// 示例1：JSON 数组转 List
String json = "[{\"id\":1,\"name\":\"张三\"},{\"id\":2,\"name\":\"李四\"}]";
List<User> users = JsonUtils.parseArray(json, User.class);
// 结果：[User(id=1, name="张三"), User(id=2, name="李四")]

// 示例2：空数组处理
String emptyJson = "[]";
List<User> users = JsonUtils.parseArray(emptyJson, User.class);
// 结果：[]（空列表，不会抛异常）

// 示例3：基本类型数组
String json = "[1,2,3,4,5]";
List<Integer> numbers = JsonUtils.parseArray(json, Integer.class);
// 结果：[1, 2, 3, 4, 5]
```

**项目实际使用场景**:
- **BPM 表单字段解析**: 解析表单字段配置
  ```java
  // ashore-module-bpm/ashore-module-bpm-biz/src/main/java/com/example/ashore/module/bpm/service/definition/BpmFormServiceImpl.java
  String fieldsJson = form.getFields();
  List<FormField> fields = JsonUtils.parseArray(fieldsJson, FormField.class);
  ```

---

#### `parseTree(String text)` - JSON 字符串转 JsonNode 树
**作用**: 将 JSON 字符串解析为 JsonNode 树结构，用于动态访问 JSON 内容

**参数**:
- `text`: JSON 字符串

**返回值**: JsonNode 对象

**使用示例**:
```java
// 示例1：解析 JSON 并动态访问
String json = "{\"user\":{\"id\":1,\"name\":\"张三\"},\"code\":200}";
JsonNode root = JsonUtils.parseTree(json);

// 访问字段
int code = root.get("code").asInt();                    // 200
String name = root.get("user").get("name").asText();    // "张三"

// 示例2：遍历数组
String json = "{\"users\":[{\"name\":\"张三\"},{\"name\":\"李四\"}]}";
JsonNode root = JsonUtils.parseTree(json);
JsonNode usersNode = root.get("users");

for (JsonNode userNode : usersNode) {
    String name = userNode.get("name").asText();
    System.out.println(name);
}

// 示例3：判断字段是否存在
if (root.has("errorCode")) {
    int errorCode = root.get("errorCode").asInt();
    // 处理错误
}
```

**项目实际使用场景**:
- **第三方 API 响应解析**: 动态解析第三方 API 的响应
- **灵活的数据处理**: 不确定 JSON 结构时使用

---

#### `isJson(String text)` - 判断字符串是否为 JSON
**作用**: 判断字符串是否为合法的 JSON 格式（对象或数组）

**参数**:
- `text`: 待判断的字符串

**返回值**: true-是 JSON，false-不是 JSON

**使用示例**:
```java
// 示例1：判断对象
boolean result = JsonUtils.isJson("{\"name\":\"张三\"}");  // true
boolean result = JsonUtils.isJson("name=张三");           // false

// 示例2：判断数组
boolean result = JsonUtils.isJson("[1,2,3]");             // true
boolean result = JsonUtils.isJson("1,2,3");               // false

// 示例3：实际应用
String data = getRequestData();
if (JsonUtils.isJson(data)) {
    // 按 JSON 处理
    User user = JsonUtils.parseObject(data, User.class);
} else {
    // 按其他格式处理
    processOtherFormat(data);
}
```

---

#### `isJsonObject(String str)` - 判断字符串是否为 JSON 对象
**作用**: 判断字符串是否为合法的 JSON 对象（不包括数组）

**参数**:
- `str`: 待判断的字符串

**返回值**: true-是 JSON 对象，false-不是 JSON 对象

**使用示例**:
```java
// 示例：区分对象和数组
boolean result = JsonUtils.isJsonObject("{\"name\":\"张三\"}");  // true
boolean result = JsonUtils.isJsonObject("[1,2,3]");             // false
boolean result = JsonUtils.isJsonObject("\"string\"");          // false

// 实际应用：根据类型处理
String data = getResponseData();
if (JsonUtils.isJsonObject(data)) {
    // 单个对象
    User user = JsonUtils.parseObject(data, User.class);
} else if (JsonUtils.isJson(data)) {
    // 可能是数组
    List<User> users = JsonUtils.parseArray(data, User.class);
}
```

---

## 三、databind 子包详解（自定义序列化器）

### 3.1 TimestampLocalDateTimeSerializer - LocalDateTime 序列化器

#### 类的作用
将 `LocalDateTime` 序列化为时间戳（毫秒数），便于前后端交互。

#### 为什么需要？
1. **前端友好**: 前端通常使用时间戳处理时间
2. **统一格式**: 避免时区问题，时间戳是通用的
3. **性能优化**: 时间戳比字符串更紧凑

#### 序列化规则
```java
LocalDateTime time = LocalDateTime.of(2024, 10, 19, 14, 30, 0);
// 序列化为时间戳（毫秒）
// 结果：1697702400000
```

#### 实际效果
```java
public class User {
    private Long id;
    private String name;
    private LocalDateTime createTime;  // 会被序列化为时间戳
}

User user = new User(1L, "张三", LocalDateTime.now());
String json = JsonUtils.toJsonString(user);
// 结果：{"id":1,"name":"张三","createTime":1697702400000}
```

---

### 3.2 TimestampLocalDateTimeDeserializer - LocalDateTime 反序列化器

#### 类的作用
将时间戳（毫秒数）反序列化为 `LocalDateTime`。

#### 为什么需要？
1. **与序列化器配套**: 与 `TimestampLocalDateTimeSerializer` 配套使用
2. **前端兼容**: 接收前端传来的时间戳
3. **灵活性**: 支持多种时间格式

#### 反序列化规则
```java
// JSON：{"createTime":1697702400000}
// 反序列化为：LocalDateTime.of(2024, 10, 19, 14, 30, 0)
```

#### 实际效果
```java
String json = "{\"id\":1,\"name\":\"张三\",\"createTime\":1697702400000}";
User user = JsonUtils.parseObject(json, User.class);
// user.getCreateTime() -> LocalDateTime.of(2024, 10, 19, 14, 30, 0)
```

---

### 3.3 NumberSerializer - 长整型序列化器

#### 类的作用
将超长 Long 值序列化为字符串，解决 JavaScript 最大安全整数问题。

#### 为什么需要？
1. **JavaScript 限制**: JavaScript 的最大安全整数是 `2^53 - 1`（9007199254740991）
2. **精度丢失**: 超过这个值的 Long 在前端会丢失精度
3. **雪花算法 ID**: 雪花算法生成的 ID 通常超过这个值

#### 序列化规则
```java
// 安全范围内的数字：正常序列化为数字
Long safeNumber = 123456L;
// JSON：123456

// 超出安全范围的数字：序列化为字符串
Long unsafeNumber = 9007199254740992L;  // 超过 2^53 - 1
// JSON："9007199254740992"
```

#### 实际效果
```java
public class User {
    private Long id;            // 通常是雪花算法 ID，会超出安全范围
    private String name;
    private Integer age;        // Integer 不会超出，正常序列化
}

User user = new User(1234567890123456789L, "张三", 18);
String json = JsonUtils.toJsonString(user);
// 结果：{"id":"1234567890123456789","name":"张三","age":18}
//       ↑ id 被序列化为字符串
```

#### 配置方法
```java
// 在 JsonUtils 的静态初始化块中已配置
static {
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    // 注册时间和数字序列化器
    SimpleModule simpleModule = new JavaTimeModule()
        .addSerializer(LocalDateTime.class, TimestampLocalDateTimeSerializer.INSTANCE)
        .addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
    objectMapper.registerModules(simpleModule);
}
```

---

## 四、实战场景总结

### 场景1：网关 JSON 响应
```java
// 网关认证失败，返回 JSON 响应
private Mono<Void> writeErrorResponse(ServerHttpResponse response, Integer code, String message) {
    // 1. 构建响应对象
    CommonResult<?> result = CommonResult.error(code, message);

    // 2. 序列化为 JSON 字节数组
    byte[] bytes = JsonUtils.toJsonByte(result);

    // 3. 写入响应
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    DataBuffer buffer = response.bufferFactory().wrap(bytes);
    return response.writeWith(Mono.just(buffer));
}
```

### 场景2：BPM 流程变量处理
```java
// 启动流程时设置变量
public void startProcess(Long userId, String processKey, Map<String, Object> variables) {
    // 1. 将变量序列化为 JSON
    String variablesJson = JsonUtils.toJsonString(variables);

    // 2. 存储到流程实例
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey);
    processInstance.setVariable("variables", variablesJson);
}

// 查询流程时解析变量
public Map<String, Object> getProcessVariables(String processInstanceId) {
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
        .processInstanceId(processInstanceId)
        .singleResult();

    // 1. 获取变量 JSON
    String variablesJson = (String) processInstance.getVariable("variables");

    // 2. 反序列化为 Map
    return JsonUtils.parseObject(variablesJson, new TypeReference<Map<String, Object>>() {});
}
```

### 场景3：前端 Long 型 ID 处理
```java
// 用户 ID 使用雪花算法生成，超出 JavaScript 安全整数范围
public class UserVO {
    private Long id;  // 1234567890123456789L
    private String name;
}

// 后端返回 JSON
User user = new User(1234567890123456789L, "张三");
String json = JsonUtils.toJsonString(user);
// 结果：{"id":"1234567890123456789","name":"张三"}
//       ↑ id 自动转为字符串，避免前端精度丢失

// 前端接收
// JavaScript
const user = JSON.parse(jsonStr);
console.log(user.id);  // "1234567890123456789"（字符串，精度完整）
```

### 场景4：时间类型前后端交互
```java
// 后端实体
public class Order {
    private Long id;
    private BigDecimal amount;
    private LocalDateTime createTime;  // 2024-10-19 14:30:00
}

// 后端序列化
Order order = new Order(1L, new BigDecimal("99.99"), LocalDateTime.now());
String json = JsonUtils.toJsonString(order);
// 结果：{"id":"1","amount":99.99,"createTime":1697702400000}
//       ↑ createTime 被序列化为时间戳

// 前端反序列化（JavaScript）
const order = JSON.parse(jsonStr);
const date = new Date(order.createTime);  // 可以直接使用时间戳创建 Date 对象

// 前端序列化（提交表单）
const orderData = {
    id: "1",
    amount: 99.99,
    createTime: new Date().getTime()  // 时间戳
};

// 后端接收并反序列化
String requestJson = getRequestBody();
Order order = JsonUtils.parseObject(requestJson, Order.class);
// order.getCreateTime() -> 自动转换为 LocalDateTime
```

### 场景5：动态解析第三方 API 响应
```java
// 第三方 API 响应格式不固定
public void handleThirdPartyResponse(String responseJson) {
    // 1. 解析为树结构
    JsonNode root = JsonUtils.parseTree(responseJson);

    // 2. 判断是否成功
    if (root.has("code")) {
        int code = root.get("code").asInt();
        if (code != 0) {
            String message = root.get("message").asText();
            throw new BusinessException("第三方 API 调用失败：" + message);
        }
    }

    // 3. 提取数据（路径不确定）
    JsonNode dataNode = root.has("data") ? root.get("data") : root.get("result");
    if (dataNode.isArray()) {
        // 数组数据
        for (JsonNode item : dataNode) {
            processItem(item);
        }
    } else {
        // 对象数据
        processObject(dataNode);
    }
}
```

### 场景6：格式化日志输出
```java
// 记录详细的请求日志
public void logRequest(HttpServletRequest request, Object requestBody) {
    AccessLog accessLog = new AccessLog();
    accessLog.setUrl(request.getRequestURI());
    accessLog.setMethod(request.getMethod());
    accessLog.setRequestBody(requestBody);
    accessLog.setCreateTime(LocalDateTime.now());

    // 格式化输出，便于查看
    log.info("API 请求日志：\n{}", JsonUtils.toJsonPrettyString(accessLog));
}

// 输出结果：
// API 请求日志：
// {
//   "url" : "/api/user/create",
//   "method" : "POST",
//   "requestBody" : {
//     "name" : "张三",
//     "age" : 18
//   },
//   "createTime" : 1697702400000
// }
```

---

## 五、注意事项

### 5.1 ObjectMapper 配置
1. **忽略未知字段**: `FAIL_ON_UNKNOWN_PROPERTIES = false`，前端多传字段不会报错
2. **忽略 null 值**: `JsonInclude.Include.NON_NULL`，null 字段不会序列化到 JSON
3. **忽略空 Bean**: `FAIL_ON_EMPTY_BEANS = false`，空对象不会报错

### 5.2 时间处理
1. **LocalDateTime 使用时间戳**: 前后端交互使用时间戳（毫秒）
2. **时区问题**: 使用系统默认时区（`ZoneId.systemDefault()`）
3. **前端处理**: 前端使用 `new Date(timestamp)` 创建日期对象

### 5.3 长整型处理
1. **雪花算法 ID**: 会被序列化为字符串
2. **前端接收**: 前端需要用字符串类型接收 ID
3. **安全范围**: -2^53+1 到 2^53-1 之间的数字正常序列化

### 5.4 异常处理
1. **统一异常**: 所有异常统一封装为 `RuntimeException`
2. **日志记录**: 解析失败会记录错误日志
3. **空值处理**: 空字符串返回 null，不抛异常

### 5.5 性能考虑
1. **复用 ObjectMapper**: 全局单例，避免重复创建
2. **toJsonByte**: 性能优于 `toJsonString().getBytes()`
3. **大对象**: 大对象序列化可能消耗较多内存

---

## 六、常见问题

### Q1: 为什么 LocalDateTime 要序列化为时间戳？
A:
1. **时区统一**: 时间戳没有时区概念，避免时区问题
2. **前端友好**: JavaScript 原生支持时间戳
3. **紧凑格式**: 数字比字符串更紧凑

### Q2: 前端如何处理 Long 型 ID？
A: 后端自动将超长 Long 序列化为字符串，前端用字符串类型接收：
```typescript
// TypeScript
interface User {
    id: string;      // Long 型 ID，后端返回字符串
    name: string;
}
```

### Q3: 如何自定义 ObjectMapper 配置？
A: 通过 `JsonUtils.init(objectMapper)` 方法：
```java
@Configuration
public class JacksonConfig {
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 自定义配置
        mapper.configure(...);
        // 初始化 JsonUtils
        JsonUtils.init(mapper);
        return mapper;
    }
}
```

### Q4: parseObject() 和 parseObject2() 有什么区别？
A:
- `parseObject()`: 使用 Jackson，标准用法
- `parseObject2()`: 使用 Hutool，用于特殊场景（如缺少 `@JsonTypeInfo` 的 class 属性）

### Q5: 为什么要忽略未知字段？
A: 前端可能传递多余的字段，忽略未知字段可以提高容错性，避免因字段不匹配导致解析失败。

### Q6: 如何处理循环引用？
A: Jackson 默认不支持循环引用，会抛异常。解决方法：
```java
// 在实体类上添加注解
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class User {
    private Long id;
    private Department department;  // 可能存在循环引用
}
```

---

## 七、最佳实践

### 实践1：统一使用 JsonUtils
```java
// 好的做法：统一使用 JsonUtils
String json = JsonUtils.toJsonString(user);
User user = JsonUtils.parseObject(json, User.class);

// 不好的做法：混用多个 JSON 库
String json = new Gson().toJson(user);  // 不推荐
String json = JSON.toJSONString(user);  // 不推荐（Fastjson）
```

### 实践2：合理使用 TypeReference
```java
// 泛型类型必须使用 TypeReference
List<User> users = JsonUtils.parseObject(json, new TypeReference<List<User>>() {});
Map<String, User> userMap = JsonUtils.parseObject(json, new TypeReference<Map<String, User>>() {});

// 简单类型可以直接使用 Class
User user = JsonUtils.parseObject(json, User.class);
```

### 实践3：格式化日志便于调试
```java
// 开发环境：使用格式化 JSON
if (isDev()) {
    log.debug("响应数据：\n{}", JsonUtils.toJsonPrettyString(response));
}

// 生产环境：使用紧凑 JSON
if (isProd()) {
    log.info("响应数据：{}", JsonUtils.toJsonString(response));
}
```

### 实践4：前端 Long 型 ID 处理
```typescript
// 后端实体
public class UserVO {
    private Long id;  // 雪花算法 ID
}

// 前端接口定义
interface UserVO {
    id: string;  // 接收为字符串
    name: string;
}

// 前端请求时转换
const userId: string = user.id;  // 字符串
axios.post('/api/user/update', { id: userId });
```

---

## 八、运行机制总结

- **类型**: 静态工具类 + 自定义序列化器
- **触发方式**: 开发人员主动调用（JsonUtils）/ 自动触发（序列化器）
- **调用位置**: Controller、Service、Gateway 等需要 JSON 操作的地方
- **依赖库**:
  - Jackson (`com.fasterxml.jackson`)
  - Hutool (`cn.hutool.json`)
  - Lombok (`@SneakyThrows`)
- **设计模式**: 门面模式（封装复杂的 Jackson API，提供简单接口）
- **线程安全**: ObjectMapper 是线程安全的，全局单例

**文档版本**: v1.0  
**最后更新**: 2025-10-19  
**维护者**: Ashore 团队  
