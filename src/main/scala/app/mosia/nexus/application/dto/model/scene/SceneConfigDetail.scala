package app.mosia.nexus.application.dto.model.scene

import app.mosia.nexus.domain.model.common.Position3D
import sttp.tapir.Schema
import zio.json.JsonCodec

/** 场景配置详情 DTO
  *
  * 用于响应，展示场景配置的详细信息 直接复用 Domain 的 Position3D 值对象
  */
case class SceneConfigDetail(
  name: String,
  robotType: String,
  robotModelUrl: String, // 转换为前端可用的 URL
  environment: String,
  obstacleCount: Int,
  startPosition: Position3D, // 复用 Domain VO
  goalPosition: Option[Position3D], // 复用 Domain VO
  sensors: List[SensorSummary]
) derives JsonCodec,
      Schema
