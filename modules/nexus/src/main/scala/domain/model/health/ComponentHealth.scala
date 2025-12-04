package app.mosia.nexus
package domain.model.health

import sttp.tapir.Schema
import zio.json.*

case class ComponentHealth(
  status: HealthStatus,
  message: Option[String] = None,
  responseTime: Option[Long] = None
) derives JsonCodec,
      Schema
