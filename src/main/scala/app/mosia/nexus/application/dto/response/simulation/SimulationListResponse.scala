package app.mosia.nexus.application.dto.response.simulation

import caliban.schema.Schema as Cs
import sttp.tapir.Schema
import zio.json.JsonCodec

case class SimulationListResponse(
  simulationId: String,
  name: String,
  description: Option[String],
  sceneName: String, // 只显示场景名称
  robotType: String, // 只显示机器人类型
  createdAt: String
) derives JsonCodec,
      Schema,
      Cs.SemiAuto
