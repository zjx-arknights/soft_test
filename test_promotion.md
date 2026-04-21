# 优惠结算系统 - 促销模块单元测试详细说明文档

**测试类:** `com.houkiang.ordersettlement.promotion.service.PromotionServiceTest`
**目标模块:** 促销计算与优惠券校验模块 (`PromotionService`, `CouponValidator`)
**测试框架:** JUnit 5

## 1. 文档概述

本文档是对 `PromotionServiceTest` 单元测试类的详细说明。该测试类负责验证电商订单结算链路中最核心、逻辑最复杂的**优惠计算模块**。测试全面覆盖了正常业务流（单券计算、多券叠加）、异常数据拦截（非法金额、非法折扣率）以及系统的防御性编程逻辑（Null 值处理、金额防穿透）。

## 2. 核心测试策略分析

本测试类综合运用了多种经典的软件测试方法，以确保核心代码的分支覆盖率与系统健壮性：

1. **场景法 (Scenario Testing):** 模拟用户真实的购物车结算场景（如叠加不同类型的优惠券）。
2. **边界值分析 (Boundary Value Analysis):** 针对折扣率的上下限（如 1.0）、减免金额的下限（0.00）以及空集合进行极限测试。
3. **无效等价类划分 (Equivalence Partitioning):** 构造诸如负数门槛、超额折扣率等系统应当拒绝的非法输入。
4. **防御性测试 (Robustness Testing):** 全面测试传入对象为 `null`、集合为 `null` 或集合包含 `null` 元素的极端场景。

## 3. 测试用例详解

测试用例按功能模块划分为以下四大类别：

### 3.1 核心业务流程测试 (正向测试)

验证系统在接收合法数据时，计算逻辑与执行顺序是否符合预期。

* **`testApplyMultipleCoupons` (满减优惠券叠加折扣券)**
  * **测试目的**: 验证多张优惠券叠加时的计算顺序和金额准确性。
  * **模拟场景**: 订单原价 100 元，依次应用“满100减20元”满减券和“9折”折扣券。
  * **预期结果**: 先减20元变为80元，再打9折，最终金额应精确计算为 72.00 元。应用的券码数量应为 2。

### 3.2 非法数据与异常拦截测试 (反向测试)

针对 `CouponValidator` 的校验逻辑，验证系统能否准确抛出 `InvalidCouponException` 业务异常。

* **`testInvalidThreshold` (优惠券门槛为负数)**
  * **测试目的**: 拦截无效等价类数据。传入门槛金额为 `-1.00` 的优惠券，预期抛出异常。
* **`testInvalidReduction` (减免金额非法)**
  * **测试目的**: 边界值测试。满减券的减免金额必须大于0，传入 `0.00`，预期抛出异常。
* **`testInvalidDiscountRate` (折扣率大于1或小于等于0)**
  * **测试目的**: 边界值与无效等价类测试。传入越界的折扣率 `1.10`，预期抛出异常以防止商家亏损。

### 3.3 极限边界与状态转换测试

测试在极端业务场景下，系统的兜底逻辑是否生效。

* **`testApplyEmptyCouponList` / `testEmptyCouponList` (空优惠券列表)**
  * **测试目的**: 集合边界测试。当传入空的优惠券列表时，系统不应报错，应直接返回等于订单原价的金额（100.00元）。
* **`testAmountDiscountToZero` (优惠金额超过原价需归零)**
  * **测试目的**: 验证金额归零保护机制 (`MoneyUtils.minZero`)。
  * **模拟场景**: 订单原价 50 元，使用一张减 100 元的固定抵扣券。
  * **预期结果**: 系统最终应付金额不能为负数，必须拦截并返回 `0.00`。

### 3.4 健壮性与 Null 值防御测试 (白盒分支覆盖)

这类测试专为提升代码的“分支覆盖率”设计，覆盖代码中所有的 `null` 检查和 `Objects.requireNonNull` 语句。

* **`testNullAttributesInCoupon` / `testCouponWithNullAttributes` (属性为 null 的优惠券)**
  * **测试目的**: 验证当 `Coupon` 实例内部关键字段（如 `thresholdAmount`, `reductionAmount`）缺失为 `null` 时，校验器能否安全地抛出 `InvalidCouponException` 而非系统级的 `NullPointerException`。
* **`testValidateStackableNulls` (集合及其元素的 null 校验)**
  * **测试目的**: 覆盖 `validateStackable` 方法的防御逻辑。分别传入 `null` 列表对象，以及包含 `null` 元素的有效列表，预期均抛出 `NullPointerException`。
* **`testValidateCouponNulls` (核心验证对象的 null 校验)**
  * **测试目的**: 验证向 `validateCoupon` 传入空的优惠券对象时系统的拦截能力。
* **`testUnsupportedCouponType` (未知的优惠券类型)**
  * **测试目的**: 追求 100% 分支覆盖率。旨在触发 `switch(couponType)` 语句的 `default` 防御性分支。

## 4. 关键技术实现规范

在编写本测试类时，严格遵循了以下开发与测试规范：

1. **精准的金额断言**: 鉴于 Java 中 `BigDecimal` 的 `equals` 方法会严格比较数值的标度（Scale，例如 `72.0` 不等于 `72.00`），本测试统一采用 `compareTo() == 0` 的方式进行断言，保证了金额比较的业务正确性。
2. **Lambda 表达式的异常捕获**: 大量使用 JUnit 5 的 `assertThrows(Class<T> expectedType, Executable executable)`，以优雅、隔离的方式验证每一处业务异常的抛出，避免测试因异常而中断。
3. **语义化的测试命名**: 结合 `@Test` 与 `@DisplayName("中文字段描述")` 注解，使得测试控制台的输出结果具备极高的可读性，直接等同于一份动态的业务需求说明书。

***