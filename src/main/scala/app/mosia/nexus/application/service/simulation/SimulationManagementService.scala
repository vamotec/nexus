package app.mosia.nexus.application.service.simulation

import app.mosia.nexus.domain.model.session.{NeuroSession, SessionId}
import app.mosia.nexus.domain.model.simulation.SimulationId
import app.mosia.nexus.domain.model.user.UserId
import zio.Task

/** 仿真管理服务
  */
trait SimulationManagementService:
  /** 从 Simulation 创建新的 Session */
  def createSessionFromSimulation(
    simulationId: SimulationId,
    userId: UserId
  ): Task[NeuroSession]

  /** Session 完成后更新 Simulation 统计 */
  def updateSimulationStatistics(sessionId: SessionId): Task[Unit]
