package app.mosia.nexus
package application.dto.response.training

import zio.json.JsonCodec

case class TrainingResultResponse(
  modelPath: String,
  finalReward: Double,
  successRate: Double,
  trainingTimeSeconds: Long
) derives JsonCodec
