package com.example.ashore.framework.common.util.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * 基于 Guava 的本地内存缓存工具类
 * 数据存储在 JVM 内存中，而非 Redis 等外部缓存
 */
public class CacheUtils {

    /**
     * 最大缓存 10000 条数据
     */
    private static final Integer CACHE_MAX_SIZE = 10000;

    /**
     * 构建异步刷新的 LoadingCache 对象
     *
     * 使用场景：适合系统级数据
     * 原因：
     * 1. 无 ThreadLocal 依赖：后台线程池执行加载，无法访问业务线程的 ThreadLocal（如用户上下文、租户ID等）
     * 2. 允许短暂脏数据：系统配置、字典数据等可以容忍短时间的旧值
     * 3. 高并发场景：避免大量请求同时阻塞等待刷新
     *
     * 注意：如果你的缓存和 ThreadLocal 有关系，要么自己处理 ThreadLocal 的传递，要么使用 {@link #buildCache(Duration, CacheLoader)} 方法
     *
     * @param duration 过期时间
     * @param loader  CacheLoader 对象
     * @return LoadingCache 对象
     */
    public static <K, V> LoadingCache<K, V> buildAsyncReloadingCache(Duration duration, CacheLoader<K, V> loader) {
        return CacheBuilder.newBuilder()
                .maximumSize(CACHE_MAX_SIZE)
                // - 刷新机制：数据过期后，由后台线程池异步加载新数据
                // - 读取行为：读取过期数据时，立即返回旧值，同时触发后台刷新
                // - 线程阻塞：不阻塞业务线程
                .refreshAfterWrite(duration)
                .build(CacheLoader.asyncReloading(loader, Executors.newCachedThreadPool()));
    }

    /**
     * 构建同步刷新的 LoadingCache 对象
     *
     * 使用场景：适合用户相关数据
     * 原因：
     * 1. 保留 ThreadLocal：当前线程执行加载，可以访问用户上下文、租户ID、权限信息等
     * 2. 数据一致性要求高：用户权限、个人信息等不能返回旧值
     * 3. 低并发或可接受阻塞：单个用户访问自己数据，偶尔阻塞可接受
     *
     * @param duration 过期时间
     * @param loader  CacheLoader 对象
     * @return LoadingCache 对象
     */
    public static <K, V> LoadingCache<K, V> buildCache(Duration duration, CacheLoader<K, V> loader) {
        return CacheBuilder.newBuilder()
                .maximumSize(CACHE_MAX_SIZE)
                // - 刷新机制：数据过期后，由当前访问线程同步加载新数据
                // - 读取行为：读取过期数据时，阻塞等待加载完成再返回
                // - 线程阻塞：阻塞当前线程
                .refreshAfterWrite(duration)
                .build(loader);
    }

}
