package app.mosia.nexus
package infrastructure.messaging.consumer

import domain.config.AppConfig
import domain.event.EmailNotificationEvent
import domain.services.infra.{EmailService, RabbitMQService}

import com.rabbitmq.client.{Connection, Delivery}
import nl.vroste.zio.amqp.*
import nl.vroste.zio.amqp.model.*
import zio.*
import zio.json.*

import java.net.URI
import java.nio.charset.StandardCharsets

/** 邮件通知消费者（RabbitMQ 示例）
  *
  * 从 RabbitMQ 队列消费邮件通知事件并处理
  *
  * 队列: nexus.notifications.email
  * Exchange: nexus.notifications
  * Routing Key: notification.email.*
  *
  * 注意: 这是一个示例消费者，展示如何从 RabbitMQ 消费通知事件
  * 实际的邮件发送逻辑应该在独立的通知服务中实现
  */
final class EmailNotificationConsumer(
  rabbit: RabbitMQService,
  config: AppConfig,
  emailService: EmailService
):

  private val queueName = s"${config.rabbitmq.queue}.email"

  /** 启动消费者
    *
    * 订阅 RabbitMQ 队列并持续消费邮件通知事件
    */
  def start: Task[Unit] =
    ZIO.scoped:
      for
        // 1. 创建通道
        channel <- rabbit.getChannel
  
        // 2. 创建消费者
        consumerStream = channel.consume(
          queue = QueueName(queueName),
          consumerTag = ConsumerTag("email-notification-consumer")
        )
        // 3. 处理消息流
        _ <- consumerStream
          .mapZIO { delivery =>
            processMessage(delivery, channel)
              .catchAll { error =>
                ZIO.logError(s"Failed to process email notification: ${error.getMessage}")
              }
          }
          .runDrain
      yield ()

  /** 处理单条消息
    *
    * @param delivery
    *   AMQP 消息
    * @param channel
    *   AMQP 通道（用于确认消息）
    */
  private def processMessage(delivery: Delivery, channel: Channel): Task[Unit] =
    for
      // 1. 解析消息内容
      messageContent <- ZIO.attempt {
        new String(delivery.getBody, StandardCharsets.UTF_8)
      }

      // 2. 反序列化为邮件通知事件
      event <- ZIO
        .fromEither(messageContent.fromJson[EmailNotificationEvent])
        .mapError(err => new RuntimeException(s"Failed to parse email notification event: $err"))

      // 3. 处理邮件发送
      _ <- handleEmailNotification(event)

      // 4. 确认消息
      _ <- channel.ack(DeliveryTag(delivery.getEnvelope.getDeliveryTag))

      // 5. 记录日志
      _ <- ZIO.logInfo(s"Processed email notification: eventId=${event.eventId}, to=${event.to}")
    yield ()

  /** 处理邮件通知
    *
    * 使用 EmailService 实际发送邮件
    *
    * @param event
    *   邮件通知事件
    */
  private def handleEmailNotification(event: EmailNotificationEvent): Task[Unit] =
    for
      _ <- ZIO.logInfo(s"Sending email: to=${event.to}, subject=${event.subject}")

      // 实际发送邮件
      _ <- event.templateId match
        case Some(templateId) if templateId == "verification-code" =>
          // 使用验证码模板
          val code = event.templateData.flatMap(_.get("code")).getOrElse("")
          val purpose = event.templateData.flatMap(_.get("purpose")).getOrElse("验证")
          emailService.sendVerificationCode(event.to, code, purpose)

        case _ =>
          // 发送普通 HTML 邮件
          emailService.sendHtml(event.to, event.subject, event.body)

      _ <- ZIO.logInfo(s"Email sent successfully: eventId=${event.eventId}")
    yield ()

object EmailNotificationConsumer:
  /** 创建并启动消费者 */
  val live: ZLayer[AppConfig & EmailService & RabbitMQService, Throwable, EmailNotificationConsumer] =
    ZLayer.scoped {
      for
        // 1. 获取配置和服务
        config <- ZIO.service[AppConfig]
        emailService <- ZIO.service[EmailService]
        rabbit <- ZIO.service[RabbitMQService]
        
        // 3. 创建消费者实例
        consumer = new EmailNotificationConsumer(rabbit, config, emailService)

//        _ <- ZIO.logInfo(s"EmailNotificationConsumer created for queue: ${config.rabbitmq.queue}.email")
      yield consumer
    }
