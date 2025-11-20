package app.mosia.nexus
package application.dto.response.training

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class TrainingResultResponse(
  modelPath: String,
  finalReward: Double,
  successRate: Double,
  trainingTimeSeconds: Long
) derives JsonCodec,
      Cs.SemiAuto,
      ArgBuilder
