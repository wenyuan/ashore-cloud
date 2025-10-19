package com.example.ashore.framework.common.util.validation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import org.springframework.util.StringUtils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 校验工具类
 *
 * <p>类作用：
 * 提供常见数据格式的校验功能（手机号、URL、XML NCName）和基于 Jakarta Bean Validation 规范的对象校验功能。
 * 简化业务代码中的数据验证逻辑，统一校验标准。
 *
 * <p>使用场景：
 * 1. 手机号格式校验：验证用户输入的手机号是否符合中国大陆手机号规范
 * 2. URL 格式校验：验证输入的字符串是否为合法的 URL 地址
 * 3. XML NCName 校验：验证 XML 元素名称或属性名称是否符合规范（常用于工作流、配置文件等场景）
 * 4. Bean 对象校验：基于 JSR-303/JSR-380 注解（如 @NotNull、@Size 等）校验 Java 对象属性
 *
 * <p>核心依赖库：
 * 1. Jakarta Validation API (jakarta.validation-api)：提供 Bean Validation 标准接口
 * 2. Hibernate Validator：Jakarta Validation 的参考实现
 * 3. Hutool (cn.hutool.core)：提供集合工具类和断言工具
 * 4. Spring Framework (spring-core)：提供字符串工具类
 *
 */
public class ValidationUtils {

    /**
     * 中国大陆手机号正则表达式
     *
     * <p>正则规则说明：
     * - ^(?:(?:\\+|00)86)? ：可选的国际区号，支持 +86 或 0086 前缀
     * - 1 ：手机号必须以 1 开头
     * - (?:(?:3[\\d])|(?:4[0,1,4-9])|(?:5[0-3,5-9])|(?:6[2,5-7])|(?:7[0-8])|(?:8[\\d])|(?:9[0-3,5-9]))：
     *   第二位数字的号段规则，支持 13x、14x（除142、143）、15x（除154）、16x（162、165-167）、
     *   17x（170-178）、18x、19x（除194）等主流运营商号段
     * - \\d{8}$ ：后续 8 位数字
     *
     * <p>支持格式示例：
     * - 13812345678
     * - +8613812345678
     * - 008613812345678
     */
    private static final Pattern PATTERN_MOBILE = Pattern.compile("^(?:(?:\\+|00)86)?1(?:(?:3[\\d])|(?:4[0,1,4-9])|(?:5[0-3,5-9])|(?:6[2,5-7])|(?:7[0-8])|(?:8[\\d])|(?:9[0-3,5-9]))\\d{8}$");

    /**
     * URL 地址正则表达式
     *
     * <p>正则规则说明：
     * - ^(https?|ftp|file):// ：协议部分，支持 http、https、ftp、file
     * - [-a-zA-Z0-9+&@#/%?=~_|!:,.;]* ：URL 主体部分，允许的字符集
     * - [-a-zA-Z0-9+&@#/%=~_|] ：URL 结尾字符，不能以某些特殊符号结尾
     *
     * <p>支持格式示例：
     * - https://www.example.com
     * - ftp://192.168.1.1:21/path
     * - file:///C:/Users/test.txt
     */
    private static final Pattern PATTERN_URL = Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

    /**
     * XML NCName（Non-Colonized Name）正则表达式
     *
     * <p>使用场景：
     * 验证 XML 元素名称、属性名称、工作流节点标识、配置项 key 等是否符合 XML NCName 规范
     *
     * <p>正则规则说明：
     * - [a-zA-Z_] ：必须以字母或下划线开头
     * - [\\-_.0-9_a-zA-Z$]* ：后续字符可以是字母、数字、下划线、连字符、点号、美元符号
     * - 注意：NCName 不能包含冒号（:），这是与 XML Name 的主要区别
     *
     * <p>支持格式示例：
     * - userTask
     * - user_task_01
     * - _startEvent
     * - task$name
     */
    private static final Pattern PATTERN_XML_NCNAME = Pattern.compile("[a-zA-Z_][\\-_.0-9_a-zA-Z$]*");

    /**
     * 校验手机号格式是否正确
     *
     * <p>相比原生实现的增强：
     * 在正则匹配前先使用 StringUtils.hasText() 进行非空和非空白字符校验，避免空指针异常和无效匹配
     *
     * @param mobile 待校验的手机号字符串，示例值："13812345678"、"+8613912345678"
     * @return true：格式正确；false：格式错误或为空
     */
    public static boolean isMobile(String mobile) {
        return StringUtils.hasText(mobile)
                && PATTERN_MOBILE.matcher(mobile).matches();
    }

    /**
     * 校验 URL 格式是否正确
     *
     * <p>相比原生实现的增强：
     * 在正则匹配前先使用 StringUtils.hasText() 进行非空和非空白字符校验，避免空指针异常和无效匹配
     *
     * @param url 待校验的 URL 字符串，示例值："https://www.baidu.com"、"ftp://192.168.1.1/file.zip"
     * @return true：格式正确；false：格式错误或为空
     */
    public static boolean isURL(String url) {
        return StringUtils.hasText(url)
                && PATTERN_URL.matcher(url).matches();
    }

    /**
     * 校验字符串是否符合 XML NCName 规范
     *
     * <p>相比原生实现的增强：
     * 在正则匹配前先使用 StringUtils.hasText() 进行非空和非空白字符校验，避免空指针异常和无效匹配
     *
     * @param str 待校验的字符串，示例值："userTask"、"start_event_01"
     * @return true：符合规范；false：不符合规范或为空
     */
    public static boolean isXmlNCName(String str) {
        return StringUtils.hasText(str)
                && PATTERN_XML_NCNAME.matcher(str).matches();
    }

    /**
     * 校验对象属性是否符合 Bean Validation 注解约束
     *
     * <p>相比原生实现的增强：
     * 1. 自动创建默认的 Validator 实例，无需手动管理 ValidatorFactory
     * 2. 使用 Hutool 的 Assert 进行非空断言，提供更友好的异常提示
     * 3. 委托给重载方法，复用校验逻辑
     *
     * <p>使用场景：
     * 在 Service 层或工具类中手动触发对象校验，适用于无法使用 Spring 的 @Validated 注解的场景
     *
     * @param object 待校验的对象，示例值：UserCreateReqVO 实例
     * @param groups 校验分组，可选参数，用于分组校验（如 Create.class、Update.class）
     *               示例值：UserCreateReqVO.CreateGroup.class
     * @throws ConstraintViolationException 校验失败时抛出，包含所有校验错误信息
     * @throws IllegalArgumentException     Validator 创建失败时抛出（极少发生）
     */
    public static void validate(Object object, Class<?>... groups) {
        // Validation.buildDefaultValidatorFactory() 创建默认的验证器工厂
        // getValidator() 获取 Validator 实例
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        // Assert.notNull() 断言 validator 不为 null，否则抛出 IllegalArgumentException
        Assert.notNull(validator);
        // 委托给重载方法执行实际的校验逻辑
        validate(validator, object, groups);
    }

    /**
     * 使用指定的 Validator 校验对象属性是否符合 Bean Validation 注解约束
     *
     * <p>相比原生实现的增强：
     * 1. 使用 Hutool 的 CollUtil.isNotEmpty() 简化集合非空判断
     * 2. 自动抛出 ConstraintViolationException，无需手动处理校验结果
     * 3. 支持分组校验，提高校验灵活性
     *
     * <p>使用场景：
     * 当需要复用同一个 Validator 实例进行多次校验时使用此方法，避免重复创建 Validator 的性能开销
     *
     * @param validator 验证器实例，通常通过依赖注入或 Validation.buildDefaultValidatorFactory().getValidator() 获取
     * @param object    待校验的对象，示例值：UserUpdateReqVO 实例
     * @param groups    校验分组，可选参数，用于分组校验（如 Create.class、Update.class）
     *                  示例值：UserUpdateReqVO.UpdateGroup.class
     * @throws ConstraintViolationException 校验失败时抛出，包含所有校验错误信息
     *                                      可通过 exception.getConstraintViolations() 获取详细错误列表
     */
    public static void validate(Validator validator, Object object, Class<?>... groups) {
        // validator.validate() 执行校验，返回所有违反约束的集合
        // groups 为可变参数，可传入多个分组 Class 对象
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object, groups);
        // CollUtil.isNotEmpty() 判断集合不为 null 且不为空
        if (CollUtil.isNotEmpty(constraintViolations)) {
            // 将校验错误集合包装成 ConstraintViolationException 抛出
            // 调用方可以捕获此异常并解析其中的错误信息
            throw new ConstraintViolationException(constraintViolations);
        }
    }

}

