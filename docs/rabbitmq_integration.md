# RabbitMQ 集成文档

## 1. 概述

Nexus 使用 **RabbitMQ** 作为外部通知系统的消息队列，用于处理邮件、短信和 Webhook 等异步通知。

### 1.1 为什么使用 RabbitMQ？

在混合事件发布策略中，我们使用三种不同的消息系统：

- **PostgreSQL Outbox** → 关键业务事件（用户注册、支付）需要事务一致性
- **Redis Streams** → 高频非关键事件（会话指标、心跳）需要高性能
- **RabbitMQ** → 外部通知事件（邮件、短信）需要可靠的异步处理和灵活的路由

RabbitMQ 的优势：
- ✅ 成熟的消息队列系统，支持复杂的路由策略
- ✅ 支持多种消息模式（点对点、发布订阅、路由、主题）
- ✅ 消息持久化，保证消息不丢失
- ✅ 死信队列（DLQ）机制，处理失败消息
- ✅ 丰富的管理界面和监控工具

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────┐
│  Nexus Service  │
│                 │
│  发布通知事件   │
└────────┬────────┘
         │
         │ publish(EmailNotificationEvent)
         ↓
┌─────────────────────────────┐
│   HybridEventPublisher      │
│  (智能路由)                 │
│  - Outbox → 关键业务        │
│  - Redis → 高频事件         │
│  - RabbitMQ → 通知事件      │
└────────┬────────────────────┘
         │
         │ shouldUseRabbitMQ(event)
         ↓
┌─────────────────────────────┐
│    RabbitMQPublisher        │
│  发布到 Exchange             │
│  exchange: nexus.notifications │
│  routingKey: notification.email.send │
└────────┬────────────────────┘
         │
         ↓
┌─────────────────────────────────────────────┐
│           RabbitMQ Server                   │
│                                             │
│  Exchange: nexus.notifications (Topic)     │
│     ├─ notification.email.*   → Queue: email    │
│     ├─ notification.sms.*     → Queue: sms      │
│     └─ notification.webhook.* → Queue: webhook  │
└────────┬──────────┬──────────┬─────────────┘
         │          │          │
         ↓          ↓          ↓
   ┌─────────┐ ┌────────┐ ┌──────────┐
   │  Email  │ │  SMS   │ │ Webhook  │
   │Consumer │ │Consumer│ │ Consumer │
   └─────────┘ └────────┘ └──────────┘
         │          │          │
         ↓          ↓          ↓
   ┌─────────┐ ┌────────┐ ┌──────────┐
   │ SendGrid│ │ Twilio │ │ HTTP POST│
   └─────────┘ └────────┘ └──────────┘
```

### 2.2 Exchange 和队列设计

**Exchange**: `nexus.notifications` (Type: Topic)

**队列绑定**:
- `nexus.notifications.email` ← `notification.email.*`
- `nexus.notifications.sms` ← `notification.sms.*`
- `nexus.notifications.webhook` ← `notification.webhook.*`

**路由键规则**:
- `notification.email.send` → 发送邮件
- `notification.sms.send` → 发送短信
- `notification.webhook.send` → 发送 Webhook

## 3. 配置

### 3.1 application.conf

```hocon
app {
  rabbitmq {
    host = "localhost"
    host = ${?RABBITMQ_HOST}
    port = 5672
    port = ${?RABBITMQ_PORT}
    username = "guest"
    username = ${?RABBITMQ_USERNAME}
    password = "guest"
    password = ${?RABBITMQ_PASSWORD}
    virtualHost = "/"
    virtualHost = ${?RABBITMQ_VIRTUAL_HOST}

    exchange = "nexus.notifications"
    exchange = ${?RABBITMQ_EXCHANGE}

    queue = "nexus.notifications"
    queue = ${?RABBITMQ_QUEUE}
  }
}
```

### 3.2 环境变量

生产环境建议使用环境变量配置：

```bash
export RABBITMQ_HOST=rabbitmq.example.com
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=nexus
export RABBITMQ_PASSWORD=your-secure-password
export RABBITMQ_VIRTUAL_HOST=/nexus
export RABBITMQ_EXCHANGE=nexus.notifications
export RABBITMQ_QUEUE=nexus.notifications
```

### 3.3 自建 RabbitMQ 服务器

使用 Docker 快速部署：

```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=nexus \
  -e RABBITMQ_DEFAULT_PASS=your-password \
  rabbitmq:3-management
```

访问管理界面: http://localhost:15672

## 4. 使用方法

### 4.1 发送邮件通知

```scala
import domain.event.EmailNotificationEvent
import domain.services.infra.DomainEventPublisher

// 在业务服务中注入 DomainEventPublisher
class UserService(eventPublisher: DomainEventPublisher):

  def registerUser(email: String, username: String): Task[User] =
    for
      // 1. 创建用户
      user <- createUser(email, username)

      // 2. 发送欢迎邮件（自动路由到 RabbitMQ）
      _ <- eventPublisher.publish(
        EmailNotificationEvent(
          eventId = UUID.randomUUID().toString,
          to = email,
          subject = "欢迎加入 Nexus！",
          body = s"<h1>欢迎，$username！</h1><p>感谢您注册 Nexus 平台。</p>",
          templateId = Some("welcome-email"),
          templateData = Some(Map("username" -> username))
        )
      )
    yield user
```

### 4.2 发送短信通知

```scala
import domain.event.SmsNotificationEvent

class SessionService(eventPublisher: DomainEventPublisher):

  def notifySessionCompleted(userId: String, phone: String): Task[Unit] =
    eventPublisher.publish(
      SmsNotificationEvent(
        eventId = UUID.randomUUID().toString,
        to = phone,
        message = "您的仿真会话已完成，请查看结果。",
        templateId = Some("session-completed")
      )
    )
```

### 4.3 发送 Webhook 通知

```scala
import domain.event.WebhookNotificationEvent

class TrainingService(eventPublisher: DomainEventPublisher):

  def notifyTrainingCompleted(webhookUrl: String, jobId: String): Task[Unit] =
    eventPublisher.publish(
      WebhookNotificationEvent(
        eventId = UUID.randomUUID().toString,
        url = webhookUrl,
        payload = Map(
          "event" -> "training.completed",
          "job_id" -> jobId,
          "timestamp" -> Instant.now().toString
        ),
        headers = Some(Map(
          "X-Webhook-Signature" -> "sha256=..."
        ))
      )
    )
```

## 5. 事件路由规则

`HybridEventPublisher` 会自动根据事件类型选择发布策略：

```scala
// 通知事件 → RabbitMQ
eventPublisher.publish(EmailNotificationEvent(...))  // → RabbitMQ
eventPublisher.publish(SmsNotificationEvent(...))    // → RabbitMQ
eventPublisher.publish(WebhookNotificationEvent(...)) // → RabbitMQ

// 关键业务事件 → PostgreSQL Outbox
eventPublisher.publish(UserCreated(...))             // → Outbox
eventPublisher.publish(PaymentProcessed(...))        // → Outbox

// 高频事件 → Redis Streams
eventPublisher.publish(SessionStarted(...))          // → Redis
eventPublisher.publish(MetricRecorded(...))          // → Redis
```

路由判断逻辑（在 `HybridEventPublisher.shouldUseRabbitMQ`）:

```scala
private def shouldUseRabbitMQ(eventTypeName: String): Boolean =
  eventTypeName match
    case name if name.contains("Notification") => true
    case name if name.contains("Email")        => true
    case name if name.contains("Sms")          => true
    case name if name.contains("Webhook")      => true
    case _ => false
```

## 6. 消费者实现

### 6.1 邮件消费者示例

`EmailNotificationConsumer` 已经作为示例实现：

```scala
// 在 Main.scala 中自动启动
emailConsumer <- ZIO.service[EmailNotificationConsumer]
_ <- emailConsumer.start.fork
```

### 6.2 自定义消费者

参考 `EmailNotificationConsumer` 实现自己的消费者：

```scala
import nl.vroste.zio.amqp.*

class SmsNotificationConsumer(connection: Amqp.Connection, config: AppConfig):

  def start: Task[Unit] =
    for
      channel <- Amqp.createChannel(connection)
      consumer <- channel.consume(
        queue = QueueName(s"${config.rabbitmq.queue}.sms"),
        consumerTag = ConsumerTag("sms-notification-consumer")
      )

      _ <- consumer
        .mapZIO { delivery =>
          processMessage(delivery, channel)
        }
        .runDrain
    yield ()

  private def processMessage(delivery: Delivery, channel: Channel): Task[Unit] =
    for
      messageContent <- ZIO.attempt(new String(delivery.body.toArray, StandardCharsets.UTF_8))
      event <- ZIO.fromEither(messageContent.fromJson[SmsNotificationEvent])
      _ <- sendSms(event)
      _ <- channel.ack(delivery.deliveryTag)
    yield ()

  private def sendSms(event: SmsNotificationEvent): Task[Unit] =
    // 调用 Twilio API 发送短信
    ???
```

## 7. 监控和运维

### 7.1 RabbitMQ 管理界面

访问: http://localhost:15672

默认账号: `guest` / `guest`

**监控指标**:
- 队列消息数量
- 消费速率
- 未确认消息数量
- 死信队列消息

### 7.2 日志

启动日志：

```
[info] RabbitMQ service initialized: localhost:5672
[info] EmailNotificationConsumer started: queue=nexus.notifications.email
```

发布日志：

```
[info] Published notification event: EmailNotificationEvent with routingKey: notification.email.send
[debug] Event routed to RabbitMQ: EmailNotificationEvent
```

消费日志：

```
[info] Processed email notification: eventId=abc123, to=user@example.com
[info] Email sent successfully: eventId=abc123
```

### 7.3 健康检查

可以通过 RabbitMQ Management API 检查健康状态：

```bash
curl -u nexus:password http://localhost:15672/api/healthchecks/node
```

## 8. 故障排查

### 8.1 连接失败

**错误**: `Failed to connect to RabbitMQ`

**排查步骤**:
1. 检查 RabbitMQ 服务是否运行: `docker ps | grep rabbitmq`
2. 检查端口是否开放: `telnet localhost 5672`
3. 检查配置中的主机和端口
4. 检查用户名密码是否正确

### 8.2 消息未被消费

**现象**: 队列中消息堆积

**排查步骤**:
1. 检查消费者是否启动: 查看启动日志
2. 检查消费者是否有错误: 查看错误日志
3. 检查队列绑定是否正确: RabbitMQ 管理界面 → Queues → Bindings
4. 检查路由键是否匹配

### 8.3 消息重复消费

**原因**: 消费者处理失败未确认，消息重新投递

**解决方案**:
1. 确保消息处理具有幂等性
2. 使用去重机制（基于 `eventId`）
3. 记录已处理的事件 ID

### 8.4 死信队列

配置死信队列处理失败消息：

```scala
channel.declareQueue(
  QueueDeclaration(
    name = QueueName("nexus.notifications.email"),
    arguments = Map(
      "x-dead-letter-exchange" -> "nexus.notifications.dlx",
      "x-dead-letter-routing-key" -> "dlq.email"
    )
  )
)
```

## 9. 最佳实践

### 9.1 消息持久化

确保消息不会因为 RabbitMQ 重启而丢失：

```scala
channel.declareQueue(
  QueueDeclaration(
    name = QueueName("nexus.notifications.email"),
    durable = true  // 队列持久化
  )
)

channel.publish(
  exchange = ExchangeName("nexus.notifications"),
  routingKey = RoutingKey("notification.email.send"),
  body = message,
  mandatory = false,
  immediate = false,
  props = BasicProperties.builder()
    .deliveryMode(2)  // 消息持久化
    .build()
)
```

### 9.2 错误重试

使用指数退避重试策略：

```scala
private def processMessageWithRetry(delivery: Delivery): Task[Unit] =
  processMessage(delivery)
    .retry(
      Schedule.exponential(1.second) && Schedule.recurs(3)
    )
    .catchAll { error =>
      ZIO.logError(s"Failed to process message after retries: ${error.getMessage}") *>
        channel.nack(delivery.deliveryTag, requeue = false)  // 发送到死信队列
    }
```

### 9.3 优雅关闭

确保应用关闭时正确清理资源：

```scala
override def close: AppTask[Unit] =
  for
    _ <- channel.close
    _ <- connection.close
    _ <- ZIO.logInfo("RabbitMQ connection closed")
  yield ()
```

## 10. 性能优化

### 10.1 预取数量

控制消费者一次预取的消息数量：

```scala
channel.basicQos(prefetchCount = 10)
```

### 10.2 批量确认

批量确认消息以提高性能：

```scala
// 每处理 10 条消息批量确认一次
var count = 0
consumer.foreach { delivery =>
  processMessage(delivery)
  count += 1
  if (count % 10 == 0) {
    channel.ack(delivery.deliveryTag, multiple = true)
  }
}
```

### 10.3 并发消费

启动多个消费者实例并发处理：

```scala
// 启动 3 个并发消费者
ZIO.collectAllPar(
  List.fill(3)(emailConsumer.start.fork)
)
```

## 11. 总结

RabbitMQ 在 Nexus 中的作用：

✅ 专门处理外部通知事件（邮件、短信、Webhook）
✅ 提供可靠的消息投递保证
✅ 支持灵活的路由和多种消费者
✅ 与 PostgreSQL Outbox 和 Redis Streams 配合，形成完整的事件发布策略

**混合事件发布策略总结**:

| 事件类型 | 发布系统 | 使用场景 | 特点 |
|---------|---------|---------|------|
| 关键业务事件 | PostgreSQL Outbox | 用户注册、支付 | 事务一致性 |
| 高频非关键事件 | Redis Streams | 会话指标、心跳 | 高性能 |
| 外部通知事件 | RabbitMQ | 邮件、短信、Webhook | 可靠异步处理 |

**下一步**:
- 实现邮件发送集成（SendGrid/AWS SES）
- 实现短信发送集成（Twilio）
- 配置死信队列和重试策略
- 添加 Prometheus 监控指标
