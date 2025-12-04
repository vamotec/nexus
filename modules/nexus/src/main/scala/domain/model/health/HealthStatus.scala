package app.mosia.nexus
package domain.model.health

import sttp.tapir.Schema
import zio.json.*


enum HealthStatus derives JsonCodec, Schema:
  case Healthy, Degraded, Unhealthy
