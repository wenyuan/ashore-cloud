package com.example.ashore.framework.common.biz.system.permission;

import com.example.ashore.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import com.example.ashore.framework.common.enums.RpcConstants;
import com.example.ashore.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.example.ashore.framework.common.pojo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = RpcConstants.SYSTEM_NAME, primary = false, fallbackFactory = PermissionCommonApi.PermissionCommonApiFallbackFactory.class)
@Tag(name = "RPC 服务 - 权限")
public interface PermissionCommonApi {

    String PREFIX = RpcConstants.SYSTEM_PREFIX + "/permission";

    @GetMapping(PREFIX + "/has-any-permissions")
    @Operation(summary = "判断是否有权限，任一一个即可")
    @Parameters({
            @Parameter(name = "userId", description = "用户编号", example = "1", required = true),
            @Parameter(name = "permissions", description = "权限", example = "read,write", required = true)
    })
    ApiResponse<Boolean> hasAnyPermissions(@RequestParam("userId") Long userId,
                                            @RequestParam("permissions") String... permissions);

    @GetMapping(PREFIX + "/has-any-roles")
    @Operation(summary = "判断是否有角色，任一一个即可")
    @Parameters({
            @Parameter(name = "userId", description = "用户编号", example = "1", required = true),
            @Parameter(name = "roles", description = "角色数组", example = "2", required = true)
    })
    ApiResponse<Boolean> hasAnyRoles(@RequestParam("userId") Long userId,
                                      @RequestParam("roles") String... roles);

    @GetMapping(PREFIX + "/get-dept-data-permission")
    @Operation(summary = "获得登录用户的部门数据权限")
    @Parameter(name = "userId", description = "用户编号", example = "2", required = true)
    ApiResponse<DeptDataPermissionRespDTO> getDeptDataPermission(@RequestParam("userId") Long userId);

    /**
     * 由于权限服务是核心功能，降级时返回错误状态，确保调用方能感知到权限检查失败。
     */
    @Slf4j
    @Component
    class PermissionCommonApiFallbackFactory implements FallbackFactory<PermissionCommonApi> {
        @Override
        public PermissionCommonApi create(Throwable cause) {
            log.error("[PermissionCommonApi][权限服务调用失败]", cause);
            return new PermissionCommonApi() {
                @Override
                public ApiResponse<Boolean> hasAnyPermissions(Long userId, String... permissions) {
                    return ApiResponse.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), "权限服务调用失败");
                }

                @Override
                public ApiResponse<Boolean> hasAnyRoles(Long userId, String... roles) {
                    return ApiResponse.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), "权限服务调用失败");
                }

                @Override
                public ApiResponse<DeptDataPermissionRespDTO> getDeptDataPermission(Long userId) {
                    return ApiResponse.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), "权限服务调用失败");
                }
            };
        }
    }

}
