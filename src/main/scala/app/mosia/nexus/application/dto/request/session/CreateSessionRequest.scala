package app.mosia.nexus.application.dto.request.session

import app.mosia.nexus.application.dto.model.scene.ObstacleDto
import app.mosia.nexus.domain.model.common.Position3D
import sttp.tapir.Schema
import zio.json.JsonCodec

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
  renderQuality: String = "medium"
) derives JsonCodec,
      Schema
