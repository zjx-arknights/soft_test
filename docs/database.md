# 第三阶段：数据库与持久化

> 本文记录第三阶段完成时的数据库能力。第四阶段已加入库存扣减、取消回补与幂等下单；当前行为见[第四阶段文档](transactional-checkout.md)。下文“阶段边界”一节为第三阶段历史说明。

## 1. 技术与结构

采用 MySQL 8.4、MyBatis Spring Boot Starter 3.0.5 和 Flyway。保持原有 Java 业务模型，持久层通过 Mapper 与数据库记录进行转换，不向核心实体添加 ORM 注解。

```text
HTTP Controller
  ├─ 结算预览 → OrderRequestAssembler → 原有金额计算服务
  └─ 订单操作 → OrderApplicationService（事务）→ 原有 OrderService
                    ↓
       ProductCatalog / CouponCatalog / OrderRepository
                    ↓
       MyBatisCatalog / MyBatisOrderRepository → Mapper → 数据库
```

| 表 | 内容 |
|------|------|
| products | 商品编号、名称、单价和库存 |
| coupons | 券码、类型、门槛、减免金额或折扣率 |
| settlement_orders | 订单状态、收货地址快照、配送方式、四项结算金额及创建/更新时间 |
| order_items | 商品名称、单价、购买数量、下单时库存快照及明细顺序 |
| order_coupons | 优惠规则快照及使用顺序 |

金额使用 `DECIMAL(19,2)`，折扣率使用 `DECIMAL(9,8)`，Java 中使用 `BigDecimal`。主键、唯一键、外键及 CHECK 约束保护基础数据完整性。订单明细和优惠快照只关联订单，不关联实时商品或券记录，因此删除或调整目录数据不会改变历史订单。

MySQL 表明确使用 InnoDB；编号采用区分大小写、重音和尾部空格的 `utf8mb4_0900_as_cs` 排序规则，保持与 HTTP 编号匹配约定一致。SQL 参数使用 MyBatis `#{...}` 绑定。

## 2. 本地直接运行

不指定 profile 时使用 `local`，通过 H2 文件数据库提供持久化体验：

```powershell
mvn spring-boot:run
```

数据写入 `soft_test/data/order-settlement.mv.db`，重启应用后仍然保留，`mvn clean` 不会删除。`data/` 已加入 Git 忽略列表。可设置 `DB_FILE` 更换数据库文件路径；同一文件仅供一个应用进程使用。

自动化测试使用 `test` profile 与独立 H2 内存数据库，不读写本地 `data/`，也不使用 MySQL 业务库。文件数据库重启测试使用单独临时目录。

## 3. 使用 MySQL

如果已安装 MySQL 8.4，先创建独立数据库，并准备有该库建表、查询和写入权限的应用账号：

```sql
CREATE DATABASE order_settlement CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs;
```

在 PowerShell 中设置连接参数后启动：

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/order_settlement?connectionTimeZone=UTC'
$env:DB_USERNAME = 'order_app'
$env:DB_PASSWORD = '替换为数据库密码'
mvn spring-boot:run '-Dspring-boot.run.profiles=mysql'
```

也可运行打包后的应用：

```powershell
java -jar target/order-settlement-1.0.0-SNAPSHOT.jar --spring.profiles.active=mysql
```

`mysql` profile 必须提供账号和密码；连接失败会使启动失败，不会自动回退到 H2。密码通过环境变量传入，不写入配置文件或代码仓库。连接远程数据库时应按部署环境配置 JDBC TLS 参数。

如果已安装 Docker，可使用仓库的 [compose.yaml](../compose.yaml) 启动本地 MySQL：

```powershell
$env:DB_PASSWORD = '设置应用账号密码'
$env:MYSQL_ROOT_PASSWORD = '设置独立的管理员密码'
docker compose up -d
$env:DB_USERNAME = 'order_app'
mvn spring-boot:run '-Dspring-boot.run.profiles=mysql'
```

Compose 使用本机 3306 端口和独立命名卷。`docker compose down` 保留数据卷；更换环境变量中的密码不会修改已初始化数据库的账号密码。

## 4. 迁移与初始数据

- `V1__create_settlement_tables.sql` 创建五张业务表。
- `V2__seed_demo_catalog.sql` 写入第二阶段相同的三件商品与三张优惠券。
- 启动时 Flyway 自动执行尚未应用的迁移；已有商品价格、库存、优惠配置及订单不会被启动脚本重置。
- Flyway 是唯一建表机制，未使用 Hibernate 自动建表或重复执行的 `data.sql`；禁用 Flyway clean。
- 已应用的迁移不可随意编辑。后续变更新增 `V3__...sql` 等迁移文件。
- 当前种子数据用于课程项目演示，不代表正式运营数据。

## 5. 订单接口

结算预览的请求格式不变，商品与优惠数据现在从数据库读取。新增接口：

| 方法 | 路径 | 行为 |
|------|------|------|
| POST | `/api/orders` | 创建并持久化待付款订单，返回 201 和 Location |
| GET | `/api/orders/{orderId}` | 读取订单、地址、商品与优惠快照，返回 200 |
| PATCH | `/api/orders/{orderId}/status` | 校验状态流转并保存，返回更新后的订单 |

创建请求与结算预览完全一致，可直接复用示例：

```powershell
$order = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/orders' -ContentType 'application/json; charset=utf-8' -InFile 'docs/examples/preview.json'
Invoke-RestMethod -Uri ('http://localhost:8080/api/orders/' + $order.orderId)
Invoke-RestMethod -Method Patch -Uri ('http://localhost:8080/api/orders/' + $order.orderId + '/status') -ContentType 'application/json' -Body '{"status":"PAID"}'
```

创建和查询响应包含 `orderId`、`status`、`items`、`address`、`deliveryType`、`coupons`、`itemAmount`、`discountAmount`、`shippingFee`、`payableAmount`。每个商品明细包含编号、名称、单价、数量和小计；`coupons` 保留创建时的优惠规则与应用顺序。示例初始状态为 `PENDING_PAYMENT`，应付金额为 `62.00`。

新增错误码：

| HTTP 状态 | code | 含义 |
|------|------|------|
| 404 | ORDER_NOT_FOUND | 订单不存在 |
| 409 | INVALID_ORDER_STATUS_TRANSITION | 状态机不允许当前流转 |
| 409 | ORDER_UPDATE_CONFLICT | 其他请求已更新状态，重新读取订单后再判断操作 |

所有原有参数校验与错误响应约定继续适用，见 [HTTP 接口文档](http-api.md)。

## 6. 事务与阶段边界

创建订单在可重复读事务内查询商品/优惠规则、校验、计算并写入主表、明细、券快照。任何一步写入失败都会回滚。查询订单不重新计算金额。状态更新复用原有状态机，并通过 `WHERE order_id = ? AND status = ?` 防止覆盖已发生的状态变更。

本阶段完成数据库持久化和订单聚合的原子写入。库存仍只做可用数量校验，不扣减或预占；券码不代表用户持有的单次券。状态接口用于业务演示，不连接真实支付。库存扣减/回补、下单幂等、用户权限及支付回调属于后续阶段；重复创建请求当前会生成不同订单。

## 7. 运行测试

常规验收：

```powershell
mvn clean verify checkstyle:check spotbugs:check
```

原生 MySQL 验收使用独立测试库，例如 `order_settlement_test`。测试会初始化目录，并临时修改/删除测试库数据，必须使用可丢弃的专用数据库。测试入口会在建立连接前校验数据库名以 `_test` 结尾。

```powershell
$env:MYSQL_TEST_URL = 'jdbc:mysql://localhost:3306/order_settlement_test?connectionTimeZone=UTC'
$env:MYSQL_TEST_USERNAME = '专用测试账号'
$env:MYSQL_TEST_PASSWORD = '专用测试密码'
mvn -Pmysql-it clean verify checkstyle:check spotbugs:check
```

该命令先执行常规测试，再由 Failsafe 将持久化与事务回滚用例运行在 MySQL 上；缺少连接参数时会失败，不会静默跳过或使用 H2 替代。结果见 [第三阶段验收报告](stage-3-verification.md)。

参考：[MyBatis 与 Spring Boot 版本兼容性](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)、[Spring Boot 数据库迁移](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html)、[MySQL 事务隔离](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html)。
