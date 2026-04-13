# 在线购物订单结算系统

本项目是“软件质量与测试”课程小组作业的被测试项目，目标是提供一个结构清晰、便于单元测试和静态分析的 Java 后端核心业务系统。

项目只实现“订单结算核心模块”，不包含前端页面、数据库、真实支付、外部网络接口。

## 版本信息

- 项目版本：`1.0.0-SNAPSHOT`
- Java 版本：`17`
- 构建工具：`Maven 3.9.x`
- 测试框架：`JUnit 5.10.2`
- 覆盖率工具：`JaCoCo 0.8.12`
- 静态分析工具：`Checkstyle 3.3.1`、`SpotBugs 4.8.6.4`
- Maven 坐标：`com.houkiang:order-settlement:1.0.0-SNAPSHOT`

## 快速检查命令

在项目根目录执行：

```powershell
mvn test
mvn checkstyle:check
mvn spotbugs:check
```

当前项目已通过上述三项检查。

## 项目架构简要说明

源码根包为：

```text
com.houkiang.ordersettlement
```

主要模块：

```text
src/main/java/com/houkiang/ordersettlement
├─ cart        商品与购物车模块
├─ promotion   优惠规则模块
├─ shipping    物流配送模块
├─ order       订单校验与状态模块
└─ common      公共异常与金额工具
```

每个业务模块基本按以下结构组织：

```text
domain      实体类、值对象、枚举类
service     核心业务逻辑
exception   模块专用异常
```

设计原则：

- 使用普通 Java 类实现业务逻辑，方便直接编写 JUnit 5 单元测试。
- 不依赖数据库、Spring、网络接口或真实支付系统。
- 金额使用 `BigDecimal` 计算。
- 订单号通过 `OrderIdGenerator` 接口抽象，测试时可以替换为固定订单号。
- 业务异常按模块拆分，便于测试时精确断言异常类型。

## 可供测试模块和功能

### 1. 商品与购物车模块

对应包：

```text
com.houkiang.ordersettlement.cart
```

核心类：

- `Product`：商品信息，包含商品编号、名称、单价、库存。
- `CartItem`：购物车条目，包含商品和购买数量。
- `ShoppingCart`：购物车聚合，维护购物车条目列表。
- `ShoppingCartService`：购物车业务服务。

可测试功能：

- 商品加入购物车：`ShoppingCartService.addItem`
- 商品移出购物车：`ShoppingCartService.removeItem`
- 商品数量修改：`ShoppingCartService.updateQuantity`
- 单个购物车条目小计计算：`ShoppingCartService.calculateItemSubtotal`
- 购物车总商品金额计算：`ShoppingCartService.calculateCartSubtotal`

建议测试场景：

- 正常加入一个商品。
- 重复加入同一个商品时数量累加。
- 数量为 `0` 或负数时抛出 `InvalidQuantityException`。
- 商品价格为负数时抛出 `InvalidPriceException`。
- 移除不存在的商品时抛出 `ProductNotFoundInCartException`。
- 修改不存在的商品数量时抛出 `ProductNotFoundInCartException`。
- 空购物车总金额为 `0`。

### 2. 优惠规则模块

对应包：

```text
com.houkiang.ordersettlement.promotion
```

核心类：

- `Coupon`：优惠券配置。
- `CouponType`：优惠券类型，包含 `FULL_REDUCTION`、`DISCOUNT`、`FIXED_AMOUNT`。
- `PromotionCalculationResult`：优惠计算结果。
- `CouponValidator`：优惠券合法性校验。
- `PromotionService`：优惠金额计算与多券叠加。

可测试功能：

- 满减优惠计算：`Coupon.fullReduction` + `PromotionService.calculateDiscount`
- 折扣优惠计算：`Coupon.discount` + `PromotionService.calculateDiscount`
- 固定金额优惠券计算：`Coupon.fixedAmount` + `PromotionService.calculateDiscount`
- 优惠券使用条件校验：`CouponValidator.validateCoupon`
- 多个优惠券叠加使用：`PromotionService.applyCoupons`
- 优惠后金额不得小于 `0`：`PromotionService.applyCoupons`

业务规则：

- 满减券示例：满 `100` 减 `20`。
- 折扣券示例：`0.9` 表示 9 折。
- 固定金额券示例：减 `10` 元。
- 多个优惠券允许叠加，并按传入列表顺序依次应用。
- 优惠金额不得超过当前可优惠金额。

建议测试场景：

- 订单金额低于满减门槛时抛出 `CouponNotApplicableException`。
- 订单金额刚好等于满减门槛时可以使用优惠券。
- 折扣率为 `0`、负数或大于 `1` 时抛出 `InvalidCouponException`。
- 固定金额券金额大于订单金额时，最终金额归零。
- 多张优惠券按顺序叠加后金额正确。
- 传入空优惠券列表时，原金额不变。

### 3. 物流配送模块

对应包：

```text
com.houkiang.ordersettlement.shipping
```

核心类：

- `Address`：收货地址。
- `DeliveryType`：配送方式，包含 `STANDARD`、`EXPRESS`、`SELF_PICKUP`。
- `RegionType`：地区类型，包含 `NORMAL`、`REMOTE`。
- `ShippingFeeRule`：运费规则。
- `ShippingCalculationResult`：运费计算结果。
- `ShippingService`：配送方式校验和运费计算。

可测试功能：

- 收货地址合法性校验：`ShippingService.validateAddress`
- 配送方式合法性校验：`ShippingService.validateDeliveryType`
- 运费计算：`ShippingService.calculateShippingFee`
- 满额包邮判断：`ShippingService.calculateShippingFee`
- 不同地区运费规则：`ShippingService.calculateShippingFee`

业务规则：

- `STANDARD + NORMAL`：基础运费 `8`，商品金额满 `88` 包邮。
- `STANDARD + REMOTE`：基础运费 `15`，不包邮。
- `EXPRESS + NORMAL`：基础运费 `15`，商品金额满 `188` 包邮。
- `EXPRESS + REMOTE`：不支持。
- `SELF_PICKUP`：运费始终为 `0`。

建议测试场景：

- 收货地址为 `null` 时抛出 `InvalidAddressException`。
- 收件人、电话、详细地址为空时抛出 `InvalidAddressException`。
- 普通地区标准配送金额为 `87.99`、`88`、`88.01` 的包邮边界。
- 偏远地区标准配送不包邮。
- 偏远地区选择加急配送时抛出 `UnsupportedDeliveryTypeException`。
- 自提方式运费始终为 `0`。

### 4. 订单校验与状态模块

对应包：

```text
com.houkiang.ordersettlement.order
```

核心类：

- `OrderCreateRequest`：创建订单请求。
- `Order`：订单聚合。
- `OrderSummary`：订单金额汇总结果。
- `OrderStatus`：订单状态，包含 `PENDING_PAYMENT`、`PAID`、`SHIPPED`、`COMPLETED`、`CANCELLED`。
- `OrderValidator`：空购物车和库存校验。
- `OrderPricingService`：商品金额、优惠金额、运费、应付金额汇总。
- `OrderStatusService`：订单状态流转校验。
- `OrderService`：创建订单和状态流转的统一入口。
- `OrderIdGenerator`：订单号生成接口。
- `DefaultOrderIdGenerator`：默认订单号生成实现。

可测试功能：

- 空购物车禁止结算：`OrderValidator.validateCartNotEmpty`
- 库存不足禁止下单：`OrderValidator.validateStock`
- 最终应付金额汇总：`OrderPricingService.calculateOrderSummary`
- 创建订单：`OrderService.createOrder`
- 订单状态合法流转判断：`OrderStatusService.canTransition`
- 订单状态流转执行：`OrderStatusService.transitionStatus`

状态流转规则：

- `PENDING_PAYMENT -> PAID`
- `PENDING_PAYMENT -> CANCELLED`
- `PAID -> SHIPPED`
- `PAID -> CANCELLED`
- `SHIPPED -> COMPLETED`
- `COMPLETED` 和 `CANCELLED` 为终态，不可继续流转。

建议测试场景：

- 空购物车创建订单时抛出 `EmptyCartException`。
- 购买数量等于库存时允许下单。
- 购买数量大于库存时抛出 `InsufficientStockException`。
- 创建订单后初始状态为 `PENDING_PAYMENT`。
- 使用自定义 `OrderIdGenerator` 返回固定订单号，断言订单号可控。
- 合法状态流转可以成功修改状态。
- 非法状态流转抛出 `InvalidOrderStatusTransitionException`。
- 综合场景：购物车金额、优惠券、运费共同计算最终应付金额。

## 小组测试分工建议

建议每位同学负责一个模块下所有功能的测试用例：

- 同学 A：商品与购物车模块，重点覆盖数量、价格、增删改查和金额小计。
- 同学 B：优惠规则模块，重点覆盖满减、折扣、固定金额、多券叠加和异常分支。
- 同学 C：物流配送模块，重点覆盖地址校验、配送方式、地区规则和包邮边界。
- 同学 D：订单校验与状态模块，重点覆盖空购物车、库存、金额汇总、订单创建和状态流转。

每个模块建议至少覆盖：

- 正常路径测试。
- 非法输入测试。
- 边界值测试。
- 异常类型断言。
- 至少一个综合场景测试。

## 设计文档

更详细的设计说明见：

```text
docs/online-shopping-order-settlement-design.md
```
