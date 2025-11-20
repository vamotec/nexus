package app.mosia.nexus
package domain.services.infra

import domain.error.AppTask
import zio.json.*
import zio.*

trait DomainEventPublisher:
  def publish[E: {JsonEncoder, Tag}](event: E): AppTask[Unit]
