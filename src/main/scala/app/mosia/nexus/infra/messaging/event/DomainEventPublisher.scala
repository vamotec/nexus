package app.mosia.nexus.infra.messaging.event

import zio.json.JsonEncoder
import zio.{Tag, Task}

trait DomainEventPublisher:
  def publish[T: {JsonEncoder, Tag}](event: T): Task[Unit]
