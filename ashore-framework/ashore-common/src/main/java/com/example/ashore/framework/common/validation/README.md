# Validation 包说明文档

## 一、概述

本包基于 **Hibernate Validator** 实现参数校验功能,提供了一系列自定义校验注解和验证器,用于简化业务开发中的参数验证逻辑。

### 1.1 包结构

```
com.example.ashore.framework.common.validation
├── InEnum.java                        # 枚举值校验注解
├── InEnumValidator.java               # 单个枚举值验证器
├── InEnumCollectionValidator.java     # 枚举值集合验证器
├── Mobile.java                        # 手机号校验注解
├── MobileValidator.java               # 手机号验证器
├── Telephone.java                     # ��话号码校验注解
├── TelephoneValidator.java            # 电话号码验证器
└── package-info.java                  # 包说明文件
```

---

## 二、核心注解与验证器

### 2.1 @InEnum - 枚举值校验

#### 2.1.1 功能说明
校验参数值是否在指定枚举类的有效值范围内,支持单个值和集合类型的校验。

#### 2.1.2 注解定义
```java
@InEnum(value = YourEnumClass.class, message = "必须在指定范围 {value}")
```

#### 2.1.3 使用场景
- 校验状态码、类型码等枚举值
- 校验多选框、标签列表等枚举集合

#### 2.1.4 ⚠️ 必须配合使用的类/接口

**1. ArrayValuable 接口 (必需)**
- **位置**: `com.example.ashore.framework.common.core.ArrayValuable`
- **作用**: 定义枚举类必须实现的接口,提供 `array()` 方法返回所有有效枚举值
- **为什么必需**: `@InEnum` 注解的 `value` 属性要求传入实现了 `ArrayValuable` 接口的枚举类,验证器会调用 `array()` 方法获取有效值列表进行校验

```java
public interface ArrayValuable<T> {
    /**
     * @return 数组
     */
    T[] array();
}
```

#### 2.1.5 使用方式

**1. 定义枚举类(必须实现 ArrayValuable 接口)**
```java
@Getter
@AllArgsConstructor
public enum UserStatusEnum implements ArrayValuable<Integer> {
    ENABLE(1, "启用"),
    DISABLE(0, "禁用");

    private final Integer value;
    private final String label;

    // 定义常量数组,避免每次调用 array() 方法时重复创建
    public static final Integer[] ARRAYS = Arrays.stream(values())
        .map(UserStatusEnum::getValue).toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;  // 返回所有有效的枚举值
    }
}
```

**2. 实际项目示例**
```java
// 示例1: 用户类型枚举 (来自 OAuth2AccessTokenCreateReqDTO)
@InEnum(value = UserTypeEnum.class, message = "用户类型必须是 {value}")
private Integer userType;

// 示例2: 社交平台类型枚举 (来自 SocialUserBindReqDTO)
@InEnum(SocialTypeEnum.class)
private Integer socialType;

// 示例3: 短信场景枚举 (来自 SmsCodeValidateReqDTO)
@InEnum(SmsSceneEnum.class)
private Integer scene;

// 示例4: 通用状态枚举 (来自 ErpWarehouseSaveReqVO)
@InEnum(CommonStatusEnum.class)
private Integer status;

// 示例5: 审核状态枚举 (来自 ErpStockOutPageReqVO)
@InEnum(ErpAuditStatus.class)
private Integer auditStatus;
```

**3. 在实体类中使用**
```java
public class UserUpdateReqVO {
    // 单个值校验
    @NotNull(message = "用户状态不能为空")
    @InEnum(value = UserStatusEnum.class, message = "用户状态必须是 {value}")
    private Integer status;

    // 集合校验
    @InEnum(value = UserStatusEnum.class, message = "批量状态必须是 {value}")
    private List<Integer> statusList;
}
```

#### 2.1.6 实现类
- **InEnumValidator**: 校验单个枚举值 (Object 类型)
- **InEnumCollectionValidator**: 校验枚举值集合 (Collection<?> 类型)

#### 2.1.7 特性
- 空值默认通过校验 (需配合 `@NotNull` 确保非空)
- 校验失败时自动替换 `{value}` 为有效值列表
- 支持 METHOD、FIELD、PARAMETER、TYPE_USE 等多种使用位置
- 初始化时缓存有效值列表,提升校验性能

---

### 2.2 @Mobile - 手机号校验

#### 2.2.1 功能说明
校验字符串是否为合法的中国大陆手机号码。

#### 2.2.2 注解定义
```java
@Mobile(message = "手机号格式不正确")
```

#### 2.2.3 使用场景
- 用户注册、登录时的手机号验证
- 联系方式字段校验
- 手机号绑定/修改功能

#### 2.2.4 ⚠️ 必须配合使用的类/接口

**1. ValidationUtils 工具类 (内部依赖)**
- **位置**: `com.example.ashore.framework.common.util.validation.ValidationUtils`
- **作用**: 提供 `isMobile(String mobile)` 静态方法,使用正则表达式校验手机号格式
- **为什么使用**: `MobileValidator` 内部调用 `ValidationUtils.isMobile()` 进行实际的格式校验
- **校验规则**:
  - 支持 11 位标准手机号: `13812345678`
  - 支持带 +86 国际区号: `+8613812345678`
  - 支持 0086 前缀: `008613812345678`
  - 覆盖主流运营商号段 (13x, 14x, 15x, 16x, 17x, 18x, 19x)

**2. @NotBlank 注解 (推荐组合使用)**
- **位置**: `jakarta.validation.constraints.NotBlank`
- **作用**: 确保字段不为 null、不为空字符串、不全是空白字符
- **为什么推荐**: `@Mobile` 对空值默认返回 true,需配合 `@NotBlank` 确保必填

#### 2.2.5 使用方式

**1. 基础用法**
```java
public class UserRegisterReqVO {
    @NotBlank(message = "手机号不能为空")
    @Mobile(message = "手机号格式不正确")
    private String mobile;
}
```

**2. 实际项目示例**
```java
// 示例1: 短信发送 (来自 SmsSendSingleToUserReqDTO)
@Mobile
private String mobile;

// 示例2: 短信验证码校验 (来自 SmsCodeValidateReqDTO)
@Mobile
private String mobile;

// 示例3: 会员用户修改手机号 (来自 AppMemberUserUpdateMobileReqVO)
@NotBlank(message = "新手机号不能为空")
@Mobile(message = "手机号格式不正确")
private String mobile;

// 示例4: 重置密码 (来自 AppMemberUserResetPasswordReqVO)
@NotBlank(message = "手机号不能为空")
@Mobile
private String mobile;

// 示例5: ERP 供应商管理 (来自 ErpSupplierSaveReqVO)
@Mobile
private String mobile;

// 示例6: Service 层方法参数校验 (来自 MemberUserService)
MemberUserDO createUserIfAbsent(@Mobile String mobile, String registerIp, Integer terminal);
```

#### 2.2.6 实现类
- **MobileValidator**: 使用 `ValidationUtils.isMobile()` 进行校验
  ```java
  public class MobileValidator implements ConstraintValidator<Mobile, String> {
      @Override
      public boolean isValid(String value, ConstraintValidatorContext context) {
          if (StrUtil.isEmpty(value)) {
              return true;  // 空值默认通过
          }
          return ValidationUtils.isMobile(value);  // 调用工具类校验
      }
  }
  ```

#### 2.2.7 特性
- 空值默认通过校验 (需配合 `@NotBlank` 确保非空)
- 支持标准 11 位手机号及带国际区号的格式
- 基于正则表达式验证,性能高效
- 可用于 FIELD、METHOD、PARAMETER 等多种位置

---

### 2.3 @Telephone - 电话号码校验

#### 2.3.1 功能说明
校验字符串是否为合法的电话号码,支持固定电话和手机号。

#### 2.3.2 注解定义
```java
@Telephone(message = "电话格式不正确")
```

#### 2.3.3 使用场景
- 企业联系方式字段校验
- 支持座机和手机的混合场景
- 客户信息录入

#### 2.3.4 ⚠️ 必须配合使用的类/接口

**1. Hutool PhoneUtil 工具类 (内部依赖)**
- **位置**: `cn.hutool.core.util.PhoneUtil`
- **作用**: 提供电话号码格式校验的静态方法
  - `PhoneUtil.isTel(String value)`: 校验固定电话号码
  - `PhoneUtil.isPhone(String value)`: 校验手机号码
- **为什么使用**: `TelephoneValidator` 内部调用这两个方法,实现对固定电话和手机号的双重支持
- **支持格式**:
  - 固定电话: `010-12345678`, `0571-87654321`, `400-800-8888`
  - 手机号: `13812345678`, `+8613912345678`
  - 带分机号: `010-12345678-1234`

**2. @NotBlank 注解 (推荐组合使用)**
- **位置**: `jakarta.validation.constraints.NotBlank`
- **作用**: 确保字段不为 null、不为空字符串、不全是空白字符
- **为什么推荐**: `@Telephone` 对空值默认返回 true,需配合 `@NotBlank` 确保必填

#### 2.3.5 使用方式

**1. 基础用法**
```java
public class CompanyInfoVO {
    @NotBlank(message = "联系电话不能为空")
    @Telephone(message = "联系电话格式不正确")
    private String contactPhone;

    @Telephone  // 选填字段,可以为空
    private String customerPhone;
}
```

**2. 实际项目示例**
```java
// 示例1: ERP 供应商管理 (来自 ErpSupplierSaveReqVO)
@Telephone
private String telephone;  // 固定电话
```

**3. 典型场景 - 手机号和座机二选一**
```java
public class ContactInfoVO {
    @Mobile
    private String mobile;      // 手机号

    @Telephone
    private String telephone;   // 座机

    // 自定义校验: 至少填写一个
    @AssertTrue(message = "手机号和固定电话至少填写一个")
    public boolean isContactValid() {
        return StrUtil.isNotBlank(mobile) || StrUtil.isNotBlank(telephone);
    }
}
```

#### 2.3.6 实现类
- **TelephoneValidator**: 使用 Hutool 的 `PhoneUtil` 进行校验
  ```java
  public class TelephoneValidator implements ConstraintValidator<Telephone, String> {
      @Override
      public boolean isValid(String value, ConstraintValidatorContext context) {
          if (CharSequenceUtil.isEmpty(value)) {
              return true;  // 空值默认通过
          }
          // 校验固定电话或手机号,任一通过即可
          return PhoneUtil.isTel(value) || PhoneUtil.isPhone(value);
      }
  }
  ```

#### 2.3.7 特性
- 空值默认通过校验 (需配合 `@NotBlank` 确保非空)
- 同时支持固定电话 (带区号) 和手机号
- 支持多种固话格式: `0571-88888888`、`010-88888888`、`400-800-8888` 等
- 支持带分机号的固定电话
- 基于 Hutool 成熟的校验逻辑,覆盖范围广

---

## 三、校验流程说明

### 3.1 校验触发时机
使用 Spring Validation 时,在以下场景会触发校验:
1. Controller 方法参数使用 `@Valid` 或 `@Validated`
2. Service 方法参数使用 `@Validated`
3. 手动调用 `Validator.validate()`

### 3.2 示例: Controller 中使用

```java
@RestController
@RequestMapping("/user")
public class UserController {

    @PostMapping("/register")
    public CommonResult<Long> register(@Valid @RequestBody UserRegisterReqVO reqVO) {
        // 进入方法前已完成校验,校验失败会抛出 MethodArgumentNotValidException
        return success(userService.register(reqVO));
    }
}
```

### 3.3 校验结果处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<?> handleValidationException(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        FieldError error = result.getFieldError();
        String message = error != null ? error.getDefaultMessage() : "参数校验失败";
        return CommonResult.error(BAD_REQUEST.getCode(), message);
    }
}
```

---

## 四、最佳实践

### 4.1 组合使用注解
```java
public class UserSaveReqVO {
    @NotBlank(message = "手机号不能为空")
    @Mobile(message = "手机号格式不正确")
    private String mobile;

    @NotNull(message = "状态不能为空")
    @InEnum(value = UserStatusEnum.class)
    private Integer status;
}
```

### 4.2 分组校验
```java
public class UserVO {
    @NotNull(groups = UpdateGroup.class, message = "更新时ID不能为空")
    private Long id;

    @NotBlank(groups = {CreateGroup.class, UpdateGroup.class})
    @Mobile
    private String mobile;
}

// 使用时指定分组
@PostMapping("/create")
public CommonResult<?> create(@Validated(CreateGroup.class) @RequestBody UserVO vo) {
    // ...
}
```

### 4.3 自定义错误消息
```java
public class OrderCreateReqVO {
    @InEnum(
        value = OrderTypeEnum.class,
        message = "订单类型错误,支持的类型: {value}"
    )
    private Integer orderType;
}
```

---

## 五、依赖关系总览

本包中的注解在使用时需要配合以下类/接口,理解这些依赖关系有助于正确使用校验功能:

### 5.1 核心依赖图

```
┌─────────────────────────────────────────────────────────────┐
│                     Validation 包依赖关系                      │
└─────────────────────────────────────────────────────────────┘

@InEnum 注解
    │
    ├─► ArrayValuable 接口 (必需)
    │   └─► 位置: com.example.ashore.framework.common.core.ArrayValuable
    │   └─► 枚举类必须实现此接口,提供 array() 方法
    │
    ├─► InEnumValidator (内部)
    │   └─► 校验单个枚举值
    │
    └─► InEnumCollectionValidator (内部)
        └─► 校验枚举值集合

@Mobile 注解
    │
    ├─► ValidationUtils 工具类 (内部依赖)
    │   └─► 位置: com.example.ashore.framework.common.util.validation.ValidationUtils
    │   └─► 提供 isMobile() 静态方法
    │
    ├─► MobileValidator (内部)
    │   └─► 调用 ValidationUtils.isMobile() 校验
    │
    └─► @NotBlank 注解 (推荐组合)
        └─► 确保非空 (因为 @Mobile 对空值返回 true)

@Telephone 注解
    │
    ├─► Hutool PhoneUtil 工具类 (外部依赖)
    │   └─► 位置: cn.hutool.core.util.PhoneUtil
    │   └─► 提供 isTel() 和 isPhone() 方法
    │
    ├─► TelephoneValidator (内部)
    │   └─► 调用 PhoneUtil.isTel() 和 PhoneUtil.isPhone() 校验
    │
    └─► @NotBlank 注解 (推荐组合)
        └─► 确保非空 (因为 @Telephone 对空值返回 true)

所有注解共同依赖:
    │
    ├─► Jakarta Validation API (jakarta.validation.*)
    │   └─► 提供 @Constraint、ConstraintValidator 等标准接口
    │
    ├─► Hibernate Validator
    │   └─► Jakarta Validation 的参考实现
    │
    ├─► Spring Boot Validation Starter (推荐)
    │   └─► spring-boot-starter-validation
    │   └─► 自动配置验证器和异常处理
    │
    └─► @Valid / @Validated 注解 (触发校验)
        └─► @Valid: Jakarta 标准注解
        └─► @Validated: Spring 扩展注解,支持分组校验
```

### 5.2 依赖使用要点

| 注解 | 必需依赖 | 推荐组合 | 依赖原因 |
|------|---------|---------|---------|
| `@InEnum` | `ArrayValuable` 接口 | `@NotNull` | 枚举类必须实现 ArrayValuable 提供有效值数组 |
| `@Mobile` | `ValidationUtils` 工具类 | `@NotBlank` | 内部调用 ValidationUtils.isMobile() 校验格式 |
| `@Telephone` | `Hutool PhoneUtil` | `@NotBlank` | 内部调用 PhoneUtil 校验固话和手机号 |

### 5.3 常见组合模式

**模式1: 枚举值 + 非空校验**
```java
@NotNull(message = "状态不能为空")
@InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
private Integer status;
```

**模式2: 手机号 + 非空校验**
```java
@NotBlank(message = "手机号不能为空")
@Mobile(message = "手机号格式不正确")
private String mobile;
```

**模式3: 电话号 + 非空校验**
```java
@NotBlank(message = "联系电话不能为空")
@Telephone(message = "电话格式不正确")
private String telephone;
```

**模式4: Service 方法参数校验**
```java
public interface UserService {
    // 方法参数直接使用校验注解
    void sendSms(@Mobile String mobile, @InEnum(SmsSceneEnum.class) Integer scene);
}
```

---

## 六、依赖说明

- **Jakarta Validation API**: 标准校验 API
- **Hibernate Validator**: 校验框架实现
- **Hutool**: 提供工具类支持(PhoneUtil 等)
- **框架内部依赖**:
  - `ArrayValuable`: 枚举值接口
  - `ValidationUtils`: 校验工具类

---

## 七、扩展开发

### 7.1 自定义校验注解步骤

**1. 定义注解**
```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IdCardValidator.class)
public @interface IdCard {
    String message() default "身份证号格式不正确";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

**2. 实现验证器**
```java
public class IdCardValidator implements ConstraintValidator<IdCard, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isEmpty(value)) {
            return true;
        }
        return IdcardUtil.isValidCard(value);
    }
}
```

**3. 使用注解**
```java
public class UserVO {
    @IdCard
    private String idCard;
}
```

---

## 八、注意事项

### 8.1 空值处理机制
**重要**: 所有自定义验证器对 null/空值默认返回 `true`,这是有意为之的设计:
- **@InEnum**: 对 `null` 返回 true → 需配合 `@NotNull` 使用
- **@Mobile**: 对空字符串返回 true → 需配合 `@NotBlank` 使用
- **@Telephone**: 对空字符串返回 true → 需配合 `@NotBlank` 使用

```java
// ❌ 错误: 允许 null 值通过
@InEnum(UserStatusEnum.class)
private Integer status;

// ✅ 正确: 确保非空且在枚举范围内
@NotNull(message = "状态不能为空")
@InEnum(UserStatusEnum.class)
private Integer status;
```

### 8.2 @InEnum 必须配合 ArrayValuable 接口
枚举类必须实现 `ArrayValuable` 接口,否则会编译失败:

```java
// ❌ 错误: 普通枚举类无法使用
public enum StatusEnum {
    ENABLE, DISABLE;
}

@InEnum(StatusEnum.class)  // 编译错误!
private Integer status;

// ✅ 正确: 实现 ArrayValuable 接口
public enum StatusEnum implements ArrayValuable<Integer> {
    ENABLE(1), DISABLE(0);

    private final Integer value;

    public static final Integer[] ARRAYS = {1, 0};

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
```

### 8.3 性能优化建议
**枚举值缓存**: 枚举值校验会在初始化时调用 `array()` 方法并缓存结果,建议在枚举类中定义静态常量数组:
```java
// ✅ 推荐: 使用静态常量避免重复创建数组
public static final Integer[] ARRAYS = Arrays.stream(values())
    .map(StatusEnum::getValue).toArray(Integer[]::new);

@Override
public Integer[] array() {
    return ARRAYS;  // 直接返回缓存的数组
}
```

**避免在循环中频繁校验**: 如需批量校验,考虑使用集合版本
```java
// ❌ 低效: 逐个校验
for (Integer status : statusList) {
    validateStatus(status);  // 每次都创建 Validator
}

// ✅ 高效: 使用集合校验
@InEnum(UserStatusEnum.class)
private List<Integer> statusList;  // 一次性校验整个集合
```

### 8.4 异常处理最佳实践
建议在全局异常处理器中统一处理校验异常:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理 @Valid/@Validated 在 Controller 参数校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<?> handleValidationException(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        FieldError error = result.getFieldError();
        String message = error != null ? error.getDefaultMessage() : "参数校验失败";
        return CommonResult.error(BAD_REQUEST.getCode(), message);
    }

    // 处理 @Validated 在 Service 方法参数校验失败
    @ExceptionHandler(ConstraintViolationException.class)
    public CommonResult<?> handleConstraintViolation(ConstraintViolationException ex) {
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        String message = violations.stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));
        return CommonResult.error(BAD_REQUEST.getCode(), message);
    }
}
```

### 8.5 国际化支持
message 属性支持使用 `{key}` 引用国际化资源文件:

```properties
# ValidationMessages.properties
user.status.invalid=用户状态必须是 {value}
mobile.format.invalid=手机号格式不正确
```

```java
@InEnum(value = UserStatusEnum.class, message = "{user.status.invalid}")
private Integer status;

@Mobile(message = "{mobile.format.invalid}")
private String mobile;
```

### 8.6 @Valid 和 @Validated 的区别
| 特性 | @Valid | @Validated |
|------|--------|-----------|
| 来源 | Jakarta Validation 标准 | Spring 框架扩展 |
| 分组校验 | ❌ 不支持 | ✅ 支持 |
| 嵌套校验 | ✅ 支持 | ✅ 支持 |
| 使用位置 | Controller/方法参数/字段 | Controller/方法参数/类 |
| Service 方法 | ❌ 不生效 | ✅ 生效 (需在类上加 @Validated) |

```java
// Controller 层: 推荐使用 @Validated (支持分组)
@PostMapping("/create")
public CommonResult<?> create(@Validated(CreateGroup.class) @RequestBody UserVO vo) {
    // ...
}

// Service 层: 必须使用 @Validated
@Service
@Validated  // 类级别注解,启用方法参数校验
public class UserServiceImpl {
    public void sendSms(@Mobile String mobile) {
        // ...
    }
}
```

### 8.7 依赖版本兼容性
- 项目使用 **Jakarta Validation** (jakarta.validation.*),不是旧版的 javax.validation
- 如果从 Spring Boot 2.x 升级到 3.x,需将所有 `javax.validation` 改为 `jakarta.validation`
- Hutool 版本需 ≥ 5.x 以确保 PhoneUtil 功能完整

### 8.8 自定义错误消息中的占位符
- `@InEnum` 支持 `{value}` 占位符,会自动替换为有效值列表
- 自定义消息时可使用此特性:
  ```java
  @InEnum(value = StatusEnum.class, message = "状态码无效,有效值为: {value}")
  private Integer status;
  // 校验失败时输出: "状态码无效,有效值为: [1, 0]"
  ```

---

## 九、常见问题

### Q1: 为什么校验不生效?

**可能原因及解决方案:**

**1. Controller 方法参数缺少 @Valid 或 @Validated**
```java
// ❌ 错误: 缺少校验触发注解
public CommonResult<?> create(@RequestBody UserVO vo) { }

// ✅ 正确: 添加 @Valid 或 @Validated
public CommonResult<?> create(@Valid @RequestBody UserVO vo) { }
```

**2. Service 方法缺少类级别的 @Validated**
```java
// ❌ 错误: 方法参数校验不生效
@Service
public class UserService {
    public void sendSms(@Mobile String mobile) { }
}

// ✅ 正确: 在类上添加 @Validated
@Service
@Validated  // 必需!
public class UserService {
    public void sendSms(@Mobile String mobile) { }
}
```

**3. 缺少依赖**
```xml
<!-- 确认 pom.xml 中包含此依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**4. 注解使用位置错误**
```java
// ❌ 错误: 注解应该在字段上,不是类上
@Mobile
public class UserVO {
    private String mobile;
}

// ✅ 正确: 注解在字段上
public class UserVO {
    @Mobile
    private String mobile;
}
```

### Q2: @InEnum 提示 "类型不兼容" 错误?

**原因**: 枚举类没有实现 `ArrayValuable` 接口

**解决方案**:
```java
// ❌ 错误: 普通枚举类
public enum StatusEnum {
    ENABLE(1), DISABLE(0);
    private final Integer value;
    // ...
}

@InEnum(StatusEnum.class)  // 编译错误: StatusEnum 不是 ArrayValuable 的子类型
private Integer status;

// ✅ 正确: 实现 ArrayValuable 接口
public enum StatusEnum implements ArrayValuable<Integer> {
    ENABLE(1), DISABLE(0);

    private final Integer value;

    public static final Integer[] ARRAYS = {1, 0};

    @Override
    public Integer[] array() {
        return ARRAYS;
    }
}
```

### Q3: 为什么空值也能通过校验?

**原因**: 所有自定义校验器对空值默认返回 `true`,这是设计行为

**解决方案**: 配合 @NotNull / @NotBlank 使用

```java
// ❌ 问题: null 值可以通过校验
@InEnum(UserStatusEnum.class)
private Integer status;

@Mobile
private String mobile;

// ✅ 正确: 组合使用确保非空
@NotNull(message = "状态不能为空")
@InEnum(UserStatusEnum.class)
private Integer status;

@NotBlank(message = "手机号不能为空")
@Mobile
private String mobile;
```

### Q4: 枚举值校验失败提示不明确?

**问题示例**:
```
校验失败: "必须在指定范围 {value}"  // {value} 没有被替换
```

**原因**: 枚举类的 `array()` 方法实现有误

**解决方案**:
```java
// ❌ 错误: array() 返回 null 或空数组
@Override
public Integer[] array() {
    return null;  // 或 return new Integer[0];
}

// ✅ 正确: 返回包含所有有效值的数组
public static final Integer[] ARRAYS = Arrays.stream(values())
    .map(StatusEnum::getValue)
    .toArray(Integer[]::new);

@Override
public Integer[] array() {
    return ARRAYS;  // 正确返回有效值列表
}
```

### Q5: 如何自定义错误消息格式?

**方法1: 直接在注解中指定**
```java
@InEnum(value = StatusEnum.class, message = "状态码必须是 {value} 之一")
private Integer status;

@Mobile(message = "请输入正确的11位手机号")
private String mobile;
```

**方法2: 使用国际化资源文件**
```properties
# ValidationMessages.properties
user.status.invalid=用户状态必须是 {value}
mobile.format.invalid=手机号格式不正确,请输入11位数字
```

```java
@InEnum(value = UserStatusEnum.class, message = "{user.status.invalid}")
private Integer status;

@Mobile(message = "{mobile.format.invalid}")
private String mobile;
```

**方法3: 全局异常处理器统一格式化**
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public CommonResult<?> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error -> {
        errors.put(error.getField(), error.getDefaultMessage());
    });
    return CommonResult.error(BAD_REQUEST, "参数校验失败", errors);
}
```

### Q6: @Mobile 和 @Telephone 有什么区别?

| 特性 | @Mobile | @Telephone |
|------|---------|-----------|
| 校验范围 | 仅手机号 (11位) | 固定电话 + 手机号 |
| 校验工具 | ValidationUtils.isMobile() | PhoneUtil.isTel() + isPhone() |
| 支持格式 | 13812345678<br>+8613812345678 | 010-12345678 (固话)<br>13812345678 (手机)<br>400-800-8888 (特服号) |
| 适用场景 | 用户注册、登录、手机绑定 | 企业联系方式、客服电话 |

**使用建议**:
```java
public class ContactInfoVO {
    @Mobile  // 个人手机号,严格校验
    private String personalMobile;

    @Telephone  // 企业电话,支持固话和手机
    private String companyPhone;
}
```

### Q7: 如何在 Service 层使用参数校验?

**必需步骤**:
1. 在 Service 实现类上添加 `@Validated` 注解
2. 在方法参数上使用校验注解
3. 处理 `ConstraintViolationException` 异常

```java
@Service
@Validated  // ← 关键: 启用方法参数校验
public class UserServiceImpl implements UserService {

    public void sendSms(@NotBlank @Mobile String mobile,
                        @NotNull @InEnum(SmsSceneEnum.class) Integer scene) {
        // 方法执行前自动校验参数
        smsService.send(mobile, scene);
    }
}
```

```java
// 全局异常处理
@ExceptionHandler(ConstraintViolationException.class)
public CommonResult<?> handleConstraintViolation(ConstraintViolationException ex) {
    String message = ex.getConstraintViolations().stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.joining("; "));
    return CommonResult.error(BAD_REQUEST, message);
}
```

### Q8: 如何批量校验集合中的枚举值?

**方法1: 使用 @InEnum 直接校验集合**
```java
public class BatchUpdateReqVO {
    @InEnum(value = UserStatusEnum.class, message = "状态列表包含无效值")
    private List<Integer> statusList;  // InEnumCollectionValidator 会自动处理
}
```

**方法2: 使用 @Valid 嵌套校验**
```java
public class UserBatchReqVO {
    @Valid  // 触发嵌套校验
    @NotEmpty(message = "用户列表不能为空")
    private List<UserItem> users;
}

@Data
public class UserItem {
    @InEnum(UserStatusEnum.class)
    private Integer status;
}
```

### Q9: ValidationUtils 和 PhoneUtil 有什么区别?

| 工具类 | 所属库 | 手机号校验方法 | 使用场景 |
|-------|--------|--------------|---------|
| **ValidationUtils** | 框架内部 | `isMobile(String)` | @Mobile 注解内部使用 |
| **PhoneUtil** | Hutool | `isPhone(String)` + `isTel(String)` | @Telephone 注解内部使用 |

**手动调用示例**:
```java
// 方式1: 使用框架工具类
if (ValidationUtils.isMobile(mobile)) {
    // 手机号格式正确
}

// 方式2: 使用 Hutool
if (PhoneUtil.isPhone(mobile)) {
    // 手机号格式正确
}

if (PhoneUtil.isTel(telephone)) {
    // 固定电话格式正确
}
```

### Q10: 如何扩展自定义校验注解?

参考 [七、扩展开发](#七扩展开发) 章节,核心步骤:

1. **定义注解** (必须配合 `@Constraint` 指定验证器)
2. **实现验证器** (实现 `ConstraintValidator<注解, 数据类型>`)
3. **使用注解** (像使用标准注解一样)

**关键点**:
- 注解必须包含 `message`, `groups`, `payload` 三个属性
- 验证器的 `isValid` 方法返回 `true` 表示校验通过
- 空值建议默认返回 `true`,让 `@NotNull/@NotBlank` 负责非空校验

---

## 十、参考资料

- [Hibernate Validator 官方文档](https://hibernate.org/validator/)
- [Jakarta Bean Validation 规范](https://beanvalidation.org/)
- [Hutool 官方文档 - 电话工具](https://hutool.cn/docs/#/core/工具类/电话工具-PhoneUtil)
