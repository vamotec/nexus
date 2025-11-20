package app.mosia.nexus
package domain.services.app

import application.dto.request.simulation.{CreateSimulationRequest, UpdateSimulationRequest}
import domain.error.AppTask
import domain.model.project.ProjectId
import domain.model.simulation.{Simulation, SimulationId}
import domain.model.user.UserId

import java.time.Instant
import zio.json.*
import zio.*
import zio.http.*

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
