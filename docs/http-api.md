# HTTP 接口说明

## 1. 范围与运行

项目使用 Java 17、Spring Boot 3.5.16 和 Spring MVC。商品价格、库存与优惠规则由 MyBatis 从数据库查询，初次迁移会写入下方演示数据。结算预览不创建订单、不扣减库存；订单创建会扣减库存，取消时回补，详见[第四阶段交易说明](transactional-checkout.md)。

在 `soft_test` 目录执行：

```powershell
mvn spring-boot:run
```

默认地址为 `http://localhost:8080`。可通过环境变量 `PORT` 或启动参数 `--server.port` 更改端口。也可以运行打包后的应用：

```powershell
mvn clean verify checkstyle:check spotbugs:check
java -jar target/order-settlement-1.0.0-SNAPSHOT.jar
```

## 2. 接口列表

| 方法 | 路径 | 成功状态 | 响应 |
|------|------|------|------|
| GET | `/api/products` | 200 | 数据库商品数组，按 productId 排序 |
| GET | `/api/coupons` | 200 | 数据库优惠券数组，按 couponCode 排序 |
| POST | `/api/settlements/preview` | 200 | 结算金额汇总及已应用券码 |
| POST | `/api/orders` | 201 / 200 | 创建订单；带幂等键的重复请求返回已有订单 |
| GET | `/api/orders/{orderId}` | 200 | 查询订单及历史快照 |
| PATCH | `/api/orders/{orderId}/status` | 200 | 更新订单状态，取消时回补库存 |

请求和响应使用 JSON。金额单位为元，以 BigDecimal 计算；金额汇总保留两位小数。接口通过 HTTP 状态码区分成功与失败。

## 3. 服务端演示数据

| 商品编号 | 名称 | 单价 | 库存 |
|------|------|------:|------:|
| P001 | Java 入门 | 50.00 | 20 |
| P002 | 软件测试实践 | 38.00 | 10 |
| P003 | 机械键盘 | 100.00 | 0 |

`GET /api/products` 每项包含 `productId`、`productName`、`unitPrice`、`stock`。

| 券码 | 类型 | 门槛 | 规则 |
|------|------|------:|------|
| FULL100_20 | FULL_REDUCTION | 100.00 | 减 20.00 元 |
| DISCOUNT90 | DISCOUNT | 0 | 9 折（discountRate = 0.90） |
| FIXED10 | FIXED_AMOUNT | 0 | 减 10.00 元 |

`GET /api/coupons` 每项包含 `couponCode`、`couponType`、`thresholdAmount`、`reductionAmount`、`discountRate`；与该券类型不相关的金额或折扣率为 null。

## 4. 结算预览

可直接使用 [preview.json](examples/preview.json) 中的请求：

```powershell
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/products'
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/coupons'
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/settlements/preview' -ContentType 'application/json; charset=utf-8' -InFile 'docs/examples/preview.json'
```

请求示例：

```json
{
  "items": [{"productId": "P001", "quantity": 2}],
  "address": {
    "recipientName": "张三",
    "phone": "13800138000",
    "detailAddress": "北京市朝阳区测试路1号",
    "regionType": "NORMAL"
  },
  "deliveryType": "STANDARD",
  "couponCodes": ["FULL100_20", "DISCOUNT90", "FIXED10"]
}
```

成功响应：

```json
{
  "itemAmount": 100.00,
  "discountAmount": 38.00,
  "shippingFee": 0.00,
  "payableAmount": 62.00,
  "appliedCouponCodes": ["FULL100_20", "DISCOUNT90", "FIXED10"]
}
```

计算为 `(100 - 20) × 0.9 - 10 = 62`。标准配送的包邮判断使用优惠前商品金额，因此最终金额低于 88 元仍然包邮。

### 请求约束

| 字段 | 约束 |
|------|------|
| items | 必填，1～100 项；元素不能为 null |
| items[].productId | 非空白字符串，最多 64 字符；必须存在于服务端目录 |
| items[].quantity | 必填整数，1～10000；合并重复商品后仍须满足库存限制 |
| address | 必填对象，自提场景也沿用现有地址校验规则 |
| address.recipientName | 非空白，最多 100 字符 |
| address.phone | 非空白，最多 32 字符；此阶段不限制国家或地区号码格式 |
| address.detailAddress | 非空白，最多 500 字符 |
| address.regionType | 必填，NORMAL 或 REMOTE |
| deliveryType | 必填，STANDARD、EXPRESS 或 SELF_PICKUP |
| couponCodes | 可省略或为 null，此时视为空列表；最多 20 项，元素非空白且最多 64 字符 |

商品编号、券码与枚举值区分大小写。重复商品条目合并数量；同一券码不得重复。不同券码按请求顺序应用，并在应用每张券时按剩余商品金额校验门槛。例如先打折，再使用满 100 元券，可能因剩余金额不足而失败。

客户端只提交商品编号和数量，不能提交单价、库存或自定义优惠规则。未知 JSON 字段、非整数数量、数字形式的枚举以及不支持的枚举值返回 400。

## 5. 统一错误响应

字段校验失败示例：

```json
{
  "code": "VALIDATION_FAILED",
  "message": "请求参数校验失败",
  "errors": [{"field": "items[0].quantity", "message": "must be greater than or equal to 1"}]
}
```

`code` 为稳定的机器可读错误码；`message` 与字段错误文本供人阅读，不保证语言或措辞不变。字段错误列表按字段名、错误文本排序，不返回被拒绝的原始值。非字段错误的 `errors` 为空数组。

| HTTP 状态 | code | 场景 |
|------|------|------|
| 400 | VALIDATION_FAILED | 必填项缺失、空购物车、数量或字符串长度越界 |
| 400 | INVALID_REQUEST | JSON 损坏、请求体缺失、非法类型或未知字段 |
| 400 | INVALID_COUPON | 同一券码重复提交 |
| 400 | INVALID_IDEMPOTENCY_KEY | 幂等键格式错误 |
| 404 | PRODUCT_NOT_FOUND / COUPON_NOT_FOUND | 商品编号或券码不存在 |
| 404 | NOT_FOUND | 路由不存在 |
| 405 | METHOD_NOT_ALLOWED | 请求方法不受支持 |
| 406 | NOT_ACCEPTABLE | 响应类型不受支持 |
| 409 | INSUFFICIENT_STOCK | 库存不足，包括重复商品合并后超出库存 |
| 409 | IDEMPOTENCY_CONFLICT | 同一幂等键用于不同请求 |
| 415 | UNSUPPORTED_MEDIA_TYPE | 请求类型不是支持的 JSON 类型 |
| 422 | COUPON_NOT_APPLICABLE | 优惠金额门槛不满足 |
| 422 | UNSUPPORTED_DELIVERY | 偏远地区选择加急配送 |
| 422 | BUSINESS_RULE_VIOLATION | 其他已知业务规则错误 |
| 500 | INTERNAL_ERROR | 未预期异常；响应不包含内部异常信息或堆栈 |

## 6. 结构与后续替换点

调用顺序：`Controller → SettlementPreviewService → OrderPricingService → 各业务服务`。

- `OrderSettlementApplication` 提供启动入口。
- `SettlementConfiguration` 通过构造器注册和组装原有业务服务，原有类可继续直接进行普通单元测试。
- `ProductCatalog`、`CouponCatalog` 定义商品及优惠规则查询接口，第三阶段由 `MyBatisCatalog` 查询数据库，保留第二阶段 HTTP 请求契约。
- `web/dto` 负责 JSON 边界与 Jakarta Validation；Controller 不计算金额。
- `ApiExceptionHandler` 将字段错误、框架错误与业务异常映射为统一 JSON 响应。
- 不同请求使用独立购物车，结算预览不会修改数据库。

Spring Boot 3.5 与 Java 17 的兼容性见[官方系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)；测试依赖通过 `spring-boot-starter-test` 统一管理，见[官方测试文档](https://docs.spring.io/spring-boot/3.5/reference/testing/index.html)。
