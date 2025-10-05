# CollectionUtils 工具类详细说明

## 📚 类的整体介绍

`CollectionUtils` 是一个强大的集合操作工具类，提供了 30+ 个静态方法来简化 Java 集合的操作。它基于 Java 8+ 的 Stream API 和函数式编程思想设计，可以大幅简化业务代码。

## 📖 Java 函数式接口说明（重要概念）

- Predicate<T>: 断言，接收 T 返回 boolean，用于过滤判断  
  例如：`Predicate<User> isAdult = user -> user.getAge() >= 18;`
- Function<T, R>: 函数，接收 T 返回 R，用于类型转换  
  例如：`Function<User, String> getName = user -> user.getName();`
- BiFunction<T, U, R>: 双参函数，接收 T 和 U 返回 R  
  例如：`BiFunction<User, User, Boolean> isSame = (u1, u2) -> u1.getId().equals(u2.getId());`
- BinaryOperator<T>: 二元操作符，接收两个 T 返回一个 T，常用于合并  
  例如：`BinaryOperator<Integer> sum = (a, b) -> a + b;`
- Supplier<T>: 供应商，无参数返回 T，用于提供对象  
  例如：`Supplier<List<String>> listSupplier = ArrayList::new;`
- Consumer<T>: 消费者，接收 T 无返回值，用于执行操作（消费数据）  
  例如：`Consumer<User> print = user -> System.out.println(user.getName());`

## 🎯 核心功能分类

### 1. 集合判断方法

#### `containsAny(Object source, Object... targets)`
**作用**：判断 source 是否在 targets 数组中
```java
// 示例：检查用户状态是否为待审核或已驳回
String status = "PENDING";
boolean needReview = CollectionUtils.containsAny(status, "PENDING", "REJECTED");
// 结果：true
```

#### `isAnyEmpty(Collection<?>... collections)`
**作用**：判断多个集合中是否有任何一个为空
```java
// 示例：检查多个列表是否都有数据
List<User> users = getUserList();
List<Order> orders = getOrderList();
if (CollectionUtils.isAnyEmpty(users, orders)) {
    throw new BusinessException("数据不完整");
}
```

#### `anyMatch(Collection<T> from, Predicate<T> predicate)`
**作用**：判断集合中是否有任何元素满足条件
```java
// 示例：检查是否有成年用户
List<User> users = getUserList();
boolean hasAdult = CollectionUtils.anyMatch(users, user -> user.getAge() >= 18);
```

---

### 2. 集合过滤方法

#### `filterList(Collection<T> from, Predicate<T> predicate)`
**作用**：过滤集合，返回满足条件的元素列表
```java
// 示例：筛选出所有已激活的用户
List<User> allUsers = getAllUsers();
List<User> activeUsers = CollectionUtils.filterList(allUsers, user -> user.isActive());

// 示例：筛选出价格大于100的商品
List<Product> products = getProducts();
List<Product> expensiveProducts = CollectionUtils.filterList(
    products,
    product -> product.getPrice() > 100
);
```

#### `distinct(Collection<T> from, Function<T, R> keyMapper)`
**作用**：根据指定的键去重
```java
// 示例：按用户ID去重（保留第一个）
List<User> users = getUserList();
List<User> uniqueUsers = CollectionUtils.distinct(users, User::getId);

// 示例：按邮箱去重
List<User> uniqueByEmail = CollectionUtils.distinct(users, User::getEmail);
```

#### `distinct(Collection<T> from, Function<T, R> keyMapper, BinaryOperator<T> cover)`
**作用**：根据键去重，并指定冲突时的保留策略
```java
// 示例：按ID去重，保留更新时间最新的
List<User> users = getUserList();
List<User> latestUsers = CollectionUtils.distinct(
    users,
    User::getId,
    (u1, u2) -> u1.getUpdateTime().after(u2.getUpdateTime()) ? u1 : u2
);
```

---

### 3. 集合转换方法（最常用）

#### `convertList(Collection<T> from, Function<T, U> func)`
**作用**：将集合中的每个元素转换为另一种类型
```java
// 示例1：提取用户名列表
List<User> users = getUserList();
List<String> names = CollectionUtils.convertList(users, User::getName);
// 结果：["张三", "李四", "王五"]

// 示例2：提取用户ID列表
List<Long> userIds = CollectionUtils.convertList(users, User::getId);
// 结果：[1L, 2L, 3L]

// 示例3：将DO转换为VO
List<UserDO> userDOs = userMapper.selectList();
List<UserVO> userVOs = CollectionUtils.convertList(userDOs, user -> {
    UserVO vo = new UserVO();
    vo.setId(user.getId());
    vo.setName(user.getName());
    return vo;
});
```

#### `convertList(Collection<T> from, Function<T, U> func, Predicate<T> filter)`
**作用**：先过滤，再转换
```java
// 示例：获取所有成年用户的姓名
List<User> users = getUserList();
List<String> adultNames = CollectionUtils.convertList(
    users,
    User::getName,
    user -> user.getAge() >= 18
);
```

#### `convertSet(Collection<T> from, Function<T, U> func)`
**作用**：转换为 Set（自动去重）
```java
// 示例：提取所有商品的分类（去重）
List<Product> products = getProducts();
Set<String> categories = CollectionUtils.convertSet(products, Product::getCategory);
// 结果：["电子", "服装", "食品"] （自动去重）
```

#### `convertMap(Collection<T> from, Function<T, K> keyFunc)`
**作用**：将列表转换为 Map，key 由 keyFunc 生成，value 是元素本身
```java
// 示例：构建用户ID到用户对象的映射
List<User> users = getUserList();
Map<Long, User> userMap = CollectionUtils.convertMap(users, User::getId);
// 结果：{1L: User(id=1, name="张三"), 2L: User(id=2, name="李四"), ...}

// 实际应用：批量查询后快速查找
List<Long> userIds = Arrays.asList(1L, 2L, 3L);
List<User> users = userMapper.selectByIds(userIds);
Map<Long, User> userMap = CollectionUtils.convertMap(users, User::getId);

// 使用 Map 快速查找，O(1) 时间复杂度
User user1 = userMap.get(1L);
User user2 = userMap.get(2L);
```

#### `convertMap(Collection<T> from, Function<T, K> keyFunc, Function<T, V> valueFunc)`
**作用**：将列表转换为 Map，key 和 value 都可以自定义
```java
// 示例：构建用户ID到用户名的映射
List<User> users = getUserList();
Map<Long, String> idToNameMap = CollectionUtils.convertMap(
    users,
    User::getId,
    User::getName
);
// 结果：{1L: "张三", 2L: "李四", 3L: "王五"}
```

---

### 4. 集合分组方法

#### `convertMultiMap(Collection<T> from, Function<T, K> keyFunc)`
**作用**：按 key 分组，返回 Map<K, List<T>>（一对多）
```java
// 示例1：按部门分组用户
List<User> users = getUserList();
Map<Long, List<User>> deptUserMap = CollectionUtils.convertMultiMap(users, User::getDeptId);
// 结果：{
//   1L: [User(deptId=1, name="张三"), User(deptId=1, name="李四")],
//   2L: [User(deptId=2, name="王五")]
// }

// 实际应用：
Long deptId = 1L;
List<User> deptUsers = deptUserMap.get(deptId);  // 获取部门1的所有用户
```

#### `convertMultiMap(Collection<T> from, Function<T, K> keyFunc, Function<T, V> valueFunc)`
**作用**：按 key 分组，value 经过转换
```java
// 示例：按部门分组，只保留用户名
List<User> users = getUserList();
Map<Long, List<String>> deptNameMap = CollectionUtils.convertMultiMap(
    users,
    User::getDeptId,
    User::getName
);
// 结果：{1L: ["张三", "李四"], 2L: ["王五"]}
```

---

### 5. 集合扁平化方法（处理嵌套集合）

#### `convertListByFlatMap(Collection<T> from, Function<T, Stream<U>> func)`
**作用**：将嵌套集合"拍平"成一维集合
```java
// 示例：获取所有用户的所有角色（用户有多个角色）
List<User> users = getUserList();  // 每个用户有 roles 字段 (List<Role>)
List<Role> allRoles = CollectionUtils.convertListByFlatMap(
    users,
    user -> user.getRoles().stream()
);
// 结果：所有用户的角色合并到一个列表中

// 另一个例子：订单 -> 订单项
List<Order> orders = getOrders();  // 每个订单有多个订单项
List<OrderItem> allItems = CollectionUtils.convertListByFlatMap(
    orders,
    order -> order.getItems().stream()
);
```

---

### 6. 集合查找方法

#### `findFirst(Collection<T> from, Predicate<T> predicate)`
**作用**：查找第一个满足条件的元素
```java
// 示例：查找第一个管理员用户
List<User> users = getUserList();
User admin = CollectionUtils.findFirst(users, user -> "ADMIN".equals(user.getRole()));
// 如果找不到，返回 null
```

#### `getFirst(List<T> from)`
**作用**：获取列表的第一个元素（安全方法，不会抛异常）
```java
// 示例：
List<User> users = getUserList();
User firstUser = CollectionUtils.getFirst(users);
// 如果列表为空，返回 null（不会抛 IndexOutOfBoundsException）
```

#### `getMaxValue(Collection<T> from, Function<T, V> valueFunc)`
**作用**：获取集合中某个字段的最大值
```java
// 示例1：获取最高价格
List<Product> products = getProducts();
BigDecimal maxPrice = CollectionUtils.getMaxValue(products, Product::getPrice);

// 示例2：获取最晚的更新时间
Date latestUpdate = CollectionUtils.getMaxValue(users, User::getUpdateTime);
```

#### `getMinValue(List<T> from, Function<T, V> valueFunc)`
**作用**：获取集合中某个字段的最小值
```java
// 示例：获取最低价格
BigDecimal minPrice = CollectionUtils.getMinValue(products, Product::getPrice);
```

---

### 7. 集合聚合方法

#### `getSumValue(Collection<T> from, Function<T, V> valueFunc, BinaryOperator<V> accumulator)`
**作用**：对集合中的某个字段求和
```java
// 示例1：计算所有商品的总价格
List<Product> products = getProducts();
BigDecimal totalPrice = CollectionUtils.getSumValue(
    products,
    Product::getPrice,
    BigDecimal::add
);

// 示例2：计算总数量（Integer）
Integer totalQuantity = CollectionUtils.getSumValue(
    products,
    Product::getStock,
    Integer::sum
);
```

---

### 8. 集合比较方法

#### `diffList(Collection<T> oldList, Collection<T> newList, BiFunction<T, T, Boolean> sameFunc)`
**作用**：对比新旧两个列表，找出新增、修改、删除的数据
```java
// 示例：对比用户角色的变化
List<UserRole> oldRoles = getOldUserRoles(userId);
List<UserRole> newRoles = getNewUserRoles(userId);

List<List<UserRole>> diff = CollectionUtils.diffList(
    oldRoles,
    newRoles,
    (old, newRole) -> old.getRoleId().equals(newRole.getRoleId())
);

List<UserRole> toCreate = diff.get(0);  // 需要新增的角色
List<UserRole> toUpdate = diff.get(1);  // 需要更新的角色
List<UserRole> toDelete = diff.get(2);  // 需要删除的角色

// 执行相应的数据库操作
if (!toCreate.isEmpty()) {
    userRoleMapper.batchInsert(toCreate);
}
if (!toUpdate.isEmpty()) {
    userRoleMapper.batchUpdate(toUpdate);
}
if (!toDelete.isEmpty()) {
    userRoleMapper.batchDelete(toDelete);
}
```

---

## 🔧 Java 函数式接口速查

| 接口 | 参数 | 返回值 | 用途 | 示例 |
|------|------|--------|------|------|
| `Predicate<T>` | T | boolean | 判断/过滤 | `user -> user.getAge() >= 18` |
| `Function<T, R>` | T | R | 转换/映射 | `User::getName` 或 `user -> user.getName()` |
| `BiFunction<T, U, R>` | T, U | R | 双参数转换 | `(u1, u2) -> u1.getId().equals(u2.getId())` |
| `BinaryOperator<T>` | T, T | T | 二元操作/合并 | `BigDecimal::add` 或 `(a, b) -> a + b` |
| `Supplier<T>` | 无 | T | 提供对象 | `ArrayList::new` |
| `Consumer<T>` | T | void | 消费/执行 | `user -> System.out.println(user)` |

---

## 💡 实际业务场景示例

### 场景1：用户列表转VO，并过滤掉未激活的用户
```java
List<UserDO> userDOs = userMapper.selectList();

// 方式1：先过滤再转换
List<UserVO> userVOs = CollectionUtils.convertList(
    userDOs,
    this::convertToVO,
    user -> user.getStatus() == 1
);

// 方式2：先转换再过滤
List<UserVO> userVOs2 = CollectionUtils.filterList(
    CollectionUtils.convertList(userDOs, this::convertToVO),
    vo -> vo.getStatus() == 1
);
```

### 场景2：批量查询并构建映射表
```java
// 获取订单中所有的用户ID
List<Order> orders = getOrders();
Set<Long> userIds = CollectionUtils.convertSet(orders, Order::getUserId);

// 批量查询用户信息
List<User> users = userMapper.selectByIds(userIds);
Map<Long, User> userMap = CollectionUtils.convertMap(users, User::getId);

// 填充订单的用户信息
orders.forEach(order -> {
    User user = userMap.get(order.getUserId());
    order.setUserName(user.getName());
});
```

### 场景3：统计每个部门的用户数
```java
List<User> users = getUserList();

// 按部门分组
Map<Long, List<User>> deptUserMap = CollectionUtils.convertMultiMap(users, User::getDeptId);

// 统计每个部门的人数
Map<Long, Integer> deptCountMap = new HashMap<>();
deptUserMap.forEach((deptId, userList) -> {
    deptCountMap.put(deptId, userList.size());
});
```

---

## ⚠️ 注意事项

1. **所有方法都是静态的**：通过 `CollectionUtils.方法名()` 调用
2. **自动过滤 null**：`convertList`、`convertSet` 等方法会自动过滤掉 null 元素
3. **空集合安全**：传入 null 或空集合会返回空集合，不会抛异常
4. **Stream 延迟执行**：内部使用 Stream API，在调用终止操作前不会执行

---

## 🎓 学习建议

1. **从简单方法开始**：先掌握 `convertList`、`filterList`、`convertMap`
2. **理解函数式接口**：重点学习 `Function`、`Predicate` 的用法
3. **对比传统写法**：体会工具类如何简化代码
4. **在实际项目中使用**：多练习才能熟练掌握

---

## 📌 运行机制总结

- **类型**：静态工具类
- **触发方式**：开发人员主动调用
- **调用位置**：Service、Controller、Mapper 等任何地方
- **依赖库**：Hutool、Guava、Spring、Java Stream API
- **设计模式**：门面模式（封装复杂操作，提供简单接口）