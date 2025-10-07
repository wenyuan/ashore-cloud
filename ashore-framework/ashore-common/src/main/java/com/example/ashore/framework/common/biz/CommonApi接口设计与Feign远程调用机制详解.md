# CommonApi 接口设计与 Feign 远程调用机制详解

## 文档概述

本文档以 `DictDataCommonApi.java` 为例，详细讲解本项目中 `ashore-common/biz` 包下各个 CommonApi 接口的设计思想、实现机制以及 Feign 远程调用的底层原理。

---

## 一、CommonApi 接口是什么

### 1.1 什么是 CommonApi

在 `ashore-framework/ashore-common/src/main/java/com/example/ashore/framework/common/biz` 目录下，有一系列以 `CommonApi` 结尾的接口，例如：

```
biz/
├── infra/
│   └── logger/
│       ├── ApiAccessLogCommonApi.java      # API访问日志接口
│       └── ApiErrorLogCommonApi.java       # API错误日志接口
└── system/
    ├── dict/
    │   └── DictDataCommonApi.java          # 字典数据接口（本文重点示例）
    ├── logger/
    │   └── OperateLogCommonApi.java        # 操作日志接口
    ├── oauth2/
    │   └── OAuth2TokenCommonApi.java       # OAuth2令牌接口
    ├── permission/
    │   └── PermissionCommonApi.java        # 权限接口
    └── tenant/
        └── TenantCommonApi.java            # 租户接口
```

这些接口统称为 **CommonApi 接口**，它们是一组**最基础的 RPC 接口**，用于提供跨模块的核心功能调用。

### 1.2 DictDataCommonApi 示例

我们以字典数据接口为例来理解：

```java
package com.example.ashore.framework.common.biz.system.dict;

import com.example.ashore.framework.common.biz.system.dict.dto.DictDataRespDTO;
import com.example.ashore.framework.common.enums.RpcConstants;
import com.example.ashore.framework.common.pojo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = RpcConstants.SYSTEM_NAME, primary = false)
@Tag(name = "RPC 服务 - 字典数据")
public interface DictDataCommonApi {

    String PREFIX = RpcConstants.SYSTEM_PREFIX + "/dict-data";

    @GetMapping(PREFIX + "/list")
    @Operation(summary = "获得指定字典类型的字典数据列表")
    @Parameter(name = "dictType", description = "字典类型", example = "SEX", required = true)
    ApiResponse<List<DictDataRespDTO>> getDictDataList(@RequestParam("dictType") String dictType);

}
```

**关键要素：**
1. `@FeignClient(name = RpcConstants.SYSTEM_NAME)` - 声明这是一个 Feign 远程调用接口，目标服务是 `system-server`
2. `@GetMapping` - 定义 HTTP 请求路径和方法
3. 接口只定义方法签名，不包含实现

---

## 二、CommonApi 的作用与使用者

### 2.1 谁在使用 CommonApi？

CommonApi 接口**主要被 Framework 层的工具类使用**，而不是业务模块直接使用。

**使用场景示例：**

#### 示例 1：DictFrameworkUtils（字典工具类）

```java
// 文件位置：ashore-framework/ashore-spring-boot-starter-excel/src/main/java/.../dict/core/DictFrameworkUtils.java

@Slf4j
public class DictFrameworkUtils {

    private static DictDataCommonApi dictDataApi;  // ← 使用 CommonApi

    /**
     * 针对 dictType 的字段数据缓存
     */
    private static final LoadingCache<String, List<DictDataRespDTO>> GET_DICT_DATA_CACHE =
        CacheUtils.buildAsyncReloadingCache(
            Duration.ofMinutes(1L),
            new CacheLoader<String, List<DictDataRespDTO>>() {
                @Override
                public List<DictDataRespDTO> load(String dictType) {
                    // 通过 CommonApi 获取字典数据
                    return dictDataApi.getDictDataList(dictType).getCheckedData();
                }
            });

    public static void init(DictDataCommonApi dictDataApi) {
        DictFrameworkUtils.dictDataApi = dictDataApi;
        log.info("[init][初始化 DictFrameworkUtils 成功]");
    }

    @SneakyThrows
    public static String parseDictDataLabel(String dictType, String value) {
        List<DictDataRespDTO> dictDatas = GET_DICT_DATA_CACHE.get(dictType);
        DictDataRespDTO dictData = CollUtil.findOne(dictDatas,
            data -> Objects.equals(data.getValue(), value));
        return dictData != null ? dictData.getLabel() : null;
    }
}
```

**工作流程：**
1. Excel 导出时需要将字典值（如 `1`）转换为字典标签（如 `男`）
2. `DictFrameworkUtils` 通过 `DictDataCommonApi` 远程调用 `system-server` 获取字典数据
3. 将字典数据缓存 1 分钟，避免重复调用

#### 示例 2：AshoreDictAutoConfiguration（自动配置）

```java
// 文件位置：ashore-framework/ashore-spring-boot-starter-excel/src/main/java/.../dict/config/AshoreDictAutoConfiguration.java

@AutoConfiguration
public class AshoreDictAutoConfiguration {

    @Bean
    public DictFrameworkUtils dictUtils(DictDataCommonApi dictDataApi) {
        DictFrameworkUtils.init(dictDataApi);  // 注入 CommonApi
        return new DictFrameworkUtils();
    }

}
```

**Spring Boot 启动时：**
1. 自动配置类扫描到 `DictDataCommonApi` 这个 Feign 接口
2. Spring 为其创建动态代理对象
3. 将代理对象注入到 `DictFrameworkUtils` 中

### 2.2 CommonApi 的设计目标

| 特性 | 说明 |
|------|------|
| **最小化接口** | 只提供最基础的查询方法，不包含复杂的业务逻辑 |
| **Framework 专用** | 供框架层工具类使用，不对业务模块开放 |
| **避免依赖污染** | 放在 `ashore-common` 中，让 framework 层无需依赖业务模块 |

---

## 三、为什么要提取 CommonApi 层？

### 3.1 问题场景：依赖循环

假设没有 CommonApi，framework 层直接依赖业务模块的 API 接口：

```
❌ 错误的依赖关系（循环依赖）

ashore-spring-boot-starter-excel (framework层)
    ↓ 需要依赖
ashore-module-system-api (业务模块API)
    ↓ 依赖
ashore-common
    ↑ 被依赖
ashore-spring-boot-starter-excel (framework层)
```

**问题：**
- Framework 层依赖业务模块
- 业务模块依赖 common
- Common 被 framework 层依赖
- **形成循环依赖！**

### 3.2 解决方案：依赖倒置原则

通过在 `ashore-common` 中提取 CommonApi 接口，打破循环依赖：

```
✅ 正确的依赖关系（单向依赖）

ashore-spring-boot-starter-excel (framework层)
    ↓ 依赖
ashore-common (包含 DictDataCommonApi)
    ↑ 被依赖
ashore-module-system-api (业务模块API)
```

**依赖关系图：**

```
┌─────────────────────────────────────────────────────────────────┐
│                         依赖方向 ↓                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────────────────────┐                      │
│  │  Framework 层                         │                      │
│  │  (ashore-spring-boot-starter-excel)   │                      │
│  └───────────┬───────────────────────────┘                      │
│              │ 依赖                                             │
│              ↓                                                  │
│  ┌──────────────────────────────┐                               │
│  │  基础层 (ashore-common)      │                               │
│  │                              │                               │
│  │  包含:                       │                               │
│  │  - DictDataCommonApi         │ ← 最基础的字典查询接口         │
│  │  - TenantCommonApi           │                               │
│  │  - PermissionCommonApi       │                               │
│  └───────────┬──────────────────┘                               │
│              ↑ 被依赖                                           │
│              │                                                  │
│  ┌───────────┴──────────────────┐                               │
│  │  业务模块API层                │                               │
│  │  (ashore-module-system-api)  │                               │
│  │                              │                               │
│  │  DictDataApi                 │ ← 继承 CommonApi + 扩展业务方法 │
│  │  extends                     │                               │
│  │  DictDataCommonApi           │                               │
│  └───────────┬──────────────────┘                               │
│              ↑ 被依赖                                           │
│              │                                                  │
│  ┌───────────┴─────────────┐                                    │
│  │  其他业务模块            │                                    │
│  │  (bpm, crm, erp...)     │                                    │
│  └─────────────────────────┘                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 对比表格

| 对比项 | 没有 CommonApi | 有 CommonApi |
|--------|----------------|--------------|
| **Framework 依赖** | 依赖 `ashore-module-system-api` | 依赖 `ashore-common` |
| **是否循环依赖** | ✅ 是 | ❌ 否 |
| **架构层次** | 混乱（Framework 依赖业务模块） | 清晰（Framework 只依赖基础层） |
| **可维护性** | 差 | 好 |

---

## 四、接口的继承与实现关系

### 4.1 三层接口/实现结构

本项目中，字典相关的接口/实现分为三层：

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          接口继承与实现关系                               │
└──────────────────────────────────────────────────────────────────────────┘

第一层：基础接口（位于 ashore-common）
┌─────────────────────────────────────────────────────────────┐
│ DictDataCommonApi                                           │
│ 位置: ashore-common/.../biz/system/dict/                    │
│                                                             │
│ @FeignClient(name = "system-server", primary = false)       │
│ public interface DictDataCommonApi {                        │
│     ApiResponse<List<DictDataRespDTO>>                      │
│         getDictDataList(String dictType);  // 基础查询       │
│ }                                                           │
└─────────────────┬───────────────────────────────────────────┘
                  │
                  │ extends (继承)
                  ↓
第二层：业务接口（位于 ashore-module-system-api）
┌─────────────────────────────────────────────────────────────┐
│ DictDataApi                                                 │
│ 位置: ashore-module-system-api/.../api/dict/                │
│                                                             │
│ @FeignClient(name = "system-server")                        │
│ public interface DictDataApi                                │
│         extends DictDataCommonApi {  // 继承基础接口         │
│                                                             │
│     // 继承了 getDictDataList() 方法                         │
│                                                             │
│     // 新增业务方法                                          │
│     ApiResponse<Boolean> validateDictDataList(              │
│         String dictType,                                    │
│         Collection<String> values                           │
│     );                                                      │
│ }                                                           │
└─────────────────┬───────────────────────────────────────────┘
                  │
                  │ implements (实现)
                  ↓
第三层：实现类（位于 ashore-module-system-server）
┌─────────────────────────────────────────────────────────────┐
│ DictDataApiImpl                                             │
│ 位置: ashore-module-system-server/.../api/dict/             │
│                                                             │
│ @RestController  // 关键！暴露为HTTP接口                     │
│ @Primary         // 标记为主Bean                            │
│ public class DictDataApiImpl implements DictDataApi {       │
│                                                             │
│     @Resource                                               │
│     private DictDataService dictDataService;                │
│                                                             │
│     @Override                                               │
│     public ApiResponse<List<DictDataRespDTO>>               │
│             getDictDataList(String dictType) {              │
│         List<DictDataDO> list =                             │
│             dictDataService                                 │
│                 .getDictDataListByDictType(dictType);       │
│         return success(BeanUtils.toBean(list,               │
│                        DictDataRespDTO.class));             │
│     }                                                       │
│                                                             │
│     @Override                                               │
│     public ApiResponse<Boolean>                             │
│             validateDictDataList(String dictType,           │
│                                  Collection<String> vals) { │
│         dictDataService.validateDictDataList(               │
│             dictType, values);                              │
│         return success(true);                               │
│     }                                                       │
│ }                                                           │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 为什么要这样设计？

#### 设计 1：CommonApi 继承自 FeignClient

```java
@FeignClient(name = "system-server", primary = false)
public interface DictDataCommonApi {
    // ...
}
```

**关键点：`primary = false`**
- 表示这个 Feign 客户端不是主要的客户端
- 因为稍后会有 `DictDataApi` 也是 `@FeignClient`，指向同一个服务
- 避免 Spring 容器中出现两个同名的 Bean 冲突，让业务模块优先使用 DictDataApi

#### 设计 2：DictDataApi 继承 CommonApi

```java
@FeignClient(name = "system-server")
public interface DictDataApi extends DictDataCommonApi {
    // 继承 getDictDataList() 方法

    // 新增业务方法
    ApiResponse<Boolean> validateDictDataList(...);
}
```

**为什么要继承？**
- **代码复用**：不用重复定义 `getDictDataList()` 方法
- **接口扩展**：在基础功能上添加更多业务方法
- **分层清晰**：CommonApi 是最小集合，DictDataApi 是完整功能

#### 设计 3：实现类用 `@RestController`

```java
@RestController  // ← 关键！
@Primary
public class DictDataApiImpl implements DictDataApi {
    // ...
}
```

**为什么用 `@RestController` 而不是 `@Service`？**

| 注解 | 作用 | 是否暴露HTTP接口 |
|------|------|------------------|
| `@Service` | 标记为Spring Bean（本地服务） | ❌ 否 |
| `@RestController` | 标记为Spring MVC Controller | ✅ 是（可被远程调用） |

**关键理解1：**
1. `DictDataApiImpl` 不仅仅是一个本地的实现类
2. 它需要**暴露为 HTTP 接口**，让其他服务通过 Feign 调用
3. `@RestController` 会自动解析接口上的 `@GetMapping` 等注解，创建对应的 HTTP 路由

**HTTP 路由示例：**
```
接口定义: @GetMapping("/rpc-api/system/dict-data/list")
         ↓
Spring MVC 自动创建路由:
         GET http://system-server:48081/rpc-api/system/dict-data/list
         ↓
路由到实现类的方法:
         DictDataApiImpl.getDictDataList()
```

**关键理解2：**

**`@Primary`** 是为了解决类型匹配的问题：

```
DictDataApiImpl 实现了 DictDataApi
        ↓
DictDataApi 继承自 DictDataCommonApi
        ↓
所以 DictDataApiImpl 同时满足两个类型:
  - DictDataApi
  - DictDataCommonApi (通过继承)
        ↓
当其他地方注入 DictDataCommonApi 类型时:
  @Resource
  private DictDataCommonApi api;
        ↓
Spring 会找到 DictDataApiImpl (因为它实现了父接口)
        ↓
@Primary 确保它是首选的实现
```

`@Primary` 在本项目的实际作用场景：
- 场景 1：单体部署（所有模块打包在一起，在一个进程）
  - Spring 容器中同时有 Feign 代理和本地实现
  - `@Primary` 确保注入本地实现（性能更好）
  ```
  单一应用进程
    ↓
    同时包含:
      - ashore-spring-boot-starter-excel (启用 CommonApi Feign)
      - system-server (包含 DictDataApiImpl)
    ↓
    Spring 容器中会有:
      Bean 1: dictDataCommonApi (Feign 代理, primary=false)
      Bean 2: dictDataApiImpl (本地实现, primary=true)
    ↓
    注入 DictDataCommonApi 类型时:
      @Resource
      private DictDataCommonApi api;
    ↓
    Spring 发现两个候选:
      - Feign 代理 (DictDataCommonApi)
      - 本地实现 (DictDataApiImpl 实现了 DictDataCommonApi)
    ↓
    没有 @Primary: ❌ 报错，不知道选哪个
    有 @Primary:   ✅ 选择 dictDataApiImpl (本地调用，更高效)

  ```
- 场景 2：本地开发环境（多模块同时启动）
  - 开发时可能在同一个 IDE 进程中运行所有服务
  - `@Primary` 确保优先使用本地实现
  ```
  bpm-server (端口 48083)
    ↓
    @Resource
    private DictDataCommonApi api;     // 字段名取api不跟任何类名（首字母小写后）一致，且不指定Bean名称
    ↓
    注入: Feign 代理对象 (dictDataApi)  // 没有名称完全匹配的，按类型 + primary
    ↓
    调用时发送 HTTP 请求到 system-server

  system-server (端口 48081)
    ↓
    没有注入 DictDataCommonApi
    ↓
    只有实现类 DictDataApiImpl
    ↓
    @Primary 在这里没有实际作用（只有一个 Bean）
  ```
- 场景 3：测试环境
  - 测试时可能混合使用 Feign 和本地调用
  - `@Primary` 提供明确的优先级
---

总结如下：

| 场景                  | 注入 DictDataCommonApi 时的结果                  |
|---------------------|--------------------------------------------|
| bpm-server (微服务部署)  | 注入 dictDataApi 的 Feign 代理（因为 `primary=true`） |
| bpm-server (明确字段名，字段名就叫 `dictDataCommonApi`) | 注入 dictDataCommonApi 的 Feign 代理（按名称匹配）     |
| system-server       | 注入 dictDataApiImpl 本地实现类（唯一候选）             |
| 单体部署                | 优先注入 dictDataApiImpl 本地实现类（`@Primary`）       |


## 五、业务模块如何调用字典服务

### 5.1 业务模块应该调用哪个接口？

| 调用方 | 应该使用的接口 | 原因 |
|--------|---------------|------|
| **Framework 层**<br>（如 Excel 工具） | `DictDataCommonApi` | 只需要基础查询功能，且避免依赖业务模块 |
| **业务模块**<br>（如 BPM、CRM、ERP） | `DictDataApi` | 需要完整功能（包括校验等业务方法） |

**注意：** 永远不要直接注入 `DictDataApiImpl`！应该依赖接口。

### 5.2 业务模块调用步骤

以 BPM 模块为例，说明如何调用字典服务：

#### 步骤 1：在 RpcConfiguration 中启用 Feign 客户端

```java
// 文件位置：shore-module-bpm/shore-module-bpm-server/src/main/java/.../rpc/config/RpcConfiguration.java
package com.example.shore.module.bpm.framework.rpc.config;

import com.example.shore.module.system.api.dict.DictDataApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "bpmRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {
    DictDataApi.class,  // ← 启用字典API的Feign客户端
    // ... 其他需要的API
})
public class RpcConfiguration {
}
```

**作用：**
- `@EnableFeignClients` 告诉 Spring：请为 `DictDataApi` 这个接口生成 Feign 动态代理对象
- Spring 启动时会扫描该接口，创建代理实现

#### 步骤 2：在 Service 中注入并使用

```java
// 示例：在 BPM 的某个 Service 中使用字典服务

package com.example.shore.module.bpm.service.task;

import com.example.shore.module.system.api.dict.DictDataApi;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

@Service
public class BpmTaskServiceImpl {

    @Resource
    private DictDataApi dictDataApi;  // ← 注入 Feign 客户端
    
    public void processTask() {
        // 1. 获取字典数据
        ApiResponse<List<DictDataRespDTO>> result = dictDataApi.getDictDataList("bpm_task_status");
    
        List<DictDataRespDTO> dictDataList = result.getCheckedData();
    
        // 2. 校验字典值是否有效
        Collection<String> values = Arrays.asList("1", "2");
        ApiResponse<Boolean> validResult = dictDataApi.validateDictDataList("bpm_task_status", values);
    
        if (validResult.getCheckedData()) {
            // 字典值有效，继续业务处理
        }
    }
}
```

### 5.3 完整的调用链路

```
┌─────────────────────────────────────────────────────────────────┐
│                    业务模块调用字典服务流程                         │
└─────────────────────────────────────────────────────────────────┘

1. BPM Service 调用
   BpmTaskServiceImpl.processTask()
       ↓
       dictDataApi.getDictDataList("bpm_task_status")

2. Feign 拦截调用
   Spring 注入的不是真实对象，而是 Feign 动态代理对象
       ↓
   FeignProxy (动态代理对象)
       ↓
   解析方法注解，构造 HTTP 请求

3. 服务发现
   从 Nacos 注册中心查询 "system-server" 的实例列表
       ↓
   获得实例地址：http://192.168.1.10:48081

4. 发送 HTTP 请求
   GET http://192.168.1.10:48081/rpc-api/system/dict-data/list?dictType=bpm_task_status

5. System-Server 接收请求
   Spring MVC 路由到 DictDataApiImpl.getDictDataList()
       ↓
   调用 DictDataService 查询数据库
       ↓
   返回 JSON 响应

6. Feign 解析响应
   将 JSON 反序列化为 ApiResponse<List<DictDataRespDTO>>
       ↓
   返回给 BPM Service
```

---

## 六、Feign 远程调用机制详解

### 6.1 什么是 Feign？

Feign 是一个**声明式 HTTP 客户端**，由 Netflix 开发（现在由 Spring Cloud 维护）。

**传统的 HTTP 调用方式：**

```java
// 使用 RestTemplate（繁琐）
String url = "http://system-server/rpc-api/system/dict-data/list?dictType=SEX";
RestTemplate restTemplate = new RestTemplate();
String json = restTemplate.getForObject(url, String.class);
// 还需要手动解析 JSON...
```

**使用 Feign（简洁）：**

```java
// 像调用本地方法一样
@Resource
private DictDataApi dictDataApi;

List<DictDataRespDTO> list = dictDataApi.getDictDataList("SEX").getCheckedData();
```

**Feign 的核心价值：**
- **声明式**：只需定义接口，不需要写实现
- **自动化**：自动处理 HTTP 请求、序列化、反序列化
- **透明化**：调用远程服务像调用本地方法一样

### 6.2 Feign 的核心机制：动态代理

#### 什么是动态代理？

Java 中，动态代理可以在运行时为接口生成实现类。

**简化理解：**

```java
// 你定义的接口
public interface DictDataApi {
    ApiResponse<List<DictDataRespDTO>> getDictDataList(String dictType);
}

// Feign 在运行时自动生成类似这样的代理类（伪代码）
public class DictDataApi$FeignProxy implements DictDataApi {

    @Override
    public ApiResponse<List<DictDataRespDTO>> getDictDataList(String dictType) {
        // 1. 解析方法上的注解
        String method = "GET";
        String url = "/rpc-api/system/dict-data/list";
        Map<String, String> params = Map.of("dictType", dictType);
    
        // 2. 从 Nacos 获取服务实例地址
        String serviceUrl = discoverService("system-server");
    
        // 3. 发送 HTTP 请求
        String fullUrl = serviceUrl + url + "?dictType=" + dictType;
        String jsonResponse = httpClient.get(fullUrl);
    
        // 4. 解析 JSON 响应
        return jsonMapper.readValue(jsonResponse,
            new TypeReference<ApiResponse<List<DictDataRespDTO>>>(){});
    }
}
```

**实际工作流程：**

```
┌──────────────────────────────────────────────────────────────┐
│               Feign 动态代理工作流程                          │
└──────────────────────────────────────────────────────────────┘

Spring Boot 启动阶段
─────────────────────────────────────────────────────────────
1. 扫描 @EnableFeignClients 注解
   ↓
   @EnableFeignClients(clients = {DictDataApi.class})

2. 查找 @FeignClient 注解的接口
   ↓
   @FeignClient(name = "system-server")
   public interface DictDataApi { ... }

3. 为接口生成动态代理对象
   ↓
   使用 JDK 动态代理 / CGLIB
   ↓
   DictDataApi proxy = (DictDataApi) Proxy.newProxyInstance(
       classLoader,
       new Class[]{DictDataApi.class},
       new FeignInvocationHandler(...)  // Feign 的核心处理器
   );

4. 将代理对象注册到 Spring 容器
   ↓
   Spring Bean: dictDataApi (实际是代理对象)


运行时调用阶段
─────────────────────────────────────────────────────────────
业务代码调用
   ↓
   dictDataApi.getDictDataList("SEX")

代理对象拦截方法调用
   ↓
   FeignInvocationHandler.invoke(method, args)

解析方法元数据
   ↓
   - 方法名: getDictDataList
   - 注解: @GetMapping("/rpc-api/system/dict-data/list")
   - 参数: @RequestParam("dictType") String dictType = "SEX"

构建 HTTP 请求
   ↓
   RequestTemplate {
       method: GET
       url: /rpc-api/system/dict-data/list
       queries: {"dictType": "SEX"}
   }

服务发现（通过 Ribbon/LoadBalancer）
   ↓
   从 Nacos 查询 "system-server" 的实例
   ↓
   实例列表: [
       "http://192.168.1.10:48081",
       "http://192.168.1.11:48081"
   ]
   ↓
   负载均衡选择一个实例
   ↓
   选中: http://192.168.1.10:48081

发送 HTTP 请求
   ↓
   GET http://192.168.1.10:48081/rpc-api/system/dict-data/list?dictType=SEX
   Headers:
       Accept: application/json
       Content-Type: application/json

接收 HTTP 响应
   ↓
   HTTP 200 OK
   {
       "code": 0,
       "data": [
           {"value": "1", "label": "男"},
           {"value": "2", "label": "女"}
       ],
       "msg": null
   }

反序列化响应
   ↓
   使用 Jackson 将 JSON 转为 Java 对象
   ↓
   ApiResponse<List<DictDataRespDTO>> result

返回给调用方
   ↓
   业务代码收到结果对象
```

### 6.3 关键注解解析

#### @FeignClient 注解

```java
@FeignClient(name = "system-server", primary = false)
public interface DictDataApi {
    // ...
}
```

**参数说明：**

| 参数 | 说明 | 示例 |
|------|------|------|
| `name` | 目标服务名（需与服务的 `spring.application.name` 一致） | `"system-server"` |
| `url` | 直接指定服务地址（一般不用，因为会用服务发现） | `"http://localhost:48081"` |
| `path` | 公共路径前缀 | `"/rpc-api/system"` |
| `fallback` | 降级处理类（服务不可用时的备用方案） | `DictDataApiFallback.class` |
| `primary` | 是否为主 Bean（解决同名 Bean 冲突） | `false` |

#### @GetMapping / @PostMapping 注解

```java
@GetMapping(PREFIX + "/list")
ApiResponse<List<DictDataRespDTO>> getDictDataList(@RequestParam("dictType") String dictType);
```

**Feign 如何解析：**
- `@GetMapping` → HTTP 方法 = GET
- `PREFIX + "/list"` → URL 路径 = `/rpc-api/system/dict-data/list`
- `@RequestParam("dictType")` → 查询参数 `dictType=SEX`

**最终生成的 HTTP 请求：**
```http
GET /rpc-api/system/dict-data/list?dictType=SEX HTTP/1.1
Host: 192.168.1.10:48081
Accept: application/json
```

#### @RestController 注解（服务端）

```java
@RestController
public class DictDataApiImpl implements DictDataApi {
    // ...
}
```

**作用：**

1. `@RestController` = `@Controller` + `@ResponseBody`
2. 告诉 Spring MVC：这个类的方法返回值需要序列化为 JSON
3. 自动创建 HTTP 路由，映射到对应的方法

**Spring MVC 自动创建的路由表：**
```
GET  /rpc-api/system/dict-data/list       → DictDataApiImpl.getDictDataList()
GET  /rpc-api/system/dict-data/valid      → DictDataApiImpl.validateDictDataList()
```

### 6.4 完整的请求流转示例

#### 场景：BPM 模块查询任务状态字典

**调用代码：**
```java
// BPM 模块的 Service
@Resource
private DictDataApi dictDataApi;

public void processTask() {
   ApiResponse<List<DictDataRespDTO>> result =
        dictDataApi.getDictDataList("bpm_task_status");
}
```

**详细流转过程：**

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         请求流转全过程                                     │
└──────────────────────────────────────────────────────────────────────────┘

【BPM 模块服务】(端口 48083)
─────────────────────────────────────────────────────────────────────────
  代码执行
    ↓
  dictDataApi.getDictDataList("bpm_task_status")
    ↓
  ┌────────────────────────────────────────────────┐
  │ Feign 动态代理对象 (DictDataApi$Proxy)           │
  ├────────────────────────────────────────────────┤
  │ 1. 拦截方法调用                                  │
  │    - 方法: getDictDataList                      │
  │    - 参数: dictType = "bpm_task_status"         │
  │                                                │
  │ 2. 解析注解元数据                                │
  │    - @FeignClient(name = "system-server")      │
  │    - @GetMapping("/rpc-api/system/dict-data/   │
  │                   list")                       │
  │    - @RequestParam("dictType")                 │
  │                                                │
  │ 3. 构建请求模板                                  │
  │    RequestTemplate {                           │
  │      method: GET                               │
  │      url: /rpc-api/system/dict-data/list       │
  │      queries: {                                │
  │        "dictType": "bpm_task_status"           │
  │      }                                         │
  │    }                                           │
  └────────────────┬───────────────────────────────┘
                   │
                   │ 4. 服务发现
                   ↓
  ┌────────────────────────────────────────────────┐
  │ Nacos 注册中心                                 │
  ├────────────────────────────────────────────────┤
  │ 查询服务: "system-server"                      │
  │                                                │
  │ 返回实例列表:                                  │
  │   - 实例1: 192.168.1.10:48081 (UP)            │
  │   - 实例2: 192.168.1.11:48081 (UP)            │
  └────────────────┬───────────────────────────────┘
                   │
                   │ 5. 负载均衡
                   ↓
  ┌────────────────────────────────────────────────┐
  │ Ribbon / Spring Cloud LoadBalancer             │
  ├────────────────────────────────────────────────┤
  │ 策略: 轮询 (Round Robin)                       │
  │ 选择: 192.168.1.10:48081                       │
  └────────────────┬───────────────────────────────┘
                   │
                   │ 6. 发送 HTTP 请求
                   ↓
  ════════════════════════════════════════════════════════════
  HTTP 请求
  ────────────────────────────────────────────────────────────
  GET http://192.168.1.10:48081/rpc-api/system/dict-data/list?dictType=bpm_task_status

  Headers:
    Accept: application/json
    Content-Type: application/json
    User-Agent: Java/17 Feign/12.1
  ════════════════════════════════════════════════════════════


【System 模块服务】(端口 48081)
─────────────────────────────────────────────────────────────────────────
  7. Spring MVC 路由
    ↓
  ┌────────────────────────────────────────────────┐
  │ DispatcherServlet                              │
  ├────────────────────────────────────────────────┤
  │ 路由查找:                                      │
  │   GET /rpc-api/system/dict-data/list           │
  │   ↓                                            │
  │ 匹配到:                                        │
  │   DictDataApiImpl.getDictDataList()            │
  └────────────────┬───────────────────────────────┘
                   │
                   │ 8. Controller 处理
                   ↓
  ┌────────────────────────────────────────────────┐
  │ @RestController                                │
  │ DictDataApiImpl                                │
  ├────────────────────────────────────────────────┤
  │ @Override                                      │
  │ public ApiResponse<List<DictDataRespDTO>>     │
  │     getDictDataList(String dictType) {         │
  │                                                │
  │   // 调用 Service 层                           │
  │   List<DictDataDO> list =                      │
  │     dictDataService                            │
  │       .getDictDataListByDictType(dictType);    │
  │                                                │
  │   // 转换 DO → DTO                             │
  │   return success(                              │
  │     BeanUtils.toBean(list,                     │
  │                      DictDataRespDTO.class)    │
  │   );                                           │
  │ }                                              │
  └────────────────┬───────────────────────────────┘
                   │
                   │ 9. Service 层查询数据库
                   ↓
  ┌────────────────────────────────────────────────┐
  │ DictDataServiceImpl                            │
  ├────────────────────────────────────────────────┤
  │ public List<DictDataDO>                        │
  │     getDictDataListByDictType(String type) {   │
  │   return dictDataMapper.selectList(            │
  │     "dict_type", type                          │
  │   );                                           │
  │ }                                              │
  └────────────────┬───────────────────────────────┘
                   │
                   │ 10. 数据库查询
                   ↓
  ┌────────────────────────────────────────────────┐
  │ MySQL 数据库                                   │
  ├────────────────────────────────────────────────┤
  │ SELECT * FROM system_dict_data                 │
  │ WHERE dict_type = 'bpm_task_status'            │
  │   AND deleted = 0                              │
  │                                                │
  │ 结果:                                          │
  │ +----+-------+-----------+---------+           │
  │ | id | value | label     | dict... |           │
  │ +----+-------+-----------+---------+           │
  │ | 1  | 1     | 待审批    | bpm...  |           │
  │ | 2  | 2     | 已通过    | bpm...  |           │
  │ | 3  | 3     | 已拒绝    | bpm...  |           │
  │ +----+-------+-----------+---------+           │
  └────────────────┬───────────────────────────────┘
                   │
                   │ 11. 返回数据
                   ↓
  Service 层 → Controller 层 → Spring MVC
                   │
                   │ 12. 序列化为 JSON
                   ↓
  ════════════════════════════════════════════════════════════
  HTTP 响应
  ────────────────────────────────────────────────────────────
  HTTP/1.1 200 OK
  Content-Type: application/json

  {
    "code": 0,
    "data": [
      {
        "value": "1",
        "label": "待审批",
        "dictType": "bpm_task_status",
        "sort": 1,
        "colorType": "primary",
        "cssClass": ""
      },
      {
        "value": "2",
        "label": "已通过",
        "dictType": "bpm_task_status",
        "sort": 2,
        "colorType": "success",
        "cssClass": ""
      },
      {
        "value": "3",
        "label": "已拒绝",
        "dictType": "bpm_task_status",
        "sort": 3,
        "colorType": "danger",
        "cssClass": ""
      }
    ],
    "msg": null
  }
  ════════════════════════════════════════════════════════════


【BPM 模块服务】(端口 48083)
─────────────────────────────────────────────────────────────────────────
  13. Feign 接收响应
    ↓
  ┌────────────────────────────────────────────────┐
  │ Feign 响应处理器                               │
  ├────────────────────────────────────────────────┤
  │ 1. 接收 JSON 字符串                            │
  │ 2. 使用 Jackson 反序列化                       │
  │    ↓                                           │
  │    ApiResponse<List<DictDataRespDTO>> result  │
  │                                                │
  │ 3. 检查响应码                                  │
  │    if (result.getCode() != 0) {                │
  │      throw new ServiceException(...)           │
  │    }                                           │
  │                                                │
  │ 4. 返回对象给调用方                            │
  └────────────────┬───────────────────────────────┘
                   │
                   │ 14. 返回给业务代码
                   ↓
  业务代码收到结果
    ↓
  ApiResponse<List<DictDataRespDTO>> result
  List<DictDataRespDTO> dictList = result.getCheckedData();

  // 可以使用字典数据了
  for (DictDataRespDTO dict : dictList) {
    System.out.println(dict.getLabel());
    // 输出: 待审批、已通过、已拒绝
  }
```

### 6.5 为什么调用接口就能调用到实现类？

很多人会困惑：我只注入了 `DictDataApi` 接口，没有注入实现类 `DictDataApiImpl`，为什么能调用成功？

**核心答案：**

```
你注入的不是接口本身，而是 Feign 为接口生成的动态代理对象！

┌─────────────────────────────────────────────────────────┐
│  你以为注入的                                            │
├─────────────────────────────────────────────────────────┤
│  DictDataApi (接口，无法实例化)                         │
└─────────────────────────────────────────────────────────┘
                        ↓
                     实际上是
                        ↓
┌─────────────────────────────────────────────────────────┐
│  实际注入的                                              │
├─────────────────────────────────────────────────────────┤
│  DictDataApi$FeignProxy (动态代理对象)                  │
│                                                         │
│  这个对象:                                              │
│  1. 实现了 DictDataApi 接口                             │
│  2. 拦截所有方法调用                                    │
│  3. 将方法调用转换为 HTTP 请求                          │
│  4. 发送请求到远程服务                                  │
│  5. 解析响应并返回                                      │
└─────────────────────────────────────────────────────────┘
```

**验证方式：**

你可以在代码中打印注入对象的实际类型：

```java
@Resource
private DictDataApi dictDataApi;

public void test() {
    System.out.println(dictDataApi.getClass().getName());
    // 输出类似: com.sun.proxy.$Proxy123
    // 或: com.example.shore.module.system.api.dict.DictDataApi$$EnhancerBySpringCGLIB$$12345678
}
```

**关键理解：**

| 概念 | 说明 |
|------|------|
| **接口** | 只是定义了契约（方法签名） |
| **代理对象** | 实现了接口，并在方法调用时执行特定逻辑（发送 HTTP 请求） |
| **远程实现类** | 位于另一个服务中，通过 HTTP 接口暴露功能 |

**完整链路：**

```
BPM Service 注入
    ↓
DictDataApi (接口)
    ↓
实际是 Feign 代理对象 (DictDataApi$Proxy)
    ↓
调用方法时，代理对象将其转换为 HTTP 请求
    ↓
发送到 system-server 服务
    ↓
路由到 DictDataApiImpl (实现类)
    ↓
执行真正的业务逻辑
    ↓
返回结果
    ↓
Feign 代理对象解析响应
    ↓
返回给 BPM Service
```

---

## 第七章：总结与最佳实践

### 7.1 核心要点总结

| 知识点 | 要点 |
|--------|------|
| **CommonApi 的定义** | 位于 `shore-common/biz` 包下，提供最基础的 RPC 接口 |
| **CommonApi 的作用** | 供 Framework 层使用，避免 Framework 依赖业务模块 |
| **为什么提取 CommonApi** | 解决循环依赖问题，实现依赖倒置原则 |
| **接口继承关系** | CommonApi (基础) ← DictDataApi (业务扩展) ← DictDataApiImpl (实现) |
| **实现类的特殊之处** | 使用 `@RestController` 而非 `@Service`，暴露为 HTTP 接口 |
| **业务模块调用方式** | 注入 `DictDataApi` 接口，像调用本地方法一样使用 |
| **Feign 核心机制** | 动态代理 + HTTP 调用 + 服务发现 |
| **为什么能调用到实现类** | Feign 为接口生成代理对象，将方法调用转为 HTTP 请求 |

### 7.2 最佳实践建议

#### ✅ 推荐做法

1. **Framework 层使用 CommonApi**
   ```java
   // ✅ 正确：Framework 工具类依赖 CommonApi
   @Resource
   private DictDataCommonApi dictDataApi;
   ```

2. **业务模块使用完整的 Api 接口**
   ```java
   // ✅ 正确：业务模块依赖 DictDataApi
   @Resource
   private DictDataApi dictDataApi;
   ```

3. **启用 Feign 客户端**
   ```java
   // ✅ 正确：在 RpcConfiguration 中声明
   @EnableFeignClients(clients = {DictDataApi.class})
   ```

4. **实现类使用 @RestController**
   ```java
   // ✅ 正确：暴露为 HTTP 接口
   @RestController
   @Primary
   public class DictDataApiImpl implements DictDataApi {
       // ...
   }
   ```

#### ❌ 错误做法

1. **Framework 层直接依赖业务模块 API**
   ```java
   // ❌ 错误：会导致循环依赖
   // Framework 层依赖了业务模块
   @Resource
   private DictDataApi dictDataApi;
   ```

2. **直接注入实现类**
   ```java
   // ❌ 错误：无法实现远程调用
   @Resource
   private DictDataApiImpl dictDataApiImpl;
   ```

3. **实现类使用 @Service**
   ```java
   // ❌ 错误：不会暴露 HTTP 接口，Feign 无法调用
   @Service
   public class DictDataApiImpl implements DictDataApi {
       // ...
   }
   ```

4. **忘记在 RpcConfiguration 中启用**
   ```java
   // ❌ 错误：Feign 不会为接口生成代理对象
   @Configuration
   public class RpcConfiguration {
       // 缺少 @EnableFeignClients
   }
   ```

### 7.3 常见问题 FAQ

#### Q1: 为什么 CommonApi 要加 `primary = false`？

```java
@FeignClient(name = "system-server", primary = false)
public interface DictDataCommonApi { }
```

**答：** 因为 `DictDataApi` 也指向同一个服务，如果都是 `primary = true`，Spring 容器会不知道该注入哪个 Bean。设置 `primary = false` 表示这不是主要的客户端。

---

#### Q2: DictDataApi 继承 DictDataCommonApi，实现类需要实现哪些方法？

**答：** 实现类需要实现**所有方法**（包括继承来的方法）：

```java
public class DictDataApiImpl implements DictDataApi {

    // 来自 DictDataCommonApi 的方法
    @Override
    public ApiResponse<List<DictDataRespDTO>> getDictDataList(String dictType) {
        // ...
    }

    // 来自 DictDataApi 的方法
    @Override
    public ApiResponse<Boolean> validateDictDataList(String dictType, Collection<String> values) {
        // ...
    }
}
```

---

#### Q3: 如果服务不可用，Feign 调用会怎样？

**答：** 默认会抛出异常。可以通过以下方式处理：

1. **配置降级类（Fallback）**
   ```java
   @FeignClient(name = "system-server", fallback = DictDataApiFallback.class)
   public interface DictDataApi { }

   @Component
   public class DictDataApiFallback implements DictDataApi {
       @Override
       public ApiResponse<List<DictDataRespDTO>> getDictDataList(String dictType) {
           // 返回默认值或空列表
           return ApiResponse.success(Collections.emptyList());
       }
   }
   ```

2. **配置重试机制**
   ```yaml
   feign:
     client:
       config:
         default:
           connect-timeout: 5000  # 连接超时
           read-timeout: 10000    # 读取超时
   ```

---

#### Q4: 如何调试 Feign 请求？

**答：** 开启 Feign 日志：

```yaml
# application.yaml
logging:
  level:
    com.example.shore.module.system.api: DEBUG  # Feign 接口所在包

feign:
  client:
    config:
      default:
        loggerLevel: FULL  # NONE, BASIC, HEADERS, FULL
```

---

#### Q5: 本地开发时，如何不通过 Feign 调用？

**答：** 本地开发时，可以直接注入实现类（需要在同一个进程中）：

```java
// 方式1: 使用 @Primary 注解
@Service
@Primary  // 优先使用本地实现
public class DictDataApiLocalImpl implements DictDataApi {
    // 本地实现
}

// 方式2: 使用 @Profile 注解
@Service
@Profile("local")  // 只在 local 环境生效
public class DictDataApiLocalImpl implements DictDataApi {
    // 本地实现
}
```

---

### 7.4 扩展阅读

如果你想深入了解相关技术，推荐阅读以下内容：

1. **Spring Cloud OpenFeign 官方文档**
   - https://spring.io/projects/spring-cloud-openfeign

2. **动态代理原理**
   - JDK 动态代理 vs CGLIB 动态代理
   - `java.lang.reflect.Proxy` 源码

3. **服务发现与注册**
   - Nacos 官方文档
   - Eureka vs Consul vs Nacos 对比

4. **负载均衡**
   - Spring Cloud LoadBalancer
   - Ribbon（已停止维护）

5. **微服务通信模式**
   - RESTful API vs gRPC vs GraphQL
   - 同步调用 vs 异步消息

---

## 附录：项目中所有 CommonApi 列表

| 接口名称 | 位置 | 作用 |
|---------|------|------|
| `DictDataCommonApi` | `common/biz/system/dict` | 字典数据查询 |
| `PermissionCommonApi` | `common/biz/system/permission` | 权限校验 |
| `TenantCommonApi` | `common/biz/system/tenant` | 租户信息查询 |
| `OperateLogCommonApi` | `common/biz/system/logger` | 操作日志记录 |
| `OAuth2TokenCommonApi` | `common/biz/system/oauth2` | OAuth2 令牌校验 |
| `ApiAccessLogCommonApi` | `common/biz/infra/logger` | API 访问日志记录 |
| `ApiErrorLogCommonApi` | `common/biz/infra/logger` | API 错误日志记录 |

这些接口的设计原理和使用方式与 `DictDataCommonApi` 完全一致。

---

**文档版本**：v1.0  
**最后更新**：2025-10-06  
**作者**：Ashore 团队  
**适用项目**：shore-cloud（基于 Spring Cloud 的微服务架构）
