package app.mosia.nexus.application.service.simulation

import app.mosia.nexus.application.dto.request.simulation.{CreateSimulationRequest, UpdateSimulationRequest}
import app.mosia.nexus.application.dto.response.simulation.SimulationResponse
import app.mosia.nexus.domain.model.project.ProjectId
import app.mosia.nexus.domain.model.simulation.{Simulation, SimulationId}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.error.AppTask
import zio.{UIO, ZIO}

import java.time.Instant

trait SimulationService:
  // 仿真配置管理
  def createSimulation(
    projectId: ProjectId,
    request: CreateSimulationRequest,
    createdBy: UserId
  ): AppTask[Simulation]

  def getSimulation(simulationId: SimulationId): AppTask[Option[Simulation]]

  def getSimulationPaged(
    userId: UserId,
    projectId: ProjectId,
    page: Int,
    pageSize: Int,
    sort: String,
    search: Option[String]
  ): UIO[(List[Simulation], Int)]

  def updateSimulation(
    simulationId: SimulationId,
    request: UpdateSimulationRequest
  ): AppTask[Simulation]

  def deleteSimulation(simulationId: SimulationId): AppTask[Unit]

  def getSimulationCounts(projectIds: Seq[ProjectId], userId: UserId): UIO[Map[ProjectId, Int]]

  def getLastRunTimes(projectIds: Seq[ProjectId], userId: UserId): UIO[Map[ProjectId, Instant]]
