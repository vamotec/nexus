package app.mosia.nexus
package domain.config.kafka

case class KafkaTopics(
  domainEvents: String,
  sessionEvents: String,
  trainingEvents: String,
  metrics: String
)
