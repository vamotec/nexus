package app.mosia.nexus
package domain.model.health

import sttp.tapir.Schema
import zio.json.*

case class HealthCheckResult(
  status: HealthStatus,
  checks: Map[String, ComponentHealth],
  timestamp: Long
) derives JsonCodec,
      Schema
