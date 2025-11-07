package app.mosia.nexus.domain.model.training

import app.mosia.nexus.domain.model.common.ValueObject

case class TrainingProgress(
  currentEpoch: Int,
  totalEpochs: Int,
  currentReward: Double,
  averageReward: Double,
  loss: Double,
  episodeLength: Int,
  metrics: Map[String, Double]
) extends ValueObject:
  def percentage: Double = (currentEpoch.toDouble / totalEpochs * 100).min(100.0)
