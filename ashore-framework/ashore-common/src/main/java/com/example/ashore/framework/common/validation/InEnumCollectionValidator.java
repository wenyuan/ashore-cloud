package com.example.ashore.framework.common.validation;

import cn.hutool.core.collection.CollUtil;
import com.example.ashore.framework.common.core.ArrayValuable;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * {@link InEnum} 注解的集合校验器实现，用于校验集合中的所有枚举值是否合法
 *
 * 设计目的：
 * 处理用户传入多个枚举值的场景（如批量操作、多选等），校验集合中的每个元素是否都在枚举的合法范围内
 *
 * 为什么需要这个校验器：
 * 1. 业务中经常需要批量操作，如批量查询订单状态、批量设置权限等
 * 2. 需要校验集合中的每个值都合法，而 {@link InEnumValidator} 只能校验单个值
 * 3. 通过专门的集合校验器，避免手动遍历集合进行校验
 *
 * 与 {@link InEnumValidator} 的区别：
 *   - {@link InEnumValidator}：校验单个值（如 Integer terminal）
 *   - {@link InEnumCollectionValidator}：校验集合（如 List&lt;Integer&gt; terminals）
 *   - 同一个 @InEnum 注解可以支持两种类型，框架会根据字段类型自动选择对应的校验器
 *
 * 使用示例：
 * // DTO 中使用
 * public class OrderQueryReqVO {
 *     @InEnum(OrderStatusEnum.class)   // 校验集合中的所有状态值都合法
 *     private List<Integer> statuses;  // 如: [10, 20, 30]
 * }
 *
 * // 合法请求: statuses = [10, 20, 30]（假设这些都是有效的订单状态）
 * // ✅ 校验通过
 *
 * // 非法请求: statuses = [10, 99, 30]（99 不是有效的订单状态）
 * // ❌ 校验失败，返回: "必须在指定范围 10,99,30"
 *
 * 相关类：
 * @see InEnum          - 对应的注解定义
 * @see InEnumValidator - 单值类型的枚举值校验器
 * @see ArrayValuable   - 枚举必须实现的接口
 */
public class InEnumCollectionValidator implements ConstraintValidator<InEnum, Collection<?>> {

    // 存储枚举的所有合法值
    private List<?> values;

    /**
     * 初始化校验器，提取枚举的所有合法值
     * 注意：此方法与 {@link InEnumValidator#initialize(InEnum)} 的实现完全相同，因为两者的初始化逻辑一致，都是提取枚举的合法值列表。
     *
     * @param annotation @InEnum 注解实例，包含要校验的枚举类信息
     */
    @Override
    public void initialize(InEnum annotation) {
        ArrayValuable<?>[] values = annotation.value().getEnumConstants();
        if (values.length == 0) {
            this.values = Collections.emptyList();
        } else {
            this.values = Arrays.asList(values[0].array());
        }
    }

    /**
     * 执行集合枚举值校验逻辑
     * 此方法在每次需要校验集合字段值时被 Bean Validation 框架自动调用
     * 判断用户输入的集合中的所有元素是否都在枚举的合法值范围内
     *
     * 校验规则：
     * 1. 空集合（null）默认通过校验 - 如需强制非空，需额外添加 @NotEmpty 注解
     * 2. 集合中的所有元素都在合法范围内则通过
     * 3. 集合中任意一个元素不在合法范围内则失败，并自定义错误消息
     *
     * 与 {@link InEnumValidator#isValid} 的区别：
     *   - 单值校验器：判断 {@code values.contains(value)}
     *   - 集合校验器：判断 {@code CollUtil.containsAll(values, list)}
     *   - 错误消息：集合校验器显示用户输入的所有值（用逗号分隔），而不是合法值列表
     *
     * @param list    待校验的集合（用户输入的值集合）
     * @param context 校验上下文，用于自定义错误消息
     * @return        true 表示校验通过，false 表示校验失败
     */
    @Override
    public boolean isValid(Collection<?> list, ConstraintValidatorContext context) {
        if (list == null) {
            return true;
        }
        // 判断合法值列表是否包含用户输入的所有值
        if (CollUtil.containsAll(values, list)) {
            return true;
        }
        // 校验不通过，自定义提示语句
        // 禁用默认的 message 的值
        context.disableDefaultConstraintViolation();
        // 重新添加错误提示语句（例如错误消息："必须在指定范围 10,99"）
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate()
                .replaceAll("\\{value}", CollUtil.join(list, ","))).addConstraintViolation();
        return false;
    }

}