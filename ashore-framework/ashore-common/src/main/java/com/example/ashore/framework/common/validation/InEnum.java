package com.example.ashore.framework.common.validation;

import com.example.ashore.framework.common.core.ArrayValuable;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 自定义的验证注解
 * 作用：检查字段的值是否在指定的枚举范围内
 *
 * 该注解的完整生态系统：
 * - @InEnum                   验证注解，标记需要验证的字段
 * - InEnumValidator           验证单个值的验证器
 * - InEnumCollectionValidator 验证集合的验证器
 * - ArrayValuable             枚举类必须实现的接口
 */
@Target({
        ElementType.METHOD,           // 可以加在方法上
        ElementType.FIELD,            // 可以加在字段上（最常用）
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.PARAMETER,        // 可以加在方法参数上
        ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)   // 在程序运行时才会检查（调用接口、提交表单时进行验证）
@Documented
@Constraint(
        validatedBy = {InEnumValidator.class, InEnumCollectionValidator.class}  // 指定两个验证器，Spring会自动根据字段类型选择合适的验证器
)
public @interface InEnum {

    Class<? extends ArrayValuable<?>> value();        // 必填：指定枚举类（必须是实现了 ArrayValuable 接口的枚举类）

    String message() default "必须在指定范围 {value}";   // 可选：错误提示

    Class<?>[] groups() default {};                   // 可选：分组验证

    Class<? extends Payload>[] payload() default {};  // 可选：元数据

}
