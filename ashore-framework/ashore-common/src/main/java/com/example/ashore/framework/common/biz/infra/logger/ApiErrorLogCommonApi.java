package com.example.ashore.framework.common.biz.infra.logger;

import com.example.ashore.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import com.example.ashore.framework.common.enums.RpcConstants;
import com.example.ashore.framework.common.pojo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = RpcConstants.INFRA_NAME, fallbackFactory = ApiErrorLogCommonApi.ApiErrorLogCommonApiFallbackFactory.class)
@Tag(name = "RPC 服务 - API 异常日志")
public interface ApiErrorLogCommonApi {

    String PREFIX = RpcConstants.INFRA_PREFIX + "/api-error-log";

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "创建 API 异常日志")
    ApiResponse<Boolean> createApiErrorLog(@Valid @RequestBody ApiErrorLogCreateReqDTO createDTO);

    /**
     * 【异步】创建 API 异常日志
     *
     * @param createDTO 异常日志 DTO
     */
    @Async
    default void createApiErrorLogAsync(ApiErrorLogCreateReqDTO createDTO) {
        createApiErrorLog(createDTO).checkError();
    }


    // ========== 静态内部类 ==========
    @Component
    @Slf4j
    class ApiErrorLogCommonApiFallbackFactory implements FallbackFactory<ApiErrorLogCommonApi> {

        @Override
        public ApiErrorLogCommonApi create(Throwable cause) {
            log.warn("[ApiErrorLogCommonApi] 创建异常日志 RPC 调用失败，将忽略", cause);
            return createDTO -> ApiResponse.success(false); // 静默失败，返回成功但结果为 false，不影响主流程
        }
    }

}
