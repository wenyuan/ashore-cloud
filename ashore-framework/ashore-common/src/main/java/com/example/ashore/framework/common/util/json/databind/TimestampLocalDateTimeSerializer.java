package com.example.ashore.framework.common.util.json.databind;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * LocalDateTime 序列化器
 * 将 LocalDateTime 对象转换为 Long 类型时间戳
 * 用于向前端返回毫秒时间戳格式的时间
 */
public class TimestampLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

    public static final TimestampLocalDateTimeSerializer INSTANCE = new TimestampLocalDateTimeSerializer();

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 将 LocalDateTime 对象，转换为 Long 时间戳
        // 转换步骤：
        // 1. value.atZone(ZoneId.systemDefault())
        //    → 将 LocalDateTime 添加时区信息，转为 ZonedDateTime
        //    → 例：2024-01-01 10:00 → 2024-01-01 10:00 Asia/Shanghai

        // 2. .toInstant()
        //    → 转为 UTC 时间的 Instant 对象

        // 3. .toEpochMilli()
        //    → 获取从 1970-01-01 00:00:00 到现在的毫秒数
        //    → 例：1704074400000

        // 如果 LocalDateTime 为 null，提前结束方法，不返回任何值（void），防止报错 java.lang.NullPointerException
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeNumber(value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

}
