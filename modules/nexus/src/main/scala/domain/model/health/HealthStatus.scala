package app.mosia.nexus
package domain.model.health

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

enum HealthStatus derives JsonCodec, Schema:
  case Healthy, Degraded, Unhealthy
