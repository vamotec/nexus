package app.mosia.nexus
package domain.model.scene

import domain.error.*
import domain.model.common.*
import org.postgresql.util.PGobject

import java.util.UUID
import io.getquill.MappedEncoding
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

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
