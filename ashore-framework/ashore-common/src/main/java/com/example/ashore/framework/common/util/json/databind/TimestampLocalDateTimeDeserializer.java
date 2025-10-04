package com.example.ashore.framework.common.util.json.databind;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * LocalDateTime 反序列化器
 * 将 Long 类型的时间戳转换为 LocalDateTime 对象
 * 用于接收前端传来的毫秒时间戳
 */
public class TimestampLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    public static final TimestampLocalDateTimeDeserializer INSTANCE = new TimestampLocalDateTimeDeserializer();

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 将 Long 时间戳，转换为 LocalDateTime 对象
        // 转换步骤：
        // 1. p.getValueAsLong()
        //    → 从 JSON 中读取 Long 值（时间戳毫秒数）
        //    → 例：1704074400000

        // 2. Instant.ofEpochMilli(...)
        //    → 将毫秒时间戳转为 Instant 对象（UTC 时间）

        // 3. LocalDateTime.ofInstant(..., ZoneId.systemDefault())
        //    → 将 Instant 按系统时区转为 LocalDateTime
        //    → 例：1704074400000 → 2024-01-01 10:00

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(p.getValueAsLong()), ZoneId.systemDefault());
    }

}
