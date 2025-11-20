package app.mosia.nexus
package application.dto.model.scene

import domain.model.common.*

import caliban.schema.{Schema as Cs, ArgBuilder}
import sttp.tapir.Schema
import zio.json.*
import zio.*

/** 障碍物 DTO - 简化版本
  *
  * 用于 API 层的障碍物配置，采用混合策略：
  *   - 复用 Domain 的简单值对象 (Position3D, Quaternion, Dimensions3D)
  *   - 扁平化枚举类型为字符串
  */
case class ObstacleDto(
  // 障碍物类型 (扁平化枚举)
  obstacleType: String, // "box", "sphere", "cylinder", "mesh"

  // 位置和姿态 (复用 Domain 值对象)
  position: Position3D,
  rotation: Option[Quaternion] = Some(Quaternion(1.0, 0.0, 0.0, 0.0)),

  // 尺寸 (复用 Domain 值对象)
  dimensions: Dimensions3D,

  // 可选属性
  material: Option[String] = None, // "wood", "metal", "plastic", "rubber"
  customMaterial: Option[MaterialDto] = None,
  dynamic: Boolean = false // 是否动态障碍物
) derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder
