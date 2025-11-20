package app.mosia.nexus
package domain.model.scene

import domain.model.common.*

case class RobotConfig(
  robotType: RobotType,
  urdfPath: String,
  startPosition: Position3D,
  startOrientation: Quaternion,
  controllable: Boolean = true
) extends ValueObject
