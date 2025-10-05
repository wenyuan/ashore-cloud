package com.example.ashore.framework.common.util.collection;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.util.ArrayUtil;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.example.ashore.framework.common.util.collection.CollectionUtils.convertList;

/**
 * Array (数组) 工具类
 *
 * 目的：
 * - 提供数组的常用操作方法，如合并、转换、安全访问等
 * - 封装 Hutool 工具库的数组相关功能
 * - 简化数组操作，避免空指针异常和数组越界异常
 * - 提供与 Collection 集合之间的转换能力
 */
public class ArrayUtils {

    /**
     * 将单个对象和多个元素合并成一个新数组
     *
     * 方法说明：
     * 这个方法主要用于函数式编程场景，将一个 Consumer 和多个 Consumer 合并成数组。
     * Consumer 是 Java 函数式接口，表示一个接受单个输入参数但不返回结果的操作（消费者）。
     *
     * 示例：
     * Consumer<String> logger1 = str -> System.out.println("Log1: " + str);
     * Consumer<String> logger2 = str -> System.out.println("Log2: " + str);
     * Consumer<String> logger3 = str -> System.out.println("Log3: " + str);
     * Consumer<String>[] allLoggers = ArrayUtils.append(logger1, logger2, logger3);
     * // 可以遍历这个数组，依次执行所有的日志记录操作
     *
     * @param object      要添加到数组开头的单个对象（可以为 null）
     * @param newElements 可变参数，要追加的其他元素
     * @return            返回合并后的新数组
     *                    如果 object 为 null，直接返回 newElements
     *                    否则返回一个新数组，第一个元素是 object，后续是 newElements 的所有元素
     * @param <T>         泛型参数，表示 Consumer 处理的数据类型
     */
    @SafeVarargs
    public static <T> Consumer<T>[] append(Consumer<T> object, Consumer<T>... newElements) {
        if (object == null) {
            return newElements;
        }

        // 创建一个新数组，长度 = 1（object） + newElements 的长度
        // ArrayUtil.newArray() 是 Hutool 提供的方法，用于创建指定类型和长度的数组
        // Consumer.class 指定数组元素的类型
        Consumer<T>[] result = ArrayUtil.newArray(Consumer.class, 1 + newElements.length);

        // 将第一个对象放在新数组的索引 0 位置
        result[0] = object;

        // System.arraycopy() 是 Java 原生的数组复制方法，性能很高
        // 参数说明：
        //   - newElements: 源数组
        //   - 0: 从源数组的索引 0 开始复制
        //   - result: 目标数组
        //   - 1: 复制到目标数组的索引 1 位置（因为索引 0 已经放了 object）
        //   - newElements.length: 要复制的元素个数
        System.arraycopy(newElements, 0, result, 1, newElements.length);

        return result;
    }

    /**
     * 将集合转换为数组（带类型转换功能）
     *
     * 方法说明：
     * 这个方法支持在转换过程中对元素进行类型转换。
     * 比如将 List<User> 转换为 String[] （每个 User 转换为 name 字符串）。
     *
     * 示例：
     * List<User> users = Arrays.asList(user1, user2, user3);
     * // 将 User 列表转换为用户名数组
     * String[] names = ArrayUtils.toArray(users, User::getName);
     *
     * @param from   源集合
     * @param mapper 转换函数，定义如何将 T 类型转换为 V 类型
     *               Function<T, V> 是函数式接口，接收 T 类型参数，返回 V 类型结果
     * @return       转换后的数组
     * @param <T>    源集合中元素的类型
     * @param <V>    目标数组中元素的类型
     */
    public static <T, V> V[] toArray(Collection<T> from, Function<T, V> mapper) {
        // 先将集合中的每个元素通过 mapper 转换为新类型的 List
        // 再调用重载的 toArray() 方法将转换后的集合转为数组
        return toArray(convertList(from, mapper));
    }

    /**
     * 将集合转换为数组（不改变元素类型）
     *
     * 方法说明：
     * 这是一个直接转换方法，不改变元素类型，只是改变数据结构（从集合到数组）。
     *
     * 示例：
     * List<String> list = Arrays.asList("a", "b", "c");
     * String[] array = ArrayUtils.toArray(list);
     *
     * @param from 源集合
     * @return     转换后的数组
     *             如果集合为空，返回空数组 new Object[0]
     *             否则返回包含所有元素的数组
     * @param <T>  元素类型
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Collection<T> from) {
        // 会同时检查 null 和 size() == 0
        if (CollectionUtil.isEmpty(from)) {
            // 返回空的 Object 数组，强制转换为 T[]
            // 这里是安全的，因为空数组不涉及实际的类型操作
            return (T[]) (new Object[0]);
        }

        // 将集合转为数组
        // 参数说明：
        //   - from: 源集合
        //   - (Class<T>) IterUtil.getElementType(from.iterator()): 元素的 Class 类型
        //     IterUtil.getElementType() 会从迭代器中推断出元素的实际类型
        //     这样可以创建正确类型的数组，而不是 Object[]
        return ArrayUtil.toArray(from, (Class<T>) IterUtil.getElementType(from.iterator()));
    }

    /**
     * 安全地获取数组中指定索引位置的元素
     *
     * 方法说明：
     * 这是一个安全的数组访问方法，避免了数组越界异常（ArrayIndexOutOfBoundsException）。
     * 如果索引越界或数组为 null，返回 null 而不是抛出异常。
     *
     * 与直接访问的区别：
     * - 直接访问：array[index]                 - 如果越界会抛出异常，程序崩溃
     * - 安全访问：ArrayUtils.get(array, index) - 越界返回 null，程序继续运行
     *
     * 使用场景示例：
     * String[] names = {"Alice", "Bob", "Charlie"};
     * String name1 = ArrayUtils.get(names, 1);  // 返回 "Bob"
     * String name5 = ArrayUtils.get(names, 5);  // 返回 null（而不是抛异常）
     * String[] nullArray = null;
     * String name = ArrayUtils.get(nullArray, 0); // 返回 null（而不是抛 NullPointerException）
     *
     * @param array 数组（可以为 null）
     * @param index 要获取的索引位置
     * @return      返回该位置的元素
     *              如果数组为 null，返回 null
     *              如果索引 >= 数组长度（越界），返回 null
     *              否则返回 array[index]
     * @param <T>   数组元素类型
     */
    public static <T> T get(T[] array, int index) {
        // 安全性检查：
        // 1. array == null：数组本身为 null
        // 2. index >= array.length：索引超出数组范围
        // 注意：这里没有检查 index < 0 的情况，负数索引仍会抛出异常
        if (null == array || index >= array.length) {
            return null;
        }

        // 通过了安全检查，直接访问数组
        return array[index];
    }

}
