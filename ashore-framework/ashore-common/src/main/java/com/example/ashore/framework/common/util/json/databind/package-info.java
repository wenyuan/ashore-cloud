/**
 * 自定义的 Jackson JSON 序列化/反序列化器
 * databind 是 Jackson 库中负责数据绑定（将 Java 对象与 JSON 互转）的核心模块名称，
 * 这个包名沿用了 Jackson 的命名习惯，表示这里存放的是数据绑定相关的自定义实现
 *
 * 需要在 Jackson 的 ObjectMapper 中注册这些序列化器
 *
 * 注册后生效范围：
 * - 应该通过 Spring 注入后使用 ObjectMapper 的方法（objectMapper.writeValueAsString、objectMapper.readValue 等）
 *   注意：手动创建新的 ObjectMapper，不会应用自定义序列化器（ObjectMapper objectMapper = new ObjectMapper();）
 * - @RestController 会自动应用反序列化器处理前端传递的数据（时间戳 -> LocalDateTime）；自动应用序列化器返回 JSON 给前端
 * - Gateway 网关场景，响应式编程（通过 CodecCustomizer 配置，也会生效）
 * - JsonUtils 工具类
 *   {@link com.example.ashore.framework.common.util.json.JsonUtils#toJsonString}
 *   {@link com.example.ashore.framework.common.util.json.JsonUtils#parseObject}
 *   {@link com.example.ashore.framework.common.util.json.JsonUtils#parseArray}
 * - Feign 远程调用
 * - Redis 缓存
 * - 消息队列
 *
 * see
 * {@link com.example.ashore.framework.jackson.config.AshoreJacksonAutoConfiguration}
 * {@link com.example.ashore.gateway.jackson.GatewayJacksonAutoConfiguration}
 * for configuration details.
 */
package com.example.ashore.framework.common.util.json.databind;
