package app.mosia.nexus.domain.model.training

import zio.json.JsonCodec

import app.mosia.nexus.domain.model.common.ValueObject

case class TrainingResult(
  modelPath: String,
  checkpointPath: String,
  finalReward: Double,
  successRate: Double,
  trainingTimeSeconds: Long,
  finalMetrics: Map[String, Double]
) extends ValueObject derives JsonCodec
