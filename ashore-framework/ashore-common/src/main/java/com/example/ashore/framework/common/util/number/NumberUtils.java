package com.example.ashore.framework.common.util.number;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数字工具类
 * 提供数字类型转换、校验、计算等常用功能，补全和增强 Hutool 的 {@link cn.hutool.core.util.NumberUtil} 工具类
 * 主要解决空值处理、类型安全转换、批量数字校验等场景需求
 *
 * 使用场景：
 * - 字符串转数字时需要处理空值或 null 的情况
 * - 批量校验字符串列表是否都是有效数字
 * - 地理位置计算（如计算两个坐标点之间的距离）
 * - BigDecimal 精确计算时需要处理 null 值
 *
 * 核心依赖库：
 * - Hutool（cn.hutool.core）：提供基础的数字工具和字符串工具
 * - JDK BigDecimal：提供高精度数字计算
 */
public class NumberUtils {

    /**
     * 安全地将字符串转换为 Long 类型，支持空值处理。
     * 与 {@link Long#valueOf(String)} 的区别是，当字符串为空或 null 时返回 null 而不是抛出异常。
     *
     * @param str 待转换的字符串
     * @return    Long 对象或 null
     */
    public static Long parseLong(String str) {
        return StrUtil.isNotEmpty(str) ? Long.valueOf(str) : null;
    }

    /**
     * 安全地将字符串解析为 Integer 类型，支持空值处理。
     * 与 {@link Integer#valueOf(String)} 的区别是，当字符串为空或 null 时返回 null 而不是抛出异常。
     *
     * @param str 待转换的字符串
     * @return    Integer 对象或 null
     */
    public static Integer parseInt(String str) {
        return StrUtil.isNotEmpty(str) ? Integer.valueOf(str) : null;
    }

    /**
     * 校验字符串列表中的所有元素是否都是有效数字
     *
     * @param values 待校验的字符串列表
     * @return       true-所有元素都是有效数字
     *               false-列表为空、null 或包含非数字元素
     */
    public static boolean isAllNumber(List<String> values) {
        // 判断集合是否为空或 null
        if (CollUtil.isEmpty(values)) {
            return false;
        }
        for (String value : values) {
            // 使用 Hutool 的 isNumber 方法判断是否为数字
            if (!NumberUtil.isNumber(value)) {
                // 只要有一个不是数字，立即返回 false
                return false;
            }
        }
        // 所有元素都是数字，返回 true
        return true;
    }

    /**
     * 通过经纬度获取地球上两点之间的距离
     *
     * 算法说明：
     * 使用 Haversine 公式（半正矢公式）计算地球表面两个坐标点之间的大圆距离。
     * 该公式通过两点的经纬度，计算它们在球面上的最短距离（大圆距离）。
     * 该公式基于球面三角学，考虑了地球的曲率（即地球表面是弯曲的而非平面）。
     * 该公式假设地球是一个完美的球体，适用于大多数地理位置距离计算场景。
     *
     * 参考 <<a href="https://gitee.com/dromara/hutool/blob/1caabb586b1f95aec66a21d039c5695df5e0f4c1/hutool-core/src/main/java/cn/hutool/core/util/DistanceUtil.java">DistanceUtil</a>> 实现，目前它已经被 hutool 删除
     *
     * @param lat1 第一个点的纬度（不是经度），取值范围 -90° 到 90°，示例值：39.9042（北京天安门纬度）
     * @param lng1 第一个点的经度，取值范围 -180° 到 180°，示例值：116.4074（北京天安门经度）
     * @param lat2 第二个点的纬度（不是经度），取值范围 -90° 到 90°，示例值：31.2304（上海外滩纬度）
     * @param lng2 第二个点的经度，取值范围 -180° 到 180°，示例值：121.4737（上海外滩经度）
     * @return     两点之间的距离，单位：千米（km），保留 4 位小数，示例值：1067.7856（北京到上海约 1067.79 千米）
     */
    public static double getDistance(double lat1, double lng1, double lat2, double lng2) {
        // 将纬度从角度转换为弧度（弧度 = 角度 × π / 180）
        // 因为三角函数使用的是弧度制
        double radLat1 = lat1 * Math.PI / 180.0;
        double radLat2 = lat2 * Math.PI / 180.0;

        // 计算两点纬度差（弧度）
        double a = radLat1 - radLat2;

        // 计算两点经度差（弧度）
        double b = lng1 * Math.PI / 180.0 - lng2 * Math.PI / 180.0;

        // Haversine 公式核心计算
        // 公式：2 × arcsin(√(sin²(Δlat/2) + cos(lat1) × cos(lat2) × sin²(Δlng/2)))
        // 这个公式计算的是两点之间的圆心角（弧度）
        double distance = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));

        // 将圆心角乘以地球半径，得到实际距离
        // 6378.137 是地球平均半径，单位：千米
        distance = distance * 6378.137;

        // 四舍五入保留 4 位小数
        // 先乘以 10000 再四舍五入，最后除以 10000
        distance = Math.round(distance * 10000d) / 10000d;
        return distance;
    }

    /**
     * 提供精确的 BigDecimal 乘法运算（支持 null 值处理）
     * 与 Hutool 的 {@link NumberUtil#mul(BigDecimal...)} 的区别是：
     * 如果参数中存在任何一个 null 值，则直接返回 null 而不是抛出异常。
     *
     * 使用场景：
     * 适用于金额计算、财务统计等需要高精度且可能存在 null 值的场景。
     * 例如：计算订单总金额 = 单价 × 数量 × 折扣率，其中折扣率可能为 null。
     *
     * @param values 多个被乘数，可变参数数组，示例值：
     *                                    - BigDecimal.valueOf(100), BigDecimal.valueOf(0.9) → 结果 90.0
     *                                    - BigDecimal.valueOf(50), null → 结果 null
     *                                    - BigDecimal.valueOf(10), BigDecimal.valueOf(2), BigDecimal.valueOf(3) → 结果 60
     * @return       所有数值的乘积
     *               如果任意一个参数为 null 则返回 null
     */
    public static BigDecimal mul(BigDecimal... values) {
        // 使用增强 for 循环遍历可变参数数组
        // values 是一个 BigDecimal 数组，可以传入任意个参数
        for (BigDecimal value : values) {
            if (value == null) {
                // 只要有一个 null，立即返回 null（空值安全处理）
                return null;
            }
        }
        // 所有参数都不为 null，调用 Hutool 的 mul 方法进行实际的乘法计算
        // NumberUtil.mul 会使用 BigDecimal 的 multiply 方法进行精确计算
        return NumberUtil.mul(values);
    }

}
