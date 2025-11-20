package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.session.{SessionId, SimSession}
import domain.model.simulation.SimulationId
import domain.model.user.UserId

trait SessionRepository:
  def save(session: SimSession): AppTask[Unit]

  def findById(id: SessionId): AppTask[Option[SimSession]]

  def findBySimulationId(simulationId: SimulationId): AppTask[List[SimSession]]

  def findByUserId(userId: UserId, limit: Int): AppTask[List[SimSession]]

  def update(session: SimSession): AppTask[Unit]

  def delete(id: SessionId): AppTask[Unit]
