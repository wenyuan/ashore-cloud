# IO 工具类使用指南

## 一、包概览

`io` 包提供了 IO 操作和文件处理的工具类，主要包含两个核心类：

- **FileUtils**: 临时文件创建工具
- **IoUtils**: IO 流处理工具

这两个类简化了文件和流的操作，特别是临时文件的管理和流的读取。

---

## 二、FileUtils 类详解

### 2.1 类的整体介绍

`FileUtils` 是文件工具类，主要解决以下问题：

1. **临时文件管理**: 创建 JVM 退出时自动删除的临时文件
2. **避免手动删除**: 不需要手动调用 `file.delete()`，JVM 退出时自动清理
3. **避免文件泄露**: 防止临时文件堆积，占用磁盘空间

**为什么要二次封装？**
- Java 原生的 `File.createTempFile()` 需要手动删除，容易遗忘
- 封装后自动调用 `deleteOnExit()`，确保文件会被清理
- 提供了便捷的重载方法，支持直接写入内容

### 2.2 核心方法详解

#### `createTempFile(String data)` - 创建临时文件并写入字符串内容
**作用**: 创建临时文件并写入 UTF-8 字符串内容，JVM 退出时自动删除

**参数**:
- `data`: 要写入的字符串内容

**返回值**: File 对象

**使用示例**:
```java
// 示例1：创建临时证书文件
String certContent = "-----BEGIN CERTIFICATE-----\nMIIC...\n-----END CERTIFICATE-----";
File certFile = FileUtils.createTempFile(certContent);
// 使用证书文件
SSLContext sslContext = loadSSLContext(certFile);

// 示例2：创建临时配置文件
String config = "server.port=8080\nserver.host=localhost";
File configFile = FileUtils.createTempFile(config);
// 使用配置文件
Properties props = new Properties();
props.load(new FileInputStream(configFile));

// 示例3：创建临时 JSON 文件
String jsonData = JsonUtils.toJsonString(dataObject);
File jsonFile = FileUtils.createTempFile(jsonData);
// 使用 JSON 文件
processJsonFile(jsonFile);

// 注意：无需手动删除，JVM 退出时自动清理
```

**项目实际使用场景**:
- **微信支付证书**: 在 `WxPayClientConfig` 中创建临时证书文件
  ```java
  // ashore-framework/ashore-spring-boot-starter-biz-pay/src/main/java/com/example/ashoreframework/pay/core/client/impl/weixin/WxPayClientConfig.java
  // 微信支付需要加载证书文件，但证书内容存储在数据库中
  // 创建临时文件来加载证书
  File certFile = FileUtils.createTempFile(config.getApiCertContent());

  // 使用证书文件初始化微信支付客户端
  WxPayConfig wxPayConfig = new WxPayConfig();
  wxPayConfig.setKeyPath(certFile.getAbsolutePath());
  ```

---

#### `createTempFile(byte[] data)` - 创建临时文件并写入字节内容
**作用**: 创建临时文件并写入字节数组内容，JVM 退出时自动删除

**参数**:
- `data`: 要写入的字节数组

**返回值**: File 对象

**使用示例**:
```java
// 示例1：创建临时图片文件
byte[] imageData = downloadImage("https://example.com/image.jpg");
File imageFile = FileUtils.createTempFile(imageData);
// 处理图片
BufferedImage image = ImageIO.read(imageFile);

// 示例2：创建临时二进制文件
byte[] binaryData = getBinaryData();
File binaryFile = FileUtils.createTempFile(binaryData);
// 使用二进制文件
processBinaryFile(binaryFile);

// 示例3：Base64 解码后创建文件
String base64Data = "iVBORw0KGgo...";
byte[] decodedData = Base64.getDecoder().decode(base64Data);
File file = FileUtils.createTempFile(decodedData);
```

**项目实际使用场景**:
- **SFTP 文件上传**: 在 `SftpFileClient` 中创建临时文件用于上传
  ```java
  // ashore-framework/ashore-spring-boot-starter-file/src/main/java/com/example/ashoreframework/file/core/client/sftp/SftpFileClient.java
  @Override
  public String upload(byte[] content, String path, String type) {
      // 创建临时文件
      File tempFile = FileUtils.createTempFile(content);

      // 上传到 SFTP 服务器
      channelSftp.put(new FileInputStream(tempFile), remotePath);

      // 返回文件路径
      return buildUrl(path);

      // 无需手动删除 tempFile，JVM 退出时自动删除
  }
  ```

---

#### `createTempFile()` - 创建空的临时文件
**作用**: 创建空的临时文件，JVM 退出时自动删除

**返回值**: File 对象

**使用示例**:
```java
// 示例1：创建空临时文件后写入内容
File tempFile = FileUtils.createTempFile();
try (FileOutputStream fos = new FileOutputStream(tempFile)) {
    fos.write("Hello World".getBytes());
}

// 示例2：创建临时文件用于下载
File downloadFile = FileUtils.createTempFile();
HttpUtil.downloadFile("https://example.com/file.zip", downloadFile);
// 处理下载的文件
processFile(downloadFile);

// 示例3：创建临时工作文件
File workFile = FileUtils.createTempFile();
// 多次写入
Files.write(workFile.toPath(), "line1\n".getBytes(), StandardOpenOption.APPEND);
Files.write(workFile.toPath(), "line2\n".getBytes(), StandardOpenOption.APPEND);
```

**项目实际使用场景**:
- **文件下载中转**: 先下载到临时文件，处理后再上传
- **文件格式转换**: 创建临时文件进行格式转换

---

## 三、IoUtils 类详解

### 3.1 类的整体介绍

`IoUtils` 是 IO 流工具类，主要解决以下问题：

1. **补充 Hutool 缺失的方法**: Hutool 的 `IoUtil` 缺少某些常用方法
2. **简化流读取**: 提供更便捷的流读取方法
3. **统一编码**: 强制使用 UTF-8 编码，避免乱码

**为什么要二次封装？**
- Hutool 的 `IoUtil.readUtf8()` 方法签名不够灵活
- 提供更符合项目规范的方法

### 3.2 核心方法详解

#### `readUtf8(InputStream in, boolean isClose)` - 从流中读取 UTF-8 内容
**作用**: 从输入流中读取内容，按 UTF-8 编码转换为字符串

**参数**:
- `in`: 输入流
- `isClose`: 是否关闭流（true-读取后关闭，false-不关闭）

**返回值**: 读取的字符串内容

**使用示例**:
```java
// 示例1：读取文件内容（自动关闭流）
try (FileInputStream fis = new FileInputStream("config.txt")) {
    String content = IoUtils.readUtf8(fis, true);
    System.out.println(content);
}

// 示例2：读取 HTTP 响应（不关闭流）
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
InputStream inputStream = conn.getInputStream();
String response = IoUtils.readUtf8(inputStream, false);
// 后续还需要使用 conn，所以不关闭流

// 示例3：读取 classpath 资源文件
InputStream is = getClass().getResourceAsStream("/template.html");
String template = IoUtils.readUtf8(is, true);

// 示例4：读取网络流
URL url = new URL("https://example.com/data.txt");
InputStream stream = url.openStream();
String data = IoUtils.readUtf8(stream, true);
```

**项目实际使用场景**:
- **读取配置文件**: 读取 classpath 下的配置文件
- **HTTP 响应读取**: 读取第三方 API 的响应内容
- **资源文件读取**: 读取模板文件、静态资源等

**注意事项**:
- `isClose=true` 时，方法内部会关闭流，外部无需再关闭
- `isClose=false` 时，需要外部手动关闭流
- 强制使用 UTF-8 编码，避免乱码问题

---

## 四、实战场景总结

### 场景1：微信支付证书加载
```java
// 微信支付需要证书文件，但证书内容存储在数据库中
// 需要创建临时文件来加载证书

public WxPayService initWxPayService(WxPayConfig config) {
    // 1. 从数据库获取证书内容（字符串）
    String certContent = config.getApiCertContent();

    // 2. 创建临时证书文件
    File certFile = FileUtils.createTempFile(certContent);

    // 3. 使用证书文件初始化微信支付
    WxPayConfig wxPayConfig = new WxPayConfig();
    wxPayConfig.setMchId(config.getMchId());
    wxPayConfig.setAppId(config.getAppId());
    wxPayConfig.setKeyPath(certFile.getAbsolutePath());  // 使用临时文件路径

    WxPayService wxPayService = new WxPayServiceImpl();
    wxPayService.setConfig(wxPayConfig);

    return wxPayService;
    // certFile 会在 JVM 退出时自动删除
}
```

### 场景2：SFTP 文件上传
```java
// SFTP 上传需要 File 对象，但我们只有 byte[] 数据
// 需要创建临时文件

public String uploadToSftp(byte[] content, String remotePath) {
    // 1. 创建临时文件
    File tempFile = FileUtils.createTempFile(content);

    // 2. 上传到 SFTP
    ChannelSftp channelSftp = getSftpChannel();
    try (FileInputStream fis = new FileInputStream(tempFile)) {
        channelSftp.put(fis, remotePath);
    }

    // 3. 返回远程路径
    return buildUrl(remotePath);

    // tempFile 会在 JVM 退出时自动删除
}
```

### 场景3：读取 classpath 资源文件
```java
// 读取邮件模板文件

public String getEmailTemplate(String templateName) {
    // 1. 从 classpath 读取模板文件
    InputStream is = getClass().getResourceAsStream("/templates/email/" + templateName + ".html");
    if (is == null) {
        throw new BusinessException("模板文件不存在");
    }

    // 2. 读取内容
    String template = IoUtils.readUtf8(is, true);

    // 3. 返回模板内容
    return template;
}

// 使用模板发送邮件
String template = getEmailTemplate("welcome");
String emailContent = template.replace("${userName}", user.getName());
emailService.send(user.getEmail(), "欢迎注册", emailContent);
```

### 场景4：文件格式转换
```java
// 将 Excel 文件转换为 CSV 文件

public File convertExcelToCsv(byte[] excelData) {
    // 1. 创建临时 Excel 文件
    File excelFile = FileUtils.createTempFile(excelData);

    // 2. 创建临时 CSV 文件
    File csvFile = FileUtils.createTempFile();

    // 3. 读取 Excel
    Workbook workbook = WorkbookFactory.create(excelFile);
    Sheet sheet = workbook.getSheetAt(0);

    // 4. 写入 CSV
    try (FileWriter writer = new FileWriter(csvFile)) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                writer.write(cell.toString() + ",");
            }
            writer.write("\n");
        }
    }

    // 5. 返回 CSV 文件
    return csvFile;

    // excelFile 和 csvFile 都会在 JVM 退出时自动删除
}
```

### 场景5：HTTP 响应内容读取
```java
// 调用第三方 API 并读取响应

public String callThirdPartyApi(String apiUrl) {
    try {
        // 1. 发送 HTTP 请求
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");

        // 2. 读取响应
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            InputStream inputStream = conn.getInputStream();
            String response = IoUtils.readUtf8(inputStream, true);
            return response;
        } else {
            InputStream errorStream = conn.getErrorStream();
            String errorResponse = IoUtils.readUtf8(errorStream, true);
            throw new BusinessException("API 调用失败：" + errorResponse);
        }
    } catch (IOException e) {
        throw new RuntimeException("HTTP 请求失败", e);
    }
}
```

### 场景6：下载文件并处理
```java
// 下载远程文件，处理后上传

public String downloadAndProcess(String downloadUrl, String uploadPath) {
    // 1. 创建临时文件用于下载
    File downloadFile = FileUtils.createTempFile();

    // 2. 下载文件
    HttpUtil.downloadFile(downloadUrl, downloadFile);

    // 3. 处理文件（例如：压缩）
    File processedFile = FileUtils.createTempFile();
    compressFile(downloadFile, processedFile);

    // 4. 上传处理后的文件
    byte[] content = FileUtil.readBytes(processedFile);
    String resultUrl = fileService.upload(content, uploadPath);

    // 5. 返回上传后的 URL
    return resultUrl;

    // downloadFile 和 processedFile 都会在 JVM 退出时自动删除
}
```

---

## 五、注意事项

### 5.1 临时文件自动删除
1. **JVM 退出时删除**: 调用了 `file.deleteOnExit()`，JVM 正常退出时会删除
2. **异常退出可能不删除**: JVM 异常终止（如 kill -9）可能不会删除
3. **不会立即删除**: 创建后不会立即删除，要等到 JVM 退出
4. **不适合长期运行**: 长期运行的服务（如 Web 应用）可能会堆积大量临时文件

### 5.2 临时文件路径
1. **系统临时目录**: 文件创建在系统临时目录（`java.io.tmpdir`）
   - Windows: `C:\Users\用户名\AppData\Local\Temp`
   - Linux: `/tmp`
2. **文件名随机**: 使用 UUID 生成唯一文件名，避免冲突
3. **无后缀名**: 创建的临时文件没有后缀名

### 5.3 流的关闭
1. **isClose=true**: 方法内部会关闭流，外部无需再关闭
2. **isClose=false**: 需要外部手动关闭流
3. **推荐使用 try-with-resources**: 自动关闭流，避免资源泄露

### 5.4 编码问题
1. **强制 UTF-8**: `IoUtils.readUtf8()` 强制使用 UTF-8 编码
2. **避免乱码**: 统一使用 UTF-8，避免中文乱码
3. **其他编码**: 如需其他编码，使用 Hutool 的 `IoUtil.read()` 方法

### 5.5 异常处理
1. **@SneakyThrows 注解**: FileUtils 方法使用了 Lombok 的 `@SneakyThrows`，会抛出受检异常
2. **捕获 IOException**: 调用时需要捕获 `IOException`
3. **IORuntimeException**: IoUtils 会抛出 Hutool 的 `IORuntimeException`

---

## 六、常见问题

### Q1: 临时文件什么时候会被删除？
A: JVM 正常退出时会删除。但注意：
- JVM 异常终止（如 kill -9）可能不会删除
- 长期运行的服务（如 Web 应用）在重启前不会删除
- 建议定期清理系统临时目录

### Q2: 为什么不立即删除临时文件？
A: 因为临时文件可能在创建后还会被使用（如微信支付证书），所以不能立即删除。使用 `deleteOnExit()` 可以确保使用完后最终会被清理。

### Q3: 长期运行的服务如何避免临时文件堆积？
A:
1. 使用完后手动删除：`file.delete()`
2. 定期清理临时目录
3. 考虑使用内存流代替文件

### Q4: 如何给临时文件指定后缀名？
A: 当前的 `createTempFile()` 不支持指定后缀。如果需要，可以使用 Java 原生的：
```java
File file = File.createTempFile("prefix", ".jpg");
file.deleteOnExit();
```

### Q5: IoUtils.readUtf8() 和 Hutool 的 IoUtil.readUtf8() 有什么区别？
A:
- `IoUtils.readUtf8(in, isClose)`: 可以控制是否关闭流
- `IoUtil.readUtf8(in)`: 不支持控制关闭行为

### Q6: 为什么临时文件使用 UUID 命名？
A: 保证文件名唯一，避免多线程或并发场景下的文件名冲突。

---

## 七、最佳实践

### 实践1：使用完立即删除（短期任务）
```java
// 对于短期任务，使用完立即删除
File tempFile = FileUtils.createTempFile(data);
try {
    // 使用临时文件
    processFile(tempFile);
} finally {
    // 立即删除
    tempFile.delete();
}
```

### 实践2：依赖自动删除（长期任务）
```java
// 对于长期任务，依赖 JVM 退出时自动删除
File certFile = FileUtils.createTempFile(certContent);
// 使用证书文件
WxPayConfig config = new WxPayConfig();
config.setKeyPath(certFile.getAbsolutePath());
// 无需手动删除，JVM 退出时自动清理
```

### 实践3：使用 try-with-resources 读取流
```java
// 推荐使用 try-with-resources 自动关闭流
try (InputStream is = getClass().getResourceAsStream("/template.html")) {
    String content = IoUtils.readUtf8(is, true);
    return content;
}
```

### 实践4：合理选择 isClose 参数
```java
// 场景1：一次性读取完毕，关闭流
String content = IoUtils.readUtf8(inputStream, true);

// 场景2：后续还需要使用连接，不关闭流
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
String response = IoUtils.readUtf8(conn.getInputStream(), false);
// 后续还需要使用 conn
conn.disconnect();
```

---

## 八、运行机制总结

- **类型**: 静态工具类
- **触发方式**: 开发人员主动调用
- **调用位置**: Service、Client 等需要文件/流操作的地方
- **依赖库**:
  - Hutool (`cn.hutool.core.io`)
  - Java IO (`java.io`)
  - Lombok (`@SneakyThrows`)
- **设计模式**: 门面模式（封装复杂的 IO 操作，提供简单接口）
- **线程安全**: 所有方法都是无状态的，线程安全

**文档版本**: v1.0  
**最后更新**: 2025-10-19  
**维护者**: Ashore 团队  
