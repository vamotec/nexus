package app.mosia.nexus.application.service.simulation

import app.mosia.nexus.domain.model.common.Position3D
import app.mosia.nexus.domain.model.session.{NeuroSession, SessionId, SessionMetrics, SessionStatus, SimulationConfigSnapshot}
import app.mosia.nexus.domain.model.simulation.SimulationId
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.domain.repository.{SessionRepository, SimulationRepository}
import zio.{Task, ZIO}

import java.time.Instant

class SimulationManagementServiceLive(
  simulationRepo: SimulationRepository,
  sessionRepo: SessionRepository
) extends SimulationManagementService:

  override def createSessionFromSimulation(
    simulationId: SimulationId,
    userId: UserId
  ): Task[NeuroSession] =
    for {
      // 1. 加载 Simulation
      simulation <- simulationRepo
        .findById(simulationId)
        .someOrFail(
          new Exception(s"Simulation not found: ${simulationId.value}")
        )

      // 2. 创建配置快照
      snapshot = SimulationConfigSnapshot(
        simulationId = simulation.id,
        simulationName = simulation.name,
        version = simulation.version.toString,
        sceneConfig = simulation.sceneConfig,
        simulationParams = simulation.simulationParams,
        snapshotAt = Instant.now()
      )

      // 3. 创建 Session
      sessionId = SessionId.generate()
      session   = NeuroSession(
        id = sessionId,
        simulationId = simulation.id,
        projectId = simulation.projectId,
        userId = userId,
        configSnapshot = snapshot,
        status = SessionStatus.Pending,
        resourceAssignment = None,
        metrics = None,
        result = None,
        createdAt = Instant.now(),
        startedAt = None,
        completedAt = None
      )

      // 4. 持久化
      _ <- sessionRepo.save(session)

    } yield session

  override def updateSimulationStatistics(sessionId: SessionId): Task[Unit] =
    for {
      // 1. 加载 Session 和结果
      session <- sessionRepo
        .findById(sessionId)
        .someOrFail(
          new Exception(s"Session not found: ${sessionId.value}")
        )

      result <- ZIO
        .fromOption(session.result)
        .orElseFail(
          new Exception("Session has no result yet")
        )

      // 2. 加载 Simulation
      simulation <- simulationRepo
        .findById(session.simulationId)
        .someOrFail(
          new Exception(s"Simulation not found: ${session.simulationId.value}")
        )

      // 3. 更新统计
      updatedSimulation = simulation.updateStatistics(result)

      // 4. 保存
      _ <- simulationRepo.update(updatedSimulation)

    } yield ()
