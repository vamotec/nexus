package app.mosia.nexus.infra.messaging.kafka

import zio.*
import zio.json.*

trait KafkaProducerService:
  def publish[T: JsonEncoder](topic: String, key: String, value: T): Task[Unit]
