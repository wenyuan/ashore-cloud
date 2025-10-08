package com.example.ashore.framework.common.util.object;

import cn.hutool.core.bean.BeanUtil;
import com.example.ashore.framework.common.pojo.PageResult;
import com.example.ashore.framework.common.util.collection.CollectionUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 * Bean 工具类
 * 提供对象属性复制和对象转换的工具方法，主要用于在不同层级之间转换 JavaBean 对象。
 * 例如：
 * - 数据库实体(DO)与业务对象(BO)、数据传输对象(DTO)、视图对象(VO)之间的转换
 * - 批量转换集合数据，如将 List<DO> 转换为 List<VO>
 * - 分页数据转换，如将 PageResult<DO> 转换为 PageResult<VO>
 * - 对象属性复制，将源对象的属性值复制到目标对象
 *
 * 核心依赖库：
 * Hutool BeanUtil：高性能的 Bean 工具类，提供底层的对象转换功能
 * 实现说明：
 * 1. 默认使用 {@link cn.hutool.core.bean.BeanUtil} 作为实现类，虽然不同 bean 工具的性能有差别，但是对绝大多数的场景，不用在意这点性能
 * 2. 针对复杂的对象转换，可以搜参考 AuthConvert 实现，通过 mapstruct + default 配合实现
 */
public class BeanUtils {

    /**
     * 将源对象转换为目标类型的新对象
     *
     * 通过反射机制创建目标类型的实例，并将源对象中同名同类型的属性值复制到新对象中。
     *
     * @param source      源对象，可以为任意类型。示例：UserDO 对象
     * @param targetClass 目标类的 Class 对象。示例：UserVO.class
     * @return            转换后的目标类型对象
     *                    如果源对象为 null 则返回 null
     * @param <T>         目标对象的泛型类型
     */
    public static <T> T toBean(Object source, Class<T> targetClass) {
        // 委托给 Hutool 的 BeanUtil 进行对象转换
        return BeanUtil.toBean(source, targetClass);
    }

    /**
     * 将源对象转换为目标类型的新对象，并在转换后执行额外操作
     *
     * 除了基本的对象转换外，还支持通过 Consumer 对转换后的对象进行自定义处理，
     * 例如设置额外的属性、进行数据校验等。
     *
     * @param source      源对象，可以为任意类型。示例：UserDO 对象
     * @param targetClass 目标类的 Class 对象。示：UserVO.class
     * @param peek        转换后的回调函数，用于对转换后的对象进行额外处理。示例：user -> user.setNickname("默认昵称")
     * @return            转换并处理后的目标类型对象
     * @param <T>         目标对象的泛型类型
     */
    public static <T> T toBean(Object source, Class<T> targetClass, Consumer<T> peek) {
        // 先执行基本的对象转换
        T target = toBean(source, targetClass);
        // 如果转换结果不为空，则执行回调函数进行额外处理
        if (target != null) {
            peek.accept(target);  // Consumer.accept() 方法用于执行自定义的处理逻辑
        }
        return target;
    }

    /**
     * 批量将源对象集合转换为目标类型的对象集合
     *
     * 遍历源集合中的每个元素，逐一转换为目标类型，并返回转换后的新集合。
     * 适用于需要批量转换数据的场景，如将数据库查询结果转换为前端展示对象。
     *
     *
     * @param source     源对象集合。示例：List<UserDO> 列表
     * @param targetType 标类的 Class 对象。示例:UserVO.class
     * @return           转换后的目标类型集合，
     *                   如果源集合为 null 则返回 null。
     * @param <S>        源对象的泛型类型
     * @param <T>        目标对象的泛型类型
     */
    public static <S, T> List<T> toBean(List<S> source, Class<T> targetType) {
        // 如果源集合为 null,直接返回 null
        if (source == null) {
            return null;
        }
        // 使用 CollectionUtils 工具类的 convertList 方法进行批量转换
        // Lambda 表达式 s -> toBean(s, targetType) 表示对每个元素执行 toBean 转换
        return CollectionUtils.convertList(source, s -> toBean(s, targetType));
    }

    /**
     * 批量将源对象集合转换为目标类型的对象集合，并对每个转换后的对象执行额外操作
     *
     * 在批量转换的基础上，支持对每个转换后的对象进行自定义处理。
     *
     * @param source     源对象集合。示例：List<UserDO> 列表
     * @param targetType 目标类的 Class 对象。示例:UserVO.class
     * @param peek       转换后的回调函数，对每个转换后的对象进行处理。示例：user -> user.setOnlineStatus("在线")
     * @return           转换并处理后的目标类型集合
     * @param <S>        源对象的泛型类型
     * @param <T>        目标对象的泛型类型
     */
    public static <S, T> List<T> toBean(List<S> source, Class<T> targetType, Consumer<T> peek) {
        // 先执行批量转换
        List<T> list = toBean(source, targetType);
        // 如果转换结果不为空，则对每个元素执行回调函数
        if (list != null) {
            list.forEach(peek);  // forEach 方法遍历集合，对每个元素执行 peek 操作
        }
        return list;
    }

    /**
     * 将分页结果对象转换为目标类型的分页结果对象
     *
     * 转换分页数据中的列表内容，保留分页信息(如总记录数)。
     * 适用于将数据库分页查询结果转换为前端所需的分页数据格式。
     *
     * @param source     源分页结果对象。示例:PageResult<UserDO> 对象
     * @param targetType 目标类的 Class 对象。示例:UserVO.class
     * @return           转换后的目标类型分页结果对象。
     * @param <S>        源对象的泛型类型
     * @param <T>        目标对象的泛型类型
     */
    public static <S, T> PageResult<T> toBean(PageResult<S> source, Class<T> targetType) {
        // 调用重载方法，peek 参数传 null 表示不需要额外处理
        return toBean(source, targetType, null);
    }

    /**
     * 将分页结果对象转换为目标类型的分页结果对象，并对每个转换后的对象执行额外操作
     *
     * 在分页数据转换的基础上，支持对转换后的每个对象进行自定义处理。
     *
     * @param source     源分页结果对象。示例:PageResult<UserDO> 对象，包含用户列表和总数
     * @param targetType 目标类的 Class 对象。示例:UserVO.class
     * @param peek       转换后的回调函数，对每个转换后的对象进行处理。示例：user -> user.setAvatarUrl("默认头像")
     * @return           转换并处理后的目标类型分页结果对象。
     * @param <S>        源对象的泛型类型
     * @param <T>        目标对象的泛型类型
     */
    public static <S, T> PageResult<T> toBean(PageResult<S> source, Class<T> targetType, Consumer<T> peek) {
        // 如果源分页对象为 null,直接返回 null
        if (source == null) {
            return null;
        }
        // 转换分页结果中的数据列表
        List<T> list = toBean(source.getList(), targetType);
        // 如果提供了回调函数，则对每个元素执行处理
        if (peek != null) {
            list.forEach(peek);
        }
        // 创建新的分页结果对象，保留原有的总记录数
        return new PageResult<>(list, source.getTotal());
    }

    /**
     * 复制源对象的属性值到目标对象
     *
     * 将源对象中与目标对象同名同类型的属性值复制到目标对象中，不会创建新对象。
     * 适用于对象属性更新场景，如将请求参数的属性值复制到数据库实体对象中。
     *
     * 复制规则：
     * - 只复制源对象和目标对象都存在的同名属性
     * - 如果源对象存在某个属性，但目标对象不存在该属性，则忽略该属性，不会报错
     * - 如果目标对象存在某个属性，但源对象不存在该属性，则目标对象的该属性值保持不变
     * - 属性名必须完全匹配(区分大小写)，属性类型需要兼容(可自动类型转换)
     *
     * @param source 源对象，提供属性值。示例：UserUpdateDTO 对象
     * @param target 目标对象，接收属性值。示例：UserDO 对象
     */
    public static void copyProperties(Object source, Object target) {
        // 如果源对象或目标对象为 null，则不执行复制操作
        if (source == null || target == null) {
            return;
        }
        // 调用 Hutool 的 copyProperties 方法进行属性复制
        // 第三个参数 false 表示不忽略大小写，严格按照属性名匹配
        BeanUtil.copyProperties(source, target, false);
    }

}
