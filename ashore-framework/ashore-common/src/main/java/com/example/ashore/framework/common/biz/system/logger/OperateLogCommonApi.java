package com.example.ashore.framework.common.biz.system.logger;

import com.example.ashore.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
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

@FeignClient(name = RpcConstants.SYSTEM_NAME, primary = false, fallbackFactory = OperateLogCommonApi.OperateLogCommonApiFallbackFactory.class)
@Tag(name = "RPC 服务 - 操作日志")
public interface OperateLogCommonApi {

    String PREFIX = RpcConstants.SYSTEM_PREFIX + "/operate-log";

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "创建操作日志")
    ApiResponse<Boolean> createOperateLog(@Valid @RequestBody OperateLogCreateReqDTO createReqDTO);

    /**
     * 【异步】创建操作日志
     * @param createReqDTO 请求
     */
    @Async
    default void createOperateLogAsync(OperateLogCreateReqDTO createReqDTO) {
        createOperateLog(createReqDTO).checkError();
    }

    /**
     * 这里的降级逻辑返回 ApiResponse.success(false) 而不是error，
     * 因为操作日志记录失败通常不应该阻断业务流程，只是标记日志记录未成功
     */
    @Slf4j
    @Component
    class OperateLogCommonApiFallbackFactory implements FallbackFactory<OperateLogCommonApi> {
        @Override
        public OperateLogCommonApi create(Throwable cause) {
            log.error("[OperateLogCommonApi][操作日志服务调用失败]", cause);
            return createReqDTO -> ApiResponse.success(false);
        }
    }

}
