package com.example.ashore.framework.common.util.io;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;

import java.io.InputStream;

/**
 * IO 工具类
 *
 * 类作用:
 * 提供输入输出流的扩展操作方法，补充 Hutool 的 {@link cn.hutool.core.io.IoUtil} 中缺失的功能。
 * 主要用于简化从输入流中读取字符串内容的操作。
 *
 * 使用场景:
 * - 从 HTTP 请求的输入流中读取请求体内容
 * - 读取上传文件的输入流内容并转换为字符串
 * - 从网络连接或 Socket 流中读取 UTF-8 编码的文本数据
 * - 读取资源文件(如配置文件、JSON 文件)的文本内容
 * - 处理 API 响应流，如调用第三方接口返回的数据流
 *
 * 核心依赖库:
 * - Hutool (cn.hutool.core.io.IoUtil)    提供底层的流读取功能,将输入流转换为字节数组
 * - Hutool (cn.hutool.core.util.StrUtil) 提供字节数组到 UTF-8 字符串的转换功能
 *
 * 为什么需要这个类:
 * - Hutool 的 IoUtil 提供了 read() 方法读取字节数组，但没有直接提供读取 UTF-8 字符串的便捷方法。
 * - 本类封装了"读取字节数组 + 转换为 UTF-8 字符串"的组合操作，简化代码调用。
 */
public class IoUtils {

    /**
     * 从输入流中读取 UTF-8 编码的字符串内容
     *
     * 作用:
     * 读取输入流的全部内容，并将其解析为 UTF-8 编码的字符串返回。
     * 可以选择是否在读取完成后自动关闭输入流。
     *
     * 实现原理:
     * - 使用 IoUtil.read() 将输入流的全部内容读取为字节数组 (byte[])
     * - 使用 StrUtil.utf8Str() 将字节数组按 UTF-8 编码转换为字符串
     * - 根据 isClose 参数决定是否关闭输入流
     *
     * 方法执行流程:
     * InputStream (输入流)
     *    ↓
     * IoUtil.read()  ← 读取为字节数组
     *    ↓
     * byte[] (字节数组)
     *    ↓
     * StrUtil.utf8Str()  ← 转换为 UTF-8 字符串
     *    ↓
     * String (返回结果)
     *
     * 使用示例:
     * // 示例1：读取 HTTP 请求体并自动关闭流
     * InputStream requestBody = request.getInputStream();
     * String content = IoUtils.readUtf8(requestBody, true);
     *
     * // 示例2：读取文件内容但不关闭流(需要后续继续使用)
     * FileInputStream fis = new FileInputStream("data.txt");
     * String content = IoUtils.readUtf8(fis, false);
     * // ... 继续使用 fis 做其他操作
     * fis.close();  // 手动关闭
     *
     * @param in      输入流 (InputStream)，要读取内容的数据源。
     *                示例：HTTP 请求的输入流、文件输入流、网络 Socket 流等
     * @param isClose 是否在读取完成后自动关闭输入流 (boolean)。
     *                true - 读取完成后自动关闭流，适用于一次性读取的场景
     *                false - 读取后不关闭流，适用于需要继续使用流的场景
     *                示例：通常传 true 即可，除非需要多次读取或后续还要操作流
     * @return        从输入流中读取并解析的 UTF-8 字符串内容 (String)
     *                示例："{\"name\":\"张三\",\"age\":25}" 或 "Hello, World!" 或多行文本内容
     * @throws IORuntimeException 读取流时发生 IO 错误，可能原因：
     *                            - 输入流已关闭或不可读
     *                            - 网络连接中断(对于网络流)
     *                            - 文件不存在或无读取权限(对于文件流)
     *                            - 磁盘读取错误
     *                            注意：IORuntimeException 是运行时异常，调用方可以选择捕获或不捕获
     */
    public static String readUtf8(InputStream in, boolean isClose) throws IORuntimeException {
        // 实现步骤:
        // 1. IoUtil.read(in, isClose) - 将输入流读取为字节数组
        //    - in: 输入流对象
        //    - isClose: true 则读取后自动调用 in.close(),false 则保持流打开状态
        // 2. StrUtil.utf8Str() - 将字节数组按 UTF-8 编码转换为字符串
        //    - UTF-8 是一种变长字符编码,可以表示全球所有语言的字符
        //    - 适用于中文、英文、emoji 等各种字符的正确解析
        return StrUtil.utf8Str(IoUtil.read(in, isClose));
    }

}
