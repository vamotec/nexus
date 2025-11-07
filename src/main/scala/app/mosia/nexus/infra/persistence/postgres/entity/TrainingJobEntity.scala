package app.mosia.nexus.infra.persistence.postgres.entity

case class TrainingJobEntity(
  id: String,
  sessionId: String,
  userId: String,
  algorithm: String,
  config: String, // JSON
  status: String,
  // 进度
  currentEpoch: Int,
  totalEpochs: Int,
  currentReward: Double,
  // 结果
  modelPath: Option[String],
  finalReward: Option[Double],
  // 时间戳
  createdAt: Long,
  updatedAt: Long,
  startedAt: Option[Long],
  completedAt: Option[Long]
)
