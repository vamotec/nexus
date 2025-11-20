package app.mosia.nexus
package domain.model.scene

import domain.error.*
import domain.model.common.{Position3D, Quaternion, ValueObject}
import org.postgresql.util.PGobject

import java.util.UUID
import io.getquill.MappedEncoding
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 传感器配置 */
case class Sensor(
  id: UUID,
  sensorType: SensorType,
  position: Position3D,
  orientation: Quaternion,
  config: SensorConfig
) derives JsonCodec
