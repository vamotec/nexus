package app.mosia.nexus.domain.model.session

import app.mosia.nexus.domain.model.training.TrainingResult

import java.time.Instant

/** 会话结果 (完成后写入，不可变) */
case class SessionResult(
  sessionId: SessionId,
  success: Boolean,
  completionTime: Double, // 仿真完成时间 (秒)

  // 任务指标
  goalReached: Boolean,
  pathLength: Double,
  collisions: Int,
  energyConsumption: Double,

  // 数据存储 (大数据放对象存储)
  trajectoryUrl: Option[String],
  videoUrl: Option[String],

  // 训练结果 (如果有)
  trainingResult: Option[TrainingResult],

  // 错误信息
  errorMessage: Option[String],
  createdAt: Instant
)

object SessionResult {
  def failed(sessionId: SessionId, reason: String): SessionResult = SessionResult(
    sessionId = sessionId,
    success = false,
    completionTime = 0.0,
    goalReached = false,
    pathLength = 0.0,
    collisions = 0,
    energyConsumption = 0.0,
    trajectoryUrl = None,
    videoUrl = None,
    trainingResult = None,
    errorMessage = Some(reason),
    createdAt = Instant.now()
  )
}
