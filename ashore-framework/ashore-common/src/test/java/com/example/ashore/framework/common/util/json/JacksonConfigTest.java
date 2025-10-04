package com.example.ashore.framework.common.util.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

/**
 * 在 Spring 配置类中自定义 ObjectMapper
 * 替换 JsonUtils 中的默认 ObjectMapper
 */
@Configuration
public class JacksonConfigTest {

    /**
     * 自定义 ObjectMapper Bean
     * Spring 会自动管理这个 Bean
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 自定义配置 1：日期格式化为 "yyyy-MM-dd HH:mm:ss" 而不是时间戳
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        // 自定义配置 2：美化输出（自动缩进）
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // 自定义配置 3：序列化时包含 null 值（覆盖默认的 NON_NULL）
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        return mapper;
    }

    /**
     * 初始化 JsonUtils，让它使用我们自定义的 ObjectMapper
     * @PostConstruct：在 Spring 容器启动后自动执行
     */
    @PostConstruct
    public void initJsonUtils() {
        // 将 Spring 管理的 ObjectMapper 注入到 JsonUtils 中
        JsonUtils.init(objectMapper());
    }
}

