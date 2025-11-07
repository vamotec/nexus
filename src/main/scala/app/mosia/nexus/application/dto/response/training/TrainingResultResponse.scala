package app.mosia.nexus.application.dto.response.training

case class TrainingResultResponse(
  modelPath: String,
  finalReward: Double,
  successRate: Double,
  trainingTimeSeconds: Long
)
