package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.project.ProjectId
import domain.model.simulation.{Simulation, SimulationId}

trait SimulationRepository:
  def save(simulation: Simulation): AppTask[Unit]

  def findById(id: SimulationId): AppTask[Option[Simulation]]

  def findByProjectId(projectId: ProjectId): AppTask[List[Simulation]]

  def update(simulation: Simulation): AppTask[Unit]

  def delete(id: SimulationId): AppTask[Unit]
