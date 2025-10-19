package com.example.ashore.framework.common.util.spring;

import cn.hutool.extra.spring.SpringUtil;

import java.util.Objects;

/**
 * Spring 工具类
 *
 * 继承自 Hutool 的 SpringUtil，扩展了额外功能
 */
public class SpringUtils extends SpringUtil {

    /**
     * 用于判断当前是否为生产环境
     *
     * @return 是否生产环境
     */
    public static boolean isProd() {
        String activeProfile = getActiveProfile();
        return Objects.equals("prod", activeProfile);
    }

}
