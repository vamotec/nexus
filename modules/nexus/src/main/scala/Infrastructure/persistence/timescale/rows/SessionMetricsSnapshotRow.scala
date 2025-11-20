package app.mosia.nexus
package infrastructure.persistence.timescale.rows

import java.time.Instant
import java.util.UUID
import io.getquill.JsonbValue
import zio.json.ast.Json
/** TimescaleDB: simulation_metrics 表映射 */
case class SessionMetricsSnapshotRow(
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
  tags: Option[JsonbValue[Json]],
  updatedAt: Instant
)
