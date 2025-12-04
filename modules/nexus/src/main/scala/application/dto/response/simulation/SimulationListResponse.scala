package app.mosia.nexus
package application.dto.response.simulation

import sttp.tapir.Schema
import zio.json.*
import zio.*

case class SimulationListResponse(
  simulationId: String,
  name: String,
  description: Option[String],
  sceneName: String, // 只显示场景名称
  robotType: String, // 只显示机器人类型
  createdAt: String
) derives JsonCodec,
      Schema
