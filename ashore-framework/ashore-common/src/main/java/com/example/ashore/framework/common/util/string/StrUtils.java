package com.example.ashore.framework.common.util.string;

import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import org.aspectj.lang.JoinPoint;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字符串工具类
 *
 * 基于 Hutool 库的二次封装
 */
public class StrUtils {

    /**
     * 字符串最大长度截取
     *
     * 二次封装原因：
     * 1. Hutool 的 maxLength 方法会在截取后自动补充 "...",占用 3 个字符
     * 2. 调用方传入的 maxLength 是期望的最终长度,需要提前减去 3 避免超长
     * 3. 简化调用方逻辑,无需手动计算省略号长度
     *
     * @param str       待截取字符串
     * @param maxLength 最大长度(包含省略号)
     * @return 截取后的字符串
     */
    public static String maxLength(CharSequence str, int maxLength) {
        return StrUtil.maxLength(str, maxLength - 3); // -3 的原因，是该方法会补充 ... 恰好
    }

    /**
     * 给定字符串是否以任何一个字符串开始
     * 给定字符串和数组为空都返回 false
     *
     * 二次封装原因：
     * 1. Hutool 的 startWithAny 方法只支持数组参数,不支持 Collection
     * 2. 项目中很多场景使用 List/Set 等集合类型,直接传入更便捷
     * 3. 避免调用方频繁进行集合与数组的转换
     *
     * @param str      给定字符串
     * @param prefixes 需要检测的开始字符串集合
     * @return 是否以任一前缀开头
     * @since 3.0.6
     */
    public static boolean startWithAny(String str, Collection<String> prefixes) {
        if (StrUtil.isEmpty(str) || ArrayUtil.isEmpty(prefixes)) {
            return false;
        }

        for (CharSequence suffix : prefixes) {
            if (StrUtil.startWith(str, suffix, false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 分割字符串转为 Long 列表
     *
     * 二次封装原因：
     * 1. Hutool 的 splitToLong 返回基本类型数组 long[],不便于后续流式操作
     * 2. 项目中更常用 List<Long> 集合类型,支持 null 值且 API 更丰富
     * 3. 统一返回类型风格,与其他业务方法保持一致
     *
     * @param value     待分割字符串
     * @param separator 分隔符
     * @return Long 列表
     */
    public static List<Long> splitToLong(String value, CharSequence separator) {
        long[] longs = StrUtil.splitToLong(value, separator);
        return Arrays.stream(longs).boxed().collect(Collectors.toList());
    }

    /**
     * 分割字符串转为 Long 集合(默认逗号分隔)
     *
     * 二次封装原因：
     * 1. 提供默认分隔符(逗号),简化常见场景的调用
     * 2. 返回 Set 自动去重,适用于 ID 列表等需要唯一性的场景
     *
     * @param value 待分割字符串
     * @return Long 集合(去重)
     */
    public static Set<Long> splitToLongSet(String value) {
        return splitToLongSet(value, StrPool.COMMA);
    }

    /**
     * 分割字符串转为 Long 集合
     *
     * 二次封装原因：
     * 1. Hutool 的 splitToLong 返回基本类型数组 long[]
     * 2. 使用 Set 自动去重,避免重复 ID
     * 3. 包装类型更适合集合操作和业务传递
     *
     * @param value     待分割字符串
     * @param separator 分隔符
     * @return Long 集合(去重)
     */
    public static Set<Long> splitToLongSet(String value, CharSequence separator) {
        long[] longs = StrUtil.splitToLong(value, separator);
        return Arrays.stream(longs).boxed().collect(Collectors.toSet());
    }

    /**
     * 分割字符串转为 Integer 列表
     *
     * 二次封装原因：
     * 1. Hutool 的 splitToInt 返回基本类型数组 int[]
     * 2. List<Integer> 更符合项目开发习惯,便于传参和后续处理
     * 3. 与 splitToLong 方法保持 API 风格统一
     *
     * @param value     待分割字符串
     * @param separator 分隔符
     * @return Integer 列表
     */
    public static List<Integer> splitToInteger(String value, CharSequence separator) {
        int[] integers = StrUtil.splitToInt(value, separator);
        return Arrays.stream(integers).boxed().collect(Collectors.toList());
    }

    /**
     * 移除字符串中，包含指定字符串的行
     *
     * 二次封装原因：
     * 1. Hutool 未提供按行过滤的工具方法
     * 2. 项目中有日志处理、代码生成等场景需要移除特定行
     * 3. 封装统一实现,避免各模块重复编写相同逻辑
     *
     * @param content  多行字符串
     * @param sequence 要匹配的字符串(包含该字符串的行将被移除)
     * @return 移除后的字符串
     */
    public static String removeLineContains(String content, String sequence) {
        if (StrUtil.isEmpty(content) || StrUtil.isEmpty(sequence)) {
            return content;
        }
        return Arrays.stream(content.split("\n"))
                .filter(line -> !line.contains(sequence))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 拼接 AOP 方法的参数
     *
     * 二次封装原因：
     * 1. AOP 日志场景中,直接序列化参数可能导致循环引用或序列化异常
     * 2. ServletRequest/ServletResponse 等对象体积大且无法序列化,需要过滤
     * 3. 封装统一的参数拼接逻辑,避免各 AOP 切面重复实现
     * 4. 支持项目从 javax 到 jakarta 的平滑迁移
     *
     * 特殊处理：排除无法序列化的参数，如 ServletRequest、ServletResponse、MultipartFile
     *
     * @param joinPoint AOP 连接点
     * @return 拼接后的参数字符串(逗号分隔)
     */
    public static String joinMethodArgs(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (ArrayUtil.isEmpty(args)) {
            return "";
        }
        return ArrayUtil.join(args, ",", item -> {
            if (item == null) {
                return "";
            }
            String clazzName = item.getClass().getName();
            if (StrUtil.startWithAny(clazzName, "javax.servlet", "jakarta.servlet", "org.springframework.web")) {
                return "";
            }
            return item;
        });
    }

}
