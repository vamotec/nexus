package app.mosia.nexus.domain.model.scene

import app.mosia.nexus.domain.model.common.{Position3D, Quaternion, ValueObject}

/** 传感器配置 */
case class Sensor(
  id: SensorId,
  sensorType: SensorType,
  position: Position3D,
  orientation: Quaternion,
  config: SensorConfig
) extends ValueObject
