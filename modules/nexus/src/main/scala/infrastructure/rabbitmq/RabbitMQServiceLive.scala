package app.mosia.nexus
package infrastructure.rabbitmq

import domain.config.AppConfig
import domain.error.*
import domain.services.infra.RabbitMQService

import com.rabbitmq.client
import com.rabbitmq.client.ConnectionFactory
import nl.vroste.zio.amqp.*
import nl.vroste.zio.amqp.model.*
import zio.*

import java.nio.charset.StandardCharsets

/** RabbitMQ 服务实现
  *
  * 基于 ZIO-AMQP 实现
  *
  * @param channel
  *   AMQP 通道
  */
final class RabbitMQServiceLive(channel: Channel) extends RabbitMQService:

  /** 发布消息到 RabbitMQ Exchange */
  override def publish(exchange: String, routingKey: String, message: String): AppTask[Unit] =
    (for
      // 1. 转换消息为字节数组
      messageBytes <- ZIO.succeed(message.getBytes(StandardCharsets.UTF_8))

      // 2. 发布消息
      _ <- channel
        .publish(
          exchange = ExchangeName(exchange),
          routingKey = RoutingKey(routingKey),
          body = messageBytes
        )
        .mapError(err => new RuntimeException(s"Failed to publish message to RabbitMQ: ${err.getMessage}", err))

      // 3. 记录日志
      _ <- ZIO.logDebug(s"Published message to exchange=$exchange, routingKey=$routingKey")
    yield ()).mapError(toAppError)

  /** 声明 Exchange */
  override def declareExchange(exchange: String, exchangeType: String = "topic", durable: Boolean = true): AppTask[Unit] =
    (for
      // 解析 exchange 类型
      exType <- ZIO.attempt {
        exchangeType match
          case "direct"  => ExchangeType.Direct
          case "topic"   => ExchangeType.Topic
          case "fanout"  => ExchangeType.Fanout
          case "headers" => ExchangeType.Headers
          case _         => ExchangeType.Topic // 默认
      }

      // 声明 Exchange
      _ <- channel
        .exchangeDeclare(ExchangeName(exchange), exType, durable = durable)
        .mapError(err => new RuntimeException(s"Failed to declare exchange: ${err.getMessage}", err))

//      _ <- ZIO.logInfo(s"Declared exchange: $exchange (type=$exchangeType, durable=$durable)")
    yield ()).mapError(toAppError)

  /** 声明队列 */
  override def declareQueue(
    queue: String,
    durable: Boolean = true,
    exclusive: Boolean = false,
    autoDelete: Boolean = false
  ): AppTask[Unit] =
    (for
      _ <- channel
        .queueDeclare(
          queue = QueueName(queue),
          durable = durable,
          exclusive = exclusive,
          autoDelete = autoDelete
        )
        .mapError(err => new RuntimeException(s"Failed to declare queue: ${err.getMessage}", err))

//      _ <- ZIO.logInfo(s"Declared queue: $queue (durable=$durable, exclusive=$exclusive, autoDelete=$autoDelete)")
    yield ()).mapError(toAppError)

  /** 绑定队列到 Exchange */
  override def bindQueue(queue: String, exchange: String, routingKey: String): AppTask[Unit] =
    (for
      _ <- channel
        .queueBind(
          QueueName(queue),
          ExchangeName(exchange),
          RoutingKey(routingKey)
        )
        .mapError(err => new RuntimeException(s"Failed to bind queue to exchange: ${err.getMessage}", err))

//      _ <- ZIO.logInfo(s"Bound queue=$queue to exchange=$exchange with routingKey=$routingKey")
    yield ()).mapError(toAppError)

  /** 关闭连接 */
  override def close: AppTask[Unit] =
    ZIO.logInfo("Closing RabbitMQ connection...")

  override def getChannel: AppTask[Channel] = ZIO.attempt(channel).mapError(toAppError)

object RabbitMQServiceLive:

  /** 创建 RabbitMQService ZLayer
    *
    * 自动管理连接和通道的生命周期
    */
  val live: ZLayer[AppConfig, Throwable, RabbitMQService] =
    ZLayer.scoped {
      for
        // 1. 获取配置
        config <- ZIO.service[AppConfig]
        rabbitConfig = config.rabbitmq

        factory <- ZIO.attempt {
          val cf = new ConnectionFactory()
          cf.setHost(rabbitConfig.host)
          cf.setPort(rabbitConfig.port)
          cf.setUsername(rabbitConfig.username)
          cf.setPassword(rabbitConfig.password)
          cf.setVirtualHost("/")  // 明确设置为 "/"
          cf
        }
        
        // 3. 建立连接（使用 Scoped 自动管理资源）
        connection <- Amqp.connect(factory)

        // 4. 创建通道
        channel <- Amqp.createChannel(connection)

        // 5. 创建服务实例
        service = new RabbitMQServiceLive(channel)

        // 6. 初始化：声明默认 Exchange 和队列
        _ <- service.declareExchange(rabbitConfig.exchange)
        _ <- service.declareQueue(s"${rabbitConfig.queue}.email")
        _ <- service.declareQueue(s"${rabbitConfig.queue}.sms")
        _ <- service.declareQueue(s"${rabbitConfig.queue}.webhook")

        // 7. 绑定队列到 Exchange（使用路由键）
        _ <- service.bindQueue(s"${rabbitConfig.queue}.email", rabbitConfig.exchange, "notification.email.*")
        _ <- service.bindQueue(s"${rabbitConfig.queue}.sms", rabbitConfig.exchange, "notification.sms.*")
        _ <- service.bindQueue(s"${rabbitConfig.queue}.webhook", rabbitConfig.exchange, "notification.webhook.*")

//        _ <- ZIO.logInfo(s"RabbitMQ service initialized: ${rabbitConfig.host}:${rabbitConfig.port}")
      yield service
    }
