package app.mosia.nexus
package domain.model.training

import domain.error.*

import zio.json.*
import zio.*
import zio.json.ast.Json

/** 训练配置 */
case class TrainingConfig(
  episodes: Int = 1000, // 训练回合数
  stepsPerEpisode: Int = 1000, // 每回合步数
  batchSize: Int = 32,
  learningRate: Double = 0.001,
  discountFactor: Double = 0.99
  //                             exploration: ExplorationConfig,
) derives JsonCodec:
  def validate: Either[String, TrainingConfig] =
    if (episodes <= 0) Left("episodes must be positive")
    else if (batchSize <= 0) Left("Batch size must be positive")
    else if (learningRate <= 0) Left("Learning rate must be positive")
    else Right(this)

object TrainingConfig:
  extension (para: TrainingConfig)
    def toJsonAst: Json =
      Json.decoder.decodeJson(para.toJson).getOrElse(Json.Obj())

  def fromJsonAst(json: Json): TrainingConfig =
    json.toJson.fromJson[TrainingConfig].getOrElse(throw new RuntimeException("Invalid training config JSON"))
