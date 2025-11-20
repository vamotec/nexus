package app.mosia.nexus
package domain.services.infra

import domain.error.AppTask
import domain.model.resource.ResourceAssignment
import domain.model.session.{SessionId, SimSession}
import domain.model.training.{TrainingInstanceAssignment, TrainingJob}

trait ResourceAllocationService:
  def allocateIsaacSimInstance(session: SimSession, clusterId: String): AppTask[ResourceAssignment]

  def allocateTrainingInstance(job: TrainingJob, clusterId: String): AppTask[TrainingInstanceAssignment]

  def releaseResources(session: SimSession): AppTask[Unit]

  def startSimulation(session: SimSession): AppTask[Unit]
