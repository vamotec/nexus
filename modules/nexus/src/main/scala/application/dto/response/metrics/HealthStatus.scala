package app.mosia.nexus
package application.dto.response.metrics

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

enum HealthStatus derives Cs.SemiAuto, ArgBuilder:
  case Excellent // FPS > 55
  case Good // FPS > 45
  case Fair // FPS > 30
  case Poor // FPS <= 30
