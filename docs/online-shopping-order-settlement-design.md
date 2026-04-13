# 在线购物订单结算系统设计文档

## 1. 设计目标

本项目用于“软件质量与测试”课程的小组作业，被测对象聚焦于“在线购物订单结算核心模块”。

设计目标如下：

- 仅实现后端核心业务逻辑，不包含前端页面、数据库、第三方支付和外部网络接口。
- 使用标准 Java Maven 项目结构，方便后续接入 JUnit 5、JaCoCo、Checkstyle、SpotBugs。
- 以“方便单元测试”为第一优先级，强调高内聚、低耦合、输入输出明确、异常边界清晰。
- 业务规则中保留足够明显的条件分支，便于开展等价类测试、边界值测试、场景法测试和路径测试。

## 2. 总体设计原则

### 2.1 可测试性优先

- 核心业务尽量放在普通 Java 类和 service 中，不依赖 Spring、数据库或网络。
- 避免全局变量和静态可变状态。
- 不直接写死系统时间；如果后续确实需要时间相关规则，可通过接口注入。
- 金额统一使用 `BigDecimal`，避免浮点误差。
- 核心方法输入、输出、异常明确，便于人工编写单元测试。

### 2.2 模块职责清晰

系统拆分为 4 个核心模块：

1. 商品与购物车模块
2. 优惠规则模块
3. 物流配送模块
4. 订单校验与状态模块

每个模块单独设置 domain、service、exception 等类，避免出现“大而全”的 God Object。

### 2.3 规则显式化

关键规则不隐藏在零散工具方法中，而是显式放在可命名的类或方法中，便于测试时直接命中某一条业务规则。

## 3. 推荐项目结构

推荐使用 Maven 标准目录结构：

```text
soft_test
├─ pom.xml
├─ src
│  ├─ main
│  │  └─ java
│  │     └─ com
│  │        └─ houkiang
│  │           └─ ordersettlement
│  │              ├─ cart
│  │              │  ├─ domain
│  │              │  ├─ service
│  │              │  └─ exception
│  │              ├─ promotion
│  │              │  ├─ domain
│  │              │  ├─ service
│  │              │  └─ exception
│  │              ├─ shipping
│  │              │  ├─ domain
│  │              │  ├─ service
│  │              │  └─ exception
│  │              ├─ order
│  │              │  ├─ domain
│  │              │  ├─ service
│  │              │  └─ exception
│  │              └─ common
│  │                 ├─ exception
│  │                 └─ util
│  └─ test
│     └─ java
│        └─ com
│           └─ houkiang
│              └─ ordersettlement
```

说明：

- `domain`：实体类、值对象、枚举类。
- `service`：核心业务规则。
- `exception`：模块级异常。
- `common`：公共异常基类、金额工具类等轻量公共组件。

## 4. 模块设计

---

## 4.1 商品与购物车模块

### 4.1.1 目标

负责购物车商品管理与金额计算，不处理优惠、运费、订单状态。

### 4.1.2 推荐类清单

#### 实体类 / 值对象

1. `Product`
- 职责：表示可购买商品的基础信息。
- 核心字段：
  - `String productId`
  - `String productName`
  - `BigDecimal unitPrice`
  - `int stock`

2. `CartItem`
- 职责：表示购物车中的单个商品项。
- 核心字段：
  - `Product product`
  - `int quantity`

3. `ShoppingCart`
- 职责：表示购物车聚合根，维护多个购物车条目。
- 核心字段：
  - `List<CartItem> items`

#### Service 类

1. `ShoppingCartService`
- 职责：提供购物车增删改查和金额计算逻辑。

#### 异常类

1. `InvalidQuantityException`
- 职责：商品数量小于等于 0 时抛出。

2. `ProductNotFoundInCartException`
- 职责：移除或修改数量时，商品不在购物车中。

3. `InvalidPriceException`
- 职责：商品价格小于 0 时抛出。

### 4.1.3 核心方法设计

#### `addItem(ShoppingCart cart, Product product, int quantity)`

- 输入：
  - `ShoppingCart cart`
  - `Product product`
  - `int quantity`
- 输出：
  - `void`
- 规则：
  - `product.unitPrice >= 0`
  - `quantity > 0`
  - 若商品已存在，则数量累加
- 可能抛出：
  - `InvalidQuantityException`
  - `InvalidPriceException`
  - `IllegalArgumentException`：购物车或商品为 `null`

#### `removeItem(ShoppingCart cart, String productId)`

- 输入：
  - `ShoppingCart cart`
  - `String productId`
- 输出：
  - `void`
- 规则：
  - 删除指定商品项
- 可能抛出：
  - `ProductNotFoundInCartException`
  - `IllegalArgumentException`

#### `updateQuantity(ShoppingCart cart, String productId, int newQuantity)`

- 输入：
  - `ShoppingCart cart`
  - `String productId`
  - `int newQuantity`
- 输出：
  - `void`
- 规则：
  - `newQuantity > 0`
  - 仅修改已存在商品
- 可能抛出：
  - `InvalidQuantityException`
  - `ProductNotFoundInCartException`
  - `IllegalArgumentException`

#### `calculateItemSubtotal(CartItem item)`

- 输入：
  - `CartItem item`
- 输出：
  - `BigDecimal`
- 规则：
  - 小计 = 单价 × 数量
- 可能抛出：
  - `InvalidQuantityException`
  - `InvalidPriceException`
  - `IllegalArgumentException`

#### `calculateCartSubtotal(ShoppingCart cart)`

- 输入：
  - `ShoppingCart cart`
- 输出：
  - `BigDecimal`
- 规则：
  - 汇总所有购物车项小计
- 可能抛出：
  - `IllegalArgumentException`

### 4.1.4 测试关注点

- 数量边界：1、0、负数。
- 价格边界：0、负数、正常值。
- 重复加入同一商品时的数量累加。
- 空购物车总金额应为 0。

---

## 4.2 优惠规则模块

### 4.2.1 目标

负责优惠券校验、优惠金额计算、叠加规则控制，不处理购物车增删和运费。

### 4.2.2 推荐类清单

#### 枚举类

1. `CouponType`
- 枚举值：
  - `FULL_REDUCTION`
  - `DISCOUNT`
  - `FIXED_AMOUNT`

#### 实体类 / 值对象

1. `Coupon`
- 职责：表示优惠券配置。
- 核心字段：
  - `String couponCode`
  - `CouponType couponType`
  - `BigDecimal thresholdAmount`
  - `BigDecimal reductionAmount`
  - `BigDecimal discountRate`
  - `boolean stackable`

2. `PromotionCalculationResult`
- 职责：封装优惠计算结果，避免方法只返回裸金额。
- 核心字段：
  - `BigDecimal originalAmount`
  - `BigDecimal discountAmount`
  - `BigDecimal finalAmount`
  - `List<String> appliedCouponCodes`

#### Service 类

1. `CouponValidator`
- 职责：校验优惠券使用条件和叠加合法性。

2. `PromotionService`
- 职责：根据优惠券列表计算最终优惠结果。

#### 异常类

1. `CouponNotApplicableException`
- 职责：优惠券不满足金额门槛或条件限制时抛出。

2. `CouponStackNotAllowedException`
- 职责：优惠券组合违反不可叠加规则时抛出。

3. `InvalidCouponException`
- 职责：优惠券配置非法时抛出。

### 4.2.3 核心方法设计

#### `validateCoupon(Coupon coupon, BigDecimal orderAmount)`

- 输入：
  - `Coupon coupon`
  - `BigDecimal orderAmount`
- 输出：
  - `void`
- 规则：
  - 满减券：订单金额必须大于等于门槛
  - 折扣券：折扣率应大于 0 且小于等于 1
  - 固定金额券：减免金额应大于 0
- 可能抛出：
  - `CouponNotApplicableException`
  - `InvalidCouponException`
  - `IllegalArgumentException`

#### `validateStackable(List<Coupon> coupons)`

- 输入：
  - `List<Coupon> coupons`
- 输出：
  - `void`
- 规则：
  - 允许多个优惠券叠加使用
  - 若后续保留 `stackable` 字段，则当前版本统一按“可叠加”处理
  - 多张券按传入顺序依次应用，便于测试顺序差异场景
- 可能抛出：
  - `IllegalArgumentException`

#### `calculateDiscount(BigDecimal orderAmount, Coupon coupon)`

- 输入：
  - `BigDecimal orderAmount`
  - `Coupon coupon`
- 输出：
  - `BigDecimal`
- 规则：
  - 满减券：返回固定减免金额
  - 折扣券：返回 `orderAmount * (1 - discountRate)`
  - 固定金额券：返回固定减免金额
  - 优惠金额不能导致最终金额小于 0
- 可能抛出：
  - `CouponNotApplicableException`
  - `InvalidCouponException`
  - `IllegalArgumentException`

#### `applyCoupons(BigDecimal orderAmount, List<Coupon> coupons)`

- 输入：
  - `BigDecimal orderAmount`
  - `List<Coupon> coupons`
- 输出：
  - `PromotionCalculationResult`
- 规则：
  - 先校验叠加规则，再依次应用优惠
  - 最终金额最小为 0
- 可能抛出：
  - `CouponNotApplicableException`
  - `CouponStackNotAllowedException`
  - `InvalidCouponException`
  - `IllegalArgumentException`

### 4.2.4 测试关注点

- 满减门槛边界：99.99、100、100.01。
- 折扣率边界：0、0.9、1、1.1。
- 固定减免金额大于订单金额时，最终金额是否归零。
- 单券、双券、多券连续叠加场景。
- 不同优惠券顺序下的结果差异场景。

---

## 4.3 物流配送模块

### 4.3.1 目标

负责配送方式校验与运费计算，不处理优惠券和订单状态。

### 4.3.2 推荐类清单

#### 枚举类

1. `DeliveryType`
- 枚举值：
  - `STANDARD`
  - `EXPRESS`
  - `SELF_PICKUP`

2. `RegionType`
- 枚举值：
  - `NORMAL`
  - `REMOTE`

#### 实体类 / 值对象

1. `Address`
- 职责：表示收货地址。
- 核心字段：
  - `String recipientName`
  - `String phone`
  - `String detailAddress`
  - `RegionType regionType`

2. `ShippingFeeRule`
- 职责：表示运费规则配置。
- 核心字段：
  - `DeliveryType deliveryType`
  - `RegionType regionType`
  - `BigDecimal baseFee`
  - `BigDecimal freeShippingThreshold`

3. `ShippingCalculationResult`
- 职责：封装运费计算结果。
- 核心字段：
  - `DeliveryType deliveryType`
  - `BigDecimal shippingFee`
  - `boolean freeShippingApplied`

#### Service 类

1. `ShippingService`
- 职责：校验配送方式，并按订单金额、地区、配送方式计算运费。

#### 异常类

1. `UnsupportedDeliveryTypeException`
- 职责：配送方式不支持或与地区不兼容时抛出。

2. `InvalidAddressException`
- 职责：地址为空、字段缺失、地区为空时抛出。

3. `InvalidShippingRuleException`
- 职责：运费规则配置非法时抛出。

### 4.3.3 核心方法设计

#### `validateAddress(Address address)`

- 输入：
  - `Address address`
- 输出：
  - `void`
- 规则：
  - 收件人、电话、详细地址不能为空
  - 地区类型不能为空
- 可能抛出：
  - `InvalidAddressException`
  - `IllegalArgumentException`

#### `validateDeliveryType(DeliveryType deliveryType, RegionType regionType)`

- 输入：
  - `DeliveryType deliveryType`
  - `RegionType regionType`
- 输出：
  - `void`
- 规则：
  - `SELF_PICKUP` 不收运费
  - 可增加规则：偏远地区不支持 `EXPRESS`
- 可能抛出：
  - `UnsupportedDeliveryTypeException`
  - `IllegalArgumentException`

#### `calculateShippingFee(BigDecimal itemAmount, Address address, DeliveryType deliveryType)`

- 输入：
  - `BigDecimal itemAmount`
  - `Address address`
  - `DeliveryType deliveryType`
- 输出：
  - `ShippingCalculationResult`
- 规则示例：
  - `SELF_PICKUP`：运费为 0
  - `STANDARD + NORMAL`：基础运费 8，满 88 包邮
  - `STANDARD + REMOTE`：基础运费 15，不包邮
  - `EXPRESS + NORMAL`：基础运费 15，满 188 包邮
  - `EXPRESS + REMOTE`：不支持
- 可能抛出：
  - `InvalidAddressException`
  - `UnsupportedDeliveryTypeException`
  - `InvalidShippingRuleException`
  - `IllegalArgumentException`

### 4.3.4 测试关注点

- 包邮边界：87.99、88、88.01。
- 不同地区对应的运费差异。
- 自提场景运费恒为 0。
- 偏远地区选择 `EXPRESS` 的异常分支。

---

## 4.4 订单校验与状态模块

### 4.4.1 目标

负责结算前校验、金额汇总、订单创建和状态流转控制，是系统总协调模块。

### 4.4.2 推荐类清单

#### 枚举类

1. `OrderStatus`
- 枚举值：
  - `PENDING_PAYMENT`
  - `PAID`
  - `SHIPPED`
  - `COMPLETED`
  - `CANCELLED`

#### 实体类 / 值对象

1. `Order`
- 职责：表示订单聚合根。
- 核心字段：
  - `String orderId`
  - `ShoppingCart shoppingCart`
  - `Address address`
  - `DeliveryType deliveryType`
  - `List<Coupon> coupons`
  - `BigDecimal itemAmount`
  - `BigDecimal discountAmount`
  - `BigDecimal shippingFee`
  - `BigDecimal payableAmount`
  - `OrderStatus status`

2. `OrderSummary`
- 职责：封装订单金额汇总结果。
- 核心字段：
  - `BigDecimal itemAmount`
  - `BigDecimal discountAmount`
  - `BigDecimal shippingFee`
  - `BigDecimal payableAmount`

3. `OrderCreateRequest`
- 职责：封装创建订单时的输入参数。
- 核心字段：
  - `ShoppingCart shoppingCart`
  - `Address address`
  - `DeliveryType deliveryType`
  - `List<Coupon> coupons`

#### Service 类

1. `OrderValidator`
- 职责：执行结算前校验，包括空购物车、库存、地址合法性。

2. `OrderPricingService`
- 职责：协调购物车金额、优惠金额、运费，生成最终应付金额。

3. `OrderService`
- 职责：创建订单，并控制订单状态流转。

4. `OrderStatusService`
- 职责：集中管理订单状态迁移规则。

5. `OrderIdGenerator`
- 职责：抽象订单号生成逻辑，避免在业务代码中直接依赖随机值或系统实现。

6. `DefaultOrderIdGenerator`
- 职责：提供默认订单号生成实现；后续测试时可替换为固定返回值的 stub 或 mock。

#### 异常类

1. `EmptyCartException`
- 职责：购物车为空时禁止结算。

2. `InsufficientStockException`
- 职责：商品购买数量超过库存时抛出。

3. `InvalidOrderStatusTransitionException`
- 职责：非法状态流转时抛出。

4. `OrderCreationException`
- 职责：订单创建输入不完整或金额汇总异常时抛出。

### 4.4.3 核心方法设计

#### `validateCartNotEmpty(ShoppingCart cart)`

- 输入：
  - `ShoppingCart cart`
- 输出：
  - `void`
- 规则：
  - 购物车至少包含一个商品项
- 可能抛出：
  - `EmptyCartException`
  - `IllegalArgumentException`

#### `validateStock(ShoppingCart cart)`

- 输入：
  - `ShoppingCart cart`
- 输出：
  - `void`
- 规则：
  - 每个购物车项数量不得大于商品库存
- 可能抛出：
  - `InsufficientStockException`
  - `IllegalArgumentException`

#### `calculateOrderSummary(OrderCreateRequest request)`

- 输入：
  - `OrderCreateRequest request`
- 输出：
  - `OrderSummary`
- 规则：
  - 商品金额 = 购物车总金额
  - 优惠金额 = 优惠模块计算结果
  - 运费 = 物流模块计算结果
  - 应付金额 = 商品金额 - 优惠金额 + 运费
  - 应付金额不得小于 0
- 可能抛出：
  - `EmptyCartException`
  - `InsufficientStockException`
  - `InvalidAddressException`
  - `CouponNotApplicableException`
  - `UnsupportedDeliveryTypeException`
  - `OrderCreationException`

#### `createOrder(OrderCreateRequest request)`

- 输入：
  - `OrderCreateRequest request`
- 输出：
  - `Order`
- 规则：
  - 先校验，再汇总金额，调用 `OrderIdGenerator` 生成订单号，最后创建状态为 `PENDING_PAYMENT` 的订单
- 可能抛出：
  - `EmptyCartException`
  - `InsufficientStockException`
  - `InvalidAddressException`
  - `OrderCreationException`
  - 以及下游模块抛出的业务异常

#### `transitionStatus(Order order, OrderStatus targetStatus)`

- 输入：
  - `Order order`
  - `OrderStatus targetStatus`
- 输出：
  - `void`
- 合法流转建议：
  - `PENDING_PAYMENT -> PAID`
  - `PENDING_PAYMENT -> CANCELLED`
  - `PAID -> SHIPPED`
  - `PAID -> CANCELLED`
  - `SHIPPED -> COMPLETED`
  - `CANCELLED` 和 `COMPLETED` 为终态，不可再迁移
- 可能抛出：
  - `InvalidOrderStatusTransitionException`
  - `IllegalArgumentException`

### 4.4.4 测试关注点

- 空购物车结算异常。
- 库存刚好足够与刚好不足的边界。
- 最终金额组成是否正确。
- 各状态合法流转与非法流转路径。

## 5. 关键业务规则汇总

### 5.1 商品与购物车

- 商品价格必须大于等于 0。
- 购买数量必须大于 0。
- 购物车总商品金额为所有条目小计之和。

### 5.2 优惠规则

- 满减券示例：满 100 减 20。
- 折扣券示例：9 折，即 `discountRate = 0.9`。
- 固定金额券示例：减 10 元。
- 不满足门槛时不可使用。
- 非叠加券不可与其他券同时使用。
- 当前版本允许多个优惠券叠加使用，并按传入顺序依次应用。
- 优惠后金额不得小于 0。

### 5.3 配送规则

- 配送方式：`STANDARD`、`EXPRESS`、`SELF_PICKUP`。
- 地区：`NORMAL`、`REMOTE`。
- 包邮门槛：
  - `STANDARD + NORMAL` 满 88 包邮。
  - `EXPRESS + NORMAL` 满 188 包邮。
  - `SELF_PICKUP` 运费始终为 0。
- `EXPRESS + REMOTE` 视为不支持。

### 5.4 订单状态规则

- 初始状态为 `PENDING_PAYMENT`。
- 已完成和已取消订单不可继续流转。
- 状态迁移必须通过显式校验方法完成。

## 6. 推荐的包内职责分配

### 6.1 `cart` 模块

- `domain` 只存放商品、购物车、购物车项等实体和值对象。
- `service` 只处理购物车操作和金额计算。
- 不直接处理优惠、地址、订单状态。

### 6.2 `promotion` 模块

- `CouponValidator` 专注校验。
- `PromotionService` 专注金额计算和结果封装。
- 这样测试时可以分别验证“是否能用”和“用完后多少钱”。

### 6.3 `shipping` 模块

- 配送方式校验与运费计算集中在 `ShippingService`。
- 地址合法性校验单独成方法，方便单元测试直接覆盖非法输入。

### 6.4 `order` 模块

- `OrderValidator` 专注下单前校验。
- `OrderPricingService` 专注金额汇总。
- `OrderStatusService` 专注状态机规则。
- `OrderService` 只做总协调，避免内部逻辑过重。

## 7. 为单元测试准备的设计建议

- 尽量让 service 方法是无副作用的普通方法，减少共享状态。
- 一个规则尽量对应一个方法或一个小型类，便于独立测试。
- 使用构造器或工厂方法校验实体入参，减少隐式非法状态。
- 对异常类型做细分，便于测试时精准断言。
- 金额计算统一保留两位小数，舍入策略建议固定为 `HALF_UP`。
- 后续代码实现中避免在 service 内部直接 `new Date()`、`UUID.randomUUID()` 等难以断言的行为。
- 订单号通过可注入的 `OrderIdGenerator` 接口生成，方便单元测试替换为固定值。

## 8. 推荐的后续实现范围

第一版代码实现建议只覆盖以下范围：

1. 基础实体、枚举、异常类。
2. 四个模块的核心 service。
3. 最小可运行的 Maven 项目结构。
4. 一个简单的 `OrderService` 作为总入口。

暂不实现：

- 用户系统
- 商品库存持久化
- 支付接口
- 数据库仓储层
- Web 接口控制器

## 9. 确认建议

如果你认可这份设计，下一步我会基于它直接生成 Maven 项目代码骨架与核心业务实现，包括：

- `pom.xml`
- 标准 `src/main/java` 目录
- 4 个模块的实体、枚举、异常、service
- 可直接用于后续 JUnit 5 / JaCoCo / Checkstyle / SpotBugs 的普通 Java 项目结构

如果你希望，我也可以在生成代码前，先把“优惠券是否允许同类型叠加”和“订单号是否需要生成器接口”这两个细节固定下来。
