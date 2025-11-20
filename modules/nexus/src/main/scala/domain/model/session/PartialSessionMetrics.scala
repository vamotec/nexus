package app.mosia.nexus
package domain.model.session

import domain.model.common.Position3D
import domain.model.simulation.SimulationId

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
)
