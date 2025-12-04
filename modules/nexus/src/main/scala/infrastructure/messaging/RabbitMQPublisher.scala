package app.mosia.nexus
package infrastructure.messaging

import domain.config.AppConfig
import domain.error.AppTask
import domain.event.{EmailNotificationEvent, NotificationEvent, SmsNotificationEvent, WebhookNotificationEvent}
import domain.services.infra.{DomainEventPublisher, RabbitMQService}

import zio.*
import zio.json.*

/** RabbitMQ 通知事件发布器
  *
  * 专门用于发布通知类事件（邮件、短信、Webhook）到 RabbitMQ
  *
  * 路由规则:
  *   - EmailNotificationEvent → notification.email.send
  *   - SmsNotificationEvent → notification.sms.send
  *   - WebhookNotificationEvent → notification.webhook.send
  *
  * 队列绑定:
  *   - nexus.notifications.email ← notification.email.*
  *   - nexus.notifications.sms ← notification.sms.*
  *   - nexus.notifications.webhook ← notification.webhook.*
  *
  * @param rabbitMQ
  *   RabbitMQ 服务
  * @param config
  *   应用配置
  */
final class RabbitMQPublisher(rabbitMQ: RabbitMQService, config: AppConfig) extends DomainEventPublisher:

  private val exchangeName = config.rabbitmq.exchange

  /** 发布通知事件到 RabbitMQ
    *
    * @param event
    *   通知事件（必须是 NotificationEvent 子类型）
    * @tparam E
    *   事件类型
    * @return
    *   Task[Unit]
    */
  override def publish[E: {JsonEncoder, Tag}](event: E): AppTask[Unit] =
    for
      // 1. 获取事件类型信息
      eventTypeName <- ZIO.succeed(getEventTypeName[E])

      // 2. 确定路由键
      routingKey = getRoutingKeyForEvent(eventTypeName)

      // 3. 序列化事件为 JSON
      eventJson <- ZIO.succeed(event.toJson)

      // 4. 发布到 RabbitMQ
      _ <- rabbitMQ.publish(exchangeName, routingKey, eventJson)

      // 5. 记录日志
      _ <- ZIO.logInfo(s"Published notification event: $eventTypeName with routingKey: $routingKey to exchange: $exchangeName")
    yield ()

  // ============================================================================
  // 辅助方法
  // ============================================================================

  /** 获取事件类型名称 */
  private def getEventTypeName[E: Tag]: String =
    summon[Tag[E]].tag.shortName

  /** 根据事件类型确定 RabbitMQ 路由键
    *
    * 路由键模式: notification.<type>.send
    *
    * @param eventTypeName
    *   事件类型名称
    * @return
    *   路由键
    */
  private def getRoutingKeyForEvent(eventTypeName: String): String =
    eventTypeName match
      // 邮件通知
      case name if name.contains("EmailNotification") || name.contains("Email") =>
        "notification.email.send"

      // 短信通知
      case name if name.contains("SmsNotification") || name.contains("Sms") =>
        "notification.sms.send"

      // Webhook 通知
      case name if name.contains("WebhookNotification") || name.contains("Webhook") =>
        "notification.webhook.send"

      // 默认路由键（通用通知）
      case _ =>
        "notification.general.send"

object RabbitMQPublisher:
  /** 创建 RabbitMQPublisher ZLayer */
  val live: ZLayer[RabbitMQService & AppConfig, Nothing, RabbitMQPublisher] =
    ZLayer.fromFunction(RabbitMQPublisher(_, _))
