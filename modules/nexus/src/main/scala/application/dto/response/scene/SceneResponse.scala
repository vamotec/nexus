package app.mosia.nexus
package application.dto.response.scene

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 场景响应 DTO
  *
  * 用于返回场景配置的简化信息
  */
case class SceneResponse(
  name: String,
  robotType: String,
  environment: String,
  obstacleCount: Int,
  sensorCount: Int
) derives Cs.SemiAuto,
      ArgBuilder
