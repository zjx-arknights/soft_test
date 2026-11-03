# 优惠规则模块测试报告

> 本文为第一阶段业务测试快照。第二阶段保留这些用例，当前依赖版本和全量测试结果见[第二阶段验收报告](docs/stage-2-verification.md)。

- 测试类：`com.houkiang.ordersettlement.promotion.service.PromotionServiceTest`
- 被测类：`PromotionService`、`CouponValidator`
- 框架：JUnit 5.10.2，含参数化测试。
- 复核日期：2026-10-21。
- 全量验收命令：`mvn clean test checkstyle:check spotbugs:check`。
- 本模块结果：17 次执行，通过 17，失败、错误、跳过均为 0。

## 1. 用例与执行数

| 测试方法 | 执行数 | 输入与验证内容 |
|------|------:|------|
| testApplyMultipleCoupons | 1 | 100 元先减 20 再打 9 折，得到 72 元，券码顺序为 C01、C02 |
| testInvalidThreshold | 1 | 负数门槛抛出 InvalidCouponException |
| testInvalidReduction | 1 | 减免金额为 0 时拒绝优惠券 |
| testInvalidDiscountRate | 3 | 参数化输入 -0.10、0、1.10，均拒绝 |
| testNullAttributesInCoupon | 1 | 门槛或减免金额缺失时抛出业务异常 |
| testValidateStackableNulls | 1 | 券列表或列表元素为 null 时拒绝 |
| testValidateCouponNulls | 1 | 优惠券对象为 null 时拒绝 |
| testFullReductionThreshold | 3 | 99.99 元不适用满 100 减 20；100.00、100.01 元均减 20 |
| testApplyEmptyCouponList | 1 | 空列表保持原金额 |
| testNullCouponList | 1 | null 券列表保持原金额，优惠额为 0，应用券码为空 |
| testAmountDiscountToZero | 1 | 50 元使用减 100 元券，最终金额归零 |
| testNullDiscountRate | 1 | 折扣率缺失时抛出 InvalidCouponException |
| testDiscountRateOne | 1 | 折扣率为 1 时优惠额为 0，保留应用券码 |
| **合计** | **17** | **11 个普通测试方法 + 2 个参数化方法，各 3 组数据** |

## 2. 第一阶段修复

原 `testUnsupportedCouponType` 没有测试代码或断言，不能证明任何行为。当前优惠券类型是有限枚举，因此将该空方法替换为满减门槛参数化测试，不通过伪造枚举值追求 default 分支覆盖。

原先重复的空列表、空属性测试分别调整为 null 券列表和缺失折扣率场景；非法折扣率扩展为参数化测试，并新增合法上界 1 的测试。

测试采用等价类、边界值和组合场景方法。判断金额数值时可用 `compareTo`，需要验证两位小数契约时使用具有相同 scale 的 `BigDecimal` 进行 `assertEquals`。

## 3. 实际覆盖率

以下为全量 109 次测试后的 JaCoCo 数据，包含 `OrderCheckoutTest` 对三类券组合流程的覆盖贡献。

| 类 | 行覆盖率 | 分支覆盖率 |
|------|------|------|
| PromotionService | 100%（25/25） | 100%（6/6） |
| CouponValidator | 96.67%（29/30） | 95.24%（20/21） |

`CouponValidator` 的枚举 switch 默认分支未覆盖。以上结果不代表所有金额、券序列或异常组合均已穷举。

报告入口：[JaCoCo HTML](target/site/jacoco/index.html)、[软件测试报告](软件测试报告.md)。生成文件位于 `target/`，可通过上述验收命令重新生成。
