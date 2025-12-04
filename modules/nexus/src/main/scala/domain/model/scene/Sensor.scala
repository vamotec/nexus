package app.mosia.nexus
package domain.model.scene

import domain.error.*
import domain.model.common.{Position3D, Quaternion, ValueObject}

import java.util.UUID
import zio.json.*

/** 传感器配置 */
case class Sensor(
  id: UUID,
  sensorType: SensorType,
  position: Position3D,
  orientation: Quaternion,
  config: SensorConfig
) derives JsonCodec
