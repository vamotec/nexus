package app.mosia.nexus.domain.model.scene

import app.mosia.nexus.domain.model.common.*

case class RobotConfig(
  robotType: RobotType,
  urdfPath: String,
  startPosition: Position3D,
  startOrientation: Quaternion,
  controllable: Boolean = true
) extends ValueObject
