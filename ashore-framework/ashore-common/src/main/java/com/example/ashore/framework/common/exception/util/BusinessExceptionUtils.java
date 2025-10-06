package com.example.ashore.framework.common.exception.util;

import com.example.ashore.framework.common.exception.ErrorCode;
import com.example.ashore.framework.common.exception.BusinessException;
import com.example.ashore.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link BusinessException} 工具类
 *
 * 使用场景：抛异常时动态拼接消息，返回错误结果时格式化消息
 * 核心目的：安全的错误消息格式化：使用 {} 作为占位符，并使用 {@link #doFormat(String, String, Object...)} 方法来格式化
 *   1. 替代 String.format 的不安全性
 *     - String.format("%s", 1, 2) 会抛异常(参数过多)
 *     - String.format("%s %s", 1) 会抛异常(参数过少)
 *     - doFormat 方法遇到参数不匹配只记录日志，不会崩溃
 *   2. 使用 {} 占位符更简洁
 *     - SLF4J 风格，比 %s 更简洁
 *     - 不需要区分 %s、%d、%f 等类型
 *
 */
@Slf4j
public class BusinessExceptionUtils {

    // =============== 和 BusinessException 的集成 ===============
    public static BusinessException exception(ErrorCode errorCode) {
        return formatException(errorCode.getCode(), errorCode.getMsg());
    }

    public static BusinessException exception(ErrorCode errorCode, Object... params) {
        return formatException(errorCode.getCode(), errorCode.getMsg(), params);
    }

    public static BusinessException formatException(String code, String messagePattern, Object... params) {
        String message = doFormat(code, messagePattern, params);
        return new BusinessException(code, message);
    }

    // 这个异常肯定会多处调用，直接单独写一个
    public static BusinessException invalidParamException(String messagePattern, Object... params) {
        return formatException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), messagePattern, params);
    }

    // =============== 消息格式化方法 ===============
    /**
     * 将错误编号对应的消息使用 params 进行格式化。
     *
     * @param code           错误编号
     * @param messagePattern 消息模版，使用 {} 作为占位符
     * @param params         参数列表，用于填充占位符
     * @return 格式化后的提示
     */
    @VisibleForTesting // 正常应该是 private，我设为 public 是为了测试，不是为了业务调用
    public static String doFormat(String code, String messagePattern, Object... params) {
        if (messagePattern == null || messagePattern.isEmpty()) {
            return "";
        }
        if (params == null || params.length == 0) {
            return messagePattern;
        }

        StringBuilder sb = new StringBuilder(messagePattern.length() + 50);
        int scanIdx  = 0; // 当前扫描位置
        int phIdx;        // 当前 {} 占位符的位置
        for (int paramIdx = 0; paramIdx < params.length; paramIdx++) {  // 遍历每一个参数
            phIdx = messagePattern.indexOf("{}", scanIdx);       // 查找下一个 {} 占位符
            if (phIdx == -1) {
                log.error("[doFormat][参数过多：错误码({})|错误内容({})|参数({})", code, messagePattern, params);
                if (scanIdx == 0) {
                    // 没有替换任何内容,直接返回原始模板
                    return messagePattern;
                } else {
                    // 补上剩余内容
                    sb.append(messagePattern.substring(scanIdx));
                    return sb.toString();
                }
            }
            sb.append(messagePattern, scanIdx, phIdx); // 添加占位符前的内容
            sb.append(params[paramIdx]);               // 替换占位符
            scanIdx = phIdx + 2;                       // 移动扫描位置

        }
        // 如果还有剩余 {} 占位符但参数已经用完,记录日志
        if (messagePattern.indexOf("{}", scanIdx) != -1) {
            log.error("[doFormat][参数过少：错误码({})|错误内容({})|参数({})", code, messagePattern, params);
        }
        sb.append(messagePattern.substring(scanIdx)); // 添加剩余内容
        return sb.toString();
    }

}
