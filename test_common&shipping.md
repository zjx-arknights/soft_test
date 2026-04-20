# common 与 shipping 模块单元测试总结

- 测试框架：JUnit 5
- 测试文件位置：`src/test/java/com/houkiang/ordersettlement/test_common_shipping/`
- 执行命令：`mvn -Dtest="MoneyUtilsTest,ShippingServiceTest" test`
- 运行结果：**Tests run: 49，Failures: 0，Errors: 0**

---

## 一、MoneyUtilsTest（15 个用例）

被测类：`com.houkiang.ordersettlement.common.util.MoneyUtils`

### 1.1 `requireNonNegative(BigDecimal amount, String fieldName)`

> 断言金额非负，并统一保留两位小数（HALF_UP）。

| 编号 | 用例名称 | 输入 | 预期结果 | 类型 |
|------|---------|------|---------|------|
| 1 | 输入 0 → 返回 0.00 | `0` | `0.00` | 正常 |
| 2 | 输入正数 12.3 → 返回 12.30 | `12.3` | `12.30` | 正常 |
| 3 | 输入多位小数 9.999 → 按 HALF_UP 返回 10.00 | `9.999` | `10.00` | 边界 |
| 4 | 输入负数 → 抛出 IllegalArgumentException | `-0.01` | 抛出 `IllegalArgumentException` | 异常 |
| 5 | 输入 null → 抛出 NullPointerException | `null` | 抛出 `NullPointerException` | 异常 |

### 1.2 `requirePositive(BigDecimal amount, String fieldName)`

> 断言金额严格大于零，并统一保留两位小数（HALF_UP）。

| 编号 | 用例名称 | 输入 | 预期结果 | 类型 |
|------|---------|------|---------|------|
| 6 | 输入最小正数 0.01 → 返回 0.01 | `0.01` | `0.01` | 正常 |
| 7 | 输入正数 100 → 返回 100.00 | `100` | `100.00` | 正常 |
| 8 | 输入 0 → 抛出 IllegalArgumentException | `0` | 抛出 `IllegalArgumentException` | 边界/异常 |
| 9 | 输入负数 → 抛出 IllegalArgumentException | `-1` | 抛出 `IllegalArgumentException` | 异常 |
| 10 | 输入 null → 抛出 NullPointerException | `null` | 抛出 `NullPointerException` | 异常 |

### 1.3 `minZero(BigDecimal amount)`

> 将负数截断为 0，正数/零统一保留两位小数（HALF_UP）。

| 编号 | 用例名称 | 输入 | 预期结果 | 类型 |
|------|---------|------|---------|------|
| 11 | 输入负数 → 返回 0.00 | `-99.99` | `0.00` | 正常 |
| 12 | 输入 0 → 返回 0.00 | `0` | `0.00` | 边界 |
| 13 | 输入正数 1.235 → 按 HALF_UP 返回 1.24 | `1.235` | `1.24` | 边界 |
| 14 | 输入正数 50.50 → 返回 50.50 | `50.50` | `50.50` | 正常 |
| 15 | 输入 null → 抛出 NullPointerException | `null` | 抛出 `NullPointerException` | 异常 |

---

## 二、ShippingServiceTest（34 个用例）

被测类：`com.houkiang.ordersettlement.shipping.service.ShippingService`

**测试前置条件（`@BeforeEach`）：** 每个用例前创建默认 `ShippingService` 实例。

**公共测试数据：**
- 普通地区地址（`normalAddress`）：收件人=张三，电话=13800138000，地址=北京市朝阳区XX路1号，地区=`NORMAL`
- 偏远地区地址（`remoteAddress`）：收件人=李四，电话=13900139000，地址=西藏自治区XX县XX乡，地区=`REMOTE`

---

### 2.1 `validateAddress(Address address)` — 地址校验

| 编号 | 用例名称 | 输入条件 | 预期结果 | 类型 |
|------|---------|---------|---------|------|
| 1 | 地址为 null | `address = null` | 抛出 `InvalidAddressException` | 异常 |
| 2 | 收件人为空字符串 | `recipientName = ""` | 抛出 `InvalidAddressException` | 异常 |
| 3 | 收件人为纯空白 | `recipientName = "   "` | 抛出 `InvalidAddressException` | 边界/异常 |
| 4 | 电话为空字符串 | `phone = ""` | 抛出 `InvalidAddressException` | 异常 |
| 5 | 详细地址为空字符串 | `detailAddress = ""` | 抛出 `InvalidAddressException` | 异常 |
| 6 | regionType 为 null | `regionType = null` | 抛出 `InvalidAddressException` | 异常 |
| 7 | 完整合法地址 | `normalAddress()` | 不抛异常 | 正常 |

---

### 2.2 `validateDeliveryType(DeliveryType, RegionType)` — 配送方式校验

| 编号 | 用例名称 | 输入条件 | 预期结果 | 类型 |
|------|---------|---------|---------|------|
| 8 | deliveryType 为 null | `null, NORMAL` | 抛出 `NullPointerException` | 异常 |
| 9 | regionType 为 null | `STANDARD, null` | 抛出 `NullPointerException` | 异常 |
| 10 | EXPRESS + REMOTE（不支持） | `EXPRESS, REMOTE` | 抛出 `UnsupportedDeliveryTypeException` | 异常 |
| 11 | STANDARD + NORMAL | `STANDARD, NORMAL` | 不抛异常 | 正常 |
| 12 | STANDARD + REMOTE | `STANDARD, REMOTE` | 不抛异常 | 正常 |
| 13 | EXPRESS + NORMAL | `EXPRESS, NORMAL` | 不抛异常 | 正常 |
| 14 | SELF_PICKUP + NORMAL | `SELF_PICKUP, NORMAL` | 不抛异常 | 正常 |
| 15 | SELF_PICKUP + REMOTE | `SELF_PICKUP, REMOTE` | 不抛异常 | 正常 |

---

### 2.3 `calculateShippingFee(BigDecimal, Address, DeliveryType)` — 运费计算

#### C1. 标准配送 + 普通地区（包邮阈值：88.00）

| 编号 | 用例名称 | 商品金额 | 预期运费 | 是否包邮 | 类型 |
|------|---------|---------|---------|---------|------|
| 16 | 低于阈值 | `87.99` | `8.00` | 否 | 边界 |
| 17 | 恰好等于阈值（边界） | `88.00` | `0.00` | 是 | 边界 |
| 18 | 高于阈值 | `88.01` | `0.00` | 是 | 边界 |

#### C2. 标准配送 + 偏远地区（不包邮）

| 编号 | 用例名称 | 商品金额 | 预期运费 | 是否包邮 | 类型 |
|------|---------|---------|---------|---------|------|
| 19 | 金额为 0 | `0` | `15.00` | 否 | 边界 |
| 20 | 金额为 200（超过包邮阈值但不包邮） | `200.00` | `15.00` | 否 | 正常 |

#### C3. 加急配送 + 普通地区（包邮阈值：188.00）

| 编号 | 用例名称 | 商品金额 | 预期运费 | 是否包邮 | 类型 |
|------|---------|---------|---------|---------|------|
| 21 | 低于阈值 | `187.99` | `15.00` | 否 | 边界 |
| 22 | 恰好等于阈值（边界） | `188.00` | `0.00` | 是 | 边界 |
| 23 | 高于阈值 | `188.01` | `0.00` | 是 | 边界 |

#### C4. 加急配送 + 偏远地区（不支持）

| 编号 | 用例名称 | 商品金额 | 预期结果 | 类型 |
|------|---------|---------|---------|------|
| 24 | 任意金额 | `100.00` | 抛出 `UnsupportedDeliveryTypeException` | 异常 |

#### C5. 自提配送（运费始终为 0）

| 编号 | 用例名称 | 地区 | 预期运费 | 是否包邮 | 类型 |
|------|---------|-----|---------|---------|------|
| 25 | SELF_PICKUP + NORMAL | 普通 | `0.00` | 是 | 正常 |
| 26 | SELF_PICKUP + REMOTE | 偏远 | `0.00` | 是 | 正常 |

#### C6. itemAmount 参数边界

| 编号 | 用例名称 | 输入 | 预期结果 | 类型 |
|------|---------|------|---------|------|
| 27 | itemAmount 为负数 | `-1.00` | 抛出 `IllegalArgumentException` | 异常 |
| 28 | itemAmount 为 null | `null` | 抛出 `NullPointerException` | 异常 |
| 29 | itemAmount 为 0 | `0` | 运费 `8.00`（STANDARD+NORMAL） | 边界 |

---

### 2.4 自定义规则异常场景（注入非法 `ShippingFeeRule`）

| 编号 | 用例名称 | 注入规则 | 预期结果 | 类型 |
|------|---------|---------|---------|------|
| 30 | baseFee 为负数 | `baseFee = -1.00` | 抛出 `InvalidShippingRuleException` | 异常 |
| 31 | freeShippingThreshold 为负数 | `threshold = -1.00` | 抛出 `InvalidShippingRuleException` | 异常 |

---

### 2.5 `key(DeliveryType, RegionType)` — 静态辅助方法

| 编号 | 用例名称 | 输入 | 预期结果 | 类型 |
|------|---------|------|---------|------|
| 32 | STANDARD + NORMAL | `STANDARD, NORMAL` | `"STANDARD:NORMAL"` | 正常 |
| 33 | EXPRESS + REMOTE | `EXPRESS, REMOTE` | `"EXPRESS:REMOTE"` | 正常 |
| 34 | SELF_PICKUP + NORMAL | `SELF_PICKUP, NORMAL` | `"SELF_PICKUP:NORMAL"` | 正常 |

---

## 三、用例覆盖统计

| 模块 | 测试类 | 用例数 | 正常路径 | 边界值 | 异常断言 |
|------|-------|-------|---------|-------|---------|
| common | `MoneyUtilsTest` | 15 | 4 | 3 | 8 |
| shipping | `ShippingServiceTest` | 34 | 11 | 10 | 13 |
| **合计** | **2 个测试类** | **49** | **15** | **13** | **21** |

## 四、运行方式

在 `soft_test` 根目录执行：

```powershell
# 仅运行本模块测试
mvn -Dtest="MoneyUtilsTest,ShippingServiceTest" test

# 运行全量测试
mvn test
```
