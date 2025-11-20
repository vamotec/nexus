package app.mosia.nexus
package application.dto.response.metrics

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class PerformanceMetrics(
  currentFps: Double,
  frameCount: Long,
  simulationTime: Double,
  wallTime: Double,
  timeRatio: Double // simulationTime / wallTime
) derives Cs.SemiAuto,
      ArgBuilder
