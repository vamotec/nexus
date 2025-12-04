package app.mosia.nexus
package domain.model.scene

import domain.model.common.ValueObject

import zio.json.*
import zio.*

case class SensorConfig(
  resolution: Option[(Int, Int)],
  frequency: Option[Double],
  range: Option[Double],
  fov: Option[Double] // Field of view
) derives JsonCodec
