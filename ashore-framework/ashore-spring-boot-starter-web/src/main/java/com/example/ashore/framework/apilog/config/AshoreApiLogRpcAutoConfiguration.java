package com.example.ashore.framework.apilog.config;

import com.example.ashore.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import com.example.ashore.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * API 日志使用到的 Feign 配置项
 */
@AutoConfiguration
@EnableFeignClients(clients = {ApiAccessLogCommonApi.class, ApiErrorLogCommonApi.class}) // 主要是引入相关的 API 服务
public class AshoreApiLogRpcAutoConfiguration {
}
