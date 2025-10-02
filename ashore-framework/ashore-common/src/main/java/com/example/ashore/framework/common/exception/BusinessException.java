package com.example.ashore.framework.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务逻辑异常 Exception
 */
@Data
@EqualsAndHashCode(callSuper = true) // 对象比较逻辑需要考虑父类字段比如message，从而正确判断是否两个异常是否相等
public final class BusinessException extends RuntimeException {

    /**
     * 业务错误码
     */
    private String code;

    /**
     * 空构造方法，避免反序列化问题
     */
    public BusinessException() {
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

}
