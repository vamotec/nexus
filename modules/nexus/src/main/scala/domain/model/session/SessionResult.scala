package app.mosia.nexus
package domain.model.session

import domain.error.*
import domain.model.training.TrainingResult

import java.time.Instant
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 会话结果 (完成后写入，不可变) */
case class SessionResult(
  sessionId: SessionId,

  // 基本结果
  success: Boolean,
  completionTime: Double, // 秒

  // 任务指标
  goalReached: Boolean,
  pathLength: Double, // 米
  collisions: Int,
  energyConsumption: Double, // 焦耳

  // 扩展指标(存储为JSON字符串)
  customMetrics: String,

  // 数据存储
  trajectoryUrl: Option[String],
  videoUrl: Option[String],

  // 训练结果 (RL/学习场景)
  trainingResult: Option[TrainingResult],
  createdAt: Instant
) derives JsonCodec

object SessionResult:
  def failed(sessionId: SessionId, reason: String): SessionResult = SessionResult(
    sessionId = sessionId,
    success = false,
    completionTime = 0.0,
    goalReached = false,
    pathLength = 1.0,
    collisions = 1,
    energyConsumption = 1.0,
    customMetrics = "",
    trajectoryUrl = None,
    videoUrl = None,
    trainingResult = None,
    createdAt = Instant.now()
  )

  def validated(
    sessionId: SessionId,
    success: Boolean,
    completionTime: Double,
    goalReached: Boolean,
    pathLength: Double,
    collisions: Int,
    energyConsumption: Double,
    customMetrics: String,
    trajectoryUrl: Option[String] = None,
    videoUrl: Option[String] = None,
    trainingResult: Option[TrainingResult] = None,
    createdAt: Instant = Instant.now()
  ): Either[AppError, SessionResult] =
    for
      _ <- Either.cond(pathLength >= 0, (), BusinessRuleViolation("Greater than zero", "pathLength must be >= 0"))
      _ <- Either.cond(collisions >= 0, (), BusinessRuleViolation("Greater than zero", "collisions must be >= 0"))
      _ <- Either.cond(
        energyConsumption >= 0,
        (),
        BusinessRuleViolation("Greater than zero", "energyConsumption must be >= 0")
      )
    yield SessionResult(
      sessionId,
      success,
      completionTime,
      goalReached,
      pathLength,
      collisions,
      energyConsumption,
      customMetrics,
      trajectoryUrl,
      videoUrl,
      trainingResult,
      createdAt
    )

//  def fromEntity(entity: SessionResultEntity): SessionResult =
//    val training =
//      for
//        model <- entity.modelPath
//        check <- entity.checkpointPath
//        reward <- entity.finalReward
//        metrics <- entity.finalMetrics
//      yield TrainingResult(
//        modelPath = model,
//        checkpointPath = check,
//        finalReward = reward,
//        finalMetrics = metrics
//      )
//    SessionResult(
//      sessionId = SessionId(entity.sessionId),
//      success = entity.success,
//      completionTime = entity.completionTime,
//      goalReached = entity.goalReached,
//      pathLength = entity.pathLength,
//      collisions = entity.collisions,
//      energyConsumption = entity.energyConsumption,
//      customMetrics = entity.customMetrics.toJson,
//      trajectoryUrl = entity.trajectoryUrl,
//      videoUrl = entity.videoUrl,
//      trainingResult = training,
//      createdAt = entity.createdAt
//    )
