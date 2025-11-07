package app.mosia.nexus.domain.model.training

import app.mosia.nexus.domain.model.common.Learning.TrainingConfig

import java.time.Instant
import app.mosia.nexus.domain.model.session.SessionId
import app.mosia.nexus.domain.model.user.UserId

/** 训练任务聚合根 */
case class TrainingJob(
  id: TrainingJobId,
  sessionId: SessionId,
  userId: UserId,
  algorithm: RLAlgorithm,
  config: TrainingConfig,
  status: TrainingStatus,
  assignment: Option[TrainingNodeAssignment],
  progress: TrainingProgress,
  result: Option[TrainingResult],
  createdAt: Instant,
  updatedAt: Instant,
  startedAt: Option[Instant] = None,
  completedAt: Option[Instant] = None
):
  def start(assignment: TrainingNodeAssignment): TrainingJob =
    copy(
      status = TrainingStatus.Running,
      assignment = Some(assignment),
      startedAt = Some(Instant.now()),
      updatedAt = Instant.now(),
    )

  def updateProgress(newProgress: TrainingProgress): TrainingJob =
    copy(progress = newProgress, updatedAt = Instant.now())

  def complete(result: TrainingResult): TrainingJob =
    copy(
      status = TrainingStatus.Completed,
      result = Some(result),
      completedAt = Some(Instant.now()),
      updatedAt = Instant.now(),
    )

  def fail(error: String): TrainingJob =
    copy(
      status = TrainingStatus.Failed,
      updatedAt = Instant.now(),
    )
