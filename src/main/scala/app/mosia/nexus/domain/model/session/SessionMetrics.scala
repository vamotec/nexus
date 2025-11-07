package app.mosia.nexus.domain.model.session

import app.mosia.nexus.domain.model.common.{Position3D, ValueObject}
import app.mosia.nexus.domain.model.simulation.SimulationId
import app.mosia.nexus.infra.persistence.timescale.entity.SessionMetricsEntity

import java.time.Instant

/** 会话指标 (实时更新) */
case class SessionMetrics(
  sessionId: SessionId, // 必须知道属于哪个会话
  simulationId: SimulationId, // 必须知道属于哪个仿真
  currentFps: Double,
  frameCount: Long,
  simulationTime: Double,
  wallTime: Double,
  robotPosition: Position3D,
  gpuUtilization: Double,
  gpuMemoryMB: Long,
  lastUpdatedAt: Instant,
  tags: Map[String, String] = Map.empty // 保留扩展性
) {
  def timeRatio: Double =
    if (wallTime > 0) simulationTime / wallTime else 0.0

  // 转换为 Entity（用于持久化）
  def toEntity: SessionMetricsEntity =
    SessionMetricsEntity(
      time = lastUpdatedAt,
      simulationId = simulationId.value,
      sessionId = Some(sessionId.value),
      fps = Some(currentFps),
      frameCount = Some(frameCount),
      simulationTime = Some(simulationTime),
      wallTime = Some(wallTime),
      robotPositionX = Some(robotPosition.x),
      robotPositionY = Some(robotPosition.y),
      robotPositionZ = Some(robotPosition.z),
      gpuUtilization = Some(gpuUtilization),
      gpuMemoryMb = Some(gpuMemoryMB),
      tags = if (tags.isEmpty) None else Some(tags)
    )
}

object SessionMetrics {
  // 从 Entity 重建（用于查询）
  def fromEntity(entity: SessionMetricsEntity): Either[String, SessionMetrics] =
    for {
      sessionId <- entity.sessionId.toRight("sessionId is required")
      fps <- entity.fps.toRight("fps is required")
      frameCount <- entity.frameCount.toRight("frameCount is required")
      simTime <- entity.simulationTime.toRight("simulationTime is required")
      wallTime <- entity.wallTime.toRight("wallTime is required")
      posX <- entity.robotPositionX.toRight("robotPositionX is required")
      posY <- entity.robotPositionY.toRight("robotPositionY is required")
      posZ <- entity.robotPositionZ.toRight("robotPositionZ is required")
      gpuUtil <- entity.gpuUtilization.toRight("gpuUtilization is required")
      gpuMem <- entity.gpuMemoryMb.toRight("gpuMemoryMb is required")
    } yield SessionMetrics(
      sessionId = SessionId(sessionId),
      simulationId = SimulationId(entity.simulationId),
      currentFps = fps,
      frameCount = frameCount,
      simulationTime = simTime,
      wallTime = wallTime,
      robotPosition = Position3D(posX, posY, posZ),
      gpuUtilization = gpuUtil,
      gpuMemoryMB = gpuMem,
      lastUpdatedAt = entity.time,
      tags = entity.tags.getOrElse(Map.empty)
    )
}
