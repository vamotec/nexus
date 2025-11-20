package app.mosia.nexus
package domain.config.kafka

case class KafkaConsumerConfig(
  groupId: String,
  autoOffsetReset: String,
  enableAutoCommit: Boolean,
  maxPollRecords: Int
)
