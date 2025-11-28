package com.example.ashore.framework.security.config;

import com.example.ashore.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import com.example.ashore.framework.common.biz.system.permission.PermissionCommonApi;
import com.example.ashore.framework.security.core.rpc.LoginUserRequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * Security 使用到 Feign 的配置项
 */
@AutoConfiguration
@EnableFeignClients(clients = {      // 主要是引入相关的 API 服务
        OAuth2TokenCommonApi.class,  // OAuth2 Token RPC 服务
        PermissionCommonApi.class    // 权限 RPC 服务
})
public class AshoreSecurityRpcAutoConfiguration {

    @Bean
    public LoginUserRequestInterceptor loginUserRequestInterceptor() {
        return new LoginUserRequestInterceptor();
    }

}
