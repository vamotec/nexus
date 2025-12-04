package app.mosia.nexus
package infrastructure.messaging

import domain.config.AppConfig
import domain.error.AppTask
import domain.repository.OutboxRepository
import domain.services.infra.{DomainEventPublisher, RabbitMQService, RedisService}

import zio.*
import zio.json.*

/** 混合事件发布器
  *
  * 智能选择发布策略:
  *   - **关键业务事件** → PostgreSQL Outbox（事务一致性）
  *   - **高频非关键事件** → Redis Streams（高性能）
  *   - **外部通知事件** → RabbitMQ（异步通知）
  *
  * 路由规则（可配置）:
  *   - UserCreated, UserUpdated → Outbox（关键）
  *   - SessionCreated, SessionStarted → Redis Streams（高频）
  *   - TrainingJobCreated → Outbox（关键）
  *   - EmailNotification, SmsNotification → RabbitMQ（通知）
  *   - 其他事件 → Redis Streams（默认）
  *
  * 使用示例:
  * {{{
  * // 在 Service 中注入 DomainEventPublisher（实际使用 HybridEventPublisher）
  * eventPublisher.publish(UserCreated(...))  // → Outbox（自动选择）
  * eventPublisher.publish(SessionStarted(...))  // → Redis Streams（自动选择）
  * eventPublisher.publish(EmailNotificationEvent(...))  // → RabbitMQ（自动选择）
  * }}}
  *
  * @param outboxPublisher
  *   PostgreSQL Outbox 发布器
  * @param redisPublisher
  *   Redis Streams 发布器
  * @param rabbitMQPublisher
  *   RabbitMQ 发布器
  */
final class HybridEventPublisher(
  outboxPublisher: PostgresOutboxPublisher,
  redisPublisher: RedisEventPublisher,
  rabbitMQPublisher: RabbitMQPublisher
) extends DomainEventPublisher:

  override def publish[E: {JsonEncoder, Tag}](event: E): AppTask[Unit] =
    val eventTypeName = summon[Tag[E]].tag.shortName

    // 根据事件类型选择发布策略
    if shouldUseRabbitMQ(eventTypeName) then
      // 使用 RabbitMQ（外部通知）
      rabbitMQPublisher.publish(event) *>
        ZIO.logDebug(s"Event routed to RabbitMQ: $eventTypeName")
    else if shouldUseOutbox(eventTypeName) then
      // 使用 Outbox（事务一致性）
      outboxPublisher.publish(event) *>
        ZIO.logDebug(s"Event routed to Outbox: $eventTypeName")
    else
      // 使用 Redis Streams（高性能）
      redisPublisher.publish(event) *>
        ZIO.logDebug(s"Event routed to Redis Streams: $eventTypeName")

  /** 判断是否应该使用 RabbitMQ
    *
    * 外部通知事件使用 RabbitMQ
    *
    * 规则:
    *   - EmailNotification：RabbitMQ（邮件通知）
    *   - SmsNotification：RabbitMQ（短信通知）
    *   - WebhookNotification：RabbitMQ（Webhook 通知）
    *   - Notification：RabbitMQ（通用通知）
    */
  private def shouldUseRabbitMQ(eventTypeName: String): Boolean =
    eventTypeName match
      // 通知相关事件 → RabbitMQ
      case name if name.contains("Notification") => true
      case name if name.contains("Email")        => true
      case name if name.contains("Sms")          => true
      case name if name.contains("Webhook")      => true

      // 其他：不使用 RabbitMQ
      case _ => false

  /** 判断是否应该使用 Outbox
    *
    * 关键业务事件使用 Outbox，其他使用 Redis Streams
    *
    * 规则:
    *   - User 相关：Outbox（用户注册、更新涉及多表操作）
    *   - Training 相关：Outbox（训练任务涉及资源分配）
    *   - Payment 相关：Outbox（支付必须保证一致性）
    *   - Session 相关：Redis Streams（高频，可容忍短暂延迟）
    *   - 其他：Redis Streams（默认）
    */
  private def shouldUseOutbox(eventTypeName: String): Boolean =
    eventTypeName match
      // 关键业务事件 → Outbox
      case name if name.startsWith("User")        => true // 用户相关
      case name if name.contains("Training")      => true // 训练任务
      case name if name.contains("Payment")       => true // 支付相关
      case name if name.contains("Order")         => true // 订单相关
      case name if name.contains("Registration")  => true // 注册相关

      // 高频非关键事件 → Redis Streams
      case name if name.startsWith("Session")     => false // 会话相关（高频）
      case name if name.contains("Metric")        => false // 指标相关（高频）
      case name if name.contains("Heartbeat")     => false // 心跳相关（高频）

      // 默认：Redis Streams
      case _ => false

object HybridEventPublisher:
  /** 创建 HybridEventPublisher */
  val live: ZLayer[PostgresOutboxPublisher & RedisEventPublisher & RabbitMQPublisher, Nothing, DomainEventPublisher] =
    ZLayer.fromFunction(HybridEventPublisher(_, _, _))

  /** 完整的事件发布器（包含所有依赖）*/
  val eventLive: ZLayer[OutboxRepository & RedisService & RabbitMQService & AppConfig, Nothing, DomainEventPublisher] =
    (PostgresOutboxPublisher.live ++ RedisEventPublisher.live ++ RabbitMQPublisher.live) >>> live
