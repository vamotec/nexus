package app.mosia.nexus
package application.dto.response.training

import zio.json.*

case class TrainingProgressResponse(
  currentEpoch: Int,
  totalEpochs: Int,
  percentage: Double,
  currentReward: Option[Double],
  averageReward: Option[Double],
  metrics: Map[String, Double]
) derives JsonCodec
