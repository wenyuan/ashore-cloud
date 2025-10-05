package com.example.ashore.framework.common.core;

/**
 * 可生成 T 数组的接口
 *
 * 设计目的：
 * 1. 为枚举类提供统一的数组值获取方式，主要用于参数校验
 * 2. 配合 {@link com.example.ashore.framework.common.validation.InEnum} 注解实现枚举值的自动校验
 *
 * 为什么需要这个接口：
 * Java 的 getEnumConstants() 只能获取枚举实例本身，没有直接方法获取所有枚举的某个字段值，
 * 但我们要验证的是字段的值（例如 [10, 20]），
 * 通过此接口可以让枚举统一暴露一个 array() 方法返回值数组，供校验器使用。
 *
 * 使用示例：
 * // 1. 枚举实现此接口
 * {@link com.example.ashore.framework.common.enums.TerminalEnum}
 *
 * // 2. 在 DTO 中使用 @InEnum 注解校验
 * public class LoginReqVO {
 *     @InEnum(TerminalEnum.class)  // 自动校验值是否在 [10, 20] 范围内
 *     private Integer terminal;
 * }
 *
 *
 * 主要使用场景：
 * - {@link com.example.ashore.framework.common.validation.InEnumValidator} - 单值校验器</li>
 * - {@link com.example.ashore.framework.common.validation.InEnumCollectionValidator} - 集合校验器</li>
 *
 * @param <T> 数组元素类型
 */
public interface ArrayValuable<T> {

    /**
     * 获取所有枚举值的数组
     *
     * @return 包含所有合法枚举值的数组，用于参数校验
     */
    T[] array();

}
