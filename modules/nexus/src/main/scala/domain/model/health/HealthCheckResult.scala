package app.mosia.nexus
package domain.model.health

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class HealthCheckResult(
  status: HealthStatus,
  checks: Map[String, ComponentHealth],
  timestamp: Long
) derives JsonCodec,
      Schema
