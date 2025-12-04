package app.mosia.nexus
package domain.model.scene

import domain.error.*
import domain.model.common.*

import java.util.UUID
import zio.json.*
import zio.*

/** 障碍物 */
case class Obstacle(
  id: UUID,
  obstacleType: ObstacleType,
  position: Position3D,
  rotation: Quaternion,
  dimensions: Dimensions3D,
  material: Option[Material] = None,
  dynamic: Boolean = false // 是否是动态障碍物
) derives JsonCodec

object Obstacle:
  def fromString(str: String): Either[String, Obstacle] =
    str.fromJson[Obstacle]
