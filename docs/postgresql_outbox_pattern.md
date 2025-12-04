# PostgreSQL Outbox Pattern 使用指南

## 概览

本项目实现了 **混合事件发布策略**：

- **PostgreSQL Outbox** → 关键业务事件（需要事务一致性）
- **Redis Streams** → 高频非关键事件（追求高性能）

## 为什么需要 Outbox Pattern？

### 问题：双写问题（Dual Write Problem）

传统方式：先写数据库，再发布事件到消息系统

```scala
// ❌ 存在问题的代码
for
  user <- userRepo.create(email, password)  // 1. 写数据库
  _ <- eventPublisher.publish(UserCreated(...))  // 2. 发布事件
yield user
```

**可能的失败场景：**
- ✅ 数据库写入成功 → ❌ 事件发布失败（消息系统故障） → **数据不一致！**
- ❌ 数据库写入失败 → ✅ 事件已发布 → **数据不一致！**

### 解决方案：Outbox Pattern

在同一事务中写入业务数据 + Outbox 事件：

```scala
// ✅ 正确的代码（使用 Outbox）
for
  user <- userRepo.create(email, password)  // 1. 写数据库
  _ <- eventPublisher.publish(UserCreated(...))  // 2. 写 Outbox 表（同一事务）
  // 提交事务 → 两者都成功或都失败
yield user

// 后台 OutboxProcessor 异步发布事件到 Redis Streams
```

**优势：**
- ✅ **原子性**: 数据和事件要么都成功，要么都失败
- ✅ **可靠性**: 即使消息系统暂时不可用，事件也不会丢失
- ✅ **顺序保证**: 按照创建时间顺序处理

---

## 架构设计

### 整体流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    Application Service                          │
│                                                                 │
│  for                                                            │
│    user <- userRepo.create(...)  ◄─┐                           │
│    _ <- eventPublisher.publish(UserCreated(...))  ◄─┐          │
│                                  │                  │          │
│  yield user                      │                  │          │
└──────────────────────────────────┼──────────────────┼──────────┘
                                   │                  │
                    ┌──────────────▼──────────────────▼─────────┐
                    │       PostgreSQL 事务                      │
                    │  ┌──────────────┐   ┌──────────────┐      │
                    │  │ users 表     │   │ event_outbox │      │
                    │  │  INSERT      │   │   INSERT     │      │
                    │  └──────────────┘   └──────────────┘      │
                    │         COMMIT（原子性）                   │
                    └────────────────────────────────────────────┘
                                         │
                            ┌────────────▼─────────────┐
                            │   OutboxProcessor        │
                            │   (后台轮询，1秒/次)      │
                            └────────────┬─────────────┘
                                         │
                            ┌────────────▼─────────────┐
                            │   Redis Streams          │
                            │   events:user            │
                            └──────────────────────────┘
                                         │
                            ┌────────────▼─────────────┐
                            │   Consumers              │
                            │   (发送邮件/通知等)        │
                            └──────────────────────────┘
```

### 关键组件

| 组件 | 职责 | 文件位置 |
|------|------|----------|
| **HybridEventPublisher** | 智能路由：关键事件→Outbox，其他→Streams | `infrastructure/messaging/HybridEventPublisher.scala` |
| **PostgresOutboxPublisher** | 将事件写入 Outbox 表（事务内） | `infrastructure/messaging/PostgresOutboxPublisher.scala` |
| **OutboxProcessor** | 后台轮询 Outbox 表，发布到 Streams | `infrastructure/messaging/OutboxProcessor.scala` |
| **OutboxRepository** | Outbox 数据访问层 | `infrastructure/persistence/postgres/repository/OutboxRepositoryLive.scala` |

---

## 使用指南

### 1. 基本用法（自动路由）

**推荐方式**：直接使用 `DomainEventPublisher`，系统自动选择策略

```scala
final class UserServiceLive(
  userRepo: UserRepository,
  eventPublisher: DomainEventPublisher  // ← 注入（实际是 HybridEventPublisher）
) extends UserService:

  override def registerUser(email: String, password: String): AppTask[User] =
    for
      // 1. 创建用户（写数据库）
      user <- userRepo.create(email, hashedPassword)

      // 2. 发布事件（自动选择 Outbox，因为是关键业务事件）
      _ <- eventPublisher.publish(
        UserEvent.UserCreated(
          userId = user.id,
          email = email,
          occurredAt = Instant.now()
        )
      )
      // ← 提交事务：user 和 outbox 事件原子性写入
    yield user
```

**自动路由规则**（在 `HybridEventPublisher.scala:69` 定义）：

```scala
UserCreated       → Outbox (关键)
TrainingStarted   → Outbox (关键)
PaymentProcessed  → Outbox (关键)
SessionStarted    → Redis Streams (高频)
MetricUpdated     → Redis Streams (高频)
```

### 2. 强制使用 Outbox

如果需要强制使用 Outbox（即使是高频事件）：

```scala
final class MyServiceLive(
  repo: MyRepository,
  outboxPublisher: PostgresOutboxPublisher  // ← 直接注入 Outbox 发布器
) extends MyService:

  override def criticalOperation: AppTask[Unit] =
    for
      _ <- repo.doSomething()
      _ <- outboxPublisher.publish(CriticalEvent(...))  // 强制走 Outbox
    yield ()
```

### 3. 强制使用 Redis Streams

如果需要强制使用 Redis Streams（即使是关键事件）：

```scala
final class MyServiceLive(
  repo: MyRepository,
  redisPublisher: RedisEventPublisherLive  // ← 直接注入 Redis 发布器
) extends MyService:

  override def highFrequencyOperation: AppTask[Unit] =
    for
      _ <- repo.doSomething()
      _ <- redisPublisher.publish(HighFreqEvent(...))  // 强制走 Redis Streams
    yield ()
```

---

## 监控和运维

### 1. 查看 Outbox 状态

#### 通过代码

```scala
for
  processor <- ZIO.service[OutboxProcessor]
  stats <- processor.getStats

  _ <- ZIO.logInfo(s"Pending: ${stats.pendingCount}")
  _ <- ZIO.logInfo(s"Processing: ${stats.processingCount}")
  _ <- ZIO.logInfo(s"Published: ${stats.publishedCount}")
  _ <- ZIO.logInfo(s"Failed: ${stats.failedCount}")

  // 查看最近的失败事件
  _ <- ZIO.foreach(stats.recentFailures) { event =>
    ZIO.logWarning(s"Failed event: ${event.eventType}, error: ${event.lastError}")
  }
yield ()
```

#### 通过 SQL

```sql
-- 1. 按状态统计
SELECT status, COUNT(*) as count
FROM event_outbox
GROUP BY status;

-- 2. 查看待处理事件
SELECT id, event_type, aggregate_id, created_at, retry_count
FROM event_outbox
WHERE status = 'PENDING'
ORDER BY created_at
LIMIT 10;

-- 3. 查看失败事件
SELECT id, event_type, aggregate_id, retry_count, last_error, created_at
FROM event_outbox
WHERE status = 'FAILED'
ORDER BY created_at DESC
LIMIT 10;

-- 4. 查看处理延迟
SELECT
  event_type,
  AVG(EXTRACT(EPOCH FROM (published_at - created_at))) as avg_latency_seconds
FROM event_outbox
WHERE status = 'PUBLISHED'
  AND published_at IS NOT NULL
GROUP BY event_type;
```

### 2. 手动重试失败事件

```sql
-- 重置失败事件为 PENDING（会被重新处理）
UPDATE event_outbox
SET
  status = 'PENDING',
  retry_count = 0,
  next_retry_at = NULL,
  last_error = NULL
WHERE status = 'FAILED'
  AND id = 'specific-event-id';
```

### 3. 清理旧事件

自动清理已配置（每小时运行，保留 7 天），也可以手动清理：

```sql
-- 删除 7 天前已发布的事件
DELETE FROM event_outbox
WHERE status = 'PUBLISHED'
  AND published_at < NOW() - INTERVAL '7 days';
```

---

## 配置选项

### application.conf

```hocon
app {
  outbox {
    enabled = true

    processor {
      pollInterval = 1s      # 轮询间隔（越短延迟越低，但数据库压力越大）
      batchSize = 100        # 每次处理的事件数量
      maxRetries = 3         # 最大重试次数
    }

    cleanup {
      enabled = true
      interval = 1h          # 清理间隔
      retentionDays = 7      # 保留天数
    }
  }

  eventPublishing {
    defaultStrategy = "hybrid"  # hybrid, outbox, streams
  }
}
```

### 环境变量

```bash
# 禁用 Outbox（仅使用 Redis Streams）
OUTBOX_ENABLED=false

# 调整轮询间隔
OUTBOX_POLL_INTERVAL=5s

# 调整批处理大小
OUTBOX_BATCH_SIZE=200

# 调整保留天数
OUTBOX_RETENTION_DAYS=30

# 切换发布策略
EVENT_PUBLISHING_STRATEGY=outbox  # 所有事件都走 Outbox
```

---

## 性能特性

### Outbox vs Redis Streams

| 指标 | Outbox | Redis Streams |
|------|--------|---------------|
| **事务一致性** | ✅ 保证 | ❌ 无保证 |
| **吞吐量** | ~1K events/sec | ~50K events/sec |
| **延迟** | ~1-5 秒（轮询间隔） | <500ms |
| **可靠性** | ✅✅✅ 极高 | ✅✅ 高 |
| **运维复杂度** | ⭐⭐⭐ 中等 | ⭐⭐ 简单 |
| **适用场景** | 关键业务事件 | 高频非关键事件 |

### 性能优化建议

1. **调整轮询间隔**
   - 低延迟需求：`pollInterval = 500ms`（增加数据库压力）
   - 正常需求：`pollInterval = 1s`（默认）
   - 低压力需求：`pollInterval = 5s`（降低数据库压力）

2. **调整批处理大小**
   - 高吞吐：`batchSize = 500`
   - 正常：`batchSize = 100`（默认）
   - 低内存：`batchSize = 20`

3. **索引优化**
   - 已自动创建索引（见 V3__create_outbox_table.sql）
   - 关键索引：`idx_outbox_pending_for_processing`（部分索引）

---

## 故障排查

### 1. Outbox 事件积压

**症状**：Pending 事件数量持续增长

**检查步骤**：

```sql
-- 1. 统计各状态事件数量
SELECT status, COUNT(*) FROM event_outbox GROUP BY status;

-- 2. 查看最老的待处理事件
SELECT * FROM event_outbox
WHERE status = 'PENDING'
ORDER BY created_at
LIMIT 10;
```

**可能原因**：
- OutboxProcessor 未启动（检查日志）
- Redis Streams 连接失败（检查 Redis 连接）
- 处理速度慢于生产速度（增大 batchSize 或 pollInterval）

**解决方法**：
- 增大 `batchSize`（100 → 500）
- 减小 `pollInterval`（1s → 500ms）
- 水平扩展应用实例（FOR UPDATE SKIP LOCKED 自动分布负载）

### 2. 事件重复处理

**症状**：同一事件被处理多次

**原因**：
- OutboxProcessor 崩溃后，事件状态未更新为 PUBLISHED
- 事务隔离级别问题

**预防措施**：
- 消费者实现幂等性（使用 `aggregate_id` 去重）
- 已使用 `FOR UPDATE SKIP LOCKED` 防止并发冲突

### 3. 失败事件过多

**症状**：FAILED 状态事件数量异常

**检查步骤**：

```sql
-- 查看失败原因分布
SELECT
  SUBSTRING(last_error, 1, 100) as error_msg,
  COUNT(*) as count
FROM event_outbox
WHERE status = 'FAILED'
GROUP BY SUBSTRING(last_error, 1, 100)
ORDER BY count DESC;
```

**常见原因**：
- Redis 连接失败
- 事件序列化错误
- 网络超时

**解决方法**：
- 修复根本原因后，手动重置失败事件为 PENDING（见上文）

---

## 最佳实践

### 1. 何时使用 Outbox

✅ **应该使用 Outbox**：
- 用户注册/登录（涉及多表操作 + 发送邮件）
- 支付处理（扣款 + 发送收据）
- 订单创建（创建订单 + 扣减库存 + 发送通知）
- 训练任务启动（分配资源 + 创建任务 + 发送通知）

❌ **不应该使用 Outbox**：
- 会话心跳（每秒数百次）
- 指标更新（每秒数千次）
- 日志事件（每秒数万次）

### 2. 事件设计原则

**好的事件设计**：

```scala
case class UserCreated(
  userId: UUID,
  email: String,
  occurredAt: Instant
) derives JsonCodec

// ✅ 包含完整信息
// ✅ 不可变
// ✅ 有时间戳
// ✅ 有聚合 ID
```

**不好的事件设计**：

```scala
case class UserEvent(
  action: String,  // ❌ 字符串类型不安全
  data: Map[String, Any]  // ❌ 无类型保证
)
```

### 3. 幂等性处理

消费者应该实现幂等性（防止重复处理）：

```scala
final class WelcomeEmailConsumer:
  def handleUserCreated(event: UserCreated): Task[Unit] =
    for
      // 1. 检查是否已发送（使用聚合 ID 去重）
      sent <- emailLogRepo.findByUserId(event.userId)

      // 2. 仅当未发送时才发送
      _ <- if sent.isEmpty then
        emailService.sendWelcomeEmail(event.email) *>
        emailLogRepo.markAsSent(event.userId)  // 记录已发送
      else
        ZIO.logInfo(s"Welcome email already sent to user ${event.userId}")
    yield ()
```

---

## 相关文件

### 核心实现
- `domain/model/outbox/OutboxEvent.scala` - Outbox 事件模型
- `domain/repository/OutboxRepository.scala` - Outbox 仓储接口
- `infrastructure/persistence/postgres/repository/OutboxRepositoryLive.scala` - Outbox 仓储实现
- `infrastructure/messaging/PostgresOutboxPublisher.scala` - Outbox 发布器
- `infrastructure/messaging/OutboxProcessor.scala` - Outbox 处理器
- `infrastructure/messaging/HybridEventPublisher.scala` - 混合发布器（智能路由）

### 数据库
- `modules/migration/src/main/resources/db/migration/V3__create_outbox_table.sql` - Outbox 表结构

### 配置
- `modules/nexus/src/main/resources/application.conf:179-222` - Outbox 配置

---

## 参考资源

- [Transactional Outbox Pattern (Chris Richardson)](https://microservices.io/patterns/data/transactional-outbox.html)
- [Implementing the Outbox Pattern](https://debezium.io/blog/2019/02/19/reliable-microservices-data-exchange-with-the-outbox-pattern/)
- [PostgreSQL FOR UPDATE SKIP LOCKED](https://www.postgresql.org/docs/current/sql-select.html#SQL-FOR-UPDATE-SHARE)

---

## 总结

**你现在拥有两套事件发布系统**：

1. **PostgreSQL Outbox** → 关键业务事件（事务一致性） 2. **Redis Streams** → 高频非关键事件（高性能）

**HybridEventPublisher 自动为你选择最佳策略** 🎯
