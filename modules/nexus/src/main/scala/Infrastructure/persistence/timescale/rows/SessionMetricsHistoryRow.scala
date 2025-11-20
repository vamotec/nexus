package app.mosia.nexus
package infrastructure.persistence.timescale.rows

import java.time.Instant
import java.util.UUID
import io.getquill.JsonbValue
import zio.json.ast.Json

case class SessionMetricsHistoryRow(
  time: Instant,
  sessionId: UUID,
  simulationId: UUID,
  currentFps: Double,
  frameCount: Long,
  simulationTime: Double,
  wallTime: Double,
  robotPositionX: Double,
  robotPositionY: Double,
  robotPositionZ: Double,
  gpuUtilization: Double,
  gpuMemoryMb: Double,
  tags: Option[JsonbValue[Json]]
)
