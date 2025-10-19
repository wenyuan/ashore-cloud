# SpringExpressionUtils 使用文档

## 一、什么是 EL 表达式

### 1.1 概念介绍

**EL（Expression Language）表达式** 是一种简洁的动态表达式语言，用于在运行时查询和操作对象数据。

**Spring EL（SpEL）** 是 Spring 框架提供的强大表达式语言，它支持：
- 运行时查询和操作对象图
- 方法调用、属性访问
- 算术、逻辑、关系运算
- 访问 Spring 容器中的 Bean
- 集合操作、正则匹配等高级功能

### 1.2 基本语法

```java
// 访问属性
#user.name

// 访问方法
#user.getName()

// 访问 Spring Bean
@userService.getUserById(#id)

// 逻辑运算
#age > 18 and #status == 'ACTIVE'

// 三元表达式
#age > 18 ? '成年' : '未成年'

// 集合操作
#users.?[age > 18]  // 过滤
#users.![name]      // 投影

// 访问静态方法
T(java.lang.Math).random()
```

---

## 二、本项目中的使用场景

`SpringExpressionUtils` 在本项目中广泛应用于以下场景：

### 2.1 CRM 数据权限校验

**场景**：从注解中动态解析业务 ID 和类型，进行数据权限校验

**代码示例**：
```java
@CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
public void updateCustomer(Long id, CustomerUpdateReqVO updateReqVO) {
    // 通过 EL 表达式 #id 从方法参数中提取客户 ID
    // ...
}
```

**实现逻辑**（`CrmPermissionAspect.java:159`）：
```java
private static Map<String, Object> parseExpressions(JoinPoint joinPoint, CrmPermission crmPermission) {
    List<String> expressionStrings = new ArrayList<>(2);
    expressionStrings.add(crmPermission.bizId());  // "#id"
    if (StrUtil.isNotEmpty(crmPermission.bizTypeValue())) {
        expressionStrings.add(crmPermission.bizTypeValue());
    }
    return SpringExpressionUtils.parseExpressions(joinPoint, expressionStrings);
}
```

### 2.2 业务链路追踪

**场景**：记录业务操作的类型和 ID，用于分布式链路追踪

**代码示例**：
```java
@BizTrace(type = "order", id = "#orderId")
public void processOrder(Long orderId) {
    // 通过 EL 表达式从参数中提取订单 ID，自动记录到链路追踪系统
    // ...
}
```

**实现逻辑**（`BizTraceAspect.java:69`）：
```java
private void setBizTag(Span span, ProceedingJoinPoint joinPoint, BizTrace trace) {
    try {
        Map<String, Object> result = SpringExpressionUtils.parseExpressions(
            joinPoint,
            asList(trace.type(), trace.id())
        );
        span.setTag(BizTrace.TYPE_TAG, MapUtil.getStr(result, trace.type()));
        span.setTag(BizTrace.ID_TAG, MapUtil.getStr(result, trace.id()));
    } catch (Exception ex) {
        log.error("[setBizTag][解析 bizType 与 bizId 发生异常]", ex);
    }
}
```

### 2.3 多租户动态开关

**场景**：根据条件动态决定是否忽略租户隔离

**代码示例**：
```java
@TenantIgnore(enable = "@config.getTenantEnable()")
public List<User> getAllUsers() {
    // 通过 EL 表达式调用 Bean 方法，动态判断是否启用租户隔离
    // ...
}
```

**实现逻辑**（`TenantIgnoreAspect.java:29`）：
```java
@Around("@annotation(tenantIgnore)")
public Object around(ProceedingJoinPoint joinPoint, TenantIgnore tenantIgnore) throws Throwable {
    Boolean oldIgnore = TenantContextHolder.isIgnore();
    try {
        // 计算条件，满足的情况下，才进行忽略
        Object enable = SpringExpressionUtils.parseExpression(tenantIgnore.enable());
        if (Boolean.TRUE.equals(enable)) {
            TenantContextHolder.setIgnore(true);
        }
        return joinPoint.proceed();
    } finally {
        TenantContextHolder.setIgnore(oldIgnore);
    }
}
```

### 2.4 物联网场景联动规则

**场景**：动态评估设备触发条件，如温度 > 30、湿度 < 50 等

**代码示例**（`IotSceneRuleMatcherHelper.java:77`）：
```java
public static boolean evaluateConditionWithOperatorEnum(
    Object sourceValue,
    IotSceneRuleConditionOperatorEnum operatorEnum,
    String paramValue
) {
    // 构建变量：{source: 35, value: 30}
    Map<String, Object> springExpressionVariables = buildSpringExpressionVariables(
        sourceValue, operatorEnum, paramValue
    );

    // 执行表达式：#source > #value
    return (Boolean) SpringExpressionUtils.parseExpression(
        operatorEnum.getSpringExpression(),
        springExpressionVariables
    );
}
```

**使用示例**：
```java
// 当设备温度 > 30 度时触发场景
evaluateCondition(currentTemperature, ">", "30")
// 实际执行的 EL 表达式：#source > #value
```

---

## 三、SpringExpressionUtils 使用方式

### 3.1 从 AOP 切面中解析表达式

#### 方法签名
```java
/**
 * 从切面中，单个解析 EL 表达式的结果
 *
 * @param joinPoint        切面点
 * @param expressionString EL 表达式
 * @return 解析结果
 */
public static Object parseExpression(JoinPoint joinPoint, String expressionString)

/**
 * 从切面中，批量解析 EL 表达式的结果
 *
 * @param joinPoint         切面点
 * @param expressionStrings EL 表达式数组
 * @return 结果 Map，key 为表达式，value 为对应值
 */
public static Map<String, Object> parseExpressions(JoinPoint joinPoint, List<String> expressionStrings)
```

#### 使用示例

**示例 1：解析方法参数**
```java
@Aspect
@Component
public class MyAspect {

    @Before("@annotation(myAnnotation)")
    public void before(JoinPoint joinPoint, MyAnnotation myAnnotation) {
        // 解析注解中的表达式
        Object userId = SpringExpressionUtils.parseExpression(
            joinPoint,
            "#user.id"  // 从方法参数 user 对象中获取 id
        );

        System.out.println("用户ID: " + userId);
    }
}

// 调用方法
public void updateUser(User user, String newName) {
    // user.id 会被自动解析
}
```

**示例 2：批量解析多个表达式**
```java
@Before("@annotation(logAnnotation)")
public void logOperation(JoinPoint joinPoint, LogAnnotation logAnnotation) {
    // 批量解析
    Map<String, Object> values = SpringExpressionUtils.parseExpressions(
        joinPoint,
        Arrays.asList("#id", "#request.name", "#request.type")
    );

    Long id = (Long) values.get("#id");
    String name = (String) values.get("#request.name");
    String type = (String) values.get("#request.type");

    log.info("操作: id={}, name={}, type={}", id, name, type);
}

// 调用方法
public void createOrder(Long id, OrderRequest request) {
    // 三个表达式会被同时解析
}
```

**示例 3：支持复杂表达式**
```java
// 注解
@Permission(bizId = "#ids", level = CrmPermissionLevelEnum.WRITE)
public void batchDelete(List<Long> ids) { }

// 解析
Map<String, Object> result = SpringExpressionUtils.parseExpressions(
    joinPoint,
    Collections.singletonList("#ids")
);
// result.get("#ids") 返回的是整个 List<Long>
```

---

### 3.2 从 Bean 工厂解析表达式

#### 方法签名
```java
/**
 * 从 Bean 工厂，解析 EL 表达式的结果
 *
 * @param expressionString EL 表达式
 * @return 执行结果
 */
public static Object parseExpression(String expressionString)

/**
 * 从 Bean 工厂，解析 EL 表达式的结果（支持自定义变量）
 *
 * @param expressionString EL 表达式
 * @param variables        变量 Map
 * @return 执行结果
 */
public static Object parseExpression(String expressionString, Map<String, Object> variables)
```

#### 使用示例

**示例 1：调用 Spring Bean 方法**
```java
// 调用 Spring 容器中的 Bean 方法
Boolean tenantEnabled = (Boolean) SpringExpressionUtils.parseExpression(
    "@tenantProperties.getEnable()"
);

// 调用带参数的 Bean 方法
User user = (User) SpringExpressionUtils.parseExpression(
    "@userService.getUserById(100)"
);
```

**示例 2：使用自定义变量**
```java
Map<String, Object> variables = new HashMap<>();
variables.put("temperature", 35);
variables.put("threshold", 30);

// 执行比较运算
Boolean result = (Boolean) SpringExpressionUtils.parseExpression(
    "#temperature > #threshold",
    variables
);
// 结果：true
```

**示例 3：复杂条件判断**
```java
Map<String, Object> variables = new HashMap<>();
variables.put("age", 25);
variables.put("status", "ACTIVE");
variables.put("level", 3);

// 复合条件
Boolean isValid = (Boolean) SpringExpressionUtils.parseExpression(
    "#age >= 18 and #status == 'ACTIVE' and #level > 2",
    variables
);
// 结果：true
```

**示例 4：集合操作**
```java
Map<String, Object> variables = new HashMap<>();
variables.put("numbers", Arrays.asList(1, 2, 3, 4, 5));

// 过滤集合
List<?> filtered = (List<?>) SpringExpressionUtils.parseExpression(
    "#numbers.?[#this > 3]",  // 过滤大于 3 的元素
    variables
);
// 结果：[4, 5]

// 映射转换
List<?> doubled = (List<?>) SpringExpressionUtils.parseExpression(
    "#numbers.![#this * 2]",  // 每个元素乘以 2
    variables
);
// 结果：[2, 4, 6, 8, 10]
```

---

## 四、最佳实践

### 4.1 性能优化建议

1. **缓存表达式解析器**：`SpringExpressionUtils` 已使用静态实例 `EXPRESSION_PARSER`，避免重复创建
2. **批量解析**：多个表达式使用 `parseExpressions()` 一次性解析，共享 `EvaluationContext`
3. **避免过于复杂的表达式**：复杂逻辑建议在 Java 代码中实现

### 4.2 安全注意事项

1. **表达式来源可信**：EL 表达式应来自系统注解或配置，不应直接接受用户输入
2. **限制表达式能力**：避免使用 `T()` 调用任意静态方法，防止安全风险
3. **异常处理**：使用 try-catch 包裹表达式解析，避免表达式错误导致系统崩溃

### 4.3 常见错误处理

```java
// 推荐的使用方式
try {
    Object result = SpringExpressionUtils.parseExpression(joinPoint, "#user.id");
    if (result == null) {
        log.warn("表达式解析结果为空");
        return;
    }
    Long userId = Long.parseLong(result.toString());
} catch (Exception e) {
    log.error("EL 表达式解析失败", e);
    throw new ServiceException("参数解析失败");
}
```

---

## 五、完整示例

### 5.1 自定义注解 + AOP + EL 表达式

```java
// 1. 定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataLog {
    String bizType();           // 业务类型
    String bizId();             // 业务 ID（支持 EL 表达式）
    String content() default ""; // 日志内容（支持 EL 表达式）
}

// 2. 定义切面
@Aspect
@Component
public class DataLogAspect {

    @Around("@annotation(dataLog)")
    public Object around(ProceedingJoinPoint joinPoint, DataLog dataLog) throws Throwable {
        // 解析表达式
        Map<String, Object> values = SpringExpressionUtils.parseExpressions(
            joinPoint,
            Arrays.asList(dataLog.bizId(), dataLog.content())
        );

        String bizId = String.valueOf(values.get(dataLog.bizId()));
        String content = String.valueOf(values.get(dataLog.content()));

        // 记录日志
        log.info("操作日志：类型={}, ID={}, 内容={}",
            dataLog.bizType(), bizId, content);

        return joinPoint.proceed();
    }
}

// 3. 使用注解
@DataLog(
    bizType = "USER",
    bizId = "#user.id",
    content = "'修改用户：' + #user.name + ' 的邮箱为：' + #newEmail"
)
public void updateUserEmail(User user, String newEmail) {
    // 业务逻辑
}
```

---

## 六、参考资料

- [Spring Expression Language (SpEL) 官方文档](https://docs.spring.io/spring-framework/reference/core/expressions.html)
- 项目中的实际应用案例：
  - `CrmPermissionAspect.java` - 数据权限
  - `BizTraceAspect.java` - 链路追踪
  - `TenantIgnoreAspect.java` - 多租户
  - `IotSceneRuleMatcherHelper.java` - 物联网规则引擎

**文档版本**: v1.0  
**最后更新**: 2025-10-19  
**维护者**: Ashore 团队  
