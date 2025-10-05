package com.example.ashore.framework.common.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Key Value 的键值对
 * 这是一个通用的键值对数据结构类,用于存储一对相关联的数据(键和值)
 *
 * 泛型说明:
 * @param <K> Key的类型,可以是任意Java对象类型
 * @param <V> Value的类型,可以是任意Java对象类型
 *
 * 使用场景:
 * 1. 当需要返回两个相关联的数据时(例如:配置项的key和value)
 * 2. 在字典、下拉框等场景中存储键值对数据
 * 3. 作为Map.Entry的简化替代品
 *
 * see《KeyValue 类设计说明文档》for configuration details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyValue<K, V> implements Serializable {

    private K key;
    private V value;

}
