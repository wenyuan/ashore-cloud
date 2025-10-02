package com.example.ashore.framework.common.exception;

import com.example.ashore.framework.common.exception.enums.GlobalErrorCodeConstants;
import lombok.Data;

/**
 * 错误码对象
 * 错误码设计成对象的原因:
 *   - 统一管理和复用，省的每个调用处手动传入 code 和 message(主要)
 *   - 国际化扩展(次要)
 * 全局错误码 参见 {@link GlobalErrorCodeConstants}
 */
@Data
public class ErrorCode {

    /**
     * 错误码
     */
    private final String code;
    /**
     * 错误提示
     */
    private final String msg;

    public ErrorCode(String code, String message) {
        this.code = code;
        this.msg = message;
    }

}