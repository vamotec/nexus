package app.mosia.nexus
package domain.config.kafka

case class KafkaProducerConfig(
  clientId: String,
  acks: String,
  retries: Int,
  batchSize: Int,
  lingerMs: Int,
  compressionType: String
)
