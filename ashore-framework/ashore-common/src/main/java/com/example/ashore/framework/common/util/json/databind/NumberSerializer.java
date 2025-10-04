package com.example.ashore.framework.common.util.json.databind;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;

import java.io.IOException;

/**
 * 自定义数字序列化器（Long/Integer 等数字）
 * 继承 Jackson 内置的 NumberSerializer
 * 解决 JavaScript 精度问题：将超出安全整数范围（±9007199254740991）的 Long 值转为字符串
 */
@JacksonStdImpl
public class NumberSerializer extends com.fasterxml.jackson.databind.ser.std.NumberSerializer {

    // JavaScript 安全整数范围：2^53 - 1 = 9007199254740991
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;
    private static final long MIN_SAFE_INTEGER = -9007199254740991L;

    // 单例模式，避免重复创建
    public static final NumberSerializer INSTANCE = new NumberSerializer(Number.class);

    // 构造方法，传入要处理的数字类型
    public NumberSerializer(Class<? extends Number> rawType) {
        super(rawType);
    }

    @Override
    public void serialize(Number value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 核心逻辑：判断数字是否在安全范围内
        if (value.longValue() >= MIN_SAFE_INTEGER && value.longValue() <= MAX_SAFE_INTEGER) {
            // 在安全范围内，按数字输出：123
            super.serialize(value, gen, serializers);
        } else {
            // 超出范围，转为字符串输出
            gen.writeString(value.toString());
        }
    }
}
