package app.mosia.nexus.infra.persistence.timescale.entity

import java.time.Instant
import java.util.UUID

/** TimescaleDB: simulation_metrics 表映射 */
case class SessionMetricsEntity(
  time: Instant,
  simulationId: UUID,
  sessionId: Option[UUID],
  fps: Option[Double],
  frameCount: Option[Long],
  simulationTime: Option[Double],
  wallTime: Option[Double],
  robotPositionX: Option[Double],
  robotPositionY: Option[Double],
  robotPositionZ: Option[Double],
  gpuUtilization: Option[Double],
  gpuMemoryMb: Option[Long],
  tags: Option[Map[String, String]] = None
)
