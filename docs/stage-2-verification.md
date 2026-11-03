# 第二阶段验收报告

> 本文为第二阶段的历史验收快照。当前交易流程的验收结果见[第四阶段验收报告](stage-4-verification.md)；`target/` 下的生成报告已随后续构建更新。

验收日期：2026-10-21。项目版本：`1.0.0-SNAPSHOT`。

## 1. 完成范围

- 接入 Java 17 / Spring Boot 3.5.16，提供独立启动入口和可执行 JAR。
- 通过 Spring 配置类和构造器组装原有业务服务，复用购物车、优惠、运费与订单金额计算。
- 提供 `GET /api/products`、`GET /api/coupons`、`POST /api/settlements/preview`。
- 按已确认方案，使用服务端只读演示商品、库存和优惠规则；客户端提交商品编号、数量、地址、配送方式和券码。
- 增加 Jakarta Validation、嵌套字段校验、明确的 HTTP 状态码和统一 JSON 错误响应。
- 通过 `ProductCatalog` / `CouponCatalog` 抽象数据查询，便于后续替换为数据库实现。
- 提供[接口文档](http-api.md)及[可直接调用的请求文件](examples/preview.json)。

第二阶段只提供结算预览，不创建或持久化订单、不扣减库存。数据库、交易一致性、真实支付与登录鉴权不属于本阶段验收范围。

## 2. 构建与静态检查

执行命令：

```powershell
mvn -B -ntp clean verify checkstyle:check spotbugs:check
```

| 项目 | 结果 |
|------|------|
| 干净构建 | BUILD SUCCESS |
| 测试执行 | 164 次通过，失败 0、错误 0、跳过 0 |
| Checkstyle | 0 项违规 |
| SpotBugs | 0 项缺陷、0 项分析错误 |
| 可执行 JAR | `target/order-settlement-1.0.0-SNAPSHOT.jar` |

检查沿用仓库的 Checkstyle 规则及 SpotBugs 排除配置，未新增排除项。静态检查范围为主代码；覆盖率未设置强制门槛。JUnit 5.12.2 与 Mockito 5.17.0 由 Spring Boot 依赖管理提供。

## 3. 测试分布与关键场景

| 测试范围 | 执行数 |
|------|------:|
| 原有业务测试与第一阶段补充场景 | 109 |
| SettlementApiTest：完整 Spring 上下文 + MockMvc | 53 |
| ApiFailureTest：MVC 异常隔离测试 | 1 |
| HttpServerSmokeTest：启动主方法、随机端口与真实 HTTP 请求 | 1 |
| 合计 | 164 |

新增测试覆盖：

- 服务端目录查询、商品金额、三类优惠券顺序叠加、优惠前金额包邮判断和配送组合。
- 重复商品合并后的库存校验、零库存、重复券码、券门槛不足及未知编号。
- 预览请求之间的购物车隔离、重复预览不扣库存。
- 必填项、嵌套字段、数量和列表长度边界、null 元素、未知字段与非法 JSON 类型。
- 禁止客户端提交价格与库存；拒绝小数数量、数字枚举及非法枚举值。
- 400 / 404 / 405 / 406 / 409 / 415 / 422 响应和未预期异常的 500 响应；不向客户端返回内部异常堆栈。

执行数以 Surefire 控制台总计及 XML 中实际 `<testcase>` 节点交叉核对，均为 164。嵌套测试的父套件属性不能直接相加作为总数；本次 MoneyUtilsTest / ShippingServiceTest 的叶子节点分别为 15 / 34。

## 4. 覆盖率

以下来自本次 `target/site/jacoco/jacoco.xml` 的全项目计数器，包含 Spring 配置、HTTP 层、目录实现和核心业务类。

| 指标 | 已覆盖 / 总数 | 覆盖率 |
|------|------:|------:|
| 行 | 537 / 557 | 96.41% |
| 分支 | 125 / 145 | 86.21% |
| 指令 | 2319 / 2411 | 96.18% |
| 方法 | 181 / 188 | 96.28% |
| 类 | 56 / 57 | 98.25% |

第一阶段报告保留当时仅业务核心的统计，覆盖范围与本阶段不同。JaCoCo 类和方法计数以工具报告为准，不等同于源码文件数。

## 5. 打包启动验收

在自动化测试之外，以 `java -jar` 启动本次构建的 JAR，在本机随机端口调用三个接口，并在完成后关闭验收进程。

| 验收项 | 实际结果 |
|------|------|
| 商品查询 | 3 件演示商品 |
| 优惠券查询 | 3 张演示优惠券 |
| 使用 examples/preview.json 结算 | 商品 100.00，优惠 38.00，运费 0.00，应付 62.00 |
| 请求不支持的 XML 响应类型 | HTTP 406，JSON 错误码 NOT_ACCEPTABLE |

构建后可在本地查看：

- [Surefire 测试报告](../target/surefire-reports/)
- [JaCoCo HTML 报告](../target/site/jacoco/index.html)
- [本次构建日志](../target/stage2-build.log)
- [本次 JAR 验收结果](../target/jar-smoke-result.json)

`target/` 未纳入版本控制，执行 `clean` 会清除其中的生成文件；本报告记录本次验收快照。
