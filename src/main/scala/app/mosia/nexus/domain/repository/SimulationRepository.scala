package app.mosia.nexus.domain.repository

import app.mosia.nexus.domain.model.project.ProjectId
import app.mosia.nexus.domain.model.simulation.{Simulation, SimulationId}
import app.mosia.nexus.infra.error.AppTask
import zio.Task

trait SimulationRepository:
  def save(simulation: Simulation): AppTask[Unit]

  def findById(id: SimulationId): AppTask[Option[Simulation]]

  def findByProjectId(projectId: ProjectId): AppTask[List[Simulation]]

  def update(simulation: Simulation): AppTask[Unit]

  def delete(id: SimulationId): AppTask[Unit]
