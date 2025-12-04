package app.mosia.nexus
package application.dto.response.scene

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
)
