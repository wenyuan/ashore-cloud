package com.example.ashore.framework.common.biz.system.tenant;

import com.example.ashore.framework.common.enums.RpcConstants;
import com.example.ashore.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.example.ashore.framework.common.pojo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = RpcConstants.SYSTEM_NAME, fallbackFactory = TenantCommonApi.TenantCommonApiFallbackFactory.class)
@Tag(name = "RPC 服务 - 多租户")
public interface TenantCommonApi {

    String PREFIX = RpcConstants.SYSTEM_PREFIX + "/tenant";

    @GetMapping(PREFIX + "/id-list")
    @Operation(summary = "获得所有租户编号")
    ApiResponse<List<Long>> getTenantIdList();

    @GetMapping(PREFIX + "/valid")
    @Operation(summary = "校验租户是否合法")
    @Parameter(name = "id", description = "租户编号", required = true, example = "1024")
    ApiResponse<Boolean> validTenant(@RequestParam("id") Long id);

    @Slf4j
    @Component
    class TenantCommonApiFallbackFactory implements FallbackFactory<TenantCommonApi> {
        @Override
        public TenantCommonApi create(Throwable cause) {
            log.error("[TenantCommonApi][租户服务调用失败]", cause);
            return new TenantCommonApi() {
                @Override
                public ApiResponse<List<Long>> getTenantIdList() {
                    return ApiResponse.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), "租户服务调用失败");
                }

                @Override
                public ApiResponse<Boolean> validTenant(Long id) {
                    return ApiResponse.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), "租户服务调用失败");
                }
            };
        }
    }

}
