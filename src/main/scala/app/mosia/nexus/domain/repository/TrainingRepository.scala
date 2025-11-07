package app.mosia.nexus.domain.repository

import app.mosia.nexus.domain.model.session.SessionId
import app.mosia.nexus.domain.model.training.{TrainingJob, TrainingJobId}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.error.AppTask

trait TrainingRepository:
  def save(job: TrainingJob): AppTask[Unit]

  def update(job: TrainingJob): AppTask[Unit]

  def findById(id: TrainingJobId): AppTask[Option[TrainingJob]]

  def findBySessionId(sessionId: SessionId): AppTask[List[TrainingJob]]

  def findByUserId(userId: UserId, limit: Int): AppTask[List[TrainingJob]]
