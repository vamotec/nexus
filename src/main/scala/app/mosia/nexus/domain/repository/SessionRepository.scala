package app.mosia.nexus.domain.repository

import zio.Task
import app.mosia.nexus.domain.model.session.{NeuroSession, SessionId}
import app.mosia.nexus.domain.model.simulation.SimulationId
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.error.AppTask

trait SessionRepository:
  def save(session: NeuroSession): AppTask[Unit]

  def findById(id: SessionId): AppTask[Option[NeuroSession]]

  def findBySimulationId(simulationId: SimulationId): AppTask[List[NeuroSession]]

  def findByUserId(userId: UserId, limit: Int): AppTask[List[NeuroSession]]

  def update(session: NeuroSession): AppTask[Unit]

  def delete(id: SessionId): AppTask[Unit]
