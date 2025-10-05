package com.example.ashore.framework.common.util.collection;

import cn.hutool.core.collection.CollUtil;

import java.util.Set;

/**
 * Set 集合工具类
 * 目的：
 * - 提供便捷的 Set 集合创建和操作方法
 * - 封装 Hutool 工具库的 Set 相关功能，统一项目中的 Set 操作入口
 */
public class SetUtils {

    /**
     * 将可变参数转换为 Set 集合（HashSet 实现）
     * 这是一个泛型方法，可以接受任意类型的元素，并将这些元素转换成一个 HashSet 集合
     *
     * 示例：
     * Set<String> names = SetUtils.asSet("张三", "李四", "王五");
     * Set<Integer> numbers = SetUtils.asSet(1, 2, 3, 4, 5);
     *
     * @param objs 可变参数，要放入 Set 中的元素。可以传入 0 个、1 个或多个元素
     * @return     返回一个包含所有传入元素的 HashSet 集合
     *             如果传入重复元素，Set 会自动去重
     *             如果不传入任何参数，返回一个空的 HashSet
     * @param <T>  泛型参数，表示集合中元素的类型（可以是任意类型，如 String、Integer、自定义对象等）
     */
    @SafeVarargs
    public static <T> Set<T> asSet(T... objs) {
        // 调用 Hutool 工具类的 newHashSet 方法创建 HashSet
        return CollUtil.newHashSet(objs);
    }

}
