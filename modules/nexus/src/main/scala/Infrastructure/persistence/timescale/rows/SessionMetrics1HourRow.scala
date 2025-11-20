package app.mosia.nexus
package infrastructure.persistence.timescale.rows

import java.time.Instant
import java.util.UUID

case class SessionMetrics1HourRow(
  bucket: Instant,
  sessionId: UUID,
  simulationId: UUID,
  avgFps: Double,
  p50Fps: Double,
  p99Fps: Double,
  avgGpuUtil: Double
)
