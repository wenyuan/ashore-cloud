package com.example.ashore.framework.common.biz.infra.logger;

import com.example.ashore.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
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

@FeignClient(name = RpcConstants.INFRA_NAME, fallbackFactory = ApiAccessLogCommonApi.ApiAccessLogCommonApiFallbackFactory.class)
@Tag(name = "RPC 服务 - API 访问日志")
public interface ApiAccessLogCommonApi {

    String PREFIX = RpcConstants.INFRA_PREFIX + "/api-access-log";

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "创建 API 访问日志")
    ApiResponse<Boolean> createApiAccessLog(@Valid @RequestBody ApiAccessLogCreateReqDTO createDTO);

    /**
     * 【异步】创建 API 访问日志
     *
     * @param createDTO 访问日志 DTO
     */
    @Async
    default void createApiAccessLogAsync(ApiAccessLogCreateReqDTO createDTO) {
        createApiAccessLog(createDTO).checkError();
    }


    // ========== 静态内部类 ==========
    @Component
    @Slf4j
    class ApiAccessLogCommonApiFallbackFactory implements FallbackFactory<ApiErrorLogCommonApi> {

        @Override
        public ApiErrorLogCommonApi create(Throwable cause) {
            log.warn("[ApiAccessLogCommonApi] 创建访问日志 RPC 调用失败，将忽略", cause);
            return createDTO -> ApiResponse.success(false); // 静默失败，返回成功但结果为 false，不影响主流程
        }
    }

}
