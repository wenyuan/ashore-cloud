package com.example.ashore.framework.common.util.io;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import lombok.SneakyThrows;

import java.io.File;

/**
 * 文件工具类
 * 提供临时文件的创建和管理功能，主要用于在程序运行期间创建临时文件，并在 JVM 退出时自动清理
 *
 * 使用场景：
 * - 文件导出时需要临时存储数据，如 Excel 导出、PDF 生成等
 * - 文件上传预处理，需要临时保存上传的文件进行校验或转换
 * - 单元测试中需要创建临时文件进行测试
 * - 缓存数据到本地临时文件，避免占用过多内存
 *
 * 核心依赖库：
 * - Hutool (cn.hutool.core.io.FileUtil) 提供文件读写操作的工具方法</li>
 * - Hutool (cn.hutool.core.util.IdUtil) 提供 UUID 生成，确保临时文件名唯一</li>
 * - Lombok (@SneakyThrows)              简化异常处理，将受检异常转为非受检异常</li>
 *
 */
public class FileUtils {

    /**
     * 创建临时文件
     * 作用：创建一个临时文件并写入字符串内容，文件以 UTF-8 编码保存。
     *      该文件会在 JVM 正常退出时自动删除，无需手动清理。
     *
     * 方法重载说明：
     * 这是一个重载方法 (Overload)，Java 允许同名方法有不同的参数类型。
     * 本方法接收 String 类型参数，另一个重载方法接收 byte[] 类型参数。
     *
     * @param data 文件内容，以字符串形式传入。
     *             示例值："用户数据导出\n姓名,年龄\n张三,25" 或 "{\"name\":\"test\"}"
     * @return     创建的临时文件对象 (File)
     *             示例: File 对象指向 /tmp/a3f5e9d8b2c14e7f.tmp (Linux)
     *                            或 C:\Users\xxx\AppData\Local\Temp\a3f5e9d8b2c14e7f.tmp (Windows)
     * @throws IOException 文件创建或写入失败时抛出 (由 @SneakyThrows 自动处理)
     */
    @SneakyThrows  // Lombok 注解: 自动捕获并重新抛出受检异常 IOException,无需显式 try-catch
    public static File createTempFile(String data) {
        // 调用无参的 createTempFile() 方法创建空临时文件
        File file = createTempFile();

        // 使用 Hutool 工具类将字符串以 UTF-8 编码写入文件
        // FileUtil.writeUtf8String() 会自动处理字符编码和文件流的关闭
        FileUtil.writeUtf8String(data, file);

        return file;  // 返回已写入内容的临时文件对象
    }

    /**
     * 创建临时文件
     * 作用：创建一个临时文件并写入字节数组内容，适用于二进制数据(如图片、PDF 等)。
     *      该文件会在 JVM 正常退出时自动删除，无需手动清理。
     *
     * 方法重载说明:
     * 这是一个重载方法 (Overload)，与 createTempFile(String data) 同名但参数类型不同。
     * 本方法用于处理二进制数据，如图片、文档等。
     *
     * @param data 文件内容,以字节数组形式传入。
     *             示例值: 图片的字节数组 byte[]{-119, 80, 78, 71...} (PNG 文件头)
     *                    或 PDF 文件的字节数组
     * @return 创建的临时文件对象 (File)
     *         示例: File 对象指向系统临时目录下的随机文件名文件
     * @throws IOException 文件创建或写入失败时抛出 (由 @SneakyThrows 自动处理)
     */
    @SneakyThrows  // Lombok 注解: 自动处理 IOException,简化异常声明
    public static File createTempFile(byte[] data) {
        // 调用无参的 createTempFile() 方法创建空临时文件
        File file = createTempFile();

        // 使用 Hutool 工具类将字节数组直接写入文件
        // FileUtil.writeBytes() 会自动处理文件流的打开和关闭
        FileUtil.writeBytes(data, file);

        return file;  // 返回已写入内容的临时文件对象
    }

    /**
     * 创建临时文件，无内容
     *
     * 作用：
     * 在系统临时目录下创建一个空文件，文件名使用 UUID 确保唯一性。
     * 该文件会在 JVM 正常退出时自动删除。
     *
     * 实现原理：
     * - 使用 File.createTempFile() 创建临时文件
     * - 使用 IdUtil.simpleUUID() 生成唯一文件名前缀，避免文件名冲突
     * - 调用 file.deleteOnExit() 注册 JVM 关闭钩子，程序退出时自动删除
     *
     * 注意事项:
     * - deleteOnExit() 仅在 JVM 正常退出时生效，异常终止(如 kill -9)不会删除
     * - 临时文件存储在系统临时目录，Linux 通常是 /tmp，Windows 通常是 %TEMP%
     * - 大量使用临时文件可能占用磁盘空间，建议及时清理或使用完立即删除
     *
     * @return 创建的空临时文件对象 (File)
     *         示例: Windows 系统返回 C:\Users\xxx\AppData\Local\Temp\a3f5e9d8b2c14e7f.tmp
     *               Linux 系统返回 /tmp/a3f5e9d8b2c14e7f.tmp
     * @throws IOException 文件创建失败时抛出，可能原因：
     *                     - 磁盘空间不足
     *                     - 临时目录不存在或无写入权限
     *                     - 文件系统错误
     *                     (由 @SneakyThrows 自动处理，调用方无需显式捕获)
     */
    @SneakyThrows  // Lombok 注解：自动将 IOException 转换为运行时异常，简化方法签名
    public static File createTempFile() {
        // 创建临时文件:
        // - 第一个参数: 文件名前缀,使用 UUID 确保唯一性 (如 "a3f5e9d8b2c14e7f")
        //   IdUtil.simpleUUID() 生成不带横线的 32 位 UUID 字符串
        // - 第二个参数: 文件扩展名,传 null 表示使用默认的 .tmp 扩展名
        // - 返回值: 创建的 File 对象,指向系统临时目录下的文件
        File file = File.createTempFile(IdUtil.simpleUUID(), null);

        // 注册 JVM 关闭钩子 (Shutdown Hook):
        // 当 JVM 正常退出时 (如程序运行结束、调用 System.exit() 等),
        // 会自动删除此文件,避免临时文件占用磁盘空间
        file.deleteOnExit();

        return file;  // 返回创建的空临时文件对象
    }

}
