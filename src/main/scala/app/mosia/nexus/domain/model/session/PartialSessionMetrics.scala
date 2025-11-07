package app.mosia.nexus.domain.model.session

import app.mosia.nexus.domain.model.common.Position3D
import app.mosia.nexus.domain.model.simulation.SimulationId
import app.mosia.nexus.infra.persistence.timescale.entity.SessionMetricsEntity

import java.time.Instant

// 部分指标快照（用于渐进式更新）
case class PartialSessionMetrics(
  sessionId: SessionId,
  simulationId: SimulationId,
  currentFps: Option[Double] = None,
  frameCount: Option[Long] = None,
  simulationTime: Option[Double] = None,
  wallTime: Option[Double] = None,
  robotPosition: Option[Position3D] = None,
  gpuUtilization: Option[Double] = None,
  gpuMemoryMB: Option[Long] = None,
  timestamp: Instant = Instant.now(),
  tags: Map[String, String] = Map.empty
) {
  def toEntity: SessionMetricsEntity =
    SessionMetricsEntity(
      time = timestamp,
      simulationId = simulationId.value,
      sessionId = Some(sessionId.value),
      fps = currentFps,
      frameCount = frameCount,
      simulationTime = simulationTime,
      wallTime = wallTime,
      robotPositionX = robotPosition.map(_.x),
      robotPositionY = robotPosition.map(_.y),
      robotPositionZ = robotPosition.map(_.z),
      gpuUtilization = gpuUtilization,
      gpuMemoryMb = gpuMemoryMB,
      tags = if (tags.isEmpty) None else Some(tags)
    )
}
