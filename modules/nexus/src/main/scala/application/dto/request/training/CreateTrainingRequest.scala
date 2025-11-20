package app.mosia.nexus
package application.dto.request.training

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class CreateTrainingRequest(
  sessionId: String,
  algorithm: String,
  epochs: Int,
  batchSize: Int,
  learningRate: Double,
  rewardFunction: String
) derives JsonCodec,
      Cs.SemiAuto,
      ArgBuilder
