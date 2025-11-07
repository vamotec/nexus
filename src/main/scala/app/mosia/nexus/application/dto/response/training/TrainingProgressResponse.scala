package app.mosia.nexus.application.dto.response.training

case class TrainingProgressResponse(
  currentEpoch: Int,
  totalEpochs: Int,
  percentage: Double,
  currentReward: Double,
  averageReward: Double,
  metrics: Map[String, Double]
)
