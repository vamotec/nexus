package app.mosia.nexus.domain.model.scene

import app.mosia.nexus.domain.model.common.ValueObject

case class SensorConfig(
  resolution: Option[(Int, Int)],
  frequency: Option[Double],
  range: Option[Double],
  fov: Option[Double], // Field of view
) extends ValueObject
