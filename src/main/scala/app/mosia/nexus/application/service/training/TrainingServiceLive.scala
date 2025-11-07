package app.mosia.nexus.application.service.training

import app.mosia.nexus.application.dto.request.training.CreateTrainingRequest
import zio.{Task, ZIO, ZLayer}
import app.mosia.nexus.application.dto.response.training.{TrainingJobResponse, TrainingProgressResponse}
import app.mosia.nexus.domain.event.TrainingEvent.TrainingJobCreated
import app.mosia.nexus.domain.model.session.SessionId
import app.mosia.nexus.domain.model.training.{RLAlgorithm, TrainingJob, TrainingJobId, TrainingProgress, TrainingStatus}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.domain.repository.{SessionRepository, TrainingRepository}
import app.mosia.nexus.infra.grpc.neuro.NeuroGrpcClient
import app.mosia.nexus.infra.messaging.event.DomainEventPublisher

import java.time.Instant
import java.util.UUID

final class TrainingServiceLive(
  trainingRepository: TrainingRepository,
  sessionRepository: SessionRepository,
  neuroClient: NeuroGrpcClient,
  eventPublisher: DomainEventPublisher
) extends TrainingService:
  /** 创建训练任务 */
  def createTrainingJob(
    userId: UserId,
    request: CreateTrainingRequest
  ): ZIO[Any, Throwable | String, TrainingJobResponse] = ???

  /** 获取训练进度 */
  def getTrainingProgress(jobId: TrainingJobId): Task[TrainingProgressResponse] = ???

  /** 停止训练 */
  def stopTrainingJob(jobId: TrainingJobId): Task[TrainingJobResponse] = ???

object TrainingServiceLive:
  val live =
    ZLayer:
      for
        repo <- ZIO.service[TrainingRepository]
        neuro <- ZIO.service[SessionRepository]
        allocator <- ZIO.service[NeuroGrpcClient]
        publisher <- ZIO.service[DomainEventPublisher]
      yield TrainingServiceLive(repo, neuro, allocator, publisher)
