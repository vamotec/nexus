package app.mosia.nexus
package domain.model.training

import domain.model.common.ValueObject

import zio.json.*
import zio.json.ast.Json

case class TrainingResult(
  modelPath: String,
  checkpointPath: String,
  finalReward: Double,
  finalMetrics: Json
) extends ValueObject derives JsonCodec
