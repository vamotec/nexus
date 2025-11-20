package app.mosia.nexus
package application.dto.response.metrics

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class PerformanceSummary(
  avgFps: Double,
  maxFps: Double,
  minFps: Double,
  p99Fps: Option[Double],
  totalFrames: Long,
  avgGpuUtilization: Double,
  peakGpuMemoryMb: Long,
  duration: Long,
  health: HealthStatus
) derives Cs.SemiAuto,
      ArgBuilder
