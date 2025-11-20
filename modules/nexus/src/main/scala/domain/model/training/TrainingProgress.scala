package app.mosia.nexus
package domain.model.training

import domain.model.common.ValueObject

case class TrainingProgress(
  currentEpoch: Int,
  totalEpochs: Int,
  currentReward: Option[Double],
  averageReward: Option[Double],
  loss: Option[Double],
  episodeLength: Int,
  metrics: Map[String, Double]
) extends ValueObject:
  def percentage: Double = (currentEpoch.toDouble / totalEpochs * 100).min(100.0)
