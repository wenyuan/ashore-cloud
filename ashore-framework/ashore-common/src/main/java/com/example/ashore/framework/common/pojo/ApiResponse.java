package com.example.ashore.framework.common.pojo;

import cn.hutool.core.lang.Assert;
import com.example.ashore.framework.common.exception.ErrorCode;
import com.example.ashore.framework.common.exception.BusinessException;
import com.example.ashore.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.example.ashore.framework.common.exception.util.BusinessExceptionUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * 通用返回
 * @param <T> 数据泛型
 */
@Data
public class ApiResponse<T> implements Serializable {

    /**
     * 错误码
     * @see ErrorCode#getCode()
     */
    private String code;
    /**
     * 错误提示，返回给用户的提示信息
     * @see ErrorCode#getMsg() ()
     */
    private String msg;
    /**
     * 返回数据，类型由泛型 T 决定
     */
    private T data;

    /**
     * 场景: 服务间调用，比如 feign 调用返回 ApiResponse<UserDTO>，需要转换为 ApiResponse<UserVO>（只复制 code 和 msg）
     * 将传入的 result 对象，转换成另外一个泛型结果的对象
     * 因为 A 方法返回的 ApiResponse 对象，不满足调用其的 B 方法的返回，所以需要进行转换。
     *
     * @param result 传入的 result 对象
     * @param <T> 返回的泛型
     * @return 新的 ApiResponse 对象
     */
    public static <T> ApiResponse<T> error(ApiResponse<?> result) {
        return error(result.getCode(), result.getMsg());
    }

    // 创建错误结果(传递错误码和消息)
    public static <T> ApiResponse<T> error(String code, String message) {
        Assert.notEquals(GlobalErrorCodeConstants.SUCCESS.getCode(), code, "当前传递的是成功时的 code！");
        ApiResponse<T> result = new ApiResponse<>();
        result.code = code;
        result.msg = message;
        return result;
    }

    // 创建错误结果(传递错误码和消息，消息带参数格式化)
    public static <T> ApiResponse<T> error(ErrorCode errorCode, Object... params) {
        Assert.notEquals(GlobalErrorCodeConstants.SUCCESS.getCode(), errorCode.getCode(), "当前传递的是成功时的 code！");
        ApiResponse<T> result = new ApiResponse<>();
        result.code = errorCode.getCode();
        result.msg = BusinessExceptionUtils.doFormat(errorCode.getCode(), errorCode.getMsg(), params);
        return result;
    }

    // 创建错误结果(传递ErrorCode对象)
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMsg());
    }

    // 创建成功结果
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> result = new ApiResponse<>();
        result.code = GlobalErrorCodeConstants.SUCCESS.getCode();
        result.data = data;
        result.msg = "";
        return result;
    }

    // 判断是否成功(静态方法)
    public static boolean isSuccess(String code) {
        return Objects.equals(code, GlobalErrorCodeConstants.SUCCESS.getCode());
    }

    @JsonIgnore // JSON 序列化时忽略这个方法，避免在返回 JSON 中出现
    public boolean isSuccess() {
        return isSuccess(code);
    }

    @JsonIgnore // JSON 序列化时忽略这个方法，避免在返回 JSON 中出现
    public boolean isError() {
        return !isSuccess();
    }


    // ========= 和 Exception 异常体系集成 =========
    /**
     * 判断是否有异常，如果有就抛出 {@link BusinessException} 异常
     */
    public void checkError() throws BusinessException {
        if (isSuccess()) {
            return;
        }
        // 业务异常
        throw new BusinessException(code, msg);
    }

    /**
     * 判断是否有异常
     * - 如果有就抛出 {@link BusinessException} 异常
     * - 如果没有就返回 {@link #data} 数据
     *
     * 场景：feign 调用后，如果失败直接抛异常，成功则返回数据(节省手动判断过程)
     */
    @JsonIgnore // 避免 jackson 序列化
    public T getDataOrThrow() {
        checkError();
        return data;
    }

    /**
     * 异常捕获后转换为返回结果, 仅支持业务异常 {@link BusinessException}
     *
     * 场景：
     * - 全局异常处理器
     * - Controller 中手动捕获
     * - 嵌套调用service方法时的异常处理
     *
     * @param businessException 业务异常
     * @return
     * @param <T>
     */
    public static <T> ApiResponse<T> error(BusinessException businessException) {
        return error(businessException.getCode(), businessException.getMessage());
    }

}
