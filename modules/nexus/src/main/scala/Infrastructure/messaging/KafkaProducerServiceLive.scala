package app.mosia.nexus
package infrastructure.messaging

import domain.services.infra.KafkaProducerService
import domain.config.AppConfig
import domain.config.kafka.KafkaConfig
import domain.error.*

import zio.*
import zio.json.*
import zio.http.*
import zio.json.ast.Json
import zio.kafka.producer.*
import zio.kafka.serde.Serde

final class KafkaProducerServiceLive(producer: Producer) extends KafkaProducerService:
  override def publish[T: JsonEncoder](topic: String, key: String, value: T): AppTask[Unit] =
    producer
      .produce(
        topic = topic,
        key = key,
        value = value.toJson, // 使用 zio-json 将我们的 case class 序列化为 JSON 字符串
        keySerializer = Serde.string,
        valueSerializer = Serde.string
      )
      .unit // .unit 将结果转换为 ZIO[..., Unit]，因为我们只关心是否发送成功，不关心具体的返回元数据
      .mapError(toAppError)

object KafkaProducerServiceLive:
  // 我们服务的 ZIO Layer 实现
  val live: ZLayer[Producer, Nothing, KafkaProducerService] =
    ZLayer.fromFunction(new KafkaProducerServiceLive(_))

  // 创建并管理底层 ZIO Kafka Producer 的 ZIO Layer
  // 它依赖于从 application.conf 加载的 KafkaConfig
  val producerLayer: ZLayer[AppConfig, Throwable, Producer] =
    ZLayer.scoped:
      ZIO
        .service[AppConfig]
        .flatMap: config =>
          Producer.make(
            ProducerSettings(config.kafka.bootstrapServers)
              .withProperty("security.protocol", "PLAINTEXT")
              .withClientId(config.kafka.producer.clientId)
              .withProperty("acks", config.kafka.producer.acks)
              .withProperty("retries", config.kafka.producer.retries.toString)
              .withProperty("batch.size", config.kafka.producer.batchSize.toString)
              .withProperty("linger.ms", config.kafka.producer.lingerMs.toString)
              .withProperty("compression.type", config.kafka.producer.compressionType)
          )
