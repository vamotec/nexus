package app.mosia.nexus
package application.dto.request.training

import zio.json.*


case class CreateTrainingRequest(
  sessionId: String,
  algorithm: String,
  epochs: Int,
  batchSize: Int,
  learningRate: Double,
  rewardFunction: String
) derives JsonCodec
