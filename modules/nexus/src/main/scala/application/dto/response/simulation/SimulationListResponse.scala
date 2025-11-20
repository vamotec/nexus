package app.mosia.nexus
package application.dto.response.simulation

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class SimulationListResponse(
  simulationId: String,
  name: String,
  description: Option[String],
  sceneName: String, // 只显示场景名称
  robotType: String, // 只显示机器人类型
  createdAt: String
) derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder
