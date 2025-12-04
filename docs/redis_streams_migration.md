# Redis Streams 迁移指南

## 概览

本项目已从 Kafka 迁移到 **Redis Streams** 作为事件流解决方案。主要原因：
- **成本优势**: Upstash Redis 比 Kafka 云服务便宜 90%+
- **需求匹配**: 当前事件流量不大，Redis Streams 性能足够
- **架构简化**: 无需管理 Kafka broker 和 Zookeeper

## 架构设计

### 事件流结构

```
┌─────────────────┐
│  Application    │
│   Services      │
└────────┬────────┘
         │ publish event
         ▼
┌─────────────────────────┐
│ RedisEventPublisher     │ ◄── DomainEventPublisher 接口
└────────┬────────────────┘
         │ xAdd
         ▼
┌──────────────────────────────────────────────┐
│           Redis Streams (Upstash)            │
│                                              │
│  events:session    ─► SessionEventConsumer   │
│  events:training   ─► TrainingEventConsumer  │
│  events:project    ─► (待实现)                │
│  events:user       ─► (待实现)                │
│  events:simulation ─► (待实现)                │
└──────────────────────────────────────────────┘
         │ xReadGroup
         ▼
┌─────────────────┐
│   Consumers     │ ◄── 消费者组（nexus-*-consumers）
│   (Workers)     │
└─────────────────┘
```

### Stream 命名规则

| Stream Key        | 事件类型          | 消费者组                    |
|-------------------|-------------------|-----------------------------|
| `events:session`  | SessionEvent      | `nexus-session-consumers`   |
| `events:training` | TrainingEvent     | `nexus-training-consumers`  |
| `events:project`  | ProjectEvent      | `nexus-project-consumers`   |
| `events:user`     | UserEvent         | `nexus-user-consumers`      |
| `events:simulation` | SimulationEvent | `nexus-simulation-consumers` |

## 配置

### 环境变量

```bash
# .env 文件
REDIS_URI=rediss://default:your-password@flying-dragon-12345.upstash.io:6379

# 可选配置
STREAMS_ENABLED=true  # 默认: true
```

### application.conf

```hocon
app {
  cache {
    redis {
      uri = "redis://localhost:6379"
      uri = ${?REDIS_URI}
    }
  }

  streams {
    enabled = true
    cleanup {
      enabled = true
      interval = 1h
      maxLength = 100000  # 保留最多 10 万条消息
    }
    consumer {
      batchSize = 10
      blockTimeMs = 1000
    }
  }
}
```

## 使用示例

### 1. 发布事件

在你的 Service 中注入 `DomainEventPublisher`：

```scala
final class SessionServiceLive(
  sessionRepo: SessionRepository,
  eventPublisher: DomainEventPublisher  // ◄── 注入
) extends SessionService:

  override def createSession(userId: UserId, config: SessionConfig): AppTask[Session] =
    for
      // 1. 创建会话
      session <- sessionRepo.create(userId, config)

      // 2. 发布事件
      _ <- eventPublisher.publish(
        SessionEvent.SessionCreated(
          sessionId = session.id,
          userId = userId,
          occurredAt = Instant.now()
        )
      )
    yield session
```

**事件会自动路由到 `events:session` Stream。**

### 2. 消费事件

消费者已在 `Main.scala` 中自动启动，无需手动操作。

查看消费者实现：
- `SessionEventConsumer.scala` - 处理会话事件
- `TrainingEventConsumer.scala` - 处理训练事件

### 3. 监控 Streams

#### 通过代码

```scala
for
  monitor <- ZIO.service[StreamsMonitorService]

  // 获取所有 Stream 健康状态
  statuses <- monitor.getHealthStatus

  // 检查单个 Stream
  sessionHealth <- monitor.getStreamHealth("events:session")

  _ <- ZIO.logInfo(s"Session stream length: ${sessionHealth.length}")
  _ <- ZIO.logInfo(s"Pending messages: ${sessionHealth.totalPending}")
  _ <- ZIO.logInfo(s"Status: ${sessionHealth.status}")
yield ()
```

#### 通过 Redis CLI

```bash
# 连接到 Upstash Redis
redis-cli -u $REDIS_URI

# 查看 Stream 长度
XLEN events:session

# 查看 Stream 信息
XINFO STREAM events:session

# 查看消费者组
XINFO GROUPS events:session

# 查看消费者组中的消费者
XINFO CONSUMERS events:session nexus-session-consumers

# 查看最近 10 条消息
XRANGE events:session - + COUNT 10

# 查看 pending 消息
XPENDING events:session nexus-session-consumers
```

### 4. 手动清理 Stream

```scala
for
  monitor <- ZIO.service[StreamsMonitorService]

  // 保留最近 10 万条消息，删除旧消息
  trimmed <- monitor.trimStream("events:session", maxLen = 100000)

  _ <- ZIO.logInfo(s"Trimmed $trimmed messages from events:session")
yield ()
```

## 消息格式

每条 Stream 消息包含以下字段：

```json
{
  "event_type": "SessionCreated",
  "payload": "{\"sessionId\":\"uuid\",\"userId\":\"uuid\",\"occurredAt\":\"2024-11-26T10:30:00Z\"}",
  "timestamp": "2024-11-26T10:30:00Z",
  "aggregate_id": "session-uuid"
}
```

## 性能特性

| 指标                  | Redis Streams   | Kafka          |
|-----------------------|-----------------|----------------|
| 吞吐量                | ~50K msgs/sec   | ~1M msgs/sec   |
| 延迟（P99）           | <500ms          | <10ms          |
| 消息持久化            | ✅ 支持          | ✅ 支持         |
| 消息顺序              | ✅ 分区内有序     | ✅ 分区内有序    |
| 消费者组              | ✅ 支持          | ✅ 支持         |
| 消息回溯              | ✅ 有限支持      | ✅ 强大          |
| 运维复杂度            | ⭐⭐ 简单         | ⭐⭐⭐⭐ 复杂   |
| 成本（Upstash）       | $5-10/月        | $100+/月       |

## 故障排查

### 1. 消费者不工作

**检查步骤：**

```bash
# 1. 验证 Stream 是否有消息
redis-cli -u $REDIS_URI XLEN events:session

# 2. 检查消费者组是否存在
redis-cli -u $REDIS_URI XINFO GROUPS events:session

# 3. 查看应用日志
tail -f logs/application.log | grep "EventConsumer"
```

**常见原因：**
- Redis 连接失败（检查 `REDIS_URI`）
- 消费者组未创建（应用会自动创建）
- 消息被快速处理（正常情况）

### 2. 消息积压

**检查 pending 消息：**

```bash
redis-cli -u $REDIS_URI XPENDING events:session nexus-session-consumers
```

**解决方法：**
- 增加消费者实例（水平扩展）
- 增大 `consumer.batchSize`
- 检查消费逻辑是否有性能问题

### 3. Stream 无限增长

**检查 Stream 长度：**

```bash
redis-cli -u $REDIS_URI XLEN events:session
```

**解决方法：**
- 确认自动清理任务运行正常（每小时触发）
- 手动清理：`monitor.trimStream("events:session", 100000)`
- 调整 `streams.cleanup.maxLength` 配置

## 迁移清单

- [x] 实现 RedisService Streams 接口
- [x] 实现 RedisEventPublisher
- [x] 迁移 SessionEventConsumer 到 Redis Streams
- [x] 迁移 TrainingEventConsumer 到 Redis Streams
- [x] 实现 StreamsMonitorService
- [x] 更新 Main.scala 依赖注入
- [x] 更新配置文件
- [x] 移除 Kafka 依赖（保留注释配置）
- [ ] 实现 ProjectEventConsumer
- [ ] 实现 UserEventConsumer
- [ ] 实现 SimulationEventConsumer
- [ ] 添加 Prometheus 监控指标
- [ ] 添加告警（Stream 长度、pending 消息）

## 未来优化

### 短期（1-2 周）
- [ ] 添加 Dead Letter Queue（处理失败消息）
- [ ] 实现消息重试机制
- [ ] 添加消息序列化版本控制

### 中期（1-3 个月）
- [ ] 实现消息压缩（减少 Redis 内存使用）
- [ ] 添加消息优先级队列
- [ ] 实现消息过滤（Consumer 端）

### 长期（6 个月+）
- [ ] 如果吞吐量增长 > 10K msgs/sec，考虑迁移回 Kafka
- [ ] 或者使用 Upstash Kafka（保持 Serverless 架构）

## 性能基准测试

### 本地测试结果

```bash
# 测试环境: M1 Mac, Redis 7.0, Upstash
# 测试工具: redis-benchmark

# 写入性能
redis-benchmark -u $REDIS_URI -t xadd -n 10000 -r 10000
# 结果: ~15K requests/sec

# 读取性能
redis-benchmark -u $REDIS_URI -t xrange -n 10000
# 结果: ~20K requests/sec
```

### 生产环境建议

- **小型应用**（<1K events/sec）: Upstash Free Tier 足够
- **中型应用**（1K-10K events/sec）: Upstash $10/月方案
- **大型应用**（>10K events/sec）: 考虑自建 Redis 或迁移到 Kafka

## 参考资源

- [Redis Streams 官方文档](https://redis.io/docs/data-types/streams/)
- [Lettuce 客户端文档](https://lettuce.io/core/release/reference/)
- [Upstash Redis 文档](https://docs.upstash.com/redis)
- [ZIO Redis](https://github.com/zio/zio-redis)（备选方案）

## 相关文件

- `infrastructure/messaging/RedisEventPublisherLive.scala` - 生产者实现
- `infrastructure/messaging/consumer/SessionEventConsumer.scala` - Session 消费者
- `infrastructure/messaging/consumer/TrainingEventConsumer.scala` - Training 消费者
- `infrastructure/messaging/StreamsMonitorService.scala` - 监控服务
- `infrastructure/redis/LettuceRedis.scala` - Lettuce 集成
- `domain/services/infra/RedisService.scala` - Redis 服务接口
- `Main.scala:74-82` - 消费者启动代码
