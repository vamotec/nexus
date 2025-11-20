package app.mosia.nexus
package domain.model.training

import domain.model.common.ValueObject
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class TrainingResult(
  modelPath: String,
  checkpointPath: String,
  finalReward: Double,
  finalMetrics: Json
) extends ValueObject derives JsonCodec
