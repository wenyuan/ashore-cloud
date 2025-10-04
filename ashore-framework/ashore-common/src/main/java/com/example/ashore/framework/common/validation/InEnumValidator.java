package com.example.ashore.framework.common.validation;

import com.example.ashore.framework.common.core.ArrayValuable;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link InEnum} 注解的校验器实现，用于校验单个枚举值是否合法
 *
 * 设计目的：
 * 提供声明式的枚举值校验能力，避免在业务代码中手动编写枚举值校验逻辑
 *
 * 为什么需要这个校验器：
 * 1. 前端传入的枚举值（如终端类型、用户状态等）需要在进入业务层前进行校验
 * 2. 手动校验会导致大量重复的 if-else 代码，且容易遗漏
 * 3. 通过注解 + 校验器的方式，可以在参数绑定阶段自动完成校验，代码更简洁
 *
 * 工作原理：
 * 1. 通过 {@link InEnum#value()} 获取枚举类（必须实现 {@link ArrayValuable} 接口）
 * 2. 调用枚举的 {@link ArrayValuable#array()} 方法获取所有合法值的数组
 * 3. 校验时判断用户输入的值是否在该数组中
 * 4. 如果不在，自定义错误提示信息，将 {value} 替换为合法值列表
 *
 * 使用示例：
 * // DTO 中使用
 * public class AppAuthLoginReqVO {
 *     @InEnum(TerminalEnum.class)  // 声明式校验，值必须在 [0,10,11,20,31] 中
 *     private Integer terminal;
 * }
 *
 * // 当用户传入 terminal=99 时，自动返回错误：
 * // "必须在指定范围 [0, 10, 11, 20, 31]"
 *
 * 相关类：
 *   - {@link InEnum} - 对应的注解定义
 *   - {@link InEnumCollectionValidator} - 集合类型的枚举值校验器
 *   - {@link ArrayValuable} - 枚举必须实现的接口
 *
 * @see InEnum
 * @see ArrayValuable
 * @see InEnumCollectionValidator
 */
public class InEnumValidator implements ConstraintValidator<InEnum, Object> {

    // 存储枚举的所有合法值
    private List<?> values;

    /**
     * 初始化校验器，提取枚举的所有合法值
     * 此方法在校验器实例化后、第一次调用 {@link #isValid} 之前被 Bean Validation 框架自动调用
     * 主要作用是从注解中获取枚举类，并提取所有合法的枚举值存储到 {@link #values} 字段中
     * Tips：所有枚举实例的 array() 返回同一个静态 ARRAYS{@link #TerminalEnum#ARRAYS}，取第一个即可
     *
     * @param annotation @InEnum 注解实例，包含要校验的枚举类信息
     */
    @Override
    public void initialize(InEnum annotation) {
        // 获取 @InEnum 注解指定的枚举类（如 TerminalEnum.class），然后获取该枚举的所有枚举常量
        ArrayValuable<?>[] values = annotation.value().getEnumConstants();
        if (values.length == 0) {
            this.values = Collections.emptyList();
        } else {
            // 通过调用枚举实例的 array() 方法获取业务值数组，转为 List 存储
            this.values = Arrays.asList(values[0].array());
        }
    }

    /**
     * 执行枚举值校验逻辑
     * 此方法在每次需要校验字段值时被 Bean Validation 框架自动调用
     * 判断用户输入的值是否在枚举的合法值范围内
     *
     * 校验规则：
     * 1. 空值（null）默认通过校验 - 如需强制非空，需额外添加 @NotNull 注解
     * 2. 值在合法范围内则通过
     * 3. 值不在合法范围内则失败，并自定义错误消息
     *
     * @param value   待校验的字段值（用户输入的值）
     * @param context 校验上下文，用于自定义错误消息
     * @return        true 表示校验通过，false 表示校验失败
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // 为空时，默认不校验，即认为通过
        if (value == null) {
            return true;
        }
        // 校验通过
        if (values.contains(value)) {
            return true;
        }
        // 校验不通过，自定义提示语句
        // 禁用默认的 message 的值（避免显示原始的 "{value}" 占位符）
        context.disableDefaultConstraintViolation();
        // 重新添加错误提示语句（例如错误消息："必须在指定范围 [0, 10, 11, 20, 31]"）
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate()
                .replaceAll("\\{value}", values.toString())).addConstraintViolation();
        return false;
    }

}
