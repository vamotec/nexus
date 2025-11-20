package app.mosia.nexus
package infrastructure.persistence.timescale.rows

import java.time.Instant
import java.util.UUID

case class SessionMetrics1MinRow(
  bucket: Instant,
  sessionId: UUID,
  simulationId: UUID,
  avgFps: Double,
  maxFps: Double,
  minFps: Double,
  avgGpuUtil: Double,
  maxGpuMemory: Long
)
