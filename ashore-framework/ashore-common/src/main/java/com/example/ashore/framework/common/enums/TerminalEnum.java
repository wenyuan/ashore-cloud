package com.example.ashore.framework.common.enums;

import com.example.ashore.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 客户端的枚举
 */
@RequiredArgsConstructor
@Getter
public enum TerminalEnum implements ArrayValuable<Integer> {

    UNKNOWN(0, "未知"), // 目的：在无法解析到 terminal 时，使用它
    WEB(1, "WEB 浏览器"),
    WECHAT_MINI_PROGRAM(10, "微信小程序"),
    WECHAT_WAP(11, "微信公众号"),
    H5(20, "H5 网页"),
    APP(31, "手机 App"),
    ;

    // 这是一个静态常量，所有枚举实例共享同一个数组对象
    public static final Integer[] ARRAYS = Arrays.stream(values()).map(TerminalEnum::getTerminal).toArray(Integer[]::new);

    /**
     * 终端
     */
    private final Integer terminal;
    /**
     * 终端名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;  // 所有枚举实例都返回同一个静态 ARRAYS
    }
}
