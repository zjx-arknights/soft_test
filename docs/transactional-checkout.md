# 第四阶段：库存事务与幂等下单

## 运行与升级

默认 `local` profile 仍使用 `data/` 中的 H2 文件数据库；`mysql` profile 的连接参数见[数据库文档](database.md)。启动时 Flyway 执行 `V3__idempotency_requests.sql`：添加库存预占标记，并创建幂等请求表。此前的 V1、V2 迁移文件保持原样。

第三阶段创建的订单没有扣过库存。迁移把这些订单的 `stock_reserved` 设为 `FALSE`，因此升级后取消旧订单不会错误增加库存。第四阶段新订单写入 `TRUE`。迁移前应备份现有数据库；迁移后的表结构由 Flyway 管理。

## 下单事务

`POST /api/orders` 在一个数据库事务中执行：

1. 如果请求携带 `Idempotency-Key`，先插入唯一的请求占用记录。
2. 查询商品与优惠规则，合并相同商品，执行库存、优惠、地址和配送校验，计算订单金额。
3. 按商品编号顺序执行条件扣减：`UPDATE products SET stock = stock - ? WHERE product_id = ? AND stock >= ?`。任一商品扣减未成功时返回 409 `INSUFFICIENT_STOCK`。
4. 写入订单主表、商品明细、优惠快照，并将幂等记录关联到订单，然后提交事务。

任一步抛出异常，扣减、订单和幂等记录一起回滚。数据库条件更新会在并发请求之间重新检查剩余库存；单靠结算前读取的库存值不足以防止超卖。多个商品按固定顺序扣减，减少相反锁顺序造成的死锁。遇到数据库可重试的更新冲突，应用在独立事务中最多尝试三次。

结算预览 `POST /api/settlements/preview` 仍只计算金额，不扣库存、不创建订单、不占用幂等键。预览不保留价格或库存；创建订单会重新查询数据库并计算。

## 取消与库存回补

`PATCH /api/orders/{orderId}/status` 沿用原有状态机：待付款和已付款订单可取消，已发货或已完成订单不能取消。状态条件更新成功后，在同一事务中回补该订单的商品数量；任何数据库写入错误都会回滚状态变更和回补操作。再次取消已经取消的订单返回 409，不会重复加库存。

只有 `stock_reserved = TRUE` 的订单才回补。若目录中的商品已被外部管理操作删除，该商品已无可更新的库存行；订单仍可取消，历史订单快照保留。当前状态接口只用于业务流程演示，未连接支付或退款系统。

## 幂等键契约

需要安全重试创建订单的客户端，应在 `POST /api/orders` 请求头中传入唯一的 `Idempotency-Key`。建议每次新下单生成随机 UUID，重试时原样复用。键只接受 16～128 位英文字母、数字、下划线或连字符；长度或字符非法返回 400 `INVALID_IDEMPOTENCY_KEY`。

```powershell
$key = [guid]::NewGuid().ToString()
$uri = 'http://localhost:8080/api/orders'
$headers = @{ 'Idempotency-Key' = $key }
$order = Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -ContentType 'application/json; charset=utf-8' -InFile 'docs/examples/preview.json'
```

| 情况 | 响应与库存 |
|------|------|
| 第一次使用新键 | 201 Created，返回新订单及 Location，扣库存一次 |
| 同一键、同一请求重试 | 200 OK，返回首次创建的订单当前状态，不再次扣库存 |
| 同一键、不同请求 | 409 `IDEMPOTENCY_CONFLICT`，不创建新订单 |
| 下单失败 | 业务错误与库存、幂等记录一起回滚；键可以重试 |
| 未提供键 | 保留原接口行为，每次有效请求都会创建新订单并扣库存 |

请求指纹由商品编号和数量、地址、配送方式、券码及其顺序计算。JSON 的字段书写顺序或空白字符不会影响指纹；商品价格或优惠配置发生变化，也不会改变已创建订单的幂等结果。同一键的重试会返回订单的**当前状态**，例如订单已取消时返回 `CANCELLED`。幂等记录保存在数据库中，因此应用重启后仍然生效。

目前没有用户身份系统，幂等键在单个数据库内全局唯一。客户端应使用不可预测的随机键并避免跨用户复用。幂等记录目前不自动过期；清理策略需要在后续运营规则确定后再制定。

## 验收

普通测试：

```powershell
mvn clean verify checkstyle:check spotbugs:check
```

原生 MySQL 集成测试使用独立的 `_test` 库，连接参数见[数据库文档](database.md)，执行：

```powershell
mvn -Pmysql-it clean verify checkstyle:check spotbugs:check
```

测试覆盖并发库存竞争、相同幂等键同时提交、重复请求、取消一次性回补、商品价格变化后的历史结果、订单入库失败时的库存和键回滚、应用重启后的幂等重试，以及第三阶段旧订单升级后的取消行为。具体执行结果见[第四阶段验收报告](stage-4-verification.md)。
