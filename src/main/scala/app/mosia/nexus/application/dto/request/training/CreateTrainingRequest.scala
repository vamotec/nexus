package app.mosia.nexus.application.dto.request.training

case class CreateTrainingRequest(
  sessionId: String,
  algorithm: String,
  epochs: Int,
  batchSize: Int,
  learningRate: Double,
  rewardFunction: String
)
