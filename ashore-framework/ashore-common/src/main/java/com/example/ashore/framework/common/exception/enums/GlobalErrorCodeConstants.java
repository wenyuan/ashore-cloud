package com.example.ashore.framework.common.exception.enums;

import com.example.ashore.framework.common.exception.ErrorCode;

/**
 * 全局错误码枚举
 * 区别于 HTTP 响应状态码，这属于业务层错误码
 * 错误码的定义参考 <a href="https://github.com/alibaba/p3c/tree/master">阿里巴巴Java开发手册(黄山版)</a>
 * - 五位字符串
 * - A 开头表示错误来源于用户
 * - B 开头表示错误来源于当前系统
 * - C 开头表示错误来源于第三方服务
 */
public interface GlobalErrorCodeConstants {

    ErrorCode SUCCESS = new ErrorCode("00000", "成功");

    // =============== 客户端错误段 ===============
    ErrorCode BAD_REQUEST = new ErrorCode("A0001", "用户端错误");
    ErrorCode REGISTER_ERROR = new ErrorCode("A0100", "用户注册错误");
    ErrorCode UNAUTHORIZED = new ErrorCode("A0200", "用户未登录");
    ErrorCode FORBIDDEN = new ErrorCode("A0300", "没有该操作权限");
    ErrorCode INVALID_PARAMS = new ErrorCode("A0400", "用户请求参数错误");
    ErrorCode NOT_FOUND = new ErrorCode("A0404", "请求未找到");
    ErrorCode METHOD_NOT_ALLOWED = new ErrorCode("A0405", "请求方法不正确");
    ErrorCode LOCKED = new ErrorCode("A0423", "请求失败，请稍后重试"); // 并发请求，不允许
    ErrorCode TOO_MANY_REQUESTS = new ErrorCode("A0429", "请求过于频繁，请稍后重试");
    ErrorCode REPEATED_REQUESTS = new ErrorCode("A0900", "重复请求，请稍后重试");
    ErrorCode DEMO_DENY = new ErrorCode("A0901", "演示模式，禁止写操作");// 重复请求

    // =============== 服务端错误段 ===============
    ErrorCode INTERNAL_SERVER_ERROR = new ErrorCode("B0001", "系统执行出错");
    ErrorCode NOT_IMPLEMENTED = new ErrorCode("B0501", "功能未实现/未开启");
    ErrorCode ERROR_CONFIGURATION = new ErrorCode("B0502", "错误的配置项");
    ErrorCode UNKNOWN = new ErrorCode("B0999", "未知错误");

    // =============== 第三方服务错误段 ===============
    ErrorCode THIRD_PARTY_ERROR = new ErrorCode("C0001", "调用第三方服务出错");

}
