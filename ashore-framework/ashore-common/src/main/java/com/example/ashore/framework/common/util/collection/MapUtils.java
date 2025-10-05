package com.example.ashore.framework.common.util.collection;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import com.example.ashore.framework.common.core.KeyValue;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Map (映射) 工具类
 *
 * 目的：
 * - 提供 Map 和 Multimap 的便捷操作方法
 * - 简化 Map 的数据提取和处理逻辑
 * - 封装 Hutool、Guava 等工具库的 Map 相关功能
 * - 提供类型安全的泛型方法，避免类型转换错误
 *
 * 核心功能：
 * - getList: 从 Multimap 中批量获取多个 key 对应的所有 value
 * - findAndThen: 查找 Map 中的 value 并执行后续操作（回调模式）
 * - convertMap: 将 KeyValue 列表转换为 Map
 */
public class MapUtils {

    /**
     * 从 Multimap 中批量获取多个 key 对应的所有 value
     *
     * 方法说明：
     * Multimap 是 Guava 提供的一对多映射数据结构，一个 key 可以对应多个 value。- 常用于分组、分类等场景
     * 本方法可以一次性传入多个 key，获取这些 key 对应的所有 value，并合并到一个 List 中。
     *
     * 使用场景示例：
     * // 假设有一个学生-课程的映射
     * Multimap<Long, String> studentCourses = ArrayListMultimap.create();
     * studentCourses.put(1L, "数学");
     * studentCourses.put(1L, "英语");
     * studentCourses.put(2L, "物理");
     * studentCourses.put(2L, "化学");
     *
     * // 获取学生 1 和学生 2 的所有课程
     * List<Long> studentIds = Arrays.asList(1L, 2L);
     * List<String> courses = MapUtils.getList(studentCourses, studentIds);
     * // 结果：["数学", "英语", "物理", "化学"]
     *
     * @param multimap Multimap 映射表（一对多的映射关系）
     * @param keys     要查询的 key 集合
     * @return         返回所有 key 对应的 value 的合并列表
     *                 如果某个 key 没有对应的 value，则跳过该 key
     *                 如果所有 key 都没有 value，返回空列表
     * @param <K>      Key 的类型
     * @param <V>      Value 的类型
     */
    public static <K, V> List<V> getList(Multimap<K, V> multimap, Collection<K> keys) {
        // 创建结果列表
        List<V> result = new ArrayList<>();

        // 遍历每个 key
        keys.forEach(k -> {
            // 从 Multimap 中获取该 key 对应的所有 value
            // Multimap.get(key) 返回一个 Collection<V>
            Collection<V> values = multimap.get(k);

            // 如果该 key 没有对应的 value（或 value 集合为空），则跳过
            if (CollectionUtil.isEmpty(values)) {
                // return 在 Lambda 表达式中相当于 continue，跳过本次循环
                return;
            }

            // 将该 key 的所有 value 添加到结果列表中
            result.addAll(values);
        });

        return result;
    }

    /**
     * 从 Map 中查找 key 对应的 value，如果找到则执行指定的操作
     *
     * 方法说明：
     * 这是一个"查找并执行"的工具方法，结合了查找和回调两个功能。
     * 它会先从 Map 中查找 key 对应的 value，如果找到且不为 null，则执行传入的 Consumer 操作。
     *
     * 适用场景：
     * - 当你需要从 Map 中获取值，并且只在值存在时才执行某些操作
     * - 避免重复的 null 检查代码
     * - 函数式编程风格的 Map 操作
     *
     * 示例：
     * Map<Long, User> userMap = ...; // 用户ID -> 用户对象的映射
     * Long userId = 100L;
     *
     * // 传统写法（繁琐）：
     * User user = userMap.get(userId);
     * if (user != null) {
     *     System.out.println(user.getName());
     *     user.setLastLoginTime(new Date());
     * }
     *
     * // 使用本方法（简洁）：
     * MapUtils.findAndThen(userMap, userId, user -> {
     *     System.out.println(user.getName());
     *     user.setLastLoginTime(new Date());
     * });
     *
     * @param map      要查找的 Map
     * @param key      要查找的 key（可以为 null）
     * @param consumer 如果找到 value，要执行的操作（Consumer 是函数式接口）
     *                 Consumer<V> 表示接收一个 V 类型的参数，不返回任何结果
     * @param <K>      Key 的类型
     * @param <V>      Value 的类型
     *
     * 方法行为：
     * - 如果 key 为 null，不执行任何操作，直接返回
     * - 如果 map 为空，不执行任何操作，直接返回
     * - 如果 map 中不存在该 key，不执行任何操作，直接返回
     * - 如果 value 为 null，不执行任何操作，直接返回
     * - 只有当 value 存在且不为 null 时，才执行 consumer 操作
     *
     * Consumer 函数式接口说明：
     * - Consumer<T> 是 Java 8 引入的函数式接口
     * - 它表示一个"消费者"，接收一个参数，执行某些操作，但不返回结果
     * - 常用于遍历、处理数据等场景
     * - 可以用 Lambda 表达式表示，如：user -> System.out.println(user)
     */
    public static <K, V> void findAndThen(Map<K, V> map, K key, Consumer<V> consumer) {
        // 安全性检查1：
        // ObjUtil.isNull() 是 Hutool 提供的工具方法，检查对象是否为 null
        // CollUtil.isEmpty() 检查集合是否为空（null 或 size == 0）
        if (ObjUtil.isNull(key) || CollUtil.isEmpty(map)) {
            return;
        }

        // 从 Map 中获取 key 对应的 value
        V value = map.get(key);

        // 安全性检查2：
        // 如果 value 为 null，直接返回
        // 这样可以避免传递 null 给 consumer，防止 NullPointerException
        if (value == null) {
            return;
        }

        // 通过了所有检查，执行 consumer 操作
        // consumer.accept(value) 会执行传入的 Lambda 表达式或方法引用
        consumer.accept(value);
    }

    /**
     * 将 KeyValue 列表转换为 Map
     *
     * 方法说明：
     * KeyValue 是本项目中定义的键值对类（see {@link com.example.ashore.framework.common.core.KeyValue}）。
     * 本方法将 KeyValue 对象的列表转换为标准的 Java Map。
     *
     * 使用场景示例：
     * // 假设有一个 KeyValue 列表
     * List<KeyValue<String, Integer>> kvList = Arrays.asList(
     *     new KeyValue<>("apple", 5),
     *     new KeyValue<>("banana", 3),
     *     new KeyValue<>("orange", 7)
     * );
     *
     * // 转换为 Map
     * Map<String, Integer> map = MapUtils.convertMap(kvList);
     * // 结果：{"apple": 5, "banana": 3, "orange": 7}
     *
     * @param keyValues KeyValue 对象的列表
     * @return          返回转换后的 LinkedHashMap（保持插入顺序的 Map）
     * @param <K>       Key 的类型
     * @param <V>       Value 的类型
     */
    public static <K, V> Map<K, V> convertMap(List<KeyValue<K, V>> keyValues) {
        // 使用 Guava 的 Maps 工具类创建 LinkedHashMap
        // 预分配容量为 keyValues.size()，避免多次扩容，提高性能
        Map<K, V> map = Maps.newLinkedHashMapWithExpectedSize(keyValues.size());

        // 遍历 KeyValue 列表，将每个键值对放入 Map
        keyValues.forEach(keyValue ->
                map.put(keyValue.getKey(), keyValue.getValue())
        );

        return map;
    }

}
