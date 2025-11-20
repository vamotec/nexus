package app.mosia.nexus
package domain.model.session

import domain.model.scene.SceneConfig
import domain.model.simulation.{SimulationId, SimulationParams}
import org.postgresql.util.PGobject

import java.time.Instant
import java.util.UUID
import io.getquill.MappedEncoding
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class SimulationConfigSnapshot(
  simulationId: UUID,
  simulationName: String,
  version: String,
  sceneConfig: SceneConfig,
  simulationParams: SimulationParams,
  snapshotAt: Instant
) derives JsonCodec

object SimulationConfigSnapshot:
  extension (cfg: SimulationConfigSnapshot)
    def toJsonAst: Json =
      Json.decoder.decodeJson(cfg.toJson).getOrElse(Json.Obj())

  def fromJsonAst(json: Json): SimulationConfigSnapshot =
    json.toJson.fromJson[SimulationConfigSnapshot].getOrElse(throw new RuntimeException("Invalid config_snapshot JSON"))
