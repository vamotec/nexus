package app.mosia.nexus
package domain.config.kafka

final case class KafkaConfig(
  bootstrapServers: List[String],
  producer: KafkaProducerConfig,
  consumer: KafkaConsumerConfig,
  topics: KafkaTopics
)
