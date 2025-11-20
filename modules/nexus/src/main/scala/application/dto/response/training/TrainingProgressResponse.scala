package app.mosia.nexus
package application.dto.response.training

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class TrainingProgressResponse(
  currentEpoch: Int,
  totalEpochs: Int,
  percentage: Double,
  currentReward: Option[Double],
  averageReward: Option[Double],
  metrics: Map[String, Double]
) derives JsonCodec,
      Cs.SemiAuto,
      ArgBuilder
