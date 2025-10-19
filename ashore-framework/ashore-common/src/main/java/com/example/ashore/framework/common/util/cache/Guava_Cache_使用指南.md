# Guava Cache 使用指南

## 一、Guava 简介

**Guava** 是 Google 开源的 Java 核心库，提供了集合、缓存、并发、I/O 等常用功能的增强实现。其中 **Guava Cache** 是一个高性能的本地内存缓存组件。

### 1.1 Maven 依赖

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <!-- 版本由 Spring Boot 统一管理，无需指定 -->
</dependency>
```

### 1.2 核心特性

- **本地内存缓存**：数据存储在 JVM 堆内存中
- **自动加载**：通过 `CacheLoader` 自动加载缺失数据
- **过期策略**：支持基于时间、大小的过期策略
- **异步刷新**：支持异步后台刷新，避免阻塞
- **统计功能**：提供命中率、加载时间等统计信息

---

## 二、Guava Cache vs Redis

| 对比维度 | Guava Cache | Redis |
|---------|-------------|-------|
| **存储位置** | JVM 本地内存 | 独立进程/远程服务器 |
| **访问速度** | 极快（纳秒级） | 较快（毫秒级，受网络影响） |
| **数据共享** | 单机不共享 | 多实例共享 |
| **容量限制** | 受 JVM 堆内存限制 | 可达 GB/TB 级 |
| **持久化** | 不支持（进程重启丢失） | 支持 RDB/AOF 持久化 |
| **分布式支持** | 不支持 | 天然支持分布式缓存 |
| **运维成本** | 无需额外部署 | 需要部署和维护 Redis |

### 2.1 Guava Cache 适用场景

✅ **适合使用 Guava Cache 的场景**：
1. **热点数据缓存**：系统配置、字典数据、权限规则等读多写少的数据
2. **计算结果缓存**：复杂计算、加密解密、正则匹配等耗时操作的结果
3. **单机应用**：不需要多实例共享数据
4. **低延迟要求**：需要纳秒级响应速度
5. **临时数据**：允许进程重启后丢失的数据

❌ **不适合使用 Guava Cache 的场景**：
1. **分布式环境**：多实例需要共享缓存数据
2. **大容量数据**：数据量超过 JVM 堆内存承受范围
3. **持久化需求**：数据不能丢失
4. **跨服务共享**：不同微服务需要访问相同缓存

### 2.2 组合使用策略

在实际项目中，通常采用 **两级缓存** 架构：

```
请求 → Guava Cache (L1) → Redis (L2) → Database
        ↓ 命中返回          ↓ 命中返回      ↓ 查询并缓存
```

- **L1（Guava）**：极热数据，纳秒级响应
- **L2（Redis）**：热数据，多实例共享
- **DB**：冷数据，持久化存储

---

## 三、业务场景示例：商品库存查询系统

### 3.1 场景描述

**电商系统中，商品库存数据需要高频访问，但允许短暂延迟更新。**

- **全局商品库存**：所有用户看到相同库存，适合异步刷新
- **用户购物车**：需要基于当前用户上下文查询，适合同步刷新

---

## 四、代码示例

### 4.1 异步刷新 - 全局商品库存缓存

```java
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * 商品库存服务
 */
public class ProductStockService {

    private final ProductStockMapper stockMapper;

    /**
     * 异步刷新缓存 - 适用于全局商品库存
     * 特点：后台线程刷新，业务线程不阻塞，可能返回旧值
     */
    private final LoadingCache<Long, Integer> asyncStockCache;

    public ProductStockService(ProductStockMapper stockMapper) {
        this.stockMapper = stockMapper;

        // 构建异步刷新缓存
        this.asyncStockCache = CacheBuilder.newBuilder()
                .maximumSize(10000)                              // 最大缓存 10000 个商品
                .refreshAfterWrite(Duration.ofMinutes(5))       // 5 分钟后刷新
                .build(CacheLoader.asyncReloading(              // 异步刷新
                        new CacheLoader<Long, Integer>() {
                            @Override
                            public Integer load(Long productId) throws Exception {
                                System.out.println("[异步加载] 从数据库加载商品库存: " + productId
                                        + ", 线程: " + Thread.currentThread().getName());
                                // 模拟数据库查询耗时
                                Thread.sleep(2000);
                                return stockMapper.getStockByProductId(productId);
                            }
                        },
                        Executors.newCachedThreadPool()             // 使用线程池异步加载
                ));
    }

    /**
     * 获取商品库存（异步刷新版本）
     * 场景：商品详情页展示库存，允许短暂的数据延迟
     */
    public Integer getProductStock(Long productId) {
        try {
            Integer stock = asyncStockCache.get(productId);
            System.out.println("[异步缓存] 获取商品库存: " + productId
                    + ", 库存: " + stock
                    + ", 线程: " + Thread.currentThread().getName());
            return stock;
        } catch (Exception e) {
            throw new RuntimeException("获取库存失败", e);
        }
    }
}
```

### 4.2 同步刷新 - 用户购物车库存检查

```java
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.time.Duration;

/**
 * 购物车服务
 */
public class ShoppingCartService {

    private final ProductStockMapper stockMapper;

    /**
     * 同步刷新缓存 - 适用于用户购物车库存检查
     * 特点：当前线程刷新，需要访问 ThreadLocal 中的用户/租户信息
     */
    private final LoadingCache<Long, Integer> syncStockCache;

    public ShoppingCartService(ProductStockMapper stockMapper) {
        this.stockMapper = stockMapper;

        // 构建同步刷新缓存
        this.syncStockCache = CacheBuilder.newBuilder()
                .maximumSize(10000)
                .refreshAfterWrite(Duration.ofMinutes(3))       // 3 分钟后刷新
                .build(new CacheLoader<Long, Integer>() {       // 同步刷新
                    @Override
                    public Integer load(Long productId) throws Exception {
                        // 从 ThreadLocal 获取当前用户上下文
                        Long userId = UserContextHolder.getUserId();
                        Long tenantId = TenantContextHolder.getTenantId();

                        System.out.println("[同步加载] 从数据库加载商品库存: " + productId
                                + ", 用户: " + userId
                                + ", 租户: " + tenantId
                                + ", 线程: " + Thread.currentThread().getName());

                        // 模拟数据库查询耗时
                        Thread.sleep(2000);

                        // 需要基于租户隔离的库存查询
                        return stockMapper.getStockByProductIdAndTenant(productId, tenantId);
                    }
                });
    }

    /**
     * 检查购物车商品库存（同步刷新版本）
     * 场景：用户下单前检查库存，必须获取最新数据，不能返回旧值
     */
    public boolean checkCartStock(Long productId, Integer quantity) {
        try {
            Integer stock = syncStockCache.get(productId);
            System.out.println("[同步缓存] 检查库存: " + productId
                    + ", 需要: " + quantity
                    + ", 可用: " + stock
                    + ", 线程: " + Thread.currentThread().getName());
            return stock >= quantity;
        } catch (Exception e) {
            throw new RuntimeException("检查库存失败", e);
        }
    }
}
```

### 4.3 ThreadLocal 上下文模拟

```java
/**
 * 用户上下文 ThreadLocal
 */
public class UserContextHolder {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}

/**
 * 租户上下文 ThreadLocal
 */
public class TenantContextHolder {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
```

### 4.4 测试代码

```java
public class CacheTest {

    public static void main(String[] args) throws Exception {
        testAsyncCache();
        System.out.println("\n" + "=".repeat(80) + "\n");
        testSyncCache();
    }

    /**
     * 测试异步刷新缓存
     */
    private static void testAsyncCache() throws Exception {
        System.out.println("【异步刷新缓存测试】");
        ProductStockService service = new ProductStockService(new MockProductStockMapper());

        // 第一次访问 - 同步加载
        System.out.println("\n>>> 时间 0s: 第一次访问商品 1001");
        service.getProductStock(1001L);

        // 5 秒内再次访问 - 命中缓存
        Thread.sleep(2000);
        System.out.println("\n>>> 时间 2s: 再次访问商品 1001（缓存未过期）");
        service.getProductStock(1001L);

        // 6 分钟后访问 - 触发异步刷新
        Thread.sleep(4000);
        System.out.println("\n>>> 时间 6s: 访问商品 1001（缓存已过期，触发异步刷新）");
        service.getProductStock(1001L);  // 立即返回旧值

        // 等待后台刷新完成
        Thread.sleep(3000);
        System.out.println("\n>>> 时间 9s: 再次访问商品 1001（已刷新完成）");
        service.getProductStock(1001L);
    }

    /**
     * 测试同步刷新缓存
     */
    private static void testSyncCache() throws Exception {
        System.out.println("【同步刷新缓存测试】");
        ShoppingCartService service = new ShoppingCartService(new MockProductStockMapper());

        // 设置当前用户上下文
        UserContextHolder.setUserId(10001L);
        TenantContextHolder.setTenantId(1L);

        try {
            // 第一次访问 - 同步加载
            System.out.println("\n>>> 时间 0s: 第一次检查商品 2001 库存");
            service.checkCartStock(2001L, 5);

            // 2 秒内再次访问 - 命中缓存
            Thread.sleep(2000);
            System.out.println("\n>>> 时间 2s: 再次检查商品 2001 库存（缓存未过期）");
            service.checkCartStock(2001L, 5);

            // 4 分钟后访问 - 触发同步刷新
            Thread.sleep(2000);
            System.out.println("\n>>> 时间 4s: 检查商品 2001 库存（缓存已过期，触发同步刷新）");
            service.checkCartStock(2001L, 5);  // 阻塞等待加载完成

        } finally {
            UserContextHolder.clear();
            TenantContextHolder.clear();
        }
    }
}

/**
 * 模拟数据库 Mapper
 */
class MockProductStockMapper implements ProductStockMapper {
    private int loadCount = 0;

    @Override
    public Integer getStockByProductId(Long productId) {
        return 100 + (loadCount++);  // 每次返回不同值，模拟库存变化
    }

    @Override
    public Integer getStockByProductIdAndTenant(Long productId, Long tenantId) {
        return 200 + (loadCount++);  // 每次返回不同值，模拟库存变化
    }
}
```

---

## 五、刷新流程图解

### 5.1 异步刷新流程

```
┌─────────────────────────────────────────────────────────────────┐
│ 时间轴：异步刷新 (refreshAfterWrite = 5分钟)                    │
└─────────────────────────────────────────────────────────────────┘

时间 0s
  ┌──────────────┐
  │ 用户请求     │ ──> Cache MISS ──> 同步加载(2s) ──> 返回 V1 (100)
  └──────────────┘                                        │
                                                          ▼
                                                    [缓存: V1=100]

时间 2s
  ┌──────────────┐
  │ 用户请求     │ ──> Cache HIT ──> 直接返回 V1 (100)
  └──────────────┘                    ▲
                                      │
                                [缓存: V1=100]
                                (距离写入 2s，未过期)

时间 6s (refreshAfterWrite 触发点)
  ┌──────────────┐
  │ 用户请求     │ ──> 检测到过期 ──> 立即返回 V1 (100)  ⚡ 用户无感知
  └──────────────┘                          │
                                            │
                                            ▼
                        ┌───────────────────────────────┐
                        │ 后台线程池开始异步加载        │
                        │ Thread: pool-1-thread-1       │
                        │ 耗时 2s                       │
                        └───────────────────────────────┘

时间 7s (刷新进行中)
  ┌──────────────┐
  │ 其他用户请求 │ ──> 仍返回旧值 V1 (100)  ⚡ 不阻塞
  └──────────────┘              │
                                │
                          [缓存: V1=100]
                          [后台加载中...]

时间 8s (刷新完成)
                          ┌────────────────┐
                          │ 后台加载完成   │
                          │ 缓存更新: V2   │
                          └────────────────┘
                                  │
                                  ▼
                          [缓存: V2=101]

时间 9s
  ┌──────────────┐
  │ 用户请求     │ ──> Cache HIT ──> 返回 V2 (101)  ✅ 已更新
  └──────────────┘

```

**关键点**：
- ✅ **用户请求不阻塞**：第 6s 时立即返回旧值，用户体验好
- ✅ **后台刷新**：由独立线程池执行，不占用业务线程
- ⚠️ **短暂脏数据**：6s-8s 期间返回旧值，需业务可容忍

---

### 5.2 同步刷新流程

```
┌─────────────────────────────────────────────────────────────────┐
│ 时间轴：同步刷新 (refreshAfterWrite = 3分钟)                    │
└─────────────────────────────────────────────────────────────────┘

时间 0s
  ┌──────────────────────┐
  │ 用户A请求            │ ──> Cache MISS ──> 同步加载(2s) ──> 返回 V1 (200)
  │ ThreadLocal:         │                      ▲
  │ - userId = 10001     │                      │ 阻塞 2s
  │ - tenantId = 1       │                      │
  └──────────────────────┘                      ▼
                                          [缓存: V1=200]

时间 2s
  ┌──────────────────────┐
  │ 用户A请求            │ ──> Cache HIT ──> 直接返回 V1 (200)
  │ ThreadLocal:         │                    ▲
  │ - userId = 10001     │                    │
  │ - tenantId = 1       │              [缓存: V1=200]
  └──────────────────────┘              (距离写入 2s，未过期)

时间 4s (refreshAfterWrite 触发点)
  ┌──────────────────────┐
  │ 用户A请求            │ ──> 检测到过期 ──> 当前线程同步加载(2s) ──> 返回 V2 (201)
  │ ThreadLocal:         │                      ▲                        │
  │ - userId = 10001     │                      │ 阻塞 2s  ⚠️           │
  │ - tenantId = 1       │                      │ 可访问 ThreadLocal     │
  └──────────────────────┘                      │                        ▼
                                                │                  [缓存: V2=201]
                                          执行线程: main
                                          ✅ 能获取到 userId, tenantId

时间 4.5s (同步加载进行中)
  ┌──────────────────────┐
  │ 用户B请求            │ ──> 返回旧值 V1 (200)  ⚡ 其他线程不阻塞
  │ (不同线程)           │              │
  └──────────────────────┘        [缓存: V1=200]
                                  [用户A线程加载中...]

时间 6s (加载完成)
  ┌──────────────────────┐
  │ 用户A请求            │ ──> Cache HIT ──> 返回 V2 (201)  ✅ 已更新
  │ ThreadLocal:         │
  │ - userId = 10001     │
  │ - tenantId = 1       │
  └──────────────────────┘

```

**关键点**：
- ⚠️ **首次请求线程阻塞**：第 4s 时用户 A 阻塞 2s 等待加载
- ✅ **保留 ThreadLocal**：加载过程可以访问 userId、tenantId
- ✅ **其他线程不阻塞**：第 4.5s 时用户 B 仍返回旧值
- ✅ **数据一致性**：不允许返回其他租户的数据

---

## 六、对比总结

### 6.1 流程对比表

| 对比项 | 异步刷新 | 同步刷新 |
|-------|---------|---------|
| **刷新触发者** | 后台线程池 | 当前请求线程 |
| **业务线程阻塞** | 否（立即返回旧值） | 是（首次请求阻塞） |
| **ThreadLocal 访问** | ❌ 不可用（线程池线程） | ✅ 可用（当前线程） |
| **数据一致性** | 允许短暂脏数据 | 加载完成前其他线程仍可能读到旧值 |
| **适用场景** | 全局配置、字典、系统级数据 | 用户权限、租户隔离数据 |

### 6.2 选择建议

**使用异步刷新的条件**：
1. ✅ 数据全局共享，不依赖用户/租户上下文
2. ✅ 可以容忍短暂的数据延迟（秒级）
3. ✅ 高并发场景，要求低延迟响应

**使用同步刷新的条件**：
1. ✅ 需要从 ThreadLocal 获取上下文（用户ID、租户ID、权限等）
2. ✅ 数据一致性要求高，不能返回其他用户/租户的数据
3. ✅ 单用户访问自己的数据，可接受偶尔阻塞

---

## 七、最佳实践

### 7.1 缓存大小设置

```java
// 根据业务数据量设置合理的最大缓存数
.maximumSize(10000)  // 商品缓存：10000 个热门商品
.maximumSize(5000)   // 用户权限：5000 个活跃用户
```

### 7.2 刷新时间设置

```java
// 根据数据变更频率设置刷新时间
.refreshAfterWrite(Duration.ofMinutes(10))  // 系统配置：10 分钟
        .refreshAfterWrite(Duration.ofMinutes(5))   // 商品库存：5 分钟
        .refreshAfterWrite(Duration.ofMinutes(3))   // 用户权限：3 分钟
```

### 7.3 异常处理

```java
public Integer getStock(Long productId) {
    try {
        return cache.get(productId);
    } catch (ExecutionException e) {
        log.error("缓存加载失败: productId={}", productId, e);
        // 降级方案：直接查询数据库
        return stockMapper.getStockByProductId(productId);
    }
}
```

### 7.4 缓存统计

```java
// 启用统计功能
LoadingCache<Long, Integer> cache = CacheBuilder.newBuilder()
                .recordStats()  // 开启统计
                .build(loader);

// 获取统计信息
CacheStats stats = cache.stats();
System.out.println("命中率: " + stats.hitRate());
        System.out.println("平均加载时间: " + stats.averageLoadPenalty() + "ns");
```

---

## 八、常见问题

### Q1: 为什么异步刷新不能访问 ThreadLocal？

**答**：异步刷新使用独立的线程池执行加载，线程池的线程无法访问业务线程的 ThreadLocal 变量。

```java
// 业务线程
UserContextHolder.setUserId(10001L);  // 设置在 Thread-1 的 ThreadLocal

// 缓存加载线程
Executors.newCachedThreadPool()      // 在 pool-1-thread-1 执行
UserContextHolder.getUserId()        // ❌ 返回 null，因为不是同一个线程
```

### Q2: refreshAfterWrite 和 expireAfterWrite 的区别？

| 方法 | 行为 | 读取过期数据 |
|-----|------|-------------|
| `refreshAfterWrite` | 刷新数据（异步/同步） | 返回旧值（不阻塞/阻塞首次） |
| `expireAfterWrite` | 删除数据 | 重新同步加载（阻塞所有请求） |

### Q3: 如何手动刷新缓存？

```java
// 手动刷新单个 key
cache.refresh(productId);

// 手动失效单个 key
cache.invalidate(productId);

// 清空所有缓存
cache.invalidateAll();
```

---

## 九、CacheUtils 在本项目中的使用

### 9.1 CacheUtils 工具类说明

本项目封装了 `CacheUtils` 工具类，提供了两个核心方法：

```java
/**
 * 构建异步刷新的 LoadingCache 对象
 *
 * 注意：如果你的缓存和 ThreadLocal 有关系,要么自己处理 ThreadLocal 的传递，
 *      要么使用 buildCache 方法
 *
 * 或者简单理解：
 * 1、和"人"相关的,使用 buildCache 方法
 * 2、和"全局"、"系统"相关的,使用当前缓存方法
 */
public static <K, V> LoadingCache<K, V> buildAsyncReloadingCache(Duration duration, CacheLoader<K, V> loader)

/**
 * 构建同步刷新的 LoadingCache 对象
 */
public static <K, V> LoadingCache<K, V> buildCache(Duration duration, CacheLoader<K, V> loader)
```

### 9.2 实际使用场景

#### 场景 1：字典数据缓存（异步刷新��

**位置**：`DictFrameworkUtils.java`
**特点**：全局共享的字典数据，无需访问 ThreadLocal

```java
/**
 * 针对 dictType 的字段数据缓存
 * 场景：系统字典数据（如：性别、状态等），所有用户共享
 */
private static final LoadingCache<String, List<DictDataRespDTO>> GET_DICT_DATA_CACHE =
    CacheUtils.buildAsyncReloadingCache(
        Duration.ofMinutes(1L), // 过期时间 1 分钟
        new CacheLoader<String, List<DictDataRespDTO>>() {
            @Override
            public List<DictDataRespDTO> load(String dictType) {
                // 远程调用获取字典数据
                return dictDataApi.getDictDataList(dictType).getCheckedData();
            }
        });

// 使用示例
@SneakyThrows
public static String parseDictDataLabel(String dictType, String value) {
    List<DictDataRespDTO> dictDatas = GET_DICT_DATA_CACHE.get(dictType);
    DictDataRespDTO dictData = CollUtil.findOne(dictDatas,
        data -> Objects.equals(data.getValue(), value));
    return dictData != null ? dictData.getLabel() : null;
}
```

**为什么使用异步刷新**：
- ✅ 字典数据全局共享，不依赖用户上下文
- ✅ 可容忍短暂的数据延迟
- ✅ 高频访问，需要极致性能

---

#### 场景 2：租户列表缓存（异步刷新）

**位置**：`TenantFrameworkServiceImpl.java`
**特点**：全局租户列表，无需用户上下文

```java
/**
 * 针对 getTenantIds() 的缓存
 * 场景：缓存系统所有租户 ID 列表，用于租户校验
 */
private final LoadingCache<Object, List<Long>> getTenantIdsCache =
    buildAsyncReloadingCache(
        Duration.ofMinutes(1L), // 过期时间 1 分钟
        new CacheLoader<Object, List<Long>>() {
            @Override
            public List<Long> load(Object key) {
                return tenantApi.getTenantIdList().getCheckedData();
            }
        });

/**
 * 针对 validTenant(Long) 的缓存
 * 场景：缓存租户是否有效的校验结果
 */
private final LoadingCache<Long, CommonResult<Boolean>> validTenantCache =
    buildAsyncReloadingCache(
        Duration.ofMinutes(1L), // 过期时间 1 分钟
        new CacheLoader<Long, CommonResult<Boolean>>() {
            @Override
            public CommonResult<Boolean> load(Long id) {
                return tenantApi.validTenant(id);
            }
        });

// 使用示例
@Override
@SneakyThrows
public List<Long> getTenantIds() {
    return getTenantIdsCache.get(Boolean.TRUE);
}

@Override
@SneakyThrows
public void validTenant(Long id) {
    validTenantCache.get(id).checkError();
}
```

**为什么使用异步刷新**：
- ✅ 租户数据系统级共享
- ✅ 变更频率低，可容忍缓存延迟
- ✅ 高频校验，需要快速响应

---

#### 场景 3：用户权限缓存（同步刷新）

**位置**：`SecurityFrameworkServiceImpl.java`
**特点**：需要访问当前登录用户的 ThreadLocal 上下文

```java
/**
 * 针对 hasAnyRoles 的缓存
 * 场景：缓存用户角色校验结果，基于当前登录用户
 */
private final LoadingCache<KeyValue<Long, List<String>>, Boolean> hasAnyRolesCache =
    buildCache(
        Duration.ofMinutes(1L), // 过期时间 1 分钟
        new CacheLoader<KeyValue<Long, List<String>>, Boolean>() {
            @Override
            public Boolean load(KeyValue<Long, List<String>> key) {
                // 远程调用权限 API，需要在当前线程执行以访问 ThreadLocal
                return permissionApi.hasAnyRoles(
                    key.getKey(),
                    key.getValue().toArray(new String[0])
                ).getCheckedData();
            }
        });

/**
 * 针对 hasAnyPermissions 的缓存
 * 场景：缓存用户权限校验结果，基于当前登录用户
 */
private final LoadingCache<KeyValue<Long, List<String>>, Boolean> hasAnyPermissionsCache =
    buildCache(
        Duration.ofMinutes(1L), // 过期时间 1 分钟
        new CacheLoader<KeyValue<Long, List<String>>, Boolean>() {
            @Override
            public Boolean load(KeyValue<Long, List<String>> key) {
                return permissionApi.hasAnyPermissions(
                    key.getKey(),
                    key.getValue().toArray(new String[0])
                ).getCheckedData();
            }
        });

// 使用示例
@Override
@SneakyThrows
public boolean hasAnyPermissions(String... permissions) {
    // 特殊：跨租户访问
    if (skipPermissionCheck()) {
        return true;
    }

    // 权限校验 - 从 SecurityFrameworkUtils 的 ThreadLocal 获取当前用户 ID
    Long userId = getLoginUserId();
    if (userId == null) {
        return false;
    }
    return hasAnyPermissionsCache.get(new KeyValue<>(userId, Arrays.asList(permissions)));
}
```

**为什么使用同步刷新**：
- ✅ 权限校验可能需要访问 ThreadLocal 中的用户上下文
- ✅ 权限数据和具体用户强相关
- ✅ 虽然示例中 userId 作为 key 传入，但 permissionApi 内部可能依赖 ThreadLocal
- ✅ 单个用户请求阻塞可接受，保证数据一致性

---

### 9.3 使用场景总结

| 缓存方法 | 适用场景 | 项目实际应用 |
|---------|---------|-------------|
| **buildAsyncReloadingCache** | 全局、系统级数据 | • 字典数据缓存<br>• 租户列表缓存<br>• 配置参数缓存<br>• 社交客户端配置 |
| **buildCache** | 用户、租户级数据 | • 用户权限缓存<br>• 用户角色缓存<br>• Token 校验缓存 |

### 9.4 使用建议

**选择 buildAsyncReloadingCache 的条件**：
1. ✅ 数据全局共享（如系统配置、字典、租户列表）
2. ✅ 不依赖 ThreadLocal（如当前用户、当前租户上下文）
3. ✅ 可容忍短暂数据延迟（秒级）
4. ✅ 高并发访问场景

**选择 buildCache 的条件**：
1. ✅ 数据和用户/租户相关
2. ✅ 加载逻辑需要访问 ThreadLocal
3. ✅ 对数据一致性要求高
4. ✅ 单用户请求可接受短暂阻塞

### 9.5 常见误区

❌ **错误用法**：对需要 ThreadLocal 的场景使用异步刷新

```java
// ❌ 错误示例：用户权限校验使用异步刷新
private final LoadingCache<Long, Boolean> cache = buildAsyncReloadingCache(
    Duration.ofMinutes(1L),
    new CacheLoader<Long, Boolean>() {
        @Override
        public Boolean load(Long userId) {
            // ❌ 后台线程池无法访问请求线程的 ThreadLocal
            Long currentUserId = SecurityContextHolder.getUserId(); // 返回 null
            return permissionApi.checkPermission(userId);
        }
    });
```

✅ **正确用法**：改用同步刷新

```java
// ✅ 正确示例：用户权限校验使用同步刷新
private final LoadingCache<Long, Boolean> cache = buildCache(
    Duration.ofMinutes(1L),
    new CacheLoader<Long, Boolean>() {
        @Override
        public Boolean load(Long userId) {
            // ✅ 当前线程可以访问 ThreadLocal
            Long currentUserId = SecurityContextHolder.getUserId();
            return permissionApi.checkPermission(userId);
        }
    });
```

---

## 十、参考资料

- [Guava 官方文档](https://github.com/google/guava/wiki/CachesExplained)
- [Guava Cache 源码解析](https://github.com/google/guava)

**文档版本**: v1.1
**最后更新**: 2025-10-19
**维护者**: Ashore 团队
