package app.mosia.nexus.infra.messaging.kafka

import app.mosia.nexus.infra.config.KafkaConfig
import zio.{Task, ZIO, ZLayer}
import zio.json.*
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde

final class KafkaProducerServiceLive(producer: Producer) extends KafkaProducerService:
  override def publish[T: JsonEncoder](topic: String, key: String, value: T): Task[Unit] =
    producer
      .produce(
        topic = topic,
        key = key,
        value = value.toJson, // 使用 zio-json 将我们的 case class 序列化为 JSON 字符串
        keySerializer = Serde.string,
        valueSerializer = Serde.string
      )
      .unit // .unit 将结果转换为 ZIO[..., Unit]，因为我们只关心是否发送成功，不关心具体的返回元数据

object KafkaProducerServiceLive:
  // 我们服务的 ZIO Layer 实现
  val live: ZLayer[Producer, Nothing, KafkaProducerService] =
    ZLayer.fromFunction(new KafkaProducerServiceLive(_))

  // 创建并管理底层 ZIO Kafka Producer 的 ZIO Layer
  // 它依赖于从 application.conf 加载的 KafkaConfig
  val producerLayer: ZLayer[KafkaConfig, Throwable, Producer] =
    ZLayer.scoped:
      ZIO
        .service[KafkaConfig]
        .flatMap: config =>
          Producer.make(
            ProducerSettings(config.bootstrapServers.split(",").toList)
              .withProperty("security.protocol", "PLAINTEXT")
              .withClientId(config.clientId)
          )
