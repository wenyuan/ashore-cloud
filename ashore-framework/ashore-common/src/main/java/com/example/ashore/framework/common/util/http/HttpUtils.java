package com.example.ashore.framework.common.util.http;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.TableMap;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP 工具类
 *
 * 核心依赖库：
 * - Hutool (cn.hutool.http.*)                    提供便捷的 HTTP 请求和 URL 处理功能
 * - Spring Web (org.springframework.web.util.*)  提供 URI 组件构建功能
 * - Jakarta Servlet API                          提供 HttpServletRequest 支持
 *
 */
public class HttpUtils {

    /**
     * 将字符串进行 UTF-8 URL 编码
     *
     * 作用：对 URL 参数值进行编码，防止特殊字符（如空格、中文、特殊符号等）在 URL 中传递时出现问题。
     * 示例：
     * encodeUtf8("Hello World")  → "Hello+World"
     * encodeUtf8("测试编码")      → "%E6%B5%8B%E8%AF%95%E7%BC%96%E7%A0%81"
     * encodeUtf8("a=1&b=2")      → "a%3D1%26b%3D2"
     *
     * @param value 需要编码的字符串
     * @return      编码后的字符串
     */
    public static String encodeUtf8(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 替换 URL 中指定的查询参数
     *
     * 作用：动态修改 URL 中某个查询参数的值，如果参数不存在则添加，如果存在则先移除再添加（确保唯一性）。
     * 示例：
     * replaceUrlQuery("http://example.com?a=1&b=2", "b", "999")
     *   → "http://example.com?a=1&b=999"
     *
     * replaceUrlQuery("http://example.com?a=1", "b", "2")
     *   → "http://example.com?a=1&b=2"
     *
     * @param url   原始 URL
     * @param key   要替换的参数名
     * @param value 新的参数值
     * @return      替换后的 URL
     */
    @SuppressWarnings("unchecked")
    public static String replaceUrlQuery(String url, String key, String value) {
        // 使用 Hutool 的 UrlBuilder 解析 URL
        UrlBuilder builder = UrlBuilder.of(url, Charset.defaultCharset());

        // 使用反射获取 UrlBuilder 内部的 query 参数集合
        // TableMap 是 Hutool 提供的双向映射表，可以同时通过 key 和 value 进行查询
        TableMap<CharSequence, CharSequence> query = (TableMap<CharSequence, CharSequence>)
                ReflectUtil.getFieldValue(builder.getQuery(), "query");

        // 先移除旧的同名参数（如果存在）
        query.remove(key);

        // 再添加新的参数
        builder.addQuery(key, value);

        // 构建并返回最终的 URL 字符串
        return builder.build();
    }

    /**
     * 移除 URL 中的所有查询参数和片段标识符
     *
     * 作用：从完整的 URL 中移除查询参数（? 后面的部分）和片段标识符（# 后面的部分），只保留基础 URL。
     * 示例：
     * removeUrlQuery("http://example.com/path?a=1&b=2#section")
     *   → "http://example.com/path"
     *
     * removeUrlQuery("http://example.com/path")
     *   → "http://example.com/path"  （没有查询参数，直接返回原 URL）
     *
     * @param url 原始 URL
     * @return    移除查询参数和片段后的 URL
     */
    public static String removeUrlQuery(String url) {
        // 检查 URL 中是否包含 '?' 字符（表示是否有查询参数）
        if (!StrUtil.contains(url, '?')) {
            return url;
        }

        // 使用 Hutool 的 UrlBuilder 解析 URL
        UrlBuilder builder = UrlBuilder.of(url, Charset.defaultCharset());

        // 移除 query（查询参数，即 ? 后面的部分）
        builder.setQuery(null);

        // 移除 fragment（片段标识符，即 # 后面的部分）
        builder.setFragment(null);

        // 构建并返回处理后的 URL
        return builder.build();
    }

    /**
     * 拼接 URL，支持查询参数重命名和 Fragment 拼接
     *
     * 作用：将参数以查询字符串或 Fragment（# 后面）的形式追加到基础 URL 上，并支持参数名的重命名映射。
     *      该方法主要用于 OAuth2 等需要构建复杂重定向 URL 的场景。
     *
     * 代码来源：Spring Security OAuth2 的 AuthorizationEndpoint 类的 append 方法
     *
     * 示例：
     * // 示例 1：作为查询参数拼接（fragment = false）
     * Map<String, String> params = new HashMap<>();
     * params.put("code", "abc123");
     * params.put("state", "xyz");
     * append("http://example.com/callback", params, null, false)
     *   → "http://example.com/callback?code=abc123&state=xyz"
     *
     * // 示例 2：作为 Fragment 拼接（fragment = true）
     * append("http://example.com/callback", params, null, true)
     *   → "http://example.com/callback#code=abc123&state=xyz"
     *
     * // 示例 3：使用参数名映射（keys 不为空）
     * Map<String, String> keys = new HashMap<>();
     * keys.put("code", "authorization_code");  // 将 code 重命名为 authorization_code
     * append("http://example.com/callback", params, keys, false)
     *   → "http://example.com/callback?authorization_code=abc123&state=xyz"
     *
     * @param base     基础 URL
     * @param query    查询参数 Map。示例值：{"code": "abc123", "state": "xyz"}
     * @param keys     参数名映射表，用于重命名参数。
     *                 示例值：{"code": "authorization_code"}，表示将 code 重命名为 authorization_code。
     *                 可以为 null
     * @param fragment 是否将参数拼接到 Fragment（# 后面）。true 表示拼接到 #，false 表示拼接到查询字符串（?）
     * @return         拼接后的完整 URL。
     */
    public static String append(String base, Map<String, ?> query, Map<String, String> keys, boolean fragment) {
        // 创建 URI 模板构建器（用于构建带占位符的 URL）
        UriComponentsBuilder template = UriComponentsBuilder.newInstance();
        // 从基础 URL 字符串创建构建器
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base);

        URI redirectUri;
        try {
            // 假设 URL 已经编码（比如从网络传输过来的），参数 true 表示已编码
            redirectUri = builder.build(true).toUri();
        } catch (Exception e) {
            // 如果解析失败，尝试以未编码方式处理（支持硬编码的未编码 URL）
            redirectUri = builder.build().toUri();
            builder = UriComponentsBuilder.fromUri(redirectUri);
        }

        // 将基础 URL 的各个组成部分复制到模板中
        // 链式调用设置：协议(scheme)、端口(port)、主机(host)、用户信息(userInfo)、路径(path)
        template.scheme(redirectUri.getScheme()).port(redirectUri.getPort()).host(redirectUri.getHost())
                .userInfo(redirectUri.getUserInfo()).path(redirectUri.getPath());

        // 判断是否将参数拼接到 Fragment（# 后面）
        if (fragment) {
            // === Fragment 模式：将参数拼接到 # 后面 ===
            StringBuilder values = new StringBuilder();

            // 如果原 URL 已经有 Fragment，先保留原有内容
            if (redirectUri.getFragment() != null) {
                String append = redirectUri.getFragment();
                values.append(append);
            }

            // 遍历所有查询参数
            for (String key : query.keySet()) {
                // 如果不是第一个参数，添加 & 分隔符
                if (values.length() > 0) {
                    values.append("&");
                }

                // 获取最终的参数名（如果有映射则使用映射后的名称）
                String name = key;
                if (keys != null && keys.containsKey(key)) {
                    name = keys.get(key);
                }

                // 构建参数占位符，格式：name={key}
                // 例如：code={code}，后续会用实际值替换 {code}
                values.append(name).append("={").append(key).append("}");
            }

            // 如果有参数，设置到 template 的 Fragment 中
            if (values.length() > 0) {
                template.fragment(values.toString());
            }

            // 使用实际参数值展开占位符，并进行 URL 编码
            UriComponents encoded = template.build().expand(query).encode();
            // 将处理好的 Fragment 设置到最终的 builder 中
            builder.fragment(encoded.getFragment());
        } else {
            // === 查询参数模式：将参数拼接到 ? 后面 ===
            for (String key : query.keySet()) {
                // 获取最终的参数名（如果有映射则使用映射后的名称）
                String name = key;
                if (keys != null && keys.containsKey(key)) {
                    name = keys.get(key);
                }

                // 添加查询参数占位符，格式：name={key}
                template.queryParam(name, "{" + key + "}");
            }

            // 保留原 URL 的 Fragment
            template.fragment(redirectUri.getFragment());

            // 使用实际参数值展开占位符，并进行 URL 编码
            UriComponents encoded = template.build().expand(query).encode();

            // 将处理好的查询字符串设置到最终的 builder 中
            builder.query(encoded.getQuery());
        }

        // 构建并返回最终的 URL 字符串
        return builder.build().toUriString();
    }

    /**
     * 从 HTTP 请求中获取 Basic 认证信息（客户端 ID 和密钥）
     *
     * 作用：解析 HTTP 请求中的 Basic 认证信息，支持两种方式：
     * - 优先从 Authorization Header 中解析（标准的 HTTP Basic 认证方式）
     * - 如果 Header 中没有，则从请求参数中获取 client_id 和 client_secret
     * 该方法主要用于 OAuth2 客户端认证场景。
     *
     * HTTP Basic 认证格式说明：
     * - Header 格式：Authorization: Basic Base64(clientId:clientSecret)
     * - 例如：clientId=myapp，clientSecret=secret123
     * - 拼接为：myapp:secret123
     * - Base64 编码后：bXlhcHA6c2VjcmV0MTIz
     * - 最终 Header：Authorization: Basic bXlhcHA6c2VjcmV0MTIz
     *
     * 示例：
     * // 示例 1：从 Authorization Header 中获取
     * // 假设请求头为：Authorization: Basic bXlhcHA6c2VjcmV0MTIz
     * String[] result = obtainBasicAuthorization(request);
     * // result[0] = "myapp"
     * // result[1] = "secret123"
     *
     * // 示例 2：从请求参数中获取
     * // 假设请求参数为：?client_id=myapp&client_secret=secret123
     * String[] result = obtainBasicAuthorization(request);
     * // result[0] = "myapp"
     * // result[1] = "secret123"
     *
     * // 示例 3：认证信息不完整
     * // 假设只有 client_id 没有 client_secret
     * String[] result = obtainBasicAuthorization(request);
     * // result = null
     *
     * @param request HTTP 请求对象
     * @return        包含客户端 ID 和密钥的数组，格式：[clientId, clientSecret]。
     *                如果认证信息不存在或不完整，返回 null
     */
    public static String[] obtainBasicAuthorization(HttpServletRequest request) {
        String clientId;
        String clientSecret;

        // === 方式 1：优先从 Authorization Header 中获取 ===
        // 获取 Authorization 请求头，格式：Basic bXlhcHA6c2VjcmV0MTIz
        String authorization = request.getHeader("Authorization");

        // 提取 "Basic " 后面的部分（即 Base64 编码的字符串）
        // 参数 true 表示忽略大小写
        authorization = StrUtil.subAfter(authorization, "Basic ", true);

        // 判断是否成功提取到认证信息
        if (StringUtils.hasText(authorization)) {
            // Base64 解码，得到原始字符串：clientId:clientSecret
            authorization = Base64.decodeStr(authorization);

            // 以冒号 ":" 分割，提取 clientId（冒号前面的部分）
            // 参数 false 表示不包含分隔符本身
            clientId = StrUtil.subBefore(authorization, ":", false);

            // 提取 clientSecret（冒号后面的部分）
            clientSecret = StrUtil.subAfter(authorization, ":", false);
        } else {
            // === 方式 2：从请求参数中获取 ===
            // 如果 Header 中没有 Basic 认证信息，尝试从 URL 参数中获取
            clientId = request.getParameter("client_id");
            clientSecret = request.getParameter("client_secret");
        }

        // 验证：只有 clientId 和 clientSecret 都不为空时才返回结果
        if (StrUtil.isNotEmpty(clientId) && StrUtil.isNotEmpty(clientSecret)) {
            return new String[]{clientId, clientSecret};
        }

        // 如果认证信息不完整，返回 null
        return null;
    }

    /**
     * 发送 HTTP POST 请求，基于 {@link cn.hutool.http.HttpUtil} 实现
     *
     * 作用：支持自定义请求头和请求体。</p>
     *
     * 封装原因：Hutool 的 HttpUtil 默认方法不支持同时传递自定义 Headers 和 RequestBody，因此封装此方法以满足实际需求。</p>
     *
     * 适用场景：
     * - 调用第三方 API 时需要传递 Token、Content-Type 等自定义请求头
     * - 发送 JSON 或 XML 格式的请求体
     * - 需要设置特殊的 User-Agent、Referer 等 HTTP 头部信息
     *
     * 示例：
     * // 示例 1：发送 JSON 请求
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Content-Type", "application/json");
     * headers.put("Authorization", "Bearer abc123token");
     * String requestBody = "{\"name\":\"张三\",\"age\":13}";
     * String response = post("https://api.example.com/user", headers, requestBody);
     * // response = "{"success":true,"data":{"id":1001}}"
     *
     * // 示例 2：发送表单请求
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Content-Type", "application/x-www-form-urlencoded");
     * String requestBody = "username=admin&password=123456";
     * String response = post("https://api.example.com/login", headers, requestBody);
     *
     * @param url         请求的目标 URL
     * @param headers     请求头 Map
     * @param requestBody 请求体内容（通常是 JSON、XML 或表单数据）。
     * @return            HTTP 响应体内容（字符串格式）
     */
    public static String post(String url, Map<String, String> headers, String requestBody) {
        // try-with-resources 语法：自动关闭 HttpResponse 资源，防止内存泄漏
        try (HttpResponse response = HttpRequest.post(url)
                .addHeaders(headers)
                .body(requestBody)
                .execute()) {
            // 返回响应体的字符串内容
            return response.body();
        }
    }

    /**
     * 发送 HTTP GET 请求，基于 {@link cn.hutool.http.HttpUtil} 实现
     *
     * 作用：支持自定义请求头。
     *
     * 封装原因：Hutool 的 HttpUtil 默认方法不支持传递自定义 Headers，因此封装此方法以满足实际需求。
     *
     * 适用场景：
     * - 调用第三方 API 时需要传递 Token、Authorization 等认证信息
     * - 需要设置特殊的 User-Agent、Accept 等 HTTP 头部信息
     * - 获取需要鉴权的资源数据
     *
     * 示例：
     * // 示例 1：带 Token 的 GET 请求
     * Map<String, String> headers = new HashMap<>();
     * headers.put("Authorization", "Bearer abc123token");
     * String response = get("https://api.example.com/user/profile", headers);
     * // response = "{"id":1001,"name":"Ashore","email":"ashore@example.com"}"
     *
     * // 示例 2：自定义 User-Agent 的 GET 请求
     * Map<String, String> headers = new HashMap<>();
     * headers.put("User-Agent", "AshoreBot/1.0");
     * headers.put("Accept", "application/json");
     * String response = get("https://api.example.com/data", headers);
     *
     * @param url     请求的目标 URL
     * @param headers 请求头 Map
     * @return        HTTP 响应体内容（字符串格式）
     */
    public static String get(String url, Map<String, String> headers) {
        // try-with-resources 语法：自动关闭 HttpResponse 资源，防止内存泄漏
        try (HttpResponse response = HttpRequest.get(url)
                .addHeaders(headers)
                .execute()) {
            // 返回响应体的字符串内容
            return response.body();
        }
    }

}
