# 订单结算系统测试总结报告

> 本文为第一阶段业务测试快照。第二阶段保留这些用例，当前依赖版本和全量测试结果见[第二阶段验收报告](docs/stage-2-verification.md)。

## 1. 引言

本报告针对订单结算系统中的**商品与购物车模块**以及**订单校验与状态模块**开展了单元测试与跨模块场景测试。测试基于 JUnit 5 和 Mockito 框架，覆盖了核心业务逻辑、异常处理和边界条件，旨在验证功能的正确性、健壮性及状态流转的合规性。

## 2. 测试范围

| 模块 | 包路径 | 核心测试类 | 测试功能点 |
|------|--------|------------|------------|
| 商品与购物车 | `com.houkiang.ordersettlement.cart` | `ShoppingCartServiceTest` | 加入商品、移除商品、修改数量、小计计算、总金额计算 |
| 订单校验与状态 | `com.houkiang.ordersettlement.order` | `OrderValidatorTest`<br>`OrderStatusServiceTest`<br>`OrderPricingServiceTest`<br>`OrderServiceTest` | 空购物车校验、库存校验、金额汇总、订单创建、状态流转 |

## 3. 测试环境

| 项目 | 内容 |
|------|------|
| 语言 | Java 17 |
| 单元测试框架 | JUnit 5.10.2 (Jupiter) |
| Mock 框架 | Mockito 5.11.0 (用于 `OrderPricingServiceTest`、`OrderServiceTest`) |
| 断言方式 | JUnit Assertions、Mockito verify |
| 构建工具 | Maven 3.9.14 |
| 测试执行 | IDE 或 Maven Surefire 插件 |

## 4. 测试用例设计与覆盖

### 4.1 商品与购物车模块 (`ShoppingCartServiceTest`)

#### 4.1.1 `addItem` 方法

| 测试场景 | 预期结果 | 测试方法 |
|----------|----------|----------|
| 正常加入一个商品 | 购物车中新增条目，数量正确 | `addItem_ShouldAddNewItem_WhenProductNotInCart` |
| 重复加入同一商品 | 数量累加 | `addItem_ShouldAccumulateQuantity_WhenProductAlreadyInCart` |
| 加入数量为 0 | 抛出 `InvalidQuantityException` | `addItem_ShouldThrowInvalidQuantityException_WhenQuantityIsZero` |
| 加入数量为负数 | 抛出 `InvalidQuantityException` | `addItem_ShouldThrowInvalidQuantityException_WhenQuantityIsNegative` |
| 商品价格为负数 | 抛出 `InvalidPriceException`（创建商品时） | `productCreation_ShouldThrowInvalidPriceException_WhenPriceIsNegative` |
| 传入 null 购物车或商品 | 抛出 `NullPointerException` | `addItem_ShouldThrowNullPointerException_WhenCartIsNull` 等 |

#### 4.1.2 `removeItem` 方法

| 测试场景 | 预期结果 | 测试方法 |
|----------|----------|----------|
| 移除存在的商品 | 购物车中条目消失 | `removeItem_ShouldRemoveItem_WhenProductExists` |
| 移除不存在的商品 | 抛出 `ProductNotFoundInCartException` | `removeItem_ShouldThrowProductNotFoundInCartException_WhenProductNotInCart` |
| 传入空白 productId | 抛出 `IllegalArgumentException` | `removeItem_ShouldThrowIllegalArgumentException_WhenProductIdIsBlank` |

#### 4.1.3 `updateQuantity` 方法

| 测试场景 | 预期结果 | 测试方法 |
|----------|----------|----------|
| 修改存在的商品数量 | 数量更新为新值 | `updateQuantity_ShouldUpdateQuantity_WhenProductExists` |
| 修改不存在的商品 | 抛出 `ProductNotFoundInCartException` | `updateQuantity_ShouldThrowProductNotFoundInCartException_WhenProductNotInCart` |
| 新数量为 0 或负数 | 抛出 `InvalidQuantityException` | `updateQuantity_ShouldThrowInvalidQuantityException_WhenNewQuantityIsZero` 等 |
| 传入空白 productId | 抛出 `IllegalArgumentException` | `updateQuantity_ShouldThrowIllegalArgumentException_WhenProductIdIsBlank` |

#### 4.1.4 金额计算

| 测试场景 | 预期结果 | 测试方法 |
|----------|----------|----------|
| 单个条目小计 | 单价 × 数量 | `calculateItemSubtotal_ShouldReturnCorrectAmount` |
| 空购物车总金额 | 0 | `calculateCartSubtotal_ShouldReturnZero_WhenCartIsEmpty` |
| 多商品总金额 | 各条目小计之和 | `calculateCartSubtotal_ShouldReturnSumOfAllItemSubtotals` |

### 4.2 订单校验与状态模块

#### 4.2.1 `OrderValidator`

| 测试场景 | 预期结果 | 测试方法 |
|----------|----------|----------|
| 空购物车校验（非空） | 通过 | `validateCartNotEmpty_ShouldPass_WhenCartIsNotEmpty` |
| 空购物车校验（空） | 抛出 `EmptyCartException` | `validateCartNotEmpty_ShouldThrowEmptyCartException_WhenCartIsEmpty` |
| 库存足够 | 通过 | `validateStock_ShouldPass_WhenStockSufficient` |
| 购买数量等于库存 | 通过 | `validateStock_ShouldPass_WhenQuantityEqualsStock` |
| 库存不足 | 抛出 `InsufficientStockException` | `validateStock_ShouldThrowInsufficientStockException_WhenQuantityExceedsStock` |

#### 4.2.2 `OrderStatusService`

| 测试场景 | 预期结果 | 测试方法 |
|----------|----------|----------|
| 合法流转：PENDING_PAYMENT → PAID | `canTransition` 返回 true | `canTransition_ShouldReturnTrue_ForValidTransitions` |
| 合法流转：PAID → SHIPPED | 同上 | 同上 |
| 合法流转：SHIPPED → COMPLETED | 同上 | 同上 |
| 合法取消：PENDING_PAYMENT → CANCELLED | 同上 | 同上 |
| 合法取消：PAID → CANCELLED | 同上 | 同上 |
| 非法流转：PAID → PENDING_PAYMENT | `canTransition` 返回 false，状态修改时抛异常 | `canTransition_ShouldReturnFalse_ForInvalidTransitions`<br>`transitionStatus_ShouldThrowInvalidOrderStatusTransitionException_WhenTransitionInvalid` |
| 终态不可变 | `canTransition` 返回 false | `canTransition_ShouldReturnFalse_ForTerminalStates` |
| 状态实际修改 | 订单状态正确更新 | `transitionStatus_ShouldUpdateOrderStatus_WhenTransitionValid` |

#### 4.2.3 `OrderPricingService`（使用 Mock）

| 测试场景 | 预期结果 | 测试方法 |
|----------|----------|----------|
| 无优惠券、标准运费 | 应付 = 商品金额 + 运费 | `calculateOrderSummary_ShouldReturnCorrectSummary_WhenNoPromotionAndStandardShipping` |
| 有优惠券折扣 | 应付 = (商品金额 - 折扣) + 运费 | `calculateOrderSummary_ShouldApplyDiscountAndShipping_WhenPromotionExists` |
| 折扣后金额为负 | 应付金额取 0（不低于 0） | `calculateOrderSummary_ShouldEnsurePayableAmountNotNegative_WhenDiscountExceedsItemAmount` |
| 空购物车校验触发 | 抛出 `EmptyCartException` | `calculateOrderSummary_ShouldValidateCartNotEmpty` |
| 库存校验触发 | 抛出 `InsufficientStockException` | `calculateOrderSummary_ShouldValidateStock` |
| 地址校验触发 | 抛出 `InvalidAddressException`，后续计算服务未被调用 | `calculateOrderSummary_ShouldValidateAddress` |
| 请求对象为 null | 抛出 `OrderCreationException` | `calculateOrderSummary_ShouldThrowOrderCreationException_WhenRequestIsNull` |

#### 4.2.4 `OrderService`

| 测试场景 | 预期结果 | 测试方法 |
|----------|----------|----------|
| 正常创建订单 | 订单号正确、状态为 `PENDING_PAYMENT`、金额字段正确 | `createOrder_ShouldReturnOrderWithPendingPaymentStatus_WhenRequestValid` |
| 请求为 null | 抛出 `OrderCreationException` | `createOrder_ShouldThrowOrderCreationException_WhenRequestIsNull` |
| 订单号生成器返回空白/null | 抛出 `OrderCreationException` | `createOrder_ShouldThrowOrderCreationException_WhenGeneratedOrderIdIsBlank` 等 |
| 状态流转委托 | 调用 `OrderStatusService.transitionStatus` | `transitionStatus_ShouldDelegateToOrderStatusService` |

## 5. 测试执行结果

复核日期：2026-10-21。执行命令：`mvn clean test checkstyle:check spotbugs:check`。
以下为全量构建中本报告涉及的测试结果，来自 `target/surefire-reports/TEST-*.xml`。

| 测试类 | 执行数 | 通过数 | 失败 / 错误 / 跳过 |
|------|------:|------:|------|
| ShoppingCartServiceTest | 18 | 18 | 0 / 0 / 0 |
| OrderValidatorTest | 7 | 7 | 0 / 0 / 0 |
| OrderStatusServiceTest | 5 | 5 | 0 / 0 / 0 |
| OrderPricingServiceTest | 7 | 7 | 0 / 0 / 0 |
| OrderServiceTest | 5 | 5 | 0 / 0 / 0 |
| OrderCheckoutTest | 1 | 1 | 0 / 0 / 0 |
| **合计** | **43** | **43** | **0 / 0 / 0** |

`OrderCheckoutTest` 使用真实购物车、优惠、配送和订单服务，仅将订单号固定。商品原价为 100 元，依次使用满 100 减 20、9 折、固定减 10 元券，最终应付 62 元。运费按优惠前商品金额判断，因此仍包邮；随后验证付款、发货、完成，以及完成后拒绝取消。

## 6. 代码覆盖率分析

以下是全量 109 次测试执行后的 JaCoCo 类级数据，包含跨模块场景对这些类的覆盖贡献；分母为该类可统计的行或分支。

| 类 | 行覆盖率 | 分支覆盖率 | 说明 |
|------|------|------|------|
| ShoppingCartService | 97.56%（40/41） | 87.50%（7/8） | 未覆盖服务内负价格防御分支 |
| OrderValidator | 100%（12/12） | 100%（6/6） | 空购物车、库存及遍历路径 |
| OrderStatusService | 100%（18/18） | 100%（2/2） | 不等同于穷举全部状态组合 |
| OrderPricingService | 100%（21/21） | 100%（2/2） | 包含真实协作与 Mock 用例 |
| OrderService | 90.00%（18/20） | 100%（6/6） | 默认构造路径未执行 |

完整数据见 [JaCoCo HTML](target/site/jacoco/index.html) 和[软件测试报告](软件测试报告.md)，生成方式见 README。

## 7. 已修复问题与后续方向

- 补齐 Mockito 依赖，解决干净构建中的测试编译失败。
- 将测试移到与包名一致的 `cart/service/` 和 `order/service/`，用 `clean` 清除旧包下残留字节码。
- 地址校验失败用例改为真实的 `InvalidAddressException` 契约；库存、地址校验失败后断言后续计算服务未被调用。
- 移除 Mock void 方法不必要的 `doNothing` 配置，保留异常桩与依赖交互验证。
- `@InjectMocks` 在当前测试中通过有参构造器注入依赖；默认构造器的存在不意味着一定会绕过 Mock。
- `MoneyUtilsTest` 已有 15 次独立执行，不能再表述为仅被间接验证。
- 后续可用参数化测试穷举订单状态组合；引入持久化后应另行测试并发状态更新与事务回滚。

## 8. 结论

本报告涉及的 43 次测试执行全部通过，已建立购物车与订单模块的回归基线。Mock 用例验证单模块职责，真实服务场景验证组合后的金额和状态行为；覆盖率按实际报告记录。
