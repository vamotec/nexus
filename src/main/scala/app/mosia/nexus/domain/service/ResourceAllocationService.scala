package app.mosia.nexus.domain.service

import app.mosia.nexus.domain.model.resource.ResourceAssignment
import zio.Task
import app.mosia.nexus.domain.model.session.{NeuroSession, SessionId}
import app.mosia.nexus.domain.model.training.{TrainingJob, TrainingNodeAssignment}

trait ResourceAllocationService:
  def allocateIsaacSimInstance(session: NeuroSession): Task[ResourceAssignment]

  def allocateTrainingNode(job: TrainingJob): Task[TrainingNodeAssignment]

  def releaseResources(sessionId: SessionId): Task[Unit]
