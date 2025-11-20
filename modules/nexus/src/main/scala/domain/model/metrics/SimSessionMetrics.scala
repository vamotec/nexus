package app.mosia.nexus
package domain.model.metrics

import domain.model.common.Position3D
import domain.model.session.SessionId
import domain.model.simulation.SimulationId

import java.time.Instant

import zio.json.ast.Json

/** 会话指标 (实时更新) */
case class SimSessionMetrics(
  sessionId: SessionId, // 必须知道属于哪个会话
  simulationId: SimulationId, // 必须知道属于哪个仿真
  currentFps: Double,
  frameCount: Long,
  simulationTime: Double,
  wallTime: Double,
  robotPosition: Position3D,
  gpuUtilization: Double,
  gpuMemoryMB: Double,
  updatedAt: Instant,
  tags: Option[Json]
):
  def timeRatio: Double =
    if (wallTime > 0) simulationTime / wallTime else 0.0
