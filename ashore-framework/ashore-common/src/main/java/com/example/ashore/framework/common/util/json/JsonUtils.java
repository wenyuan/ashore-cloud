package com.example.ashore.framework.common.util.json;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.ashore.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import com.example.ashore.framework.common.util.json.databind.TimestampLocalDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON 工具类
 * 提供 Java 对象与 JSON 字符串之间的转换功能
 * 基于Jackson 库进行封装，简化 JSON 操作
 */
@Slf4j
public class JsonUtils {

    /**
     * Jackson 核心对象:负责 JSON 序列化和反序列化
     * 此处可以自动生成 getObjectMapper() 方法
     * static 类级别变量，所有实例共享
     */
    @Getter
    private static ObjectMapper objectMapper = new ObjectMapper();

    // 静态代码块：在类加载时执行一次，用于初始化 ObjectMapper 配置
    static {
        // 配置1：序列化时，遇到空 Bean(没有任何属性的对象)不抛出异常
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 配置2：反序列化时，遇到未知属性不报错（JSON 里有但 Java 类里没有的字段）-- 提高容错性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 配置3：序列化时忽略值为 null 的属性 -- 减少 JSON 体积)
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 配置4：处理 LocalDateTime 时间类型的序列化/反序列化（转换成时间戳格式）
        SimpleModule simpleModule = new JavaTimeModule()
                .addSerializer(LocalDateTime.class, TimestampLocalDateTimeSerializer.INSTANCE)
                .addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
        objectMapper.registerModules(simpleModule);
    }

    /**
     * 初始化 objectMapper 属性
     * 允许将自定义配置的 ObjectMapper 注入到 JsonUtils 中，从而替换 JsonUtils 中的默认 ObjectMapper
     *
     * 场景：
     * 在实际项目中，你可能需要自定义配置 ObjectMapper，比如：
     * - 修改日期格式
     * - 添加自定义序列化器
     * - 调整其他 Jackson 行为
     *
     * 示例：{@link com.example.ashore.framework.common.util.json.JacksonConfigTest}
     *
     * @param objectMapper 外部传入的 ObjectMapper 对象
     */
    public static void init(ObjectMapper objectMapper) {
        JsonUtils.objectMapper = objectMapper;
    }

    // =============== 序列化方法：Java 对象 -> JSON ===============

    /**
     * 将 Java 对象转换为 JSON 字符串
     * 示例：
     * User user = new User("张三", 18);
     * String json = JsonUtils.toJsonString(user);
     * 结果：{"name":"张三","age":18}
     *
     * @param object 要转换的 Java 对象
     * @return       JSON 字符串
     */
    @SneakyThrows
    public static String toJsonString(Object object) {
        return objectMapper.writeValueAsString(object);
    }

    /**
     * 将 Java 对象转换为 JSON 字节数组
     * 场景：网络传输、文件存储等需要字节数组的场景
     * 示例：
     * User user = new User("李四", 20);
     * byte[] jsonBytes = JsonUtils.toJsonByte(user);
     * 可用于写入文件或网络传输
     *
     * @param object 要转换的 Java 对象
     * @return       JSON 字节数组
     */
    @SneakyThrows
    public static byte[] toJsonByte(Object object) {
        return objectMapper.writeValueAsBytes(object);
    }

    /**
     * 将 Java 对象转换为格式化的 JSON 字符串(带缩进和换行)
     * 场景：日志输出、调试、API 文档展示等需要可读性的场景
     * 示例：
     * User user = new User("王五", 25);
     * String json = JsonUtils.toJsonPrettyString(user);
     * 结果：
     * {
     *    "name" : "王五",
     *    "age" : 25
     * }
     *
     * @param object 要转换的 Java 对象
     * @return       格式化的 JSON 字符串
     */
    @SneakyThrows
    public static String toJsonPrettyString(Object object) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    // =============== 序列化方法：Java 对象 -> JSON =============== 结束

    // =============== 反序列化方法：JSON -> Java 对象 ===============

    /**
     * 将 JSON 字符串解析为指定类型的对象(基础版本)
     * 示例：
     * String json = "{\"name\":\"赵六\",\"age\":30}";
     * User user = JsonUtils.parseObject(json, User.class);
     *
     * @param text  JSON 字符串
     * @param clazz 目标类型的 Class 对象
     * @return      解析后的对象,如果 text 为空则返回 null
     * @param <T>   泛型:返回值类型由传入的 Class 决定
     */
    public static <T> T parseObject(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, clazz);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 从 JSON 字符串中提取指定路径的节点并解析为对象
     * 区别：与上面方法的区别是多了 path 参数，可以提取嵌套 JSON 中的某个字段
     * 示例：
     * String json = "{\"data\":{\"user\":{\"name\":\"孙七\",\"age\":35}}}";
     * User user = JsonUtils.parseObject(json, "data.user", User.class);
     * // 直接提取 data.user 节点解析为 User 对象
     *
     * @param text  JSON 字符串
     * @param path  JSON 路径(如 "data.user")
     * @param clazz clazz 目标类型的 Class 对象
     * @return      解析后的对象,如果 text 为空则返回 null
     * @param <T>   泛型:返回值类型由传入的 Class 决定
     */
    public static <T> T parseObject(String text, String path, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(text);  // 解析为树结构
            JsonNode pathNode = treeNode.path(path);          // 获取指定路径的节点
            return objectMapper.readValue(pathNode.toString(), clazz);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串解析为指定类型的对象(支持复杂泛型)
     * 区别：使用 Type 而不是 Class，可以处理泛型擦除问题
     * 示例：
     * String json = "[\"apple\",\"banana\"]";
     * Type type = new TypeToken<List<String>>(){}.getType();
     * List<String> list = JsonUtils.parseObject(json, type);
     *
     * @param text JSON 字符串
     * @param type type 目标类型(可以是带泛型的类型)
     * @return          解析后的对象,如果 text 为空则返回 null
     * @param <T>
     */
    public static <T> T parseObject(String text, Type type) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructType(type));
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字节数组解析为指定类型的对象
     * 区别：输入是 byte[] 而不是 String，适用于从网络或文件读取的场景
     * 示例：
     * byte[] jsonBytes = "{\"name\":\"周八\"}".getBytes();
     * Type type = User.class;
     * User user = JsonUtils.parseObject(jsonBytes, type);
     *
     * @param text JSON 字节数组
     * @param type type 目标类型
     * @return     解析后的对象，如果 text 为空则返回 null
     * @param <T>
     */
    public static <T> T parseObject(byte[] text, Type type) {
        if (ArrayUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructType(type));
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将字符串解析成指定类型的对象(特殊场景版本)
     * 场景：在使用 @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) 注解时,
     * 如果 JSON 中没有 class 属性，使用 parseObject 会报错，此方法可以解决
     *
     * 区别：使用 Hutool 的 JSONUtil 而不是 Jackson
     * 示例：
     * String json = "{\"name\":\"吴九\"}";  // 没有 class 属性
     * User user = JsonUtils.parseObject2(json, User.class);
     *
     * @param text 字符串
     * @param clazz 类型
     * @return 对象
     */
    public static <T> T parseObject2(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        return JSONUtil.toBean(text, clazz);
    }

    /**
     * 将 JSON 字节数组解析为指定类型的对象
     * 区别：使用 Class 而不是 Type，适合简单类型
     * 示例：
     * byte[] jsonBytes = "{\"name\":\"郑十\"}".getBytes();
     * User user = JsonUtils.parseObject(jsonBytes, User.class);
     *
     * @param bytes JSON 字节数组
     * @param clazz 目标类型的 Class 对象
     * @return      解析后的对象,如果 bytes 为空则返回 null
     * @param <T>
     */
    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (ArrayUtil.isEmpty(bytes)) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            log.error("json parse err,json:{}", bytes, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字符串解析为指定类型的对象(使用 TypeReference)
     * 优势：TypeReference 可以保留泛型信息。解决泛型擦除问题
     * 示例：
     * String json = "{\"users\":[{\"name\":\"user1\"},{\"name\":\"user2\"}]}";
     * Map<String, List<User>> result = JsonUtils.parseObject(json,new TypeReference<Map<String, List<User>>>(){});
     *
     * @param text          JSON 字符串
     * @param typeReference 类型引用(保留泛型信息)
     * @return              解析后的对象
     * @param <T>
     */
    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(text, typeReference);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析 JSON 字符串成指定类型的对象,如果解析失败,则返回 null(静默模式)
     * 区别：不抛出异常，失败时返回 null，适合不确定 JSON 格式是否正确的场景
     * 示例：
     * String json = "invalid json";
     * User user = JsonUtils.parseObjectQuietly(json, new TypeReference<User>(){});
     * // user 为 null，不会抛异常
     *
     * @param text          字符串
     * @param typeReference 类型引用
     * @return              指定类型的对象，解析失败返回 null
     */
    public static <T> T parseObjectQuietly(String text, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(text, typeReference);
        } catch (IOException e) {
            return null;
        }
    }

    // =============== 反序列化方法：JSON -> Java 对象 =============== 结束

    // =============== 数组解析方法：JSON -> List 集合 ===============

    /**
     * 将 JSON 数组字符串解析为 List 集合
     * 示例：
     * String json = "[{\"name\":\"user1\"},{\"name\":\"user2\"}]";
     * List<User> users = JsonUtils.parseArray(json, User.class);
     *
     * @param text  JSON 数组字符串
     * @param clazz 集合元素的类型
     * @return      集合,如果 text 为空则返回空 ArrayList
     * @param <T>
     */
    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 从 JSON 中提取指定路径的数组节点并解析为 List
     * 区别：多了 path 参数，可以提取嵌套 JSON 中的数组字段
     * 示例：
     * String json = "{\"data\":{\"users\":[{\"name\":\"user1\"},{\"name\":\"user2\"}]}}";
     * List<User> users = JsonUtils.parseArray(json, "data.users", User.class);
     *
     * @param text  JSON 字符串
     * @param path  JSON 路径(如 "data.users")
     * @param clazz 集合元素的类型
     * @return      集合,如果 text 为空则返回 null
     * @param <T>
     */
    public static <T> List<T> parseArray(String text, String path, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(text);
            JsonNode pathNode = treeNode.path(path);
            return objectMapper.readValue(pathNode.toString(), objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    // =============== 数组解析方法：JSON -> List 集合 =============== 结束

    // =============== 树结构解析：JSON -> JsonNode ===============

    /**
     * 将 JSON 字符串解析为 JsonNode 树结构
     * 作用：JsonNode 是 Jackson 的树模型，可以灵活操作 JSON 节点
     * 示例：
     * String json = "{\"name\":\"test\",\"age\":18}";
     * JsonNode node = JsonUtils.parseTree(json);
     * String name = node.get("name").asText();  // "test"
     * int age = node.get("age").asInt();        // 18
     *
     * @param text 字符串
     * @return     JsonNode 树节点
     */
    public static JsonNode parseTree(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 JSON 字节数组解析为 JsonNode 树结构
     * 区别：输入是 byte[] 而不是 String
     * 示例：
     * byte[] jsonBytes = "{\"name\":\"test\"}".getBytes();
     * JsonNode node = JsonUtils.parseTree(jsonBytes);
     *
     * @param text JSON 字节数组
     * @return     JsonNode 树节点
     */
    public static JsonNode parseTree(byte[] text) {
        try {
            return objectMapper.readTree(text);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    // =============== 树结构解析：JSON -> JsonNode =============== 结束

    // =============== 工具方法：JSON 格式判断 ===============

    /**
     * 判断字符串是否为 JSON 格式(包括对象和数组)
     * 示例：
     * JsonUtils.isJson("{\"name\":\"test\"}");  // true
     * JsonUtils.isJson("[1,2,3]");              // true
     * JsonUtils.isJson("not json");             // false
     *
     * @param text 字符串
     * @return     true=是 JSON 格式,false=不是
     */
    public static boolean isJson(String text) {
        return JSONUtil.isTypeJSON(text);
    }

    /**
     * 判断字符串是否为 JSON 对象格式(不包括数组)
     * 区别：只判断 JSON 对象 {}，不包括 JSON 数组 []
     * 示例：
     * JsonUtils.isJsonObject("{\"name\":\"test\"}");  // true
     * JsonUtils.isJsonObject("[1,2,3]");              // false
     *
     * @param str 字符串
     * @return    true=是 JSON 格式,false=不是
     */
    public static boolean isJsonObject(String str) {
        return JSONUtil.isTypeJSONObject(str);
    }

    // =============== 工具方法：JSON 格式判断 =============== 结束
}
