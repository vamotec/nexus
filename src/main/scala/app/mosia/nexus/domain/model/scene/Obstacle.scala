package app.mosia.nexus.domain.model.scene

import app.mosia.nexus.domain.model.common.*

/** 障碍物 */
case class Obstacle(
  id: ObstacleId,
  obstacleType: ObstacleType,
  position: Position3D,
  rotation: Quaternion,
  dimensions: Dimensions3D,
  material: Option[Material] = None,
  dynamic: Boolean = false, // 是否是动态障碍物
) extends ValueObject
