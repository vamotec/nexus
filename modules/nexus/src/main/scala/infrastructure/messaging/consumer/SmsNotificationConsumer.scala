package app.mosia.nexus
package infrastructure.messaging.consumer

import domain.config.AppConfig
import domain.event.SmsNotificationEvent
import domain.services.infra.{RabbitMQService, SmsService}

import com.rabbitmq.client.Delivery
import nl.vroste.zio.amqp.*
import nl.vroste.zio.amqp.model.*
import zio.*
import zio.json.*

import java.nio.charset.StandardCharsets

/** 短信通知消费者（RabbitMQ）
  *
  * 从 RabbitMQ 队列消费短信通知事件并发送短信
  *
  * 队列: nexus.notifications.sms
  * Exchange: nexus.notifications
  * Routing Key: notification.sms.*
  */
final class SmsNotificationConsumer(
  rabbit: RabbitMQService,
  config: AppConfig,
  smsService: SmsService
):

  private val queueName = s"${config.rabbitmq.queue}.sms"

  /** 启动消费者 */
  def start: Task[Unit] =
    ZIO.scoped:
      for
        // 1. 创建通道
        channel <- rabbit.getChannel

        // 2. 创建消费者
        consumerStream = channel.consume(
          queue = QueueName(queueName),
          consumerTag = ConsumerTag("sms-notification-consumer")
        )

        // 3. 记录启动日志
//        _ <- ZIO.logInfo(s"SmsNotificationConsumer started: queue=$queueName")

        // 4. 处理消息流
        _ <- consumerStream
          .mapZIO { delivery =>
            processMessage(delivery, channel)
              .catchAll { error =>
                ZIO.logError(s"Failed to process SMS notification: ${error.getMessage}")
              }
          }
          .runDrain
      yield ()

  /** 处理单条消息 */
  private def processMessage(delivery: Delivery, channel: Channel): Task[Unit] =
    for
      // 1. 解析消息内容
      messageContent <- ZIO.attempt {
        new String(delivery.getBody, StandardCharsets.UTF_8)
      }

      // 2. 反序列化为短信通知事件
      event <- ZIO
        .fromEither(messageContent.fromJson[SmsNotificationEvent])
        .mapError(err => new RuntimeException(s"Failed to parse SMS notification event: $err"))

      // 3. 发送短信
      _ <- handleSmsNotification(event)

      // 4. 确认消息
      _ <- channel.ack(DeliveryTag(delivery.getEnvelope.getDeliveryTag))

      // 5. 记录日志
      _ <- ZIO.logInfo(s"Processed SMS notification: eventId=${event.eventId}, to=${event.to}")
    yield ()

  /** 处理短信通知 */
  private def handleSmsNotification(event: SmsNotificationEvent): Task[Unit] =
    for
      _ <- ZIO.logInfo(s"Sending SMS: to=${event.to}, message=${event.message}")

      // 发送短信
      _ <- event.templateId match
        case Some(_) =>
          // 使用模板发送验证码
          val code = event.templateData.flatMap(_.get("code")).getOrElse(event.message)
          smsService.sendVerificationCode(event.to, code)

        case None =>
          // 发送普通通知
          smsService.sendNotification(event.to, event.message)

      _ <- ZIO.logInfo(s"SMS sent successfully: eventId=${event.eventId}")
    yield ()

object SmsNotificationConsumer:
  /** 创建并启动消费者 */
  val live: ZLayer[AppConfig & SmsService & RabbitMQService, Throwable, SmsNotificationConsumer] =
    ZLayer.scoped {
      for
        // 1. 获取配置
        config <- ZIO.service[AppConfig]
        smsService <- ZIO.service[SmsService]
        rabbit <- ZIO.service[RabbitMQService]

        // 3. 创建消费者实例
        consumer = new SmsNotificationConsumer(rabbit, config, smsService)

//        _ <- ZIO.logInfo(s"SmsNotificationConsumer created for queue: ${config.rabbitmq.queue}.sms")
      yield consumer
    }
