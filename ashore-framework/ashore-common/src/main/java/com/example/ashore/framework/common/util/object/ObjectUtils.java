package com.example.ashore.framework.common.util.object;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * 对象工具类
 * 作用：提供对象的常用操作方法，包括对象克隆、比较、空值处理等功能
 *
 * 核心依赖库：
 * - Hutool - ObjectUtil：提供对象克隆功能
 * - Hutool - ReflectUtil：提供反射操作功能，用于字段访问和修改
 */
public class ObjectUtils {

    /**
     * 复制对象，并自动将 id 字段设置为 null（常用于实体对象的复制场景）
     *
     * 示例：
     * User newUser = ObjectUtils.cloneIgnoreId(user, u -> u.setName("李四"));
     *
     * @param object   被复制的源对象
     * @param consumer 消费者函数，用于在克隆后对新对象进行二次编辑，
     * @return         复制后的新对象，其 id 字段为 null
     * @param <T>      泛型类型参数，表示被克隆对象的类型
     */
    public static <T> T cloneIgnoreId(T object, Consumer<T> consumer) {
        // 使用 Hutool 的 ObjectUtil.clone() 方法进行深度克隆
        T result = ObjectUtil.clone(object);

        // 通过反射获取对象的 "id" 字段（Field 是 Java 反射 API 中表示类字段的类）
        Field field = ReflectUtil.getField(object.getClass(), "id");

        // 如果对象存在 id 字段，则将其设置为 null
        if (field != null) {
            // 使用反射设置字段值，第一个参数是目标对象，第二个参数是字段对象，第三个参数是要设置的值
            ReflectUtil.setFieldValue(result, field, null);
        }

        // 如果克隆成功且消费者不为 null，则执行消费者函数对克隆对象进行二次编辑
        if (result != null) {
            // Consumer.accept() 方法会执行传入的 Lambda 表达式或方法引用
            consumer.accept(result);
        }

        return result;
    }

    /**
     * 比较两个实现了 Comparable 接口的对象，返回较大的那个
     *
     * 使用场景：
     * - 需要从两个可能为空的对象中选择较大值
     * - 比较日期、数字等实现了 Comparable 接口的对象
     * 示例：
     * // 比较两个整数
     * Integer result1 = ObjectUtils.max(10, 20);      // 返回 20
     *
     * // 处理 null 值
     * Integer result2 = ObjectUtils.max(null, 20);    // 返回 20
     * Integer result3 = ObjectUtils.max(10, null);    // 返回 10
     * Integer result4 = ObjectUtils.max(null, null);  // 返回 null
     *
     * // 比较日期
     * LocalDate date1 = LocalDate.of(2023, 1, 1);
     * LocalDate date2 = LocalDate.of(2024, 1, 1);
     * LocalDate maxDate = ObjectUtils.max(date1, date2);  // 返回 2024-01-01
     *
     * @param obj1 第一个对象，可以为 null
     * @param obj2 第二个对象，可以为 null
     * @return     两个对象中较大的一个，如果其中一个为 null 则返回另一个，
     *             如果都为 null 则返回 null
     * @param <T>  泛型类型参数，必须实现 Comparable 接口，以支持对象间的比较
     */
    public static <T extends Comparable<T>> T max(T obj1, T obj2) {
        // 如果第一个对象为 null，直接返回第二个对象（可能也为 null）
        if (obj1 == null) {
            return obj2;
        }
        // 如果第二个对象为 null，直接返回第一个对象（此时第一个对象不为 null）
        if (obj2 == null) {
            return obj1;
        }
        // 两个对象都不为 null 时，使用 compareTo 方法比较
        // compareTo 返回值 > 0 表示 obj1 大于 obj2，使用三元运算符 ? : 选择较大值
        return obj1.compareTo(obj2) > 0 ? obj1 : obj2;
    }

    /**
     * 按顺序检查传入的多个对象，返回第一个不为 null 的对象（类似于提供默认值的功能）
     *
     * 使用场景：
     * - 配置项的多级默认值处理，例如：优先使用用户配置，其次使用系统配置，最后使用默认配置
     * - 数据回填时的优先级选择
     * - 需要从多个可能为 null 的数据源中选择第一个有效值
     *
     * 示例：
     * // 示例 1：选择第一个非 null 值
     * String result1 = ObjectUtils.defaultIfNull(null, "默认值", "备用值");
     * // 返回 "默认值"
     *
     * // 示例 2：所有值都为 null
     * String result2 = ObjectUtils.defaultIfNull(null, null, null);
     * // 返回 null
     *
     * // 示例 3：配置项优先级处理
     * String userConfig = null;
     * String systemConfig = "system.conf";
     * String defaultConfig = "default.conf";
     * String config = ObjectUtils.defaultIfNull(userConfig, systemConfig, defaultConfig);
     * // 返回 "system.conf"
     *
     * // 示例 4：第一个值不为 null
     * Integer result3 = ObjectUtils.defaultIfNull(10, 20, 30);
     * // 返回 10
     *
     * @param array 可变参数数组，包含需要检查的对象，
     * @return      数组中第一个非 null 的对象，如果所有对象都为 null 则返回 null，
     * @param <T>   泛型类型参数，表示对象的类型
     */
    @SafeVarargs  // 该注解表示这个可变参数方法不会对泛型数组参数进行不安全的操作
    public static <T> T defaultIfNull(T... array) {
        for (T item : array) {
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    /**
     * 检查给定对象是否与候选数组中的任意一个元素相等（类似于 SQL 中的 IN 操作）
     *
     * 使用场景：
     * - 判断状态码是否为多个有效值之一
     * - 检查用户角色是否在允许的角色列表中
     * - 验证枚举值是否在指定的枚举集合中
     * - 简化多个 || (或) 条件的判断
     *
     * 示例：
     * // 示例 1：判断状态码
     * Integer status = 200;
     * boolean isSuccess = ObjectUtils.equalsAny(status, 200, 201, 204);
     * // 返回 true
     *
     * // 示例 2：检查字符串
     * String role = "ADMIN";
     * boolean hasPermission = ObjectUtils.equalsAny(role, "ADMIN", "SUPER_ADMIN");
     * // 返回 true
     *
     * // 示例 3：对象不在候选列表中
     * String type = "DELETE";
     * boolean isModify = ObjectUtils.equalsAny(type, "INSERT", "UPDATE");
     * // 返回 false
     *
     * // 示例 4：处理 null 值
     * String value = null;
     * boolean result = ObjectUtils.equalsAny(value, "A", "B", null);
     * // 返回 true（因为候选值中有 null）
     *
     * @param obj   待检查的对象，可以为 null
     * @param array 候选值的可变参数数组
     * @return      如果对象等于数组中的任意一个元素返回 true，否则返回 false
     * @param <T>   泛型类型参数，表示对象的类型
     */
    @SafeVarargs
    public static <T> boolean equalsAny(T obj, T... array) {
        // Arrays.asList() 将数组转换为 List 集合
        // List.contains() 方法检查集合是否包含指定元素（使用 equals 方法比较）
        return Arrays.asList(array).contains(obj);
    }

    /**
     * 判断传入的多个对象中是否至少有一个非空（非 null 且非空字符串、空集合等）
     * 是 ObjectUtil.isAllEmpty() 的取反操作
     *
     * 使用场景：
     * - 表单验证时，判断多个字段是否至少填写了一个
     * - 检查查询条件是否至少提供了一个有效条件
     * - 验证多个可选参数中是否至少传入了一个有效值
     *
     * 示例：
     * // 示例 1：至少有一个非空值
     * boolean result1 = ObjectUtils.isNotAllEmpty(null, "", "有值");
     * // 返回 true（因为 "有值" 不为空）
     *
     * // 示例 2：全部为空
     * boolean result2 = ObjectUtils.isNotAllEmpty(null, "", new ArrayList<>());
     * // 返回 false（全部都为空）
     *
     * // 示例 3：检查查询条件
     * String name = null;
     * String email = "test@example.com";
     * String phone = "";
     * boolean hasCondition = ObjectUtils.isNotAllEmpty(name, email, phone);
     * // 返回 true（email 有值）
     *
     * // 示例 4：全部非空
     * boolean result3 = ObjectUtils.isNotAllEmpty("A", "B", "C");
     * // 返回 true
     *
     * 注意：此方法依赖 Hutool 的 ObjectUtil.isAllEmpty() 方法，
     * 该方法会判断以下情况为"空"：
     * - null 值
     * - 空字符串 ""
     * - 空集合（Collection）
     * - 空数组
     * - 空 Map
     *
     * @param objs 需要检查的多个对象，可变参数，
     * @return     如果至少有一个对象非空返回 true，如果全部为空返回 false
     */
    public static boolean isNotAllEmpty(Object... objs) {
        return !ObjectUtil.isAllEmpty(objs);
    }

}
