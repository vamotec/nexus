package app.mosia.nexus
package domain.services.infra

import domain.error.AppTask

import zio.json.*
import zio.*

trait KafkaProducerService:
  def publish[T: JsonEncoder](topic: String, key: String, value: T): AppTask[Unit]
