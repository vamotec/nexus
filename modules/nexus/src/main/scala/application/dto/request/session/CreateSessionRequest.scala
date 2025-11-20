package app.mosia.nexus
package application.dto.request.session

import application.dto.model.scene.ObstacleDto
import domain.model.common.Position3D

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 创建会话请求 DTO
  *
  * 用于快速创建仿真会话（基于已有的 Simulation 模板） 直接复用 Domain 的 Position3D 值对象
  */
case class CreateSessionRequest(
  projectId: String,
  sceneName: String,
  robotType: String,
  robotPosition: Position3D, // 复用 Domain VO
  obstacles: List[ObstacleDto],
  environment: String,
  realTime: Boolean = true,
  renderQuality: String = "medium",
  mode: String = "manual" // 会话模式: manual, training, hybrid
) derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder
