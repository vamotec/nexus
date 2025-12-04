package app.mosia.nexus
package application.dto.model.scene

import domain.model.common.Position3D

import zio.json.*
import zio.*
import zio.json.ast.Json

/** 场景配置 DTO - 简化版本
  *
  * 用于 API 层的场景配置，采用混合策略：
  *   - 直接复用简单值对象 (Position3D)
  *   - 扁平化枚举类型为字符串
  *   - 简化复杂嵌套为 DTO 列表
  *   - 高级配置使用 JSON
  */
case class SceneConfigDto(
  // 场景基本信息
  name: String,

  // 机器人配置 (扁平化)
  robotType: String, // "franka_panda", "ur5", "kuka", "custom:xxx"
  robotUrdf: Option[String] = None, // 自定义 URDF 文件路径 (可选)

  // 环境配置 (扁平化)
  environment: Json, // "warehouse", "factory", "laboratory", "outdoor"

  // 位置配置 (复用值对象)
  startPosition: Position3D,
  goalPosition: Option[Position3D] = None,

  // 障碍物列表 (简化 DTO)
  obstacles: List[ObstacleDto] = List.empty,

  // 传感器列表 (简化 DTO，可选)
  sensors: List[SensorDto] = List.empty,

  // 高级配置 (JSON，用于环境光照、物理等复杂配置)
  advancedConfig: Option[Json] = None
  // 例如: {"lighting": {"intensity": 1.0}, "physics": {"gravity": -9.81}}
) derives JsonCodec
