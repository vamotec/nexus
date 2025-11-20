package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.session.SessionId
import domain.model.training.{TrainingJob, TrainingJobId}
import domain.model.user.UserId

trait TrainingRepository:
  def save(job: TrainingJob): AppTask[Unit]

  def update(job: TrainingJob): AppTask[Unit]

  def findById(id: TrainingJobId): AppTask[Option[TrainingJob]]

  def findBySessionId(sessionId: SessionId): AppTask[List[TrainingJob]]

  def findByUserId(userId: UserId, limit: Int): AppTask[List[TrainingJob]]
