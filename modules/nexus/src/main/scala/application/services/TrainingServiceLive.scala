package app.mosia.nexus
package application.services

import application.dto.request.training.CreateTrainingRequest
import application.dto.response.training.{TrainingJobResponse, TrainingProgressResponse, TrainingResultResponse}
import application.util.toZIOOrFail
import domain.error.*
import domain.event.TrainingEvent.TrainingJobCreated
import domain.model.session.SessionId
import domain.model.training.*
import domain.model.user.UserId
import domain.repository.{SessionRepository, TrainingRepository}
import domain.services.app.TrainingService
import domain.services.infra.{DomainEventPublisher, ResourceAllocationService}

import java.time.Instant
import io.grpc.Status

import zio.json.*
import zio.*

final class TrainingServiceLive(
  trainingRepository: TrainingRepository,
  sessionRepository: SessionRepository,
  resourceAllocation: ResourceAllocationService,
  eventPublisher: DomainEventPublisher
) extends TrainingService:

  /** 创建训练任务 */
  override def createTrainingJob(
    userId: UserId,
    request: CreateTrainingRequest
  ): AppTask[TrainingJobResponse] =
    for
      sessionId <- SessionId.fromString(request.sessionId)
      algorithm = RLAlgorithm.fromString(request.algorithm)

      // 2. 验证 Session 存在（确保 findById 返回 ZIO）
      sessionOpt <- sessionRepository.findById(sessionId)
      session <- sessionOpt.toZIOOrFail(NotFound("Session", request.sessionId))

      // 3. 验证 Session 所有者
      _ <- ZIO
        .fail(PermissionDenied("query", "Session"))
        .when(session.userId != userId)

      // 4. 验证 Session 模式支持训练资源 (Training 或 Hybrid)
      _ <- ZIO
        .fail(GrpcServiceError("neuro", "mode validate", Status.ABORTED))
        .unless(session.mode.needsTrainingResources)

      // 5. 创建 TrainingJob 领域模型
      now   = Instant.now()
      jobId = TrainingJobId.generate()

      trainingConfig = TrainingConfig(
        episodes = request.epochs,
        batchSize = request.batchSize,
        learningRate = request.learningRate
      )

      trainingJob = TrainingJob(
        id = jobId,
        sessionId = sessionId,
        userId = userId,
        algorithm = None,
        config = trainingConfig,
        status = TrainingStatus.Queued,
        assignment = TrainingInstanceAssignment(
          instanceId = None, gpuIds = List.empty, assignedAt = now
        ),
        progress = TrainingProgress(
          currentEpoch = 0,
          totalEpochs = request.epochs,
          currentReward = Some(0.0),
          averageReward = Some(0.0),
          loss = Some(0.0),
          episodeLength = 0,
          metrics = Map.empty
        ),
        result = None,
        createdAt = now,
        startedAt = None,
        completedAt = None
      )

      // 6. 使用 ResourceAllocationService 分配训练实例
      assignment <- resourceAllocation.allocateTrainingInstance(trainingJob, session.clusterId)

      // 7. 更新 TrainingJob 的资源分配信息
      updatedJob = trainingJob.copy(
        assignment = assignment,
        status = TrainingStatus.Running,
        startedAt = Some(Instant.now())
      )

      // 8. 保存到数据库
      _ <- trainingRepository.save(updatedJob)

      // 9. 发布领域事件
      _ <- eventPublisher
        .publish(TrainingJobCreated(jobId, sessionId, now))
        .catchAll(err => ZIO.logWarning(s"Failed to publish TrainingJobCreated event: $err"))

      // 10. 构建响应
      response = toTrainingJobResponse(updatedJob)
    yield response

  /** 获取单个训练任务 */
  override def getTrainingJob(jobId: TrainingJobId): AppTask[Option[TrainingJobResponse]] =
    for
      // 1. 查询训练任务
      jobOpt <- trainingRepository.findById(jobId)

      // 2. 转换为响应
      responseOpt = jobOpt.map(toTrainingJobResponse)
    yield responseOpt

  /** 获取用户的训练任务列表 */
  override def getMyTrainingJobs(userId: UserId, limit: Int): AppTask[List[TrainingJobResponse]] =
    for
      // 1. 查询用户的训练任务
      jobs <- trainingRepository.findByUserId(userId, limit)

      // 2. 转换为响应列表
      responses = jobs.map(toTrainingJobResponse)
    yield responses

  /** 获取训练进度 */
  override def getTrainingProgress(jobId: TrainingJobId): AppTask[TrainingProgressResponse] =
    for
      // 1. 查询训练任务
      jobOpt <- trainingRepository.findById(jobId)
      job <- ZIO
        .fromOption(jobOpt)
        .orElseFail(NotFound("training job", jobId.value.toString))

      // 2. 转换为响应
      response = TrainingProgressResponse(
        currentEpoch = job.progress.currentEpoch,
        totalEpochs = job.progress.totalEpochs,
        percentage = job.progress.percentage,
        currentReward = job.progress.currentReward,
        averageReward = job.progress.averageReward,
        metrics = job.progress.metrics
      )
    yield response

  /** 停止训练 */
  override def stopTrainingJob(jobId: TrainingJobId): AppTask[TrainingJobResponse] =
    for
      // 1. 查询训练任务
      jobOpt <- trainingRepository.findById(jobId)
      job <- ZIO
        .fromOption(jobOpt)
        .orElseFail(NotFound("training job", jobId.value.toString))

      sessionOpt <- sessionRepository.findById(job.sessionId)
      session <- sessionOpt.toZIOOrFail(NotFound("Session", job.sessionId.value.toString))
      // 2. 验证状态（只有 Running/Queued 可以停止）
      _ <- ZIO
        .fail(BusinessRuleViolation("Only stop job in status at Running/Queued", job.status.toString))
        .when(job.status != TrainingStatus.Running && job.status != TrainingStatus.Queued)

      // 3. 使用 ResourceAllocationService 释放训练资源
      _ <- resourceAllocation.releaseResources(session)

      // 4. 更新状态为 Cancelled
      updatedJob = job.copy(
        status = TrainingStatus.Cancelled,
        completedAt = Some(Instant.now())
      )

      // 5. 保存到数据库
      _ <- trainingRepository.update(updatedJob)

      // 6. 构建响应
      response = toTrainingJobResponse(updatedJob)
    yield response

  // ============================================================================
  // 辅助方法
  // ============================================================================

  private def toTrainingJobResponse(job: TrainingJob): TrainingJobResponse =
    TrainingJobResponse(
      id = job.id.value.toString,
      sessionId = job.sessionId.value.toString,
      algorithm = job.algorithm.toString,
      status = job.status.toString,
      progress = Some(
        TrainingProgressResponse(
          currentEpoch = job.progress.currentEpoch,
          totalEpochs = job.progress.totalEpochs,
          percentage = job.progress.percentage,
          currentReward = job.progress.currentReward,
          averageReward = job.progress.averageReward,
          metrics = job.progress.metrics
        )
      ),
      result = job.result.map(r =>
        TrainingResultResponse(
          modelPath = r.modelPath,
          finalReward = r.finalReward,
          successRate = 0.0, // TODO: 从 metrics 计算
          trainingTimeSeconds = job.completedAt
            .zip(job.startedAt)
            .map((completed, started) => (completed.toEpochMilli - started.toEpochMilli) / 1000)
            .getOrElse(0L)
        )
      ),
      createdAt = job.createdAt.toEpochMilli,
      startedAt = job.startedAt.map(_.toEpochMilli),
      completedAt = job.completedAt.map(_.toEpochMilli)
    )

object TrainingServiceLive:
  val live =
    ZLayer:
      for
        trainingRepo <- ZIO.service[TrainingRepository]
        sessionRepo <- ZIO.service[SessionRepository]
        resourceAllocation <- ZIO.service[ResourceAllocationService]
        publisher <- ZIO.service[DomainEventPublisher]
      yield TrainingServiceLive(trainingRepo, sessionRepo, resourceAllocation, publisher)
