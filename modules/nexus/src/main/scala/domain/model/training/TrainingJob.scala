package app.mosia.nexus
package domain.model.training

import domain.model.session.SessionId
import domain.model.simulation.SimulationId
import domain.model.user.UserId

import java.time.Instant
import java.util.UUID

/** 训练任务聚合根 */
case class TrainingJob(
  id: TrainingJobId,
  sessionId: SessionId,
  userId: UserId,
  algorithm: Option[RLAlgorithm],
  config: TrainingConfig,
  status: TrainingStatus,
  assignment: TrainingInstanceAssignment,
  progress: TrainingProgress,
  result: Option[TrainingResult],
  createdAt: Instant,
  startedAt: Option[Instant] = None,
  completedAt: Option[Instant] = None
):
  def start(assignment: TrainingInstanceAssignment): TrainingJob =
    copy(
      status = TrainingStatus.Running,
      assignment = assignment,
      startedAt = Some(Instant.now())
    )

  def updateProgress(newProgress: TrainingProgress): TrainingJob =
    copy(progress = newProgress)

  def complete(result: TrainingResult): TrainingJob =
    copy(
      status = TrainingStatus.Completed,
      result = Some(result),
      completedAt = Some(Instant.now()),
    )

  def fail(error: String): TrainingJob =
    copy(
      status = TrainingStatus.Failed,
    )
