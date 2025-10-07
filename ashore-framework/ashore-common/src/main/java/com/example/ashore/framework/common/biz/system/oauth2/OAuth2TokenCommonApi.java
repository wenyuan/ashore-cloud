package com.example.ashore.framework.common.biz.system.oauth2;

import com.example.ashore.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import com.example.ashore.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import com.example.ashore.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenRespDTO;
import com.example.ashore.framework.common.enums.RpcConstants;
import com.example.ashore.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.example.ashore.framework.common.pojo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = RpcConstants.SYSTEM_NAME, fallbackFactory = OAuth2TokenCommonApi.OAuth2TokenCommonApiFallbackFactory.class)
@Tag(name = "RPC 服务 - OAuth2.0 令牌")
public interface OAuth2TokenCommonApi {

    String PREFIX = RpcConstants.SYSTEM_PREFIX + "/oauth2/token";

    /**
     * 校验 Token 的 URL 地址，主要是提供给 Gateway 使用
     */
    @SuppressWarnings("HttpUrlsUsage")
    String URL_CHECK = "http://" + RpcConstants.SYSTEM_NAME + PREFIX + "/check";

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "创建访问令牌")
    ApiResponse<OAuth2AccessTokenRespDTO> createAccessToken(@Valid @RequestBody OAuth2AccessTokenCreateReqDTO reqDTO);

    @GetMapping(PREFIX + "/check")
    @Operation(summary = "校验访问令牌")
    @Parameter(name = "accessToken", description = "访问令牌", required = true, example = "xxx")
    ApiResponse<OAuth2AccessTokenCheckRespDTO> checkAccessToken(@RequestParam("accessToken") String accessToken);

    @DeleteMapping(PREFIX + "/remove")
    @Operation(summary = "移除访问令牌")
    @Parameter(name = "accessToken", description = "访问令牌", required = true, example = "xxx")
    ApiResponse<OAuth2AccessTokenRespDTO> removeAccessToken(@RequestParam("accessToken") String accessToken);

    @PutMapping(PREFIX + "/refresh")
    @Operation(summary = "刷新访问令牌")
    @Parameters({
            @Parameter(name = "refreshToken", description = "刷新令牌", required = true, example = "haha"),
            @Parameter(name = "clientId", description = "客户端编号", required = true, example = "xxx-xxx")
    })
    ApiResponse<OAuth2AccessTokenRespDTO> refreshAccessToken(@RequestParam("refreshToken") String refreshToken,
                                                              @RequestParam("clientId") String clientId);

    @Slf4j
    @Component
    class OAuth2TokenCommonApiFallbackFactory implements FallbackFactory<OAuth2TokenCommonApi> {
        @Override
        public OAuth2TokenCommonApi create(Throwable cause) {
            log.error("[OAuth2TokenCommonApi][OAuth2 令牌服务调用失败]", cause);
            return new OAuth2TokenCommonApi() {
                @Override
                public ApiResponse<OAuth2AccessTokenRespDTO> createAccessToken(OAuth2AccessTokenCreateReqDTO reqDTO) {
                    return ApiResponse.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), "令牌服务调用失败");
                }

                @Override
                public ApiResponse<OAuth2AccessTokenCheckRespDTO> checkAccessToken(String accessToken) {
                    return ApiResponse.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), "令牌服务调用失败");
                }

                @Override
                public ApiResponse<OAuth2AccessTokenRespDTO> removeAccessToken(String accessToken) {
                    return ApiResponse.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), "令牌服务调用失败");
                }

                @Override
                public ApiResponse<OAuth2AccessTokenRespDTO> refreshAccessToken(String refreshToken, String clientId) {
                    return ApiResponse.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), "令牌服务调用失败");
                }
            };
        }
    }

}
