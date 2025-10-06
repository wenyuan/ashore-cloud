package com.example.ashore.framework.web.core.filter;

import com.example.ashore.framework.common.util.servlet.ServletUtils;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

/**
 * Request Body 缓存 Wrapper
 * 包装原始请求，只重写需要改变的方法，其他方法委托给原始对象
 *
 * 设计原理：
 * - 继承 HttpServletRequestWrapper 使用装饰器模式包装原始请求
 * - 在构造时一次性读取请求体，缓存到字节数组中
 * - 重写 getInputStream() 和 getReader() 方法，每次从缓存中创建新流返回
 * - 实现请求体的可重复读取，解决原生 HttpServletRequest 流只能读一次的限制
 */
public class CacheRequestBodyWrapper extends HttpServletRequestWrapper {

    /**
     * 缓存请求体内容（字节数组，存储请求体的原始字节数据）
     * - 使用 final 修饰：一旦在构造方法中赋值，不可再改变，保证线程安全
     * - 使用 byte[] 类型：保存原始字节数据，避免编码问题
     * - 私有变量：外部无法直接访问，只能通过 getInputStream() 等方法间接读取
     */
    private final byte[] body;

    /**
     * 构造方法：创建包装器并缓存请求体
     *
     * @param request 原始 HTTP 请求对象
     */
    public CacheRequestBodyWrapper(HttpServletRequest request) {
        super(request);  // 调用父类构造，保存原始请求对象的引用
        body = ServletUtils.getBodyBytes(request);  // 读取一次原始请求体的所有字节，后续所有读取都从 body 数组中获取
    }

    /**
     * 获取字符流读取器（重写方法）
     * - 原始方法直接读取底层流，只能读一次
     * - 重写后基于缓存的 body 创建流，可以多次调用
     *
     * 实现原理：
     * 1. this.getInputStream()：调用下面的 getInputStream() 方法，返回基于 body 数组的字节流
     * 2. InputStreamReader：将字节流转换为字符流（处理字符编码）
     * 3. BufferedReader：包装成缓冲字符流，提高读取效率，支持 readLine() 等便捷方法
     *
     * 使用场景：
     * - 当代码调用 request.getReader() 读取请求体时会调用此方法
     * - 每次调用都会创建新的 Reader，从头开始读取缓存内容
     *
     * @return BufferedReader 字符流读取器
     */
    @Override
    public BufferedReader getReader() {
        // 基于缓存的字节流创建字符流，每次调用都创建新对象，实现可重复读取
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }

    /**
     * 获取请求体长度（int 类型，重写方法）
     * - 原始方法返回原始流的长度，可能在流被读取后变得不准确
     * - 重写后直接返回缓存数组的长度，保证准确性
     * - 某些框架或组件可能依赖此方法判断是否有请求体内容
     *
     * 注意：
     * - int 类型最大值约 2GB，如果请求体超过此大小会溢出（实际应用中很少有超过 2GB 的 JSON 请求体）
     * - 对于大文件上传场景，不会使用此缓存过滤器（已在 shouldNotFilter 中排除）
     *
     * @return 请求体字节数
     */
    @Override
    public int getContentLength() {
        return body.length;
    }

    /**
     * 获取请求体长度（long 类型，重写方法）
     * 支持更大的长度，约 9EB（百亿亿字节），理论上不会溢出
     * Servlet 3.1+ 规范推荐使用此方法替代 getContentLength()
     *
     * @return 请求体字节数
     */
    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    /**
     * 获取字节流输入流（重写方法，核心实现）
     *   - 这是实现可重复读取的核心方法
     *   - 原始方法返回底层 Socket 流，只能读一次
     *   - 重写后每次调用都基于缓存的 body 数组创建新流，可以多次读取
     * 实现原理：
     *   - 基于缓存的 body 数组创建 ByteArrayInputStream
     *   - 创建匿名内部类实现 ServletInputStream 接口
     *   - 所有读取操作委托给 ByteArrayInputStream
     *   - 每次调用此方法都创建新的 ByteArrayInputStream，指针从头开始
     * 为什么使用匿名内部类：
     *   - 需要实现 ServletInputStream 抽象类（不是普通的 InputStream）
     *   - 匿名内部类可以访问外部的 final 变量（body 和 inputStream）
     *   - 避免创建额外的类文件，代码更紧凑
     *
     * @return ServletInputStream 字节流输入流
     */
    @Override
    public ServletInputStream getInputStream() {
        // 每次调用都创建新的 ByteArrayInputStream，从 body 数组的第一个字节开始读取
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(body);

        // 返回 ServletInputStream 的匿名内部类实现
        return new ServletInputStream() {

            // 读取一个字节，返回 0-255 的值，流结束返回 -1
            @Override
            public int read() {
                return inputStream.read();
            }

            // 判断流是否已读取完成，用于异步读取场景，判断是否还有数据
            // 实际应该根据 ByteArrayInputStream 的状态判断
            // 由于本项目不使用异步读取，简化返回 false 不影响功能
            @Override
            public boolean isFinished() {
                return false;
            }

            // 判断流是否准备好读取
            // 用于异步读取场景，判断是否可以立即读取而不阻塞
            // ByteArrayInputStream 基于内存，理论上总是准备好的
            // 由于本项目不使用异步读取，简化返回 false 不影响功能
            @Override
            public boolean isReady() {
                return false;
            }

            // 设置异步读取监听器
            // 用于异步读取场景，当数据可读时触发回调
            // 如果实现异步读取，需要在数据可读时调用 listener.onDataAvailable()
            // 本项目采用同步读取模式，不需要异步监听器，空实现不影响同步读取功能
            @Override
            public void setReadListener(ReadListener readListener) {}

            // 获取可读取的字节数
            // 某些框架或工具可能依赖此方法判断数据量
            // ByteArrayInputStream 基于固定长度的字节数组，body.length 就是总的可读字节数
            // 即使已读取部分数据，此方法仍返回总长度（简化实现）
            // 严格实现应该返回剩余未读字节数，但对本项目影响不大
            @Override
            public int available() {
                return body.length;
            }

        };
    }

}
