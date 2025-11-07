package app.mosia.nexus.infra.dto

import app.mosia.nexus.domain.model.common.RenderQuality
import app.mosia.nexus.domain.model.project.ProjectId
import app.mosia.nexus.domain.model.resource.{ControlEndpoint, IsaacSimInstanceId, ResourceAssignment, StreamEndpoint}
import app.mosia.nexus.domain.model.scene.{EnvironmentType, RobotType, SceneConfig}
import app.mosia.nexus.domain.model.session.{NeuroSession, SessionId, SessionMetrics, SessionStatus}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.persistence.postgres.entity.SessionEntity

import java.time.Instant

object RepositoryMapper:
  /** Domain → Entity */
//  def toSessionEntity(session: NeuroSession): SessionEntity =
//    SessionEntity(
//      id = session.id.value,
//      userId = session.userId.value,
//      projectId = session.projectId.value,
//      sceneName = session.configSnapshot.sceneConfig.name,
//      robotType = session.configSnapshot.sceneConfig.robotType match {
//        case RobotType.FrankaPanda => "franka_panda"
//        case RobotType.UR5 => "ur5"
//        case RobotType.Kuka => "kuka"
//        case RobotType.Custom(name) => name
//      },
//      environment = session.configSnapshot.sceneConfig.environment.environmentType match {
//        case EnvironmentType.Warehouse => "warehouse"
//        case EnvironmentType.Factory => "factory"
//        case EnvironmentType.Laboratory => "laboratory"
//        case EnvironmentType.Outdoor => "outdoor"
//      },
//      status = session.status match {
//        case SessionStatus.Pending => "pending"
//        case SessionStatus.Initializing => "initializing"
//        case SessionStatus.Running => "running"
//        case SessionStatus.Paused => "paused"
//        case SessionStatus.Stopped => "stopped"
//        case SessionStatus.Failed => "failed"
//      },
//      isaacSimInstanceId = session.resourceAssignment.map(_.isaacSimInstance.value),
//      nucleusPath = session.resourceAssignment.map(_.nucleusPath),
//      streamHost = session.resourceAssignment.map(_.streamEndpoint.host),
//      streamPort = session.resourceAssignment.map(_.streamEndpoint.port),
//      controlWsUrl = session.resourceAssignment.map(_.controlEndpoint.wsUrl),
//      fps = session.metrics.currentFps,
//      frameCount = session.metrics.frameCount,
//      gpuUtilization = session.metrics.gpuUtilization,
//      gpuMemoryMB = session.metrics.gpuMemoryMB,
//      createdAt = session.createdAt.toEpochMilli,
//      startedAt = session.startedAt.map(_.toEpochMilli),
//      completedAt = session.completedAt.map(_.toEpochMilli)
//    )

  /** Entity → Domain */
//  def fromSessionEntity(entity: SessionEntity, scene: SceneConfig): Session =
//    Session(
//      id = SessionId(entity.id),
//      simulationId = ,
//      userId = UserId(entity.userId),
//      projectId = ProjectId(entity.projectId),
//      configSnapshot = {
//
//      },
//      status = SessionStatus.valueOf(entity.status),
//      metrics = SessionMetrics(
//        fps = entity.fps,
//        frameCount = entity.frameCount,
//        simulationTime = 0.0,
//        realTime = 0.0,
//        gpuUtilization = entity.gpuUtilization,
//        gpuMemoryMB = entity.gpuMemoryMB
//      ),
//      createdAt = Instant.ofEpochMilli(entity.createdAt),
//      startedAt = entity.startedAt.map(Instant.ofEpochMilli),
//      completedAt = entity.completedAt.map(Instant.ofEpochMilli)
//    )
  val const = 0
