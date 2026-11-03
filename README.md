# 在线购物订单结算系统

本项目由“软件质量与测试”课程的 Java 订单结算核心系统演进而来，现已完成第四阶段：使用 Spring Boot 提供 HTTP 接口，通过 MyBatis 接入 MySQL，并实现库存事务与幂等下单。

商品价格、库存和优惠规则来自数据库。结算预览只计算金额；订单创建会在同一事务中扣减库存并保存订单与历史快照，取消订单会回补库存。带 `Idempotency-Key` 的重复请求只创建一次订单。真实支付和用户权限属于后续范围。

## 版本信息

- 项目版本：`1.0.0-SNAPSHOT`
- Java 版本：`17`
- Web 框架：`Spring Boot 3.5.16`、`Spring MVC`、`Jakarta Validation`
- 持久层：`MyBatis Spring Boot Starter 3.0.5`、`MySQL 8.4`，支持本地 H2 文件数据库
- 数据库迁移：`Flyway 11.7.2`
- 构建工具：`Maven 3.9.x`
- 测试框架：`JUnit 5.12.2`（由 Spring Boot 管理）
- Mock 框架：`Mockito 5.17.0`（由 Spring Boot 管理）
- 覆盖率工具：`JaCoCo 0.8.12`
- 静态分析工具：`Checkstyle 3.3.1`、`SpotBugs 4.8.6.4`
- Maven 坐标：`com.houkiang:order-settlement:1.0.0-SNAPSHOT`

## 启动与调用

在 `soft_test` 目录执行：

```powershell
mvn spring-boot:run
```

默认监听 `8080` 端口，可用环境变量 `PORT` 调整。默认 `local` profile 将数据持久化到 `data/` 下的 H2 文件；首次启动自动迁移表结构并写入演示目录，重启不会重置数据。使用 MySQL 时设置连接环境变量，并启用 `mysql` profile，详见[数据库运行文档](docs/database.md)。

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/api/products` | 查询服务端商品价格与库存 |
| GET | `/api/coupons` | 查询服务端优惠规则 |
| POST | `/api/settlements/preview` | 校验请求、商品与优惠券，返回结算金额 |
| POST | `/api/orders` | 创建订单并扣减库存；可用 `Idempotency-Key` 安全重试 |
| GET | `/api/orders/{orderId}` | 查询订单与历史快照 |
| PATCH | `/api/orders/{orderId}/status` | 校验并保存状态；取消时回补已预占库存 |

另开终端，在同一目录调用示例请求：

```powershell
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/settlements/preview' -ContentType 'application/json; charset=utf-8' -InFile 'docs/examples/preview.json'
```

示例购买两件 P001，依次应用满减、折扣和固定金额券，返回商品金额 `100.00`、优惠 `38.00`、运费 `0.00`、应付 `62.00`。
请求约束、错误码和完整示例见 [HTTP 接口文档](docs/http-api.md)；库存事务与幂等语义见[第四阶段文档](docs/transactional-checkout.md)。

## 构建与质量检查

```powershell
mvn clean verify checkstyle:check spotbugs:check
java -jar target/order-settlement-1.0.0-SNAPSHOT.jar
```

首次执行需要联网下载 Maven 插件和测试依赖；依赖缓存完整后可添加 `-o` 离线运行。
验收使用 `clean` 清除旧测试字节码，确保结果来自当前源码。Surefire 在未发现任何测试时会使构建失败。

2026-11-02 第四阶段验收：**201 次常规测试、33 次原生 MySQL 集成测试全部成功**，失败、错误、跳过均为 0，Checkstyle 与 SpotBugs 均为 0 项。常规测试的全项目行覆盖率 **96.30%（729/757）**，分支覆盖率 **85.64%（167/195）**。

常规测试保留前三阶段的 182 次执行，新增并发库存、取消回补、幂等重试、失败回滚和旧订单迁移测试。MySQL 集成测试通过 `-Pmysql-it` 显式启用，需要专用测试库，配置见[数据库运行文档](docs/database.md)。验收结果见[第四阶段验收报告](docs/stage-4-verification.md)；[第三阶段](docs/stage-3-verification.md)和[第二阶段](docs/stage-2-verification.md)报告保留历史快照。

- [HTML 覆盖率报告](target/site/jacoco/index.html)、[XML 覆盖率数据](target/site/jacoco/jacoco.xml)。
- [Surefire 执行报告](target/surefire-reports/)、[第一阶段业务测试报告](软件测试报告.md)。

以上生成报告位于 `target/`，每次构建会更新，执行验收命令后可查看。静态检查针对主代码，使用仓库现有规则及 SpotBugs 排除配置；未设置覆盖率门槛。

测试源码中，购物车测试位于 `cart/service/`，订单测试位于 `order/service/`，路径与包名一致。
`OrderCheckoutTest` 使用真实服务串联购物车、三类优惠券、运费和订单状态流转，仅将订单号替换为固定值。

## 项目架构简要说明

源码根包为：

```text
com.houkiang.ordersettlement
```

主要模块：

```text
src/main/java/com/houkiang/ordersettlement
├─ OrderSettlementApplication.java  Spring Boot 启动入口
├─ config      Spring 服务装配
├─ web         HTTP Controller、请求/响应 DTO、统一异常处理
├─ catalog     商品与优惠券查询接口
├─ application 订单事务、库存扣减与幂等编排
├─ persistence MyBatis 仓储实现、Mapper 与数据库记录
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

- 核心业务使用普通 Java 类实现，由配置类装配为 Spring Bean，保留直接进行单元测试的能力。
- Controller 负责 HTTP 边界，预览服务负责查询目录和编排业务，金额计算复用原有服务。
- 请求 DTO 使用 Jakarta Validation 校验，业务异常和请求错误映射为统一 JSON 与对应 HTTP 状态码。
- 通过 `ProductCatalog`、`CouponCatalog`、`OrderRepository` 隔离数据库实现。
- 保存订单历史快照，原子写入主表与明细；条件扣减防止超卖，取消时回补库存。
- 通过持久化幂等键防止请求重试重复下单，状态条件更新防止并发覆盖。
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
