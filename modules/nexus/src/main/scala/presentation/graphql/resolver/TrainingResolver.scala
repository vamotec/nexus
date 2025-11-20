package app.mosia.nexus
package presentation.graphql.resolver

import domain.services.infra.JwtContent
import application.dto.response.training.TrainingProgressResponse
import domain.error.*
import domain.model.training.TrainingJobId
import domain.model.user.UserId
import domain.services.app.TrainingService
import presentation.graphql.schema.TrainingSchema.*

import zio.{Duration, ZIO}
import zio.stream.ZStream

/** Training GraphQL Resolver
  *
  * 负责将 GraphQL 查询/变更映射到 TrainingService
  *
  * 职责：
  *   - 查询训练任务（单个/列表）
  *   - 创建、停止训练任务
  *   - 实时训练进度订阅
  */
object TrainingResolver:

  /** 查询解析器 */
  def queries(service: TrainingService, jwtContent: JwtContent) = TrainingQueries(
    // 根据 ID 查询单个训练任务
    trainingJob = jobId =>
      (for
        // 1. 获取训练任务
        jobOpt <- service.getTrainingJob(jobId)

        // 2. 验证任务存在
        job <- ZIO
          .fromOption(jobOpt)
          .orElseFail(NotFound("TrainingJob", s"Training job not found: ${jobId.value}"))
      yield job).mapError(_.toCalibanError),

    // 查询当前用户的训练任务列表
    myTrainingJobs = limit =>
      (for
        payload <- jwtContent.get.someOrFail(InvalidInput("access token", "Invalid payload"))
        userId <- UserId.fromString(payload.userIdStr)

        // 1. 获取用户的训练任务
        jobs <- service.getMyTrainingJobs(userId, limit)
      yield jobs).mapError(_.toCalibanError),

    // 查询训练进度
    trainingProgress = jobId =>
      service
        .getTrainingProgress(jobId)
        .mapError(err =>
          caliban.CalibanError.ExecutionError(
            s"Failed to get training progress: ${err.getMessage}"
          )
        )
  )

  /** 变更解析器 */
  def mutations(service: TrainingService, jwtContent: JwtContent) = TrainingMutations(
    // 创建训练任务
    createTrainingJob = request =>
      (for
        payload <- jwtContent.get.someOrFail(InvalidInput("access token", "Invalid payload"))
        userId <- UserId.fromString(payload.userIdStr)

        // 1. 调用 service 创建训练任务
        response <- service.createTrainingJob(userId, request)
      yield response).mapError(_.toCalibanError),

    // 停止训练任务
    stopTrainingJob = jobId =>
      service
        .stopTrainingJob(jobId)
        .mapError(err =>
          caliban.CalibanError.ExecutionError(
            s"Failed to stop training job: ${err.getMessage}"
          )
        )
  )

  /** 订阅解析器 */
  def subscriptions(service: TrainingService) = TrainingSubscriptions(
    // 训练进度流订阅
    trainingProgressStream = args =>
      val interval = Duration.fromMillis(args.intervalMs.getOrElse(2000).toLong)

      // 定期轮询训练进度（实际应该从 Kafka/Redis 读取实时事件）
      ZStream
        .repeatZIOWithSchedule(
          service
            .getTrainingProgress(args.jobId)
            .catchAll { error =>
              // 如果任务不存在或失败，返回一个空进度
              ZIO.succeed(
                TrainingProgressResponse(
                  currentEpoch = 0,
                  totalEpochs = 0,
                  percentage = 0.0,
                  currentReward = Some(0.0),
                  averageReward = Some(0.0),
                  metrics = Map.empty
                )
              )
            },
          zio.Schedule.spaced(interval)
        )
  )
